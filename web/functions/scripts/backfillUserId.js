#!/usr/bin/env node
/**
 * Backfill one-shot do campo `userId` (dono) nas coleções do Financeiro e da Academia.
 *
 * Antes desta migração os documentos de `transactions`, `cards`, `transactions_category`,
 * `body_measurements`, `exercises`, `workouts` e `workout_sessions` não tinham dono.
 * Este script grava `userId = <uid>` em todos os documentos que AINDA NÃO possuem o campo
 * (documentos com `userId` já preenchido nunca são alterados). Idempotente.
 *
 * Uso (a partir da raiz do repositório):
 *   npm --prefix web\functions run migrate:user-id -- --project=baila-675c6 --uid=<uid> --dry-run
 *   npm --prefix web\functions run migrate:user-id -- --project=baila-675c6 --uid=<uid>
 *
 * Opções:
 *   --uid=<uid>              obrigatório; precisa existir no Firebase Auth do projeto alvo
 *   --project=<projectId>    default: GCLOUD_PROJECT / GOOGLE_CLOUD_PROJECT ou web/.firebaserc
 *   --collections=a,b        subconjunto das coleções (default: todas)
 *   --dry-run                só relata
 *
 * Credenciais: Application Default Credentials (`gcloud auth application-default login`).
 */

const fs = require("fs");
const path = require("path");
const admin = require("firebase-admin");

const ALL_COLLECTIONS = [
  "transactions",
  "cards",
  "transactions_category",
  "body_measurements",
  "exercises",
  "workouts",
  "workout_sessions",
];
const BATCH_LIMIT = 500;

const args = process.argv.slice(2);
const dryRun = args.includes("--dry-run");

function option(name, fallback) {
  const prefix = `--${name}=`;
  const found = args.find((a) => a.startsWith(prefix));
  return found ? found.slice(prefix.length) : fallback;
}

function resolveProjectId() {
  const fromArg = option("project", undefined);
  if (fromArg) return fromArg;
  const fromEnv = process.env.GCLOUD_PROJECT || process.env.GOOGLE_CLOUD_PROJECT;
  if (fromEnv) return fromEnv;
  try {
    const rcPath = path.resolve(__dirname, "..", "..", ".firebaserc");
    const rc = JSON.parse(fs.readFileSync(rcPath, "utf8"));
    return rc.projects && rc.projects.default;
  } catch (_) {
    return undefined;
  }
}

const uid = option("uid", "").trim();
const projectId = resolveProjectId();
const collections = option("collections", ALL_COLLECTIONS.join(","))
  .split(",")
  .map((c) => c.trim())
  .filter(Boolean);

function validateArgs() {
  if (!uid) throw new Error("Informe --uid=<uid do Firebase Auth>.");
  if (!projectId) throw new Error("Não foi possível determinar o projeto; use --project=<id>.");
  const unknown = collections.filter((c) => !ALL_COLLECTIONS.includes(c));
  if (unknown.length > 0) {
    throw new Error(
      `Coleções desconhecidas: ${unknown.join(", ")}. Permitidas: ${ALL_COLLECTIONS.join(", ")}.`
    );
  }
}

function needsOwner(data) {
  return typeof data.userId !== "string" || data.userId.length === 0;
}

async function backfillCollection(db, collectionName) {
  const snap = await db.collection(collectionName).get();
  const pending = snap.docs.filter((doc) => needsOwner(doc.data()));
  const foreign = snap.docs.filter((doc) => !needsOwner(doc.data()) && doc.data().userId !== uid);

  console.log(
    `[${collectionName}] ${snap.size} documento(s), ${pending.length} sem userId` +
      (foreign.length > 0 ? `, ${foreign.length} de outro(s) usuário(s) (mantidos)` : "")
  );

  if (pending.length === 0) return 0;
  if (dryRun) {
    pending.forEach((doc) => console.log(`  - ${doc.id}`));
    return pending.length;
  }

  for (let i = 0; i < pending.length; i += BATCH_LIMIT) {
    const chunk = pending.slice(i, i + BATCH_LIMIT);
    const batch = db.batch();
    chunk.forEach((doc) => batch.update(doc.ref, { userId: uid }));
    await batch.commit();
    console.log(`  commit ${i + chunk.length}/${pending.length}`);
  }
  return pending.length;
}

async function main() {
  validateArgs();
  admin.initializeApp({ projectId });

  // Garante que o UID pertence ao projeto alvo (dev e prd têm contas com UIDs diferentes)
  const user = await admin.auth().getUser(uid).catch((err) => {
    throw new Error(`UID ${uid} não encontrado no Auth do projeto ${projectId}: ${err.message}`);
  });

  console.log(
    `Backfill userId=${uid} (${user.email || "sem e-mail"}) em ${collections.join(", ")}` +
      ` (projeto: ${projectId})${dryRun ? " [DRY RUN]" : ""}`
  );

  const db = admin.firestore();
  let total = 0;
  for (const collectionName of collections) {
    total += await backfillCollection(db, collectionName);
  }

  console.log(
    dryRun
      ? `Concluído (dry run): ${total} documento(s) seriam atualizados.`
      : `Concluído: ${total} documento(s) atualizados.`
  );
}

main().catch((err) => {
  console.error("Falha no backfill:", err.message || err);
  process.exit(1);
});
