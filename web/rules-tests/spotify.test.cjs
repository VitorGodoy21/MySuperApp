const { before, after, beforeEach, test } = require("node:test");
const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { doc, setDoc, getDoc, updateDoc } = require("firebase/firestore");

let env;
before(async () => {
    env = await initializeTestEnvironment({ projectId: "demo-mysuperapp-recipes" });
});
after(async () => { if (env) await env.cleanup(); });
beforeEach(async () => { await env.clearFirestore(); });

const database = (uid) => uid ? env.authenticatedContext(uid).firestore() : env.unauthenticatedContext().firestore();

function spotifyQr(overrides = {}) {
    return {
        userId: "alice", identifier: "Sala", type: "SPOTIFY", staticUrl: "https://baila.space/qr/?id=q1",
        redirectUrl: "", text: "", imageUrl: "", imagePath: "",
        spotifyUri: "spotify:playlist:37i9dQZF1DXcBWIGoYBM5M", spotifyName: "Today's Top Hits",
        spotifySubtitle: "Spotify • 50 músicas", spotifyImageUrl: "https://i.scdn.co/image/abc",
        spotifyDeviceId: "dev1", spotifyDeviceName: "Todo lugar", spotifyShuffle: true, ...overrides,
    };
}

test("owner can create and update a SPOTIFY qrcode with the spotify fields", async () => {
    const db = database("alice");
    const target = doc(db, "qrcodes/q1");
    await assertSucceeds(setDoc(target, spotifyQr()));
    await assertSucceeds(updateDoc(target, { spotifyUri: "spotify:track:4uLU6hMCjMI75M1A2tKUQC", spotifyShuffle: false }));
    await assertSucceeds(setDoc(doc(db, "qrcodes/q2"), spotifyQr({ type: "REDIRECT", redirectUrl: "https://x.y" })));
});

test("spotify fields are type- and size-validated", async () => {
    const db = database("alice");
    for (const invalid of [
        { spotifyUri: 1 }, { spotifyUri: "x".repeat(121) }, { spotifyName: null }, { spotifyShuffle: "yes" },
        { spotifyDeviceId: {} }, { spotifyDeviceName: "x".repeat(201) }, { spotifyImageUrl: 5 },
    ]) await assertFails(setDoc(doc(db, "qrcodes/q1"), spotifyQr(invalid)));
    await setDoc(doc(db, "qrcodes/q1"), spotifyQr());
    await assertFails(updateDoc(doc(db, "qrcodes/q1"), { spotifyShuffle: "yes" }));
});

test("spotify tokens and auth states are backend-only", async () => {
    for (const uid of ["alice", null]) {
        const db = database(uid);
        await assertFails(getDoc(doc(db, "spotify_tokens/alice")));
        await assertFails(setDoc(doc(db, "spotify_tokens/alice"), { accessToken: "x" }));
        await assertFails(getDoc(doc(db, "spotify_auth_states/s1")));
        await assertFails(setDoc(doc(db, "spotify_auth_states/s1"), { uid: "alice" }));
    }
});

test("users.spotify status is readable only by the account owner", async () => {
    await assertSucceeds(setDoc(doc(database("alice"), "users/alice"), { spotify: { connected: true, displayName: "Alice" } }));
    await assertSucceeds(getDoc(doc(database("alice"), "users/alice")));
    await assertFails(getDoc(doc(database("bob"), "users/alice")));
});
