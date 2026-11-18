const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_cinema_1";
const BOB_UID = "bob_cinema_2";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write sources or library", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("sources").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("library").get());

  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("sources").doc("src_1").set({
      userId: ALICE_UID,
      title: "Test Channel",
      url: "https://example.com/live.m3u8",
      sourceType: "CHANNEL",
      category: "Live TV",
      channelCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Owner can list empty user-scoped subcollections and CRUD MediaSource", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const sourcesRef = aliceDb.collection("users").doc(ALICE_UID).collection("sources");

  // Listing empty subcollection must succeed
  await assertSucceeds(sourcesRef.get());

  // Valid create (no @DocumentId `id` field, null optional fields omitted)
  await assertSucceeds(
    sourcesRef.doc("src_1").set({
      userId: ALICE_UID,
      title: "Open Cinema Feed",
      url: "https://example.com/movies.m3u",
      sourceType: "PLAYLIST_M3U",
      category: "Movies",
      logoUrl: "https://example.com/logo.png",
      channelCount: 25,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid partial update
  await assertSucceeds(
    sourcesRef.doc("src_1").update({
      title: "Open Cinema Feed HD",
      channelCount: 30,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable field mutation (createdAt)
  await assertFails(
    sourcesRef.doc("src_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field / @DocumentId `id` field on create
  await assertFails(
    sourcesRef.doc("src_2").set({
      id: "src_2",
      userId: ALICE_UID,
      title: "Bad Source",
      url: "https://example.com/bad.m3u8",
      sourceType: "CHANNEL",
      category: "Live",
      channelCount: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject invalid sourceType enum
  await assertFails(
    sourcesRef.doc("src_3").set({
      userId: ALICE_UID,
      title: "Invalid Type",
      url: "https://example.com/stream.mp4",
      sourceType: "PIRATE_STREAM",
      category: "Movies",
      channelCount: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(sourcesRef.doc("src_1").delete());
});

test("Owner can CRUD LibraryItem with watch progress and favorite toggle", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const libRef = aliceDb.collection("users").doc(ALICE_UID).collection("library");

  await assertSucceeds(libRef.get());

  await assertSucceeds(
    libRef.doc("item_1").set({
      userId: ALICE_UID,
      title: "Sintel (2010)",
      streamUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
      posterUrl: "https://example.com/sintel.jpg",
      sourceName: "Blender Open Movies",
      mediaType: "MOVIE",
      favorite: true,
      lastPositionMs: 12000,
      durationMs: 888000,
      lastWatchedAt: created,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    libRef.doc("item_1").update({
      lastPositionMs: 45000,
      favorite: false,
      lastWatchedAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Future timestamp must fail
  await assertFails(
    libRef.doc("item_1").update({
      lastWatchedAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user isolation blocks Bob from reading or writing Alice's subcollections", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const now = new Date(Date.now() - 5000);
    await ctx.firestore().collection("users").doc(ALICE_UID).collection("sources").doc("alice_src").set({
      userId: ALICE_UID,
      title: "Alice Channel",
      url: "https://example.com/alice.m3u8",
      sourceType: "CHANNEL",
      category: "Movies",
      channelCount: 0,
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("sources").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("sources").doc("alice_src").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("sources").doc("alice_src").update({
      title: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("sources").doc("alice_src").delete());
});
