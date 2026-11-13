const { before, after, beforeEach, test } = require("node:test");
const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { doc, setDoc, getDoc, getDocs, collection, deleteDoc } = require("firebase/firestore");

let env;
before(async () => {
    env = await initializeTestEnvironment({ projectId: "demo-mysuperapp-recipes" });
});
after(async () => { if (env) await env.cleanup(); });
beforeEach(async () => {
    await env.clearFirestore();
    // Escrito pelo backend (Admin SDK), que ignora as regras.
    await env.withSecurityRulesDisabled(async (ctx) => {
        await setDoc(doc(ctx.firestore(), "users/alice/financial_reports/2026-11"), {
            schemaVersion: 1, month: "2026-11", summary: { income: 1 },
        });
    });
});

const database = (uid) => uid ? env.authenticatedContext(uid).firestore() : env.unauthenticatedContext().firestore();

test("only the owner can read their financial reports", async () => {
    await assertSucceeds(getDoc(doc(database("alice"), "users/alice/financial_reports/2026-11")));
    await assertSucceeds(getDocs(collection(database("alice"), "users/alice/financial_reports")));
    await assertFails(getDoc(doc(database("bob"), "users/alice/financial_reports/2026-11")));
    await assertFails(getDocs(collection(database("bob"), "users/alice/financial_reports")));
    await assertFails(getDoc(doc(database(null), "users/alice/financial_reports/2026-11")));
});

test("financial reports are never writable from the client, not even by the owner", async () => {
    const db = database("alice");
    await assertFails(setDoc(doc(db, "users/alice/financial_reports/2026-12"), { month: "2026-12" }));
    await assertFails(setDoc(doc(db, "users/alice/financial_reports/2026-11"), { summary: { income: 2 } }, { merge: true }));
    await assertFails(deleteDoc(doc(db, "users/alice/financial_reports/2026-11")));
});
