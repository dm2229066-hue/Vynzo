package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.unwrapFirestoreError
import com.example.data.model.AdminConfig
import com.example.data.model.AppNotification
import com.example.data.model.Bookmark
import com.example.data.model.ChatThread
import com.example.data.model.FollowRelation
import com.example.data.model.HashtagEntry
import com.example.data.model.Post
import com.example.data.model.Story
import com.example.data.model.StoryHighlight
import com.example.data.model.UiState
import com.example.data.model.UserPrivateInfo
import com.example.data.model.UserProfile
import com.example.data.repository.AdminRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.PostRepository
import com.example.data.repository.StoryRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.VynzoPreferencesRepository
import com.example.ui.theme.VynzoThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class VynzoSessionViewModel(
  val currentUserId: String,
  val userRepository: UserRepository,
  val postRepository: PostRepository,
  val storyRepository: StoryRepository,
  val chatRepository: ChatRepository,
  val notificationRepository: NotificationRepository,
  val adminRepository: AdminRepository,
  val prefsRepository: VynzoPreferencesRepository
) : ViewModel() {

  private val _feedLimit = MutableStateFlow(20L)
  private val _isAdmin = MutableStateFlow(false)
  val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

  private val _snackEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val snackEvents = _snackEvents.asSharedFlow()

  val myProfileState: StateFlow<UiState<UserProfile?>> =
    userRepository.observeUserProfile(currentUserId)
      .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
      .catch { e ->
        val root = unwrapFirestoreError(e)
        emit(UiState.Error(root.message ?: "Failed to load profile"))
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val myPrivateInfo: StateFlow<UserPrivateInfo?> =
    userRepository.observePrivateInfo(currentUserId)
      .catch { _ -> emit(null) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

  val publicFeedState: StateFlow<UiState<List<Post>>> =
    _feedLimit.flatMapLatest { limit -> postRepository.observePublicFeed(limit) }
      .map<List<Post>, UiState<List<Post>>> { UiState.Success(it) }
      .catch { e ->
        val root = unwrapFirestoreError(e)
        emit(UiState.Error(root.message ?: "Failed to load feed"))
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val reelsFeedState: StateFlow<UiState<List<Post>>> =
    postRepository.observeReelsFeed(30L)
      .map<List<Post>, UiState<List<Post>>> { UiState.Success(it) }
      .catch { e ->
        val root = unwrapFirestoreError(e)
        emit(UiState.Error(root.message ?: "Failed to load Reels"))
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val storiesState: StateFlow<List<Story>> =
    storyRepository.observeActiveStories()
      .catch { _ -> emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val myHighlights: StateFlow<List<StoryHighlight>> =
    storyRepository.observeHighlights(currentUserId)
      .catch { _ -> emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val activeCreators: StateFlow<List<UserProfile>> =
    userRepository.observeActiveCreators(40L)
      .catch { _ -> emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val myFollowing: StateFlow<List<FollowRelation>> =
    userRepository.observeFollowing(currentUserId)
      .catch { _ -> emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val myFollowers: StateFlow<List<FollowRelation>> =
    userRepository.observeFollowers(currentUserId)
      .catch { _ -> emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val incomingFollowRequests: StateFlow<List<FollowRelation>> =
    userRepository.observeIncomingFollowRequests(currentUserId)
      .catch { _ -> emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val myBookmarks: StateFlow<List<Bookmark>> =
    postRepository.observeBookmarks(currentUserId)
      .catch { _ -> emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val chatThreads: StateFlow<List<ChatThread>> =
    chatRepository.observeChatThreads(currentUserId)
      .catch { _ -> emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val notifications: StateFlow<List<AppNotification>> =
    notificationRepository.observeNotifications(currentUserId)
      .catch { _ -> emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val trendingHashtags: StateFlow<List<HashtagEntry>> =
    postRepository.observeTrendingHashtags()
      .catch { _ -> emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val adsConfig: StateFlow<AdminConfig> =
    adminRepository.observeAdsConfig()
      .catch { _ -> emit(AdminConfig()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AdminConfig())

  val myPosts: StateFlow<List<Post>> =
    postRepository.observeUserPosts(currentUserId)
      .catch { _ -> emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val myTaggedPosts: StateFlow<List<Post>> =
    postRepository.observeTaggedPosts(currentUserId)
      .catch { _ -> emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  init {
    viewModelScope.launch {
      _isAdmin.value = adminRepository.checkIsAdmin()
      userRepository.setOnlineStatus(true)
    }
  }

  fun notifyMessage(msg: String) {
    _snackEvents.tryEmit(msg)
  }

  fun loadMoreFeed() {
    _feedLimit.value = (_feedLimit.value + 15L).coerceAtMost(100L)
  }

  fun refreshFeed() {
    val current = _feedLimit.value
    _feedLimit.value = if (current == 20L) 21L else 20L
  }

  fun setThemeMode(mode: VynzoThemeMode) {
    viewModelScope.launch {
      prefsRepository.setThemeMode(mode)
      userRepository.updatePrivatePreferences(mapOf("themeMode" to mode.wireValue))
    }
  }

  fun setAdsConsent(accepted: Boolean) {
    viewModelScope.launch {
      prefsRepository.setAdsConsentAccepted(accepted)
      userRepository.updatePrivatePreferences(mapOf("adsConsentAccepted" to accepted))
    }
  }

  fun currentProfileOrNull(): UserProfile? =
    (myProfileState.value as? UiState.Success)?.data

  fun toggleLike(post: Post, currentlyLiked: Boolean) {
    val actor = currentProfileOrNull() ?: return
    viewModelScope.launch {
      postRepository.toggleLike(post, actor, currentlyLiked)
        .onFailure { notifyMessage(it.localizedMessage ?: "Unable to update like") }
    }
  }

  fun toggleBookmark(postId: String, currentlySaved: Boolean, collectionName: String = "All Saved") {
    viewModelScope.launch {
      postRepository.toggleBookmark(postId, collectionName, currentlySaved)
        .onSuccess { saved ->
          notifyMessage(if (saved) "Saved to $collectionName" else "Removed from saved")
        }
        .onFailure { notifyMessage(it.localizedMessage ?: "Failed to update bookmark") }
    }
  }

  fun sharePost(post: Post) {
    val actor = currentProfileOrNull() ?: return
    viewModelScope.launch {
      postRepository.incrementShare(post, actor)
    }
  }

  fun toggleFollow(target: UserProfile, existingRelation: FollowRelation?) {
    val me = currentProfileOrNull() ?: return
    viewModelScope.launch {
      if (existingRelation == null) {
        userRepository.followOrRequestUser(me, target)
          .onSuccess { status ->
            notifyMessage(if (status == "pending") "Follow request sent" else "Following @${target.username}")
          }
          .onFailure { notifyMessage(it.localizedMessage ?: "Follow failed") }
      } else {
        userRepository.unfollowUser(target.userId, existingRelation.status == "accepted")
          .onSuccess { notifyMessage("Unfollowed @${target.username}") }
          .onFailure { notifyMessage(it.localizedMessage ?: "Unfollow failed") }
      }
    }
  }

  fun respondToFollowRequest(relation: FollowRelation, accept: Boolean) {
    viewModelScope.launch {
      userRepository.respondToFollowRequest(relation, accept)
        .onSuccess { notifyMessage(if (accept) "Accepted @${relation.followerUsername}" else "Request removed") }
        .onFailure { notifyMessage(it.localizedMessage ?: "Action failed") }
    }
  }

  fun reportContent(targetType: String, targetId: String, reason: String, details: String = "") {
    viewModelScope.launch {
      postRepository.submitReport(targetType, targetId, reason, details)
        .onSuccess { notifyMessage("Report submitted to Vynzo Trust & Safety") }
        .onFailure { notifyMessage(it.localizedMessage ?: "Failed to submit report") }
    }
  }

  fun markAllNotificationsRead() {
    val list = notifications.value
    if (list.none { !it.read }) return
    viewModelScope.launch {
      notificationRepository.markAllRead(list)
    }
  }
}
