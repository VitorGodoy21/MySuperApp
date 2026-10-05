#!/usr/bin/env node
/**
 * Backfill one-shot do campo `transactionType` no Firestore.
 *
 * Preenche `transactionType = "EXPENSE"` em todos os documentos de
 * `transactions_category` e `transactions` que ainda não possuem o campo
 * (ou possuem um valor inválido). Documentos já marcados como EXPENSE/INCOME
 * não são alterados.
 *
 * Uso (a partir da raiz do repositório):
 *   npm --prefix web\functions run migrate:transaction-type -- --dry-run   # só relata
 *   npm --prefix web\functions run migrate:transaction-type                # aplica
 *
 * Credenciais: Application Default Credentials. Ex.:
 *   set GOOGLE_APPLICATION_CREDENTIALS=C:\caminho\service-account.json
 * ou `gcloud auth application-default login`. O projeto é lido de
 * GCLOUD_PROJECT / GOOGLE_CLOUD_PROJECT ou de web/.firebaserc (default).
 */

const fs = require("fs");
const path = require("path");
const admin = require("firebase-admin");

const VALID_TYPES = new Set(["EXPENSE", "INCOME"]);
const DEFAULT_TYPE = "EXPENSE";
const COLLECTIONS = ["transactions_category", "transactions"];
const BATCH_LIMIT = 500;

const dryRun = process.argv.includes("--dry-run");

function resolveProjectId() {
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

function needsBackfill(data) {
  const value = data.transactionType;
  return typeof value !== "string" || !VALID_TYPES.has(value);
}

async function backfillCollection(db, collectionName) {
  const snap = await db.collection(collectionName).get();
  const pending = snap.docs.filter((doc) => needsBackfill(doc.data()));

  console.log(
    `[${collectionName}] ${snap.size} documento(s), ${pending.length} sem transactionType válido`
  );

  if (pending.length === 0 || dryRun) {
    pending.forEach((doc) => console.log(`  - ${doc.id}`));
    return pending.length;
  }

  for (let i = 0; i < pending.length; i += BATCH_LIMIT) {
    const chunk = pending.slice(i, i + BATCH_LIMIT);
    const batch = db.batch();
    chunk.forEach((doc) => batch.update(doc.ref, { transactionType: DEFAULT_TYPE }));
    await batch.commit();
    console.log(`  commit ${i + chunk.length}/${pending.length}`);
  }
  return pending.length;
}

async function main() {
  const projectId = resolveProjectId();
  admin.initializeApp(projectId ? { projectId } : undefined);
  const db = admin.firestore();

  console.log(
    `Backfill transactionType=${DEFAULT_TYPE} em ${COLLECTIONS.join(", ")}` +
      ` (projeto: ${projectId || "ADC"})${dryRun ? " [DRY RUN]" : ""}`
  );

  let total = 0;
  for (const collectionName of COLLECTIONS) {
    total += await backfillCollection(db, collectionName);
  }

  console.log(
    dryRun
      ? `Concluído (dry run): ${total} documento(s) seriam atualizados.`
      : `Concluído: ${total} documento(s) atualizados.`
  );
}

main().catch((err) => {
  console.error("Falha no backfill:", err);
  process.exit(1);
});
