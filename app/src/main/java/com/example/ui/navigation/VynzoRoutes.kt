package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface VynzoRoute {
  // 5 Main Bottom Navigation Tabs
  @Serializable data object Home : VynzoRoute
  @Serializable data object Explore : VynzoRoute
  @Serializable data object Create : VynzoRoute
  @Serializable data object Reels : VynzoRoute
  @Serializable data object Profile : VynzoRoute

  // Detail & Secondary Screens
  @Serializable data class UserProfile(val userId: String) : VynzoRoute
  @Serializable data class Comments(val postId: String, val postAuthorId: String) : VynzoRoute
  @Serializable data object ChatList : VynzoRoute
  @Serializable data class ChatDetail(val chatId: String, val peerId: String) : VynzoRoute
  @Serializable data object Notifications : VynzoRoute
  @Serializable data object Settings : VynzoRoute
  @Serializable data object EditProfile : VynzoRoute
  @Serializable data class Connections(val userId: String, val initialTab: Int = 0) : VynzoRoute
  @Serializable data object SavedPosts : VynzoRoute
  @Serializable data class StoryViewer(val authorId: String) : VynzoRoute
  @Serializable data object AdminDashboard : VynzoRoute
  @Serializable data class InfoPage(val pageType: String) : VynzoRoute // help, about, terms, privacy
}
