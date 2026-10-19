#!/usr/bin/env node
/**
 * Cópia one-shot dos dados de Academia entre projetos Firebase.
 *
 * Copia as coleções `exercises` e `workouts` (nessa ordem, pois treinos
 * referenciam exercícios por ID de documento) do projeto de origem (dev)
 * para o de destino (prd), preservando os IDs. Documentos já existentes no
 * destino são sobrescritos; nada é apagado no destino.
 *
 * Uso (a partir da raiz do repositório):
 *   npm --prefix web\functions run copy:workout-data -- --dry-run   # só relata
 *   npm --prefix web\functions run copy:workout-data                # aplica
 *
 * Opções:
 *   --source=<projectId>     origem  (default: my-super-app-dev-d6819)
 *   --target=<projectId>     destino (default: baila-675c6)
 *   --collections=a,b        subconjunto de exercises,workouts (default: ambas)
 *
 * Credenciais: Application Default Credentials com acesso aos dois projetos
 * (`gcloud auth application-default login`).
 */

const admin = require("firebase-admin");

const DEFAULT_SOURCE = "my-super-app-dev-d6819";
const DEFAULT_TARGET = "baila-675c6";
const ALL_COLLECTIONS = ["exercises", "workouts"];
const BATCH_LIMIT = 500;

const args = process.argv.slice(2);
const dryRun = args.includes("--dry-run");

function option(name, fallback) {
  const prefix = `--${name}=`;
  const found = args.find((a) => a.startsWith(prefix));
  return found ? found.slice(prefix.length) : fallback;
}

const sourceProject = option("source", DEFAULT_SOURCE);
const targetProject = option("target", DEFAULT_TARGET);
const collections = option("collections", ALL_COLLECTIONS.join(","))
  .split(",")
  .map((c) => c.trim())
  .filter(Boolean);

function validateArgs() {
  if (sourceProject === targetProject) {
    throw new Error(`Origem e destino são o mesmo projeto (${sourceProject}).`);
  }
  const unknown = collections.filter((c) => !ALL_COLLECTIONS.includes(c));
  if (unknown.length > 0) {
    throw new Error(
      `Coleções desconhecidas: ${unknown.join(", ")}. Permitidas: ${ALL_COLLECTIONS.join(", ")}.`
    );
  }
}

function warnOrphanExercises(exercises, workouts) {
  if (!exercises || !workouts) return;
  const ids = new Set(exercises.map((d) => d.id));
  workouts.forEach((doc) => {
    const data = doc.data();
    const refs = new Set([
      ...(Array.isArray(data.exerciseIds) ? data.exerciseIds : []),
      ...(Array.isArray(data.items) ? data.items.map((i) => i && i.exerciseId) : []),
    ]);
    const missing = [...refs].filter((id) => id && !ids.has(id));
    if (missing.length > 0) {
      console.warn(
        `  aviso: workout ${doc.id} ("${data.name || ""}") referencia exercício(s) inexistente(s) na origem: ${missing.join(", ")}`
      );
    }
  });
}

async function copyCollection(targetDb, name, docs) {
  const existing = new Set();
  if (docs.length > 0) {
    const refs = docs.map((d) => targetDb.collection(name).doc(d.id));
    const targetSnaps = await targetDb.getAll(...refs);
    targetSnaps.forEach((s) => s.exists && existing.add(s.id));
  }

  const created = docs.length - existing.size;
  console.log(
    `[${name}] ${docs.length} documento(s) na origem → ${created} novo(s), ${existing.size} sobrescrito(s)`
  );
  docs.forEach((d) =>
    console.log(`  ${existing.has(d.id) ? "~" : "+"} ${d.id}  ${d.data().name || ""}`)
  );

  if (dryRun || docs.length === 0) return { created, overwritten: existing.size };

  for (let i = 0; i < docs.length; i += BATCH_LIMIT) {
    const chunk = docs.slice(i, i + BATCH_LIMIT);
    const batch = targetDb.batch();
    chunk.forEach((d) => batch.set(targetDb.collection(name).doc(d.id), d.data()));
    await batch.commit();
    console.log(`  commit ${i + chunk.length}/${docs.length}`);
  }
  return { created, overwritten: existing.size };
}

async function main() {
  validateArgs();

  const sourceApp = admin.initializeApp({ projectId: sourceProject }, "source");
  const targetApp = admin.initializeApp({ projectId: targetProject }, "target");
  const sourceDb = sourceApp.firestore();
  const targetDb = targetApp.firestore();

  console.log(
    `Copiando ${collections.join(", ")}: ${sourceProject} → ${targetProject}${dryRun ? " [DRY RUN]" : ""}`
  );

  // Lê tudo da origem antes de qualquer escrita no destino.
  const snapshots = {};
  for (const name of ALL_COLLECTIONS) {
    if (collections.includes(name)) {
      snapshots[name] = (await sourceDb.collection(name).get()).docs;
    }
  }
  warnOrphanExercises(snapshots.exercises, snapshots.workouts);

  let created = 0;
  let overwritten = 0;
  for (const name of ALL_COLLECTIONS) {
    if (!snapshots[name]) continue;
    const result = await copyCollection(targetDb, name, snapshots[name]);
    created += result.created;
    overwritten += result.overwritten;
  }

  console.log(
    dryRun
      ? `Concluído (dry run): ${created} documento(s) seriam criados e ${overwritten} sobrescritos.`
      : `Concluído: ${created} documento(s) criados e ${overwritten} sobrescritos em ${targetProject}.`
  );
}

main().catch((err) => {
  console.error("Falha na cópia:", err.message || err);
  process.exit(1);
});
