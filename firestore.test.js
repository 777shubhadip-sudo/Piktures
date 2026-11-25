const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

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

test("Unauthenticated user: cannot read or write user profile or synced items", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("synced_items").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      themeMode: "DARK",
      gridColumns: 4,
      relockTimeout: "IMMEDIATE",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("synced_items").doc("media_1").set({
      userId: ALICE_UID,
      mediaKey: "1",
      displayName: "sunset.jpg",
      bucketName: "Camera",
      mediaType: "IMAGE",
      favorite: true,
      sizeBytes: 204800,
      dateTakenMs: 1700000000000,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated owner: valid UserCloudProfile create, partial update, and security bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Mirror Kotlin UserCloudProfile create payload (no @DocumentId `id` field)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      themeMode: "DARK",
      gridColumns: 4,
      relockTimeout: "IMMEDIATE",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Mirror Kotlin partial update payload (createdAt preserved)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      themeMode: "LIGHT",
      gridColumns: 3,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field (Shadow Update Test)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject out-of-range gridColumns
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      gridColumns: 99,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future tampered timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      themeMode: "SYSTEM",
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Authenticated owner: valid SyncedMediaItem create, partial update, list query, and delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Empty subcollection list query succeeds without undefined property error
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("synced_items").get()
  );

  // Mirror Kotlin SyncedMediaItem create payload
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("synced_items").doc("media_42").set({
      userId: ALICE_UID,
      mediaKey: "42",
      displayName: "IMG_2026_01.jpg",
      bucketName: "Camera",
      mediaType: "IMAGE",
      favorite: true,
      note: "Mountain sunrise",
      sizeBytes: 4120000,
      dateTakenMs: 1730000000000,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Mirror Kotlin note partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("synced_items").doc("media_42").update({
      note: "Golden hour ridge",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Mirror Kotlin collection list query
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("synced_items")
      .orderBy("updatedAt", "desc")
      .get()
  );

  // Reject unexpected @DocumentId `id` field on create
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("synced_items").doc("media_99").set({
      id: "media_99",
      userId: ALICE_UID,
      mediaKey: "99",
      displayName: "bad.jpg",
      bucketName: "Camera",
      mediaType: "IMAGE",
      favorite: true,
      sizeBytes: 1000,
      dateTakenMs: 1730000000000,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject oversized note (> 500 chars)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("synced_items").doc("media_42").update({
      note: "x".repeat(501),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable mediaKey change
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("synced_items").doc("media_42").update({
      mediaKey: "999",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Owner delete succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("synced_items").doc("media_42").delete()
  );
});

test("Cross-user isolation: Alice cannot read, list, write, or delete Bob's profile or synced items", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    const created = new Date(Date.now() - 10000);
    await adminDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      email: "bob@example.com",
      themeMode: "DARK",
      gridColumns: 4,
      relockTimeout: "IMMEDIATE",
      createdAt: created,
      updatedAt: created,
    });
    await adminDb.collection("users").doc(BOB_UID).collection("synced_items").doc("media_7").set({
      userId: BOB_UID,
      mediaKey: "7",
      displayName: "bob_secret.mp4",
      bucketName: "Downloads",
      mediaType: "VIDEO",
      favorite: true,
      sizeBytes: 987654,
      dateTakenMs: 1730000000000,
      createdAt: created,
      updatedAt: created,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).update({ themeMode: "LIGHT" }));
  await assertFails(aliceDb.collection("users").doc(BOB_UID).delete());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("synced_items").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("synced_items").doc("media_7").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("synced_items").doc("media_7").update({ note: "Hijacked" })
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("synced_items").doc("media_7").delete()
  );
});
