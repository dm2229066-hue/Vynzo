package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.example.ui.settings.InfoPageScreen
import com.example.ui.theme.VynzoTheme
import com.example.ui.theme.VynzoThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `app name and tagline match Vynzo branding`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("Vynzo", context.getString(R.string.app_name))
    assertEquals("Connect. Share. Create.", context.getString(R.string.tagline))
  }

  @Test
  fun `info page renders About Vynzo content`() {
    composeTestRule.setContent {
      VynzoTheme(themeMode = VynzoThemeMode.DARK) {
        InfoPageScreen(pageType = "about", onBack = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("About Vynzo").assertIsDisplayed()
    composeTestRule.onNodeWithText("Connect. Share. Create.").assertIsDisplayed()
  }
}
