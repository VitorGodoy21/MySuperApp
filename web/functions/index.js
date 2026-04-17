const { onRequest } = require('firebase-functions/v2/https');
const { defineSecret } = require('firebase-functions/params');
const logger = require('firebase-functions/logger');
const admin = require('firebase-admin');
const { createHash } = require('crypto');

admin.initializeApp();


function getClientIp(req) {
  const forwardedFor = req.headers['x-forwarded-for'];
  if (forwardedFor && typeof forwardedFor === 'string') {
    const firstIp = forwardedFor.split(',')[0].trim();
    if (firstIp) return firstIp;
  }

  if (req.ip) return req.ip;
  if (req.socket && req.socket.remoteAddress) return req.socket.remoteAddress;

  return null;
}

function normalizePayload(req) {
  const body = req.body;

  if (body && typeof body === 'object') {
    return body;
  }

  if (typeof body === 'string') {
    try {
      return JSON.parse(body);
    } catch (error) {
      logger.warn('Invalid JSON string payload', error);
    }
  }

  // Fallback for text/plain (sendBeacon) — Express não parseia este content-type
  if (req.rawBody) {
    try {
      return JSON.parse(req.rawBody.toString());
    } catch (error) {
      logger.warn('Invalid raw body payload', error);
    }
  }

  return {};
}

async function resolveGeoByIp(ipAddress, key) {
  if (!ipAddress) {
    return {
      city: null,
      region: null,
      country: null,
      latitude: null,
      longitude: null,
      method: 'IPAPI_UNAVAILABLE'
    };
  }

  const fields = 'status,message,country,city,regionName,lat,lon';
  const url = key
    ? `https://pro.ip-api.com/json/${encodeURIComponent(ipAddress)}?key=${key}&fields=${fields}`
    : `http://ip-api.com/json/${encodeURIComponent(ipAddress)}?fields=${fields}`;

  try {
    const response = await fetch(url, { signal: AbortSignal.timeout(5000) });

    if (!response.ok) {
      throw new Error(`ip-api.com returned ${response.status}`);
    }

    const data = await response.json();

    if (data.status !== 'success') {
      throw new Error(`ip-api.com: ${data.message || data.status}`);
    }

    return {
      city: data.city || null,
      region: data.regionName || null,
      country: data.country || null,
      latitude: typeof data.lat === 'number' ? data.lat : null,
      longitude: typeof data.lon === 'number' ? data.lon : null,
      method: key ? 'IPAPI_PRO' : 'IPAPI_FREE'
    };
  } catch (error) {
    logger.error('ip-api.com lookup failed', error);
    return {
      city: null,
      region: null,
      country: null,
      latitude: null,
      longitude: null,
      method: 'IPAPI_ERROR'
    };
  }
}

exports.logQrScan = onRequest(
  {
    region: 'us-central1',
    timeoutSeconds: 10,
    invoker: 'public'
  },
  async (req, res) => {
    res.set('Access-Control-Allow-Origin', '*');
    res.set('Access-Control-Allow-Methods', 'POST, OPTIONS');
    res.set('Access-Control-Allow-Headers', 'Content-Type');

    if (req.method === 'OPTIONS') {
      res.status(204).send('');
      return;
    }

    if (req.method !== 'POST') {
      res.status(405).json({ error: 'Method not allowed' });
      return;
    }

    const payload = normalizePayload(req);
    const qrCodeId = payload.qrCodeId;

    if (!qrCodeId || typeof qrCodeId !== 'string') {
      res.status(400).json({ error: 'qrCodeId is required' });
      return;
    }

    const ipAddress = getClientIp(req);
    const geo = await resolveGeoByIp(ipAddress, null);

    const scanId = typeof payload.scanId === 'string' ? payload.scanId : null;
    const sessionId = typeof payload.sessionId === 'string' ? payload.sessionId : null;

    const accessLog = {
      scanId,
      sessionId,
      qrCodeId,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      loggedAt: payload.loggedAt || null,
      pageUrl: payload.pageUrl || null,
      pagePath: payload.pagePath || null,
      referrer: payload.referrer || null,
      userAgent: payload.userAgent || req.headers['user-agent'] || null,
      language: payload.language || null,
      platform: payload.platform || null,
      screenWidth: payload.screenWidth || null,
      screenHeight: payload.screenHeight || null,
      timeZone: payload.timeZone || null,
      utmSource: payload.utmSource || null,
      utmMedium: payload.utmMedium || null,
      utmCampaign: payload.utmCampaign || null,
      city: geo.city,
      region: geo.region,
      country: geo.country,
      latitude: geo.latitude,
      longitude: geo.longitude,
      method: geo.method,
      status: 'completed'
    };

    try {
      const logsRef = admin
        .firestore()
        .collection('qrcodes')
        .doc(qrCodeId)
        .collection('access_logs');

      if (scanId) {
        await logsRef.doc(scanId).set(
          {
            ...accessLog,
            ipAddress: admin.firestore.FieldValue.delete()
          },
          { merge: true }
        );
      } else {
        await logsRef.add(accessLog);
      }

      res.status(200).json({ ok: true });
    } catch (error) {
      logger.error('Failed to save qr access log', error);
      res.status(500).json({ error: 'Failed to persist log' });
    }
  }
);

// ─── addMuralComment ──────────────────────────────────────────────────────────

const MURAL_RL_MAX = 5;
const MURAL_RL_WINDOW_MS = 10 * 60 * 1000; // 10 minutes
const ALLOWED_ORIGIN = 'https://baila.space';

function sanitizeField(value, maxLen) {
  if (typeof value !== 'string') return '';
  // Remove C0/C1 control characters except tab (\t), newline (\n) and carriage return (\r)
  return value.replace(/[\x00-\x08\x0B\x0C\x0E-\x1F\x7F-\x9F]/g, '').slice(0, maxLen);
}

exports.addMuralComment = onRequest(
  {
    region: 'us-central1',
    timeoutSeconds: 15,
    invoker: 'public'
  },
  async (req, res) => {
    const origin = req.headers['origin'];
    if (origin === ALLOWED_ORIGIN) {
      res.set('Access-Control-Allow-Origin', ALLOWED_ORIGIN);
    }
    res.set('Vary', 'Origin');
    res.set('Access-Control-Allow-Methods', 'POST, OPTIONS');
    res.set('Access-Control-Allow-Headers', 'Content-Type');

    if (req.method === 'OPTIONS') {
      res.status(204).send('');
      return;
    }

    if (req.method !== 'POST') {
      res.status(405).json({ error: 'Method not allowed' });
      return;
    }

    // ── Parse body ──────────────────────────────────────────────────────────
    let body = req.body;
    if (typeof body === 'string') {
      try { body = JSON.parse(body); } catch { body = {}; }
    } else if (!body || typeof body !== 'object') {
      body = {};
    }

    // ── Validate inputs ─────────────────────────────────────────────────────
    const rawQrCodeId = body.qrCodeId;
    const rawText = body.text;
    const rawAuthor = body.author;

    if (!rawQrCodeId || typeof rawQrCodeId !== 'string' || rawQrCodeId.length > 128) {
      res.status(400).json({ error: 'qrCodeId inválido.' });
      return;
    }

    const text = sanitizeField(typeof rawText === 'string' ? rawText.trim() : '', 500);
    if (!text) {
      res.status(400).json({ error: 'O comentário não pode estar vazio.' });
      return;
    }

    const author = sanitizeField(typeof rawAuthor === 'string' ? rawAuthor.trim() : '', 60);

    // ── Verify QR code exists and is type MURAL ──────────────────────────────
    const db = admin.firestore();
    let qrDoc;
    try {
      qrDoc = await db.collection('qrcodes').doc(rawQrCodeId).get();
    } catch (error) {
      logger.error('Failed to fetch qrcode doc', error);
      res.status(500).json({ error: 'Erro interno.' });
      return;
    }

    if (!qrDoc.exists || qrDoc.data().type !== 'MURAL') {
      res.status(404).json({ error: 'Mural não encontrado.' });
      return;
    }

    // ── Rate limiting by IP ──────────────────────────────────────────────────
    const ipAddress = getClientIp(req);
    if (ipAddress) {
      const ipHash = createHash('sha256').update(`${ipAddress}:mural`).digest('hex');
      const rlRef = db.collection('_rate_limits').doc(ipHash);
      try {
        const now = Date.now();
        const allowed = await db.runTransaction(async (tx) => {
          const rlDoc = await tx.get(rlRef);
          if (!rlDoc.exists) {
            tx.set(rlRef, { count: 1, windowStart: now });
            return true;
          }
          const { count, windowStart } = rlDoc.data();
          if (now - windowStart >= MURAL_RL_WINDOW_MS) {
            tx.set(rlRef, { count: 1, windowStart: now });
            return true;
          }
          if (count >= MURAL_RL_MAX) {
            return false;
          }
          tx.update(rlRef, { count: count + 1 });
          return true;
        });

        if (!allowed) {
          res.status(429).json({ error: 'Muitos comentários. Aguarde alguns minutos.' });
          return;
        }
      } catch (error) {
        // Fail-open: do not block the user if rate limit check itself fails
        logger.error('Rate limit check failed, allowing request', error);
      }
    }

    // ── Write comment via Admin SDK (bypasses Firestore Security Rules) ──────
    // Highlighted flag: set server-side only via App Check — never trusted from req.body
    let highlighted = false;
    const appCheckToken = req.headers['x-firebase-appcheck'];
    if (appCheckToken && typeof appCheckToken === 'string') {
      try {
        await admin.appCheck().verifyToken(appCheckToken);
        highlighted = true;
      } catch (error) {
        // Invalid or expired token — treat as regular (non-highlighted) comment
        logger.warn('App Check token invalid, comment will not be highlighted', { error: error.message });
      }
    }

    try {
      await db.collection('qrcodes').doc(rawQrCodeId).collection('comments').add({
        text,
        author,
        highlighted,
        timestamp: admin.firestore.FieldValue.serverTimestamp()
      });
      res.status(201).json({ ok: true });
    } catch (error) {
      logger.error('Failed to save mural comment', error);
      res.status(500).json({ error: 'Erro ao salvar comentário.' });
    }
  }
);
