#!/usr/bin/env node
/**
 * Spike local: confirma que as Alexas (e o grupo multi-room, ex.: "Todo lugar")
 * aparecem como dispositivos Spotify Connect e aceitam `PUT /me/player/play`.
 *
 * Não usa Firebase. Requer um app criado em https://developer.spotify.com/dashboard
 * com o redirect URI http://127.0.0.1:8899/callback cadastrado.
 *
 *   set SPOTIFY_CLIENT_ID=...
 *   set SPOTIFY_CLIENT_SECRET=...
 *   node scripts/spotifySpike.js                       # lista dispositivos
 *   node scripts/spotifySpike.js --device="Todo lugar" --uri=spotify:playlist:37i9dQZF1DXcBWIGoYBM5M
 */

const http = require('http');
const { randomBytes } = require('crypto');
const { exec } = require('child_process');

const CLIENT_ID = process.env.SPOTIFY_CLIENT_ID;
const CLIENT_SECRET = process.env.SPOTIFY_CLIENT_SECRET;
const PORT = 8899;
const REDIRECT_URI = `http://127.0.0.1:${PORT}/callback`;
const SCOPES = 'user-read-playback-state user-modify-playback-state playlist-read-private';

const args = Object.fromEntries(process.argv.slice(2).map((a) => {
  const [k, ...v] = a.replace(/^--/, '').split('=');
  return [k, v.join('=') || true];
}));

if (!CLIENT_ID || !CLIENT_SECRET) {
  console.error('Defina SPOTIFY_CLIENT_ID e SPOTIFY_CLIENT_SECRET no ambiente.');
  process.exit(1);
}

function openBrowser(url) {
  const cmd = process.platform === 'win32' ? `start "" "${url.replace(/&/g, '^&')}"`
    : process.platform === 'darwin' ? `open "${url}"` : `xdg-open "${url}"`;
  exec(cmd, () => {});
}

function waitForCode(expectedState) {
  return new Promise((resolve, reject) => {
    const server = http.createServer((req, res) => {
      const url = new URL(req.url, REDIRECT_URI);
      if (url.pathname !== '/callback') { res.statusCode = 404; res.end(); return; }
      const code = url.searchParams.get('code');
      const state = url.searchParams.get('state');
      res.setHeader('Content-Type', 'text/html; charset=utf-8');
      if (!code || state !== expectedState) {
        res.end('<h2>Falha na autorização</h2>'); server.close(); reject(new Error('code/state inválidos')); return;
      }
      res.end('<h2>OK! Pode voltar ao terminal.</h2>'); server.close(); resolve(code);
    });
    server.listen(PORT, '127.0.0.1');
  });
}

async function token(params) {
  const res = await fetch('https://accounts.spotify.com/api/token', {
    method: 'POST',
    headers: {
      Authorization: `Basic ${Buffer.from(`${CLIENT_ID}:${CLIENT_SECRET}`).toString('base64')}`,
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    body: new URLSearchParams(params).toString()
  });
  const data = await res.json();
  if (!res.ok) throw new Error(`token: ${JSON.stringify(data)}`);
  return data;
}

async function api(accessToken, path, options = {}) {
  const res = await fetch(`https://api.spotify.com/v1${path}`, {
    method: options.method || 'GET',
    headers: { Authorization: `Bearer ${accessToken}`, ...(options.body ? { 'Content-Type': 'application/json' } : {}) },
    body: options.body ? JSON.stringify(options.body) : undefined
  });
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }
  return { status: res.status, data };
}

(async () => {
  const state = randomBytes(8).toString('hex');
  const authUrl = `https://accounts.spotify.com/authorize?${new URLSearchParams({
    response_type: 'code', client_id: CLIENT_ID, scope: SCOPES, redirect_uri: REDIRECT_URI, state
  })}`;
  console.log('Abrindo o navegador para autorizar...\n' + authUrl + '\n');
  openBrowser(authUrl);
  const code = await waitForCode(state);
  const tokens = await token({ grant_type: 'authorization_code', code, redirect_uri: REDIRECT_URI });

  const me = await api(tokens.access_token, '/me');
  console.log(`Conta: ${me.data.display_name} (${me.data.product})`);
  if (me.data.product !== 'premium') console.warn('⚠️  Sem Premium o play vai falhar (PREMIUM_REQUIRED).');

  const devices = await api(tokens.access_token, '/me/player/devices');
  console.log('\nDispositivos Spotify Connect:');
  console.table((devices.data.devices || []).map((d) => ({ name: d.name, type: d.type, id: d.id, active: d.is_active, restricted: d.is_restricted })));
  if (!(devices.data.devices || []).length) {
    console.log('Nenhum dispositivo. Diga "Alexa, abrir Spotify" (ou toque algo pelo app do Spotify na Alexa) e rode de novo.');
  }

  if (args.device && args.uri) {
    const target = (devices.data.devices || []).find((d) => d.name.toLowerCase() === String(args.device).toLowerCase());
    if (!target) { console.error(`Dispositivo "${args.device}" não encontrado.`); process.exit(2); }
    const isTrack = String(args.uri).startsWith('spotify:track:');
    const play = await api(tokens.access_token, `/me/player/play?device_id=${encodeURIComponent(target.id)}`, {
      method: 'PUT',
      body: isTrack ? { uris: [args.uri] } : { context_uri: args.uri }
    });
    console.log(`\nPUT /me/player/play em "${target.name}" → HTTP ${play.status}`, play.data || '');
  } else {
    console.log('\nPara testar o play: --device="Todo lugar" --uri=spotify:playlist:<id>');
  }
})().catch((error) => { console.error(error); process.exit(1); });
