const { before, after, beforeEach, test } = require("node:test");
const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { doc, setDoc, getDoc, getDocs, collection, query, where, updateDoc, deleteDoc, serverTimestamp, runTransaction } = require("firebase/firestore");
const { ref, uploadBytes, getBytes, deleteObject } = require("firebase/storage");

let env;
before(async () => {
    env = await initializeTestEnvironment({ projectId: "demo-mysuperapp-recipes" });
});
after(async () => { if (env) await env.cleanup(); });
beforeEach(async () => { await env.clearFirestore(); });

function recipe(overrides = {}) {
    return {
        userId: "alice", name: "Bolo", description: "", ingredients: [{ name: "Farinha", quantity: "1 xícara" }],
        steps: ["Misture"], prepMinutes: 10, cookMinutes: null, yield: "4 porções", category: "DESSERT",
        difficulty: "EASY", notes: "", isFavorite: false, imagePath: null,
        createdAt: serverTimestamp(), updatedAt: serverTimestamp(), ...overrides,
    };
}
const database = (uid) => uid ? env.authenticatedContext(uid).firestore() : env.unauthenticatedContext().firestore();

test("owner can create via transaction, list, read, favorite and delete", async () => {
    const db = database("alice");
    const target = doc(db, "recipes/r1");
    await assertSucceeds(runTransaction(db, async tx => {
        const existing = await tx.get(target);
        if (!existing.exists()) tx.set(target, recipe());
    }));
    await assertSucceeds(getDoc(target));
    await assertSucceeds(getDocs(query(collection(db, "recipes"), where("userId", "==", "alice"))));
    await assertSucceeds(updateDoc(target, { isFavorite: true, updatedAt: serverTimestamp() }));
    await assertSucceeds(deleteDoc(target));
});

test("other accounts and logged-out users cannot access or mutate recipes", async () => {
    await setDoc(doc(database("alice"), "recipes/r1"), recipe());
    for (const uid of ["bob", null]) {
        const db = database(uid);
        const target = doc(db, "recipes/r1");
        await assertFails(getDoc(target));
        await assertFails(getDocs(query(collection(db, "recipes"), where("userId", "==", "alice"))));
        await assertFails(updateDoc(target, { name: "Outra", updatedAt: serverTimestamp() }));
        await assertFails(deleteDoc(target));
        await assertFails(setDoc(doc(db, "recipes/r2"), recipe()));
    }
    await assertFails(getDocs(collection(database("alice"), "recipes")));
});

test("ownership, timestamps, fields and private photo paths are validated", async () => {
    const db = database("alice");
    const target = doc(db, "recipes/r1");
    await setDoc(target, recipe());
    for (const invalid of [
        { userId: "bob" }, { name: "" }, { ingredients: [] }, { steps: [] },
        { prepMinutes: -1 }, { cookMinutes: 1.5 }, { difficulty: "UNKNOWN" },
        { category: "UNKNOWN" }, { notes: "x".repeat(4001) }, { isFavorite: "yes" },
        { imagePath: "recipe_images/bob/r1/photo.jpg" }, { imagePath: "recipe_images/alice/r2/photo.jpg" },
        { unexpected: true },
    ]) await assertFails(updateDoc(target, { ...invalid, updatedAt: serverTimestamp() }));
    await assertSucceeds(updateDoc(target, { imagePath: "recipe_images/alice/r1/photo.jpg", updatedAt: serverTimestamp() }));
    await assertFails(setDoc(doc(db, "recipes/invalid"), recipe({ userId: "bob" })));
});

test("recipes feature flag accepts only booleans and is private", async () => {
    const alice = doc(database("alice"), "users/alice");
    await assertSucceeds(setDoc(alice, { featureFlags: { recipes: false } }));
    await assertSucceeds(setDoc(alice, { featureFlags: { recipes: true, diet: false } }));
    await assertFails(setDoc(alice, { featureFlags: { recipes: "yes" } }));
    await assertFails(setDoc(alice, { featureFlags: { unexpected: true } }));
    await assertFails(getDoc(doc(database("bob"), "users/alice")));
});

test("photo read, upload and removal are private and validate size and MIME type", async () => {
    const ownerStorage = env.authenticatedContext("alice").storage();
    const owner = ref(ownerStorage, "recipe_images/alice/r1/photo.jpg");
    await assertSucceeds(uploadBytes(owner, new Uint8Array([1, 2, 3]), { contentType: "image/jpeg" }));
    await assertSucceeds(getBytes(owner));
    for (const context of [env.authenticatedContext("bob"), env.unauthenticatedContext()]) {
        const other = ref(context.storage(), "recipe_images/alice/r1/photo.jpg");
        await assertFails(getBytes(other));
        await assertFails(uploadBytes(other, new Uint8Array([1]), { contentType: "image/jpeg" }));
        await assertFails(deleteObject(other));
    }
    await assertFails(uploadBytes(owner, new Uint8Array([1]), { contentType: "text/plain" }));
    await assertFails(uploadBytes(owner, new Uint8Array(5 * 1024 * 1024), { contentType: "image/jpeg" }));
    await assertSucceeds(deleteObject(owner));
});
