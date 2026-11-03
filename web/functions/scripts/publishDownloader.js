#!/usr/bin/env node
/**
 * Publica o instalador do auxiliar de downloads (e o pacote de fontes
 * correspondentes) no Firebase Storage do projeto, para a aba Downloads do
 * portal web. Os objetos ficam em `downloader/` e são legíveis apenas por
 * contas autenticadas (web/storage.rules); nenhum token de download público é
 * gerado.
 *
 * Uso (a partir da raiz do repositório), depois de `desktop\downloader\build.ps1`:
 *   npm --prefix web\functions run publish:downloader -- --dry-run   # só valida/relata
 *   npm --prefix web\functions run publish:downloader                # envia
 *
 * Opções:
 *   --project=<projectId>  default: baila-675c6 (Storage não está habilitado no dev)
 *   --installer=<path>     default: desktop/downloader/output/MySuperAppDownloader-Setup.exe
 *   --sources=<path>       default: desktop/downloader/output/MySuperAppDownloader-<versão>-sources.zip
 *   --version=<x.y.z>      default: lida de desktop/downloader/installer.iss (AppVersion)
 *   --notes=a;b;c          notas exibidas no portal (separadas por ';')
 *   --force                permite sobrescrever objetos versionados já publicados
 *   --cors-only            só garante o CORS do bucket para os domínios do portal
 *                          (necessário para o fetch `alt=media` no navegador)
 *
 * Credenciais: Application Default Credentials (`gcloud auth application-default login`).
 */

const fs = require("fs");
const path = require("path");
const crypto = require("crypto");
const admin = require("firebase-admin");

const DEFAULT_PROJECT = "baila-675c6";
const REPO_ROOT = path.resolve(__dirname, "..", "..", "..");
const DOWNLOADER_DIR = path.join(REPO_ROOT, "desktop", "downloader");
const STORAGE_PREFIX = "downloader/";
const MIN_INSTALLER_BYTES = 50 * 1024 * 1024;
const MIN_SOURCES_BYTES = 10 * 1024 * 1024;

const args = process.argv.slice(2);
const dryRun = args.includes("--dry-run");
const force = args.includes("--force");
const corsOnly = args.includes("--cors-only");

function option(name, fallback) {
  const prefix = `--${name}=`;
  const found = args.find((a) => a.startsWith(prefix));
  return found ? found.slice(prefix.length) : fallback;
}

function readVersionFromIss() {
  const iss = fs.readFileSync(path.join(DOWNLOADER_DIR, "installer.iss"), "utf8");
  const match = iss.match(/#define\s+AppVersion\s+"(\d+\.\d+\.\d+)"/);
  if (!match) throw new Error("Não foi possível ler AppVersion de installer.iss.");
  return match[1];
}

const projectId = option("project", DEFAULT_PROJECT);
const version = option("version", readVersionFromIss());
if (!/^\d+\.\d+\.\d+$/.test(version)) throw new Error(`Versão inválida: ${version}`);
const installerPath = path.resolve(
  option("installer", path.join(DOWNLOADER_DIR, "output", "MySuperAppDownloader-Setup.exe"))
);
const sourcesPath = path.resolve(
  option("sources", path.join(DOWNLOADER_DIR, "output", `MySuperAppDownloader-${version}-sources.zip`))
);
const notes = option("notes", "")
  .split(";")
  .map((n) => n.trim())
  .filter(Boolean);

function describeFile(filePath, minBytes, label) {
  if (!fs.existsSync(filePath)) throw new Error(`${label} não encontrado: ${filePath}`);
  const size = fs.statSync(filePath).size;
  if (size < minBytes) {
    throw new Error(`${label} tem apenas ${size} bytes (mínimo esperado ${minBytes}); build incompleto?`);
  }
  const hash = crypto.createHash("sha256");
  hash.update(fs.readFileSync(filePath));
  return { size, sha256: hash.digest("hex") };
}

async function uploadObject(bucket, localPath, objectName, contentType) {
  const file = bucket.file(objectName);
  const [exists] = await file.exists();
  if (exists && !force) {
    throw new Error(`${objectName} já existe no bucket; use --force para sobrescrever.`);
  }
  if (dryRun) {
    console.log(`[dry-run] upload ${localPath} -> gs://${bucket.name}/${objectName}${exists ? " (sobrescreveria)" : ""}`);
    return;
  }
  await bucket.upload(localPath, {
    destination: objectName,
    resumable: true,
    metadata: { contentType, cacheControl: "private, max-age=0" },
  });
  // Garante que nenhum token de download público ficou associado ao objeto.
  await file.setMetadata({ metadata: { firebaseStorageDownloadTokens: null } });
  console.log(`enviado gs://${bucket.name}/${objectName}`);
}

// Downloads `?alt=media` bem-sucedidos só trazem Access-Control-Allow-Origin se o
// bucket tiver CORS configurado (respostas de erro e o preflight não dependem
// disso). Sem isto o fetch do portal falha com TypeError apesar do 200.
const PORTAL_ORIGINS = ["https://mysuperapp.com.br", "https://baila.space", "https://baila-675c6.web.app", "https://baila-675c6.firebaseapp.com"];
const PORTAL_CORS = [{ origin: PORTAL_ORIGINS, method: ["GET", "HEAD"], responseHeader: ["Content-Type", "Content-Length", "Content-Range", "Cache-Control"], maxAgeSeconds: 3600 }];

async function ensureCors(bucket) {
  const [meta] = await bucket.getMetadata();
  const current = JSON.stringify(meta.cors || []);
  if (current === JSON.stringify(PORTAL_CORS)) return;
  if (dryRun) {
    console.log(`[dry-run] configuraria CORS do bucket (atual: ${current}) para ${PORTAL_ORIGINS.join(", ")}`);
    return;
  }
  await bucket.setCorsConfiguration(PORTAL_CORS);
  console.log(`CORS do bucket configurado para ${PORTAL_ORIGINS.join(", ")}`);
}

async function main() {
  if (corsOnly) {
    console.log(`Projeto: ${projectId}${dryRun ? " (dry-run)" : ""} — somente CORS`);
    admin.initializeApp({ projectId, storageBucket: `${projectId}.firebasestorage.app` });
    await ensureCors(admin.storage().bucket());
    return;
  }
  const installer = describeFile(installerPath, MIN_INSTALLER_BYTES, "Instalador");
  const sources = describeFile(sourcesPath, MIN_SOURCES_BYTES, "Pacote de fontes");
  const installerObject = `${STORAGE_PREFIX}MySuperAppDownloader-Setup-${version}.exe`;
  const sourcesObject = `${STORAGE_PREFIX}MySuperAppDownloader-${version}-sources.zip`;
  const manifest = {
    version,
    installer: { path: installerObject, size: installer.size, sha256: installer.sha256, fileName: "MySuperAppDownloader-Setup.exe" },
    sources: { path: sourcesObject, size: sources.size, sha256: sources.sha256, fileName: `MySuperAppDownloader-${version}-sources.zip` },
    publishedAt: new Date().toISOString(),
    notes,
  };
  console.log(`Projeto: ${projectId}${dryRun ? " (dry-run)" : ""}`);
  console.log(JSON.stringify(manifest, null, 2));

  admin.initializeApp({ projectId, storageBucket: `${projectId}.firebasestorage.app` });
  const bucket = admin.storage().bucket();
  await ensureCors(bucket);
  await uploadObject(bucket, installerPath, installerObject, "application/vnd.microsoft.portable-executable");
  await uploadObject(bucket, sourcesPath, sourcesObject, "application/zip");

  const latest = bucket.file(`${STORAGE_PREFIX}latest.json`);
  if (dryRun) {
    console.log(`[dry-run] gravaria gs://${bucket.name}/${latest.name}`);
    return;
  }
  await latest.save(JSON.stringify(manifest, null, 2), {
    resumable: false,
    metadata: { contentType: "application/json", cacheControl: "private, max-age=0" },
  });
  await latest.setMetadata({ metadata: { firebaseStorageDownloadTokens: null } });
  console.log(`manifesto gravado em gs://${bucket.name}/${latest.name}`);
}

main().catch((err) => {
  console.error(err.message || err);
  process.exit(1);
});
