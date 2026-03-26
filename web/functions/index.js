const { onRequest } = require('firebase-functions/v2/https');
const { defineSecret } = require('firebase-functions/params');
const logger = require('firebase-functions/logger');
const admin = require('firebase-admin');

admin.initializeApp();

const ipinfoToken = defineSecret('IPINFO_TOKEN');

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

async function resolveGeoByIp(ipAddress, token) {
  if (!ipAddress || !token) {
    return {
      city: null,
      country: null,
      latitude: null,
      longitude: null,
      method: 'IPINFO_UNAVAILABLE'
    };
  }

  try {
    const response = await fetch(
      `https://ipinfo.io/${encodeURIComponent(ipAddress)}/json?token=${token}`,
      { signal: AbortSignal.timeout(5000) }
    );

    if (!response.ok) {
      throw new Error(`ipinfo.io returned ${response.status}`);
    }

    const data = await response.json();

    let latitude = null;
    let longitude = null;
    if (data.loc && typeof data.loc === 'string') {
      const [lat, lng] = data.loc.split(',').map(Number);
      if (!isNaN(lat) && !isNaN(lng)) {
        latitude = lat;
        longitude = lng;
      }
    }

    return {
      city: data.city || null,
      country: data.country || null,
      latitude,
      longitude,
      method: 'IPINFO'
    };
  } catch (error) {
    logger.error('ipinfo.io lookup failed', error);
    return {
      city: null,
      country: null,
      latitude: null,
      longitude: null,
      method: 'IPINFO_ERROR'
    };
  }
}

exports.logQrScan = onRequest(
  {
    region: 'us-central1',
    timeoutSeconds: 10,
    secrets: [ipinfoToken],
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
    const geo = await resolveGeoByIp(ipAddress, ipinfoToken.value());

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
