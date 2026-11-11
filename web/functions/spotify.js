// ─── Spotify integration (Smart Tag type SPOTIFY) ─────────────────────────────
//
// Fluxo:
//   app → spotifyAuthStart (callable) → navegador → accounts.spotify.com →
//   /api/spotify-callback (spotifyAuthCallback) grava spotify_tokens/{uid} e
//   users/{uid}.spotify → app observa users/{uid}.spotify.
//
//   Tag NFC / QR → web/public/qr/index.html → POST /api/play-spotify (playSpotify)
//   → token do dono do QR → PUT /me/player/play?device_id=<Alexa / grupo>.
//
// Tokens do Spotify nunca saem do backend. Secrets:
//   firebase functions:secrets:set SPOTIFY_CLIENT_ID
//   firebase functions:secrets:set SPOTIFY_CLIENT_SECRET

const { onRequest, onCall, HttpsError } = require('firebase-functions/v2/https');
const { defineSecret } = require('firebase-functions/params');
const logger = require('firebase-functions/logger');
const admin = require('firebase-admin');
const { createHash, randomBytes } = require('crypto');

const SPOTIFY_CLIENT_ID = defineSecret('SPOTIFY_CLIENT_ID');
const SPOTIFY_CLIENT_SECRET = defineSecret('SPOTIFY_CLIENT_SECRET');

const REGION = 'us-central1';
const SPOTIFY_ACCOUNTS = 'https://accounts.spotify.com';
const SPOTIFY_API = 'https://api.spotify.com/v1';
const SPOTIFY_REDIRECT_URI = 'https://baila.space/api/spotify-callback';
const SPOTIFY_SCOPES = [
  'user-read-playback-state',
  'user-modify-playback-state',
  'playlist-read-private',
  'playlist-read-collaborative',
  'user-read-private'
].join(' ');

const TOKENS_COLLECTION = 'spotify_tokens';
const AUTH_STATES_COLLECTION = 'spotify_auth_states';
const AUTH_STATE_TTL_MS = 10 * 60 * 1000;
const TOKEN_EXPIRY_MARGIN_MS = 60 * 1000;

// Rate limit do disparo público (/api/play-spotify)
const PLAY_RL_WINDOW_MS = 60 * 1000;
const PLAY_RL_MAX_PER_IP_QR = 10;
const PLAY_RL_MAX_PER_QR = 30;

const PLAY_ALLOWED_ORIGINS = new Set([
  'https://baila.space',
  'https://mysuperapp.com.br',
  'https://baila-675c6.web.app',
  'https://baila-675c6.firebaseapp.com'
]);

// Códigos de erro simples, compartilhados com o app e com a página do QR.
const ERR = {
  NOT_CONNECTED: 'NOT_CONNECTED',
  NEEDS_RECONNECT: 'NEEDS_RECONNECT',
  DEVICE_NOT_FOUND: 'DEVICE_NOT_FOUND',
  PREMIUM_REQUIRED: 'PREMIUM_REQUIRED',
  RATE_LIMITED: 'RATE_LIMITED',
  INVALID_TARGET: 'INVALID_TARGET',
  SPOTIFY_ERROR: 'SPOTIFY_ERROR'
};

class SpotifyError extends Error {
  constructor(code, message, status) {
    super(message || code);
    this.code = code;
    this.status = status || 500;
  }
}

function db() {
  return admin.firestore();
}

function basicAuthHeader() {
  const raw = `${SPOTIFY_CLIENT_ID.value()}:${SPOTIFY_CLIENT_SECRET.value()}`;
  return `Basic ${Buffer.from(raw).toString('base64')}`;
}

// ─── Tokens ──────────────────────────────────────────────────────────────────

async function requestToken(params) {
  const response = await fetch(`${SPOTIFY_ACCOUNTS}/api/token`, {
    method: 'POST',
    headers: {
      Authorization: basicAuthHeader(),
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    body: new URLSearchParams(params).toString(),
    signal: AbortSignal.timeout(8000)
  });

  let data = {};
  try { data = await response.json(); } catch { /* corpo vazio */ }

  if (!response.ok) {
    const error = new SpotifyError(
      data.error === 'invalid_grant' ? ERR.NEEDS_RECONNECT : ERR.SPOTIFY_ERROR,
      data.error_description || data.error || `Spotify token endpoint ${response.status}`,
      response.status
    );
    error.spotifyError = data.error;
    throw error;
  }
  return data;
}

async function saveTokens(uid, tokenResponse, previous) {
  const expiresAt = Date.now() + (Number(tokenResponse.expires_in) || 3600) * 1000;
  const payload = {
    accessToken: tokenResponse.access_token,
    refreshToken: tokenResponse.refresh_token || (previous && previous.refreshToken) || null,
    scope: tokenResponse.scope || (previous && previous.scope) || null,
    expiresAt,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  };
  await db().collection(TOKENS_COLLECTION).doc(uid).set(payload, { merge: true });
  return payload;
}

async function markNeedsReconnect(uid) {
  try {
    await db().collection('users').doc(uid).set(
      { spotify: { connected: false, needsReconnect: true } },
      { merge: true }
    );
  } catch (error) {
    logger.warn('Failed to flag spotify needsReconnect', { uid, message: error.message });
  }
}

/** Retorna um access token válido do usuário, renovando quando necessário. */
async function getValidAccessToken(uid) {
  const snap = await db().collection(TOKENS_COLLECTION).doc(uid).get();
  if (!snap.exists) {
    throw new SpotifyError(ERR.NOT_CONNECTED, 'Conta do Spotify não conectada.', 412);
  }
  const tokens = snap.data();
  if (tokens.accessToken && tokens.expiresAt && tokens.expiresAt - TOKEN_EXPIRY_MARGIN_MS > Date.now()) {
    return tokens.accessToken;
  }
  if (!tokens.refreshToken) {
    await markNeedsReconnect(uid);
    throw new SpotifyError(ERR.NEEDS_RECONNECT, 'Reconecte sua conta do Spotify.', 412);
  }
  try {
    const refreshed = await requestToken({
      grant_type: 'refresh_token',
      refresh_token: tokens.refreshToken
    });
    const saved = await saveTokens(uid, refreshed, tokens);
    return saved.accessToken;
  } catch (error) {
    if (error.code === ERR.NEEDS_RECONNECT) await markNeedsReconnect(uid);
    throw error;
  }
}

// ─── Chamadas à Web API ──────────────────────────────────────────────────────

/**
 * Faz uma chamada autenticada à Web API. Retorna `{ status, data }`; lança
 * SpotifyError para 401/403/429 e demais erros, mapeando os motivos conhecidos.
 */
async function spotifyFetch(accessToken, path, options = {}) {
  const url = path.startsWith('http') ? path : `${SPOTIFY_API}${path}`;
  const response = await fetch(url, {
    method: options.method || 'GET',
    headers: {
      Authorization: `Bearer ${accessToken}`,
      ...(options.body ? { 'Content-Type': 'application/json' } : {})
    },
    body: options.body ? JSON.stringify(options.body) : undefined,
    signal: AbortSignal.timeout(options.timeoutMs || 8000)
  });

  if (response.status === 204) return { status: 204, data: null };

  let data = null;
  const text = await response.text();
  if (text) {
    try { data = JSON.parse(text); } catch { data = { raw: text }; }
  }

  if (response.ok) return { status: response.status, data };

  const reason = data && data.error && (data.error.reason || data.error.message);
  if (response.status === 401) {
    throw new SpotifyError(ERR.NEEDS_RECONNECT, 'Sessão do Spotify expirada.', 401);
  }
  if (response.status === 403 && reason === 'PREMIUM_REQUIRED') {
    throw new SpotifyError(ERR.PREMIUM_REQUIRED, 'O Spotify Premium é necessário para controlar a reprodução.', 403);
  }
  if (response.status === 429) {
    const error = new SpotifyError(ERR.RATE_LIMITED, 'Muitas requisições ao Spotify. Tente novamente em instantes.', 429);
    error.retryAfter = Number(response.headers.get('retry-after')) || null;
    throw error;
  }
  if (response.status === 404 && reason === 'NO_ACTIVE_DEVICE') {
    throw new SpotifyError(ERR.DEVICE_NOT_FOUND, 'Dispositivo não disponível no Spotify.', 404);
  }
  const error = new SpotifyError(ERR.SPOTIFY_ERROR, reason || `Spotify ${response.status}`, response.status);
  error.reason = reason;
  throw error;
}

function pickImage(images) {
  if (!Array.isArray(images) || images.length === 0) return '';
  // Spotify ordena da maior para a menor; a do meio é boa para listas
  const sorted = [...images].sort((a, b) => (b.width || 0) - (a.width || 0));
  return (sorted[Math.min(1, sorted.length - 1)] || sorted[0]).url || '';
}

function mapPlaylist(item) {
  if (!item || !item.uri) return null;
  return {
    uri: item.uri,
    kind: 'PLAYLIST',
    name: item.name || '',
    subtitle: item.owner && item.owner.display_name
      ? `${item.owner.display_name} • ${item.tracks && typeof item.tracks.total === 'number' ? `${item.tracks.total} músicas` : 'playlist'}`
      : 'Playlist',
    imageUrl: pickImage(item.images)
  };
}

function mapTrack(item) {
  if (!item || !item.uri) return null;
  const artists = Array.isArray(item.artists) ? item.artists.map((a) => a.name).filter(Boolean).join(', ') : '';
  const album = item.album && item.album.name ? item.album.name : '';
  return {
    uri: item.uri,
    kind: 'TRACK',
    name: item.name || '',
    subtitle: [artists, album].filter(Boolean).join(' • '),
    imageUrl: pickImage(item.album && item.album.images)
  };
}

function mapDevice(device) {
  return {
    id: device.id || '',
    name: device.name || '',
    type: device.type || '',
    isActive: Boolean(device.is_active),
    isRestricted: Boolean(device.is_restricted)
  };
}

async function listDevices(accessToken) {
  const { data } = await spotifyFetch(accessToken, '/me/player/devices');
  return (data && Array.isArray(data.devices) ? data.devices : []).map(mapDevice);
}

/**
 * Resolve o dispositivo alvo: primeiro pelo id salvo, depois pelo nome
 * (ids de dispositivos Alexa/grupos podem mudar entre sessões).
 */
function resolveDevice(devices, deviceId, deviceName) {
  if (deviceId) {
    const byId = devices.find((d) => d.id === deviceId);
    if (byId) return byId;
  }
  if (deviceName) {
    const target = deviceName.trim().toLowerCase();
    const byName = devices.find((d) => d.name.trim().toLowerCase() === target);
    if (byName) return byName;
  }
  return null;
}

function isValidSpotifyUri(uri) {
  return typeof uri === 'string' && /^spotify:(playlist|track|album|artist):[A-Za-z0-9]+$/.test(uri);
}

// ─── Callables (app autenticado) ─────────────────────────────────────────────

function requireAuth(request) {
  const uid = request.auth && request.auth.uid;
  if (!uid) throw new HttpsError('unauthenticated', 'Entre na sua conta.');
  return uid;
}

function toHttpsError(error) {
  if (error instanceof HttpsError) return error;
  if (error instanceof SpotifyError) {
    const codeMap = {
      [ERR.NOT_CONNECTED]: 'failed-precondition',
      [ERR.NEEDS_RECONNECT]: 'failed-precondition',
      [ERR.PREMIUM_REQUIRED]: 'permission-denied',
      [ERR.RATE_LIMITED]: 'resource-exhausted',
      [ERR.DEVICE_NOT_FOUND]: 'not-found',
      [ERR.INVALID_TARGET]: 'invalid-argument'
    };
    return new HttpsError(codeMap[error.code] || 'internal', error.message, { code: error.code });
  }
  logger.error('Unexpected spotify error', error);
  return new HttpsError('internal', 'Erro ao falar com o Spotify.', { code: ERR.SPOTIFY_ERROR });
}

const callableOptions = { region: REGION, secrets: [SPOTIFY_CLIENT_ID, SPOTIFY_CLIENT_SECRET], timeoutSeconds: 20 };

exports.spotifyAuthStart = onCall(callableOptions, async (request) => {
  const uid = requireAuth(request);
  const state = randomBytes(24).toString('base64url');
  const now = Date.now();
  await db().collection(AUTH_STATES_COLLECTION).doc(state).set({
    uid,
    redirectUri: SPOTIFY_REDIRECT_URI,
    createdAt: now,
    expiresAt: now + AUTH_STATE_TTL_MS
  });

  const params = new URLSearchParams({
    response_type: 'code',
    client_id: SPOTIFY_CLIENT_ID.value(),
    scope: SPOTIFY_SCOPES,
    redirect_uri: SPOTIFY_REDIRECT_URI,
    state,
    show_dialog: 'false'
  });
  return { authUrl: `${SPOTIFY_ACCOUNTS}/authorize?${params.toString()}` };
});

exports.spotifyDisconnect = onCall(callableOptions, async (request) => {
  const uid = requireAuth(request);
  await db().collection(TOKENS_COLLECTION).doc(uid).delete();
  await db().collection('users').doc(uid).set(
    { spotify: { connected: false, needsReconnect: false, displayName: null, product: null } },
    { merge: true }
  );
  return { ok: true };
});

exports.spotifyMyPlaylists = onCall(callableOptions, async (request) => {
  const uid = requireAuth(request);
  const offset = Math.max(0, Number(request.data && request.data.offset) || 0);
  const limit = Math.min(50, Math.max(1, Number(request.data && request.data.limit) || 50));
  try {
    const token = await getValidAccessToken(uid);
    const { data } = await spotifyFetch(token, `/me/playlists?limit=${limit}&offset=${offset}`);
    const items = (data && Array.isArray(data.items) ? data.items : []).map(mapPlaylist).filter(Boolean);
    return { items, total: data && typeof data.total === 'number' ? data.total : items.length, nextOffset: data && data.next ? offset + limit : null };
  } catch (error) {
    throw toHttpsError(error);
  }
});

exports.spotifySearch = onCall(callableOptions, async (request) => {
  const uid = requireAuth(request);
  const query = typeof request.data?.query === 'string' ? request.data.query.trim().slice(0, 120) : '';
  if (!query) return { items: [] };
  const requested = Array.isArray(request.data?.types) ? request.data.types : ['playlist', 'track'];
  const types = requested.map((t) => String(t).toLowerCase()).filter((t) => t === 'playlist' || t === 'track');
  if (types.length === 0) throw new HttpsError('invalid-argument', 'types inválido.');
  const limit = Math.min(20, Math.max(1, Number(request.data?.limit) || 20));
  try {
    const token = await getValidAccessToken(uid);
    const params = new URLSearchParams({ q: query, type: types.join(','), limit: String(limit), market: 'from_token' });
    const { data } = await spotifyFetch(token, `/search?${params.toString()}`);
    const playlists = (data && data.playlists && Array.isArray(data.playlists.items) ? data.playlists.items : []).map(mapPlaylist);
    const tracks = (data && data.tracks && Array.isArray(data.tracks.items) ? data.tracks.items : []).map(mapTrack);
    return { items: [...playlists, ...tracks].filter(Boolean) };
  } catch (error) {
    throw toHttpsError(error);
  }
});

exports.spotifyDevices = onCall(callableOptions, async (request) => {
  const uid = requireAuth(request);
  try {
    const token = await getValidAccessToken(uid);
    return { devices: await listDevices(token) };
  } catch (error) {
    throw toHttpsError(error);
  }
});

// ─── OAuth callback (/api/spotify-callback) ──────────────────────────────────

function escapeHtml(value) {
  return String(value || '')
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function renderCallbackPage({ ok, title, message }) {
  const color = ok ? '#1DB954' : '#e53935';
  return `<!DOCTYPE html>
<html lang="pt-BR"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="robots" content="noindex"><title>${escapeHtml(title)}</title>
<style>
body{margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;background:#121212;color:#fff;font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;text-align:center;padding:24px;box-sizing:border-box}
.card{max-width:420px;background:#1e1e1e;border-radius:16px;padding:32px 24px;box-shadow:0 8px 32px rgba(0,0,0,.4)}
.icon{width:64px;height:64px;border-radius:50%;background:${color};display:flex;align-items:center;justify-content:center;margin:0 auto 16px;font-size:32px}
h1{font-size:20px;margin:0 0 8px}p{margin:0;color:#b3b3b3;line-height:1.5}
</style></head><body><div class="card"><div class="icon">${ok ? '✓' : '!'}</div>
<h1>${escapeHtml(title)}</h1><p>${escapeHtml(message)}</p></div></body></html>`;
}

exports.spotifyAuthCallback = onRequest(
  { region: REGION, timeoutSeconds: 20, invoker: 'public', secrets: [SPOTIFY_CLIENT_ID, SPOTIFY_CLIENT_SECRET] },
  async (req, res) => {
    res.set('Cache-Control', 'no-store');
    const { code, state, error: oauthError } = req.query || {};

    if (typeof state !== 'string' || !state) {
      res.status(400).send(renderCallbackPage({ ok: false, title: 'Link inválido', message: 'Volte ao app e tente conectar novamente.' }));
      return;
    }

    const stateRef = db().collection(AUTH_STATES_COLLECTION).doc(state);
    const stateSnap = await stateRef.get();
    const stateData = stateSnap.exists ? stateSnap.data() : null;
    // O state é de uso único, independente do resultado
    await stateRef.delete().catch(() => {});

    if (!stateData || !stateData.uid || stateData.expiresAt < Date.now()) {
      res.status(400).send(renderCallbackPage({ ok: false, title: 'Sessão expirada', message: 'O link de conexão expirou. Volte ao app e tente novamente.' }));
      return;
    }

    if (oauthError || typeof code !== 'string' || !code) {
      logger.warn('Spotify authorization denied', { uid: stateData.uid, oauthError });
      res.status(400).send(renderCallbackPage({ ok: false, title: 'Conexão cancelada', message: 'A autorização no Spotify não foi concluída. Você pode tentar de novo pelo app.' }));
      return;
    }

    try {
      const tokens = await requestToken({
        grant_type: 'authorization_code',
        code,
        redirect_uri: stateData.redirectUri || SPOTIFY_REDIRECT_URI
      });
      const saved = await saveTokens(stateData.uid, tokens, null);

      let profile = {};
      try {
        profile = (await spotifyFetch(saved.accessToken, '/me')).data || {};
      } catch (profileError) {
        logger.warn('Spotify /me failed after connect', { uid: stateData.uid, message: profileError.message });
      }

      await db().collection('users').doc(stateData.uid).set({
        spotify: {
          connected: true,
          needsReconnect: false,
          displayName: profile.display_name || profile.id || null,
          product: profile.product || null,
          connectedAt: admin.firestore.FieldValue.serverTimestamp()
        }
      }, { merge: true });

      const premiumHint = profile.product && profile.product !== 'premium'
        ? ' Atenção: controlar a reprodução exige Spotify Premium.'
        : '';
      res.status(200).send(renderCallbackPage({
        ok: true,
        title: 'Spotify conectado!',
        message: `Conta ${profile.display_name || ''} vinculada ao MySuperApp. Você já pode fechar esta página e voltar ao app.${premiumHint}`
      }));
    } catch (error) {
      logger.error('Spotify code exchange failed', { uid: stateData.uid, message: error.message });
      res.status(500).send(renderCallbackPage({ ok: false, title: 'Falha ao conectar', message: 'Não foi possível concluir a conexão com o Spotify. Tente novamente pelo app.' }));
    }
  }
);

// ─── Disparo público (/api/play-spotify) ─────────────────────────────────────

async function consumeRateLimit(key, max, windowMs) {
  const ref = db().collection('_rate_limits').doc(createHash('sha256').update(key).digest('hex'));
  const now = Date.now();
  return db().runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    if (!snap.exists || now - snap.data().windowStart >= windowMs) {
      tx.set(ref, { count: 1, windowStart: now });
      return true;
    }
    const { count } = snap.data();
    if (count >= max) return false;
    tx.update(ref, { count: count + 1 });
    return true;
  });
}

function getClientIp(req) {
  const forwardedFor = req.headers['x-forwarded-for'];
  if (typeof forwardedFor === 'string' && forwardedFor.split(',')[0].trim()) {
    return forwardedFor.split(',')[0].trim();
  }
  return req.ip || (req.socket && req.socket.remoteAddress) || null;
}

function parseBody(req) {
  let body = req.body;
  if (typeof body === 'string') {
    try { body = JSON.parse(body); } catch { body = {}; }
  }
  if ((!body || typeof body !== 'object') && req.rawBody) {
    try { body = JSON.parse(req.rawBody.toString()); } catch { body = {}; }
  }
  return body && typeof body === 'object' ? body : {};
}

function sendPlayError(res, status, code, message, extra) {
  res.status(status).json({ error: message, code, ...(extra || {}) });
}

exports.playSpotify = onRequest(
  { region: REGION, timeoutSeconds: 20, invoker: 'public', secrets: [SPOTIFY_CLIENT_ID, SPOTIFY_CLIENT_SECRET] },
  async (req, res) => {
    const origin = req.headers['origin'];
    if (origin && PLAY_ALLOWED_ORIGINS.has(origin)) {
      res.set('Access-Control-Allow-Origin', origin);
    }
    res.set('Vary', 'Origin');
    res.set('Access-Control-Allow-Methods', 'POST, OPTIONS');
    res.set('Access-Control-Allow-Headers', 'Content-Type');
    res.set('Cache-Control', 'no-store');

    if (req.method === 'OPTIONS') { res.status(204).send(''); return; }
    if (req.method !== 'POST') { sendPlayError(res, 405, 'METHOD_NOT_ALLOWED', 'Method not allowed'); return; }

    const body = parseBody(req);
    const qrCodeId = typeof body.qrCodeId === 'string' ? body.qrCodeId.trim() : '';
    if (!qrCodeId || qrCodeId.length > 128) {
      sendPlayError(res, 400, 'INVALID_REQUEST', 'qrCodeId inválido.');
      return;
    }

    // ── Rate limit (fail-open) ───────────────────────────────────────────────
    try {
      const ip = getClientIp(req) || 'unknown';
      const [ipOk, qrOk] = await Promise.all([
        consumeRateLimit(`${ip}:${qrCodeId}:play`, PLAY_RL_MAX_PER_IP_QR, PLAY_RL_WINDOW_MS),
        consumeRateLimit(`${qrCodeId}:play`, PLAY_RL_MAX_PER_QR, PLAY_RL_WINDOW_MS)
      ]);
      if (!ipOk || !qrOk) {
        sendPlayError(res, 429, ERR.RATE_LIMITED, 'Muitos toques seguidos. Aguarde um minuto.');
        return;
      }
    } catch (error) {
      logger.error('playSpotify rate limit failed, allowing request', error);
    }

    // ── QR ───────────────────────────────────────────────────────────────────
    let qr;
    try {
      const snap = await db().collection('qrcodes').doc(qrCodeId).get();
      qr = snap.exists ? snap.data() : null;
    } catch (error) {
      logger.error('playSpotify failed to load qrcode', error);
      sendPlayError(res, 500, 'INTERNAL', 'Erro interno.');
      return;
    }

    if (!qr || qr.type !== 'SPOTIFY') {
      sendPlayError(res, 404, 'NOT_FOUND', 'Smart Tag do Spotify não encontrada.');
      return;
    }
    if (!qr.userId || !isValidSpotifyUri(qr.spotifyUri)) {
      sendPlayError(res, 422, ERR.INVALID_TARGET, 'Esta tag ainda não tem uma playlist ou música configurada.');
      return;
    }

    try {
      const token = await getValidAccessToken(qr.userId);

      const devices = await listDevices(token);
      const device = resolveDevice(devices, qr.spotifyDeviceId, qr.spotifyDeviceName);
      if (!device) {
        sendPlayError(res, 404, ERR.DEVICE_NOT_FOUND,
          `O dispositivo "${qr.spotifyDeviceName || 'configurado'}" não está disponível no Spotify agora.`,
          { availableDevices: devices.map((d) => d.name) });
        return;
      }

      // Ids de Alexa/grupos mudam; mantém o documento atualizado (best-effort)
      if (device.id && device.id !== qr.spotifyDeviceId) {
        db().collection('qrcodes').doc(qrCodeId)
          .set({ spotifyDeviceId: device.id }, { merge: true })
          .catch((e) => logger.warn('Failed to refresh spotifyDeviceId', { qrCodeId, message: e.message }));
      }

      const deviceQuery = `device_id=${encodeURIComponent(device.id)}`;
      const shuffle = qr.spotifyShuffle === true;
      const isContext = !qr.spotifyUri.startsWith('spotify:track:');

      // O shuffle só pega com dispositivo ativo; tenta antes e, se falhar, depois do play.
      let shuffleApplied = false;
      if (shuffle && isContext) {
        shuffleApplied = await spotifyFetch(token, `/me/player/shuffle?state=true&${deviceQuery}`, { method: 'PUT' })
          .then(() => true).catch(() => false);
      }

      const playBody = isContext ? { context_uri: qr.spotifyUri } : { uris: [qr.spotifyUri] };
      await spotifyFetch(token, `/me/player/play?${deviceQuery}`, { method: 'PUT', body: playBody });

      if (shuffle && isContext && !shuffleApplied) {
        await spotifyFetch(token, `/me/player/shuffle?state=true&${deviceQuery}`, { method: 'PUT' })
          .catch((e) => logger.warn('Shuffle after play failed', { qrCodeId, message: e.message }));
      }

      logger.info('playSpotify ok', { qrCodeId, device: device.name, uri: qr.spotifyUri });
      res.status(200).json({ ok: true, deviceName: device.name, name: qr.spotifyName || '' });
    } catch (error) {
      if (error instanceof SpotifyError) {
        const status = error.code === ERR.SPOTIFY_ERROR ? 502 : error.status;
        logger.warn('playSpotify failed', { qrCodeId, code: error.code, message: error.message });
        sendPlayError(res, status, error.code, humanizePlayError(error, qr), error.retryAfter ? { retryAfter: error.retryAfter } : undefined);
        return;
      }
      logger.error('playSpotify unexpected error', error);
      sendPlayError(res, 500, 'INTERNAL', 'Erro ao iniciar a reprodução.');
    }
  }
);

function humanizePlayError(error, qr) {
  switch (error.code) {
    case ERR.NOT_CONNECTED:
    case ERR.NEEDS_RECONNECT:
      return 'O dono desta tag precisa conectar (ou reconectar) a conta do Spotify no MySuperApp.';
    case ERR.PREMIUM_REQUIRED:
      return 'Controlar a reprodução exige uma conta Spotify Premium.';
    case ERR.DEVICE_NOT_FOUND:
      return `O dispositivo "${(qr && qr.spotifyDeviceName) || 'configurado'}" não está disponível no Spotify agora. Diga "Alexa, abrir Spotify" e tente de novo.`;
    case ERR.RATE_LIMITED:
      return 'O Spotify pediu uma pausa. Tente novamente em alguns segundos.';
    default:
      return 'O Spotify não conseguiu iniciar a reprodução agora.';
  }
}

// Exportado para testes/scripts
exports._internal = { resolveDevice, isValidSpotifyUri, mapPlaylist, mapTrack, pickImage, ERR };
