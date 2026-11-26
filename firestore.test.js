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
const ADMIN_EMAIL = "rahul11223344d@gmail.com";

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

const pastDate = () => new Date(Date.now() - 2000);
const futureDate = () => new Date(Date.now() + 60000);

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").where("status", "==", "active").get());
  await assertFails(unauth.collection("posts").where("visibility", "==", "public").get());
  await assertFails(unauth.collection("stories").where("visibility", "==", "public").get());
  await assertFails(
    unauth.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice",
      displayName: "Alice",
      status: "active",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Users: public profile create, partial update, shadow update block, and PII isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const created = pastDate();

  // Valid create
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice_creates",
      displayName: "Alice Lin",
      bio: "Visual storyteller",
      status: "active",
      followersCount: 0,
      followingCount: 0,
      postsCount: 0,
      verified: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject self-verification shadow update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice2",
      displayName: "Alice",
      status: "active",
      verified: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject invalid ID or @DocumentId injection
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      verified: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Valid partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      bio: "Updated bio",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob can read Alice's public profile & query active profiles
  await assertSucceeds(bobDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(bobDb.collection("users").where("status", "==", "active").get());
  // Unfiltered query on users fails
  await assertFails(bobDb.collection("users").get());

  // Private PII doc: owner-only
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("info").set({
      userId: ALICE_UID,
      email: "alice@example.com",
      themeMode: "dark",
      notifyLikes: true,
      updatedAt: pastDate(),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("private").doc("info").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("private").get());
});

test("Posts & Reels: create, filtered list, engagement update, and comments", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate();

  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorUsername: "alice_creates",
      mediaType: "image",
      mediaUrls: ["res://img_featured_city"],
      caption: "Sunset rooftop #vynzo",
      hashtags: ["vynzo"],
      visibility: "public",
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("posts").doc("post_future").set({
      authorId: ALICE_UID,
      authorUsername: "alice_creates",
      mediaType: "image",
      mediaUrls: ["res://img_featured_city"],
      visibility: "public",
      createdAt: futureDate(),
      updatedAt: futureDate(),
    })
  );

  // Bob can query public posts, cannot run unfiltered list
  await assertSucceeds(bobDb.collection("posts").where("visibility", "==", "public").get());
  await assertFails(bobDb.collection("posts").get());

  // Bob cannot hijack Alice's caption, but can update likesCount & create his like doc
  await assertFails(
    bobDb.collection("posts").doc("post_1").update({
      caption: "Hacked",
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("likes").doc(BOB_UID).set({
      userId: BOB_UID,
      createdAt: pastDate(),
    })
  );
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").update({
      likesCount: 1,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Comments
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("comments").doc("c_1").set({
      postId: "post_1",
      postAuthorId: ALICE_UID,
      authorId: BOB_UID,
      authorUsername: "bob",
      text: "Amazing shot!",
      hidden: false,
      createdAt: pastDate(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").collection("comments").where("hidden", "==", false).get()
  );
  // Post author can hide comment
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").collection("comments").doc("c_1").update({
      hidden: true,
    })
  );
});

test("Direct Messages: strictly restricted to participants", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext("eve_789").firestore();
  const chatId = "alice_123_bob_456";

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set({
      participantUids: [ALICE_UID, BOB_UID],
      lastMessageText: "Hey Bob!",
      lastSenderId: ALICE_UID,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).collection("messages").doc("m_1").set({
      chatId,
      participantUids: [ALICE_UID, BOB_UID],
      senderId: BOB_UID,
      messageType: "text",
      text: "Hey Alice!",
      deleted: false,
      createdAt: pastDate(),
    })
  );

  await assertFails(eveDb.collection("chats").doc(chatId).get());
  await assertFails(eveDb.collection("chats").doc(chatId).collection("messages").doc("m_1").get());
  await assertSucceeds(
    aliceDb.collection("chats").where("participantUids", "array-contains", ALICE_UID).get()
  );
});

test("Admin RBAC: blocks unverified email spoofing and non-admins, allows verified admin", async () => {
  const spoofAdminDb = testEnv.authenticatedContext("attacker", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  const realAdminDb = testEnv.authenticatedContext("real_admin", {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  // Spoofed email (email_verified: false) cannot write app_config or resolve reports
  await assertFails(
    spoofAdminDb.collection("app_config").doc("ads_settings").set({
      active: true,
      adsEnabled: false,
      nativeFeedInterval: 5,
      interstitialCooldownSec: 120,
      updatedAt: pastDate(),
    })
  );

  // Verified admin succeeds
  await assertSucceeds(
    realAdminDb.collection("app_config").doc("ads_settings").set({
      active: true,
      adsEnabled: true,
      nativeFeedInterval: 5,
      interstitialCooldownSec: 120,
      updatedAt: pastDate(),
    })
  );
});
