package com.example

import com.example.ads.VynzoAdManager
import com.example.data.model.Post
import com.example.data.model.UserProfile
import com.example.ui.theme.VynzoThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VynzoModelAndUiTest {

  @Test
  fun `userProfile toCreateMap excludes id and enforces lowercase username`() {
    val profile = UserProfile(
      id = "doc_123",
      userId = "uid_abc",
      username = "  AlexCreator  ",
      displayName = "Alex Studio",
      bio = "Visual artist",
      avatarUrl = "data:image/jpeg;base64,abc",
      verified = true
    )
    val map = profile.toCreateMap()
    assertFalse("Document ID must never be serialized inside Firestore create map", map.containsKey("id"))
    assertEquals("alexcreator", map["username"])
    assertEquals("active", map["status"])
    assertEquals(false, map["verified"])
    assertEquals(0L, map["followersCount"])
  }

  @Test
  fun `post toCreateMap caps hashtags and media and omits blank optional fields`() {
    val tags = (1..40).map { "tag$it" }
    val post = Post(
      id = "post_99",
      authorId = "uid_abc",
      authorUsername = "alex",
      mediaType = "carousel",
      mediaUrls = (1..15).map { "https://example.com/$it.jpg" },
      hashtags = tags,
      caption = "Sunset vibes",
      locationName = ""
    )
    val map = post.toCreateMap()
    assertFalse(map.containsKey("id"))
    assertFalse(map.containsKey("locationName"))
    assertEquals(10, (map["mediaUrls"] as List<*>).size)
    assertEquals(30, (map["hashtags"] as List<*>).size)
    assertEquals("public", map["visibility"])
  }

  @Test
  fun `theme mode wire conversion defaults to dark safely`() {
    assertEquals(VynzoThemeMode.LIGHT, VynzoThemeMode.fromWire("light"))
    assertEquals(VynzoThemeMode.SYSTEM, VynzoThemeMode.fromWire("system"))
    assertEquals(VynzoThemeMode.DARK, VynzoThemeMode.fromWire("unknown"))
  }

  @Test
  fun `admob test unit ids are non empty defaults`() {
    assertTrue(VynzoAdManager.bannerUnitId().startsWith("ca-app-pub-"))
    assertTrue(VynzoAdManager.interstitialUnitId().startsWith("ca-app-pub-"))
    assertTrue(VynzoAdManager.rewardedUnitId().startsWith("ca-app-pub-"))
    assertTrue(VynzoAdManager.nativeUnitId().startsWith("ca-app-pub-"))
  }
}
