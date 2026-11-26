package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.credentials.CredentialManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.ads.VynzoAdManager
import com.example.data.firebase.VynzoAuthManager
import com.example.data.firebase.VynzoFirebaseHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainActivityStartupTest {

  @get:Rule
  val composeTestRule = createEmptyComposeRule()

  @Before
  fun setUp() {
    VynzoFirebaseHelper.forceUninitializedForTest = false
  }

  @After
  fun tearDown() {
    VynzoFirebaseHelper.forceUninitializedForTest = false
  }

  private fun ensureTestFirebaseInitialized(context: Context) {
    if (FirebaseApp.getApps(context).isEmpty()) {
      runCatching { FirebaseApp.initializeApp(context) }.getOrNull()
        ?: FirebaseApp.initializeApp(
          context,
          FirebaseOptions.Builder()
            .setApplicationId("1:123456789012:android:abcdef1234567890")
            .setApiKey("AIzaSyTestKeyForUnitTestsOnly00000000000")
            .setProjectId("vynzo-unit-test-project")
            .build()
        )
    }
  }

  @Test
  fun `MainActivity launches splash and onboarding without crashing when Firebase is initialized`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    ensureTestFirebaseInitialized(context)

    ActivityScenario.launch(MainActivity::class.java).use {
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithText("VYNZO").assertIsDisplayed()
      composeTestRule.onNodeWithText("Connect. Share. Create.").assertIsDisplayed()

      // Advance past splash delay (1100ms) into onboarding tour
      composeTestRule.mainClock.advanceTimeBy(1500L)
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("onboarding_next_button").assertIsDisplayed()
    }
  }

  @Test
  fun `MainActivity shows graceful Firebase setup screen instead of crashing when Firebase config is missing`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    VynzoFirebaseHelper.forceUninitializedForTest = true
    FirebaseApp.clearInstancesForTest()

    val status = VynzoFirebaseHelper.diagnoseConfig(context)
    assertFalse(status.isFirebaseInitialized)
    assertTrue(status.hasFirestoreDatabaseId)

    ActivityScenario.launch(MainActivity::class.java).use {
      composeTestRule.waitForIdle()
      assertFalse(VynzoFirebaseHelper.isFirebaseReady(context))
      composeTestRule.onNodeWithText("Firebase Configuration Missing").assertIsDisplayed()
      composeTestRule.onNodeWithText("Firebase App ID").assertIsDisplayed()
      composeTestRule.onNodeWithText("Google Sign-In Client ID").assertIsDisplayed()
      composeTestRule.onNodeWithText("Firestore Database ID").assertIsDisplayed()
      composeTestRule.onNodeWithTag("firebase_retry_button").performScrollTo().assertIsDisplayed()
      composeTestRule.onNodeWithTag("firebase_preview_tour_button").performScrollTo().performClick()
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("onboarding_next_button").assertIsDisplayed()
    }
  }

  @Test
  fun `VynzoAuthManager and VynzoAdManager fail safely when Firebase is uninitialized`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    VynzoFirebaseHelper.forceUninitializedForTest = true
    val credManager = CredentialManager.create(context)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var unauthenticatedCalled = false
    VynzoAuthManager.attemptAutoSignIn(
      context = context,
      credentialManager = credManager,
      scope = scope,
      onAuthSuccess = {},
      onUnauthenticated = { unauthenticatedCalled = true }
    )
    assertTrue(unauthenticatedCalled)

    var authError: String? = null
    VynzoAuthManager.onGoogleSignInClicked(
      context = context,
      credentialManager = credManager,
      scope = scope,
      onAuthSuccess = {},
      onAuthError = { authError = it }
    )
    assertTrue(authError?.contains("Firebase") == true)

    VynzoAdManager.initialize(context, personalizedConsent = true)
    scope.cancel()
  }
}
