package com.example.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.VynzoNativeFeedAdCard
import com.example.data.model.Post
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.ReportContentDialog
import com.example.ui.components.VynzoAvatar
import com.example.ui.components.VynzoFeedSkeleton
import com.example.ui.components.VynzoPostCard
import com.example.ui.components.VynzoWordmark
import com.example.ui.theme.LocalVynzoColors
import com.example.ui.viewmodel.VynzoSessionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  viewModel: VynzoSessionViewModel,
  onOpenMessages: () -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenCreateStory: () -> Unit,
  onOpenCreatePost: () -> Unit,
  onOpenStoryViewer: (authorId: String) -> Unit,
  onOpenUserProfile: (userId: String) -> Unit,
  onOpenComments: (postId: String, postAuthorId: String) -> Unit,
  onOpenExploreWithTag: (tag: String) -> Unit
) {
  val colors = LocalVynzoColors.current
  val scope = rememberCoroutineScope()

  val myProfileState by viewModel.myProfileState.collectAsStateWithLifecycle()
  val feedState by viewModel.publicFeedState.collectAsStateWithLifecycle()
  val stories by viewModel.storiesState.collectAsStateWithLifecycle()
  val following by viewModel.myFollowing.collectAsStateWithLifecycle()
  val bookmarks by viewModel.myBookmarks.collectAsStateWithLifecycle()
  val creators by viewModel.activeCreators.collectAsStateWithLifecycle()
  val notifications by viewModel.notifications.collectAsStateWithLifecycle()
  val chatThreads by viewModel.chatThreads.collectAsStateWithLifecycle()
  val adsConfig by viewModel.adsConfig.collectAsStateWithLifecycle()
  val privateInfo by viewModel.myPrivateInfo.collectAsStateWithLifecycle()

  val myProfile = (myProfileState as? UiState.Success)?.data
  val blockedSet = remember(privateInfo) { privateInfo?.blockedUids?.toSet() ?: emptySet() }
  val followingMap = remember(following) { following.associateBy { it.targetId } }
  val bookmarkedSet = remember(bookmarks) { bookmarks.map { it.postId }.toSet() }
  val creatorMap = remember(creators) { creators.associateBy { it.userId } }

  val unreadNotifCount = remember(notifications) { notifications.count { !it.read } }
  val unreadMsgCount = remember(chatThreads, viewModel.currentUserId) {
    chatThreads.sumOf { (it.unreadCounts[viewModel.currentUserId] ?: 0L).toInt() }
  }

  var feedTab by remember { mutableStateOf(0) } // 0 = For You, 1 = Following
  var isRefreshing by remember { mutableStateOf(false) }
  var reportTargetPost by remember { mutableStateOf<Post?>(null) }

  val listState = rememberLazyListState()
  val shouldLoadMore by remember {
    derivedStateOf {
      val total = listState.layoutInfo.totalItemsCount
      val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
      total > 4 && lastVisible >= total - 3
    }
  }
  LaunchedEffect(shouldLoadMore) {
    if (shouldLoadMore) viewModel.loadMoreFeed()
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Home Header Bar
    Surface(
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
      tonalElevation = 1.dp
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        VynzoWordmark(fontSize = 22)
        Spacer(Modifier.weight(1f))

        IconButton(
          onClick = onOpenNotifications,
          modifier = Modifier.testTag("home_notifications_button")
        ) {
          BadgedBox(
            badge = {
              if (unreadNotifCount > 0) {
                Badge { Text(unreadNotifCount.coerceAtMost(99).toString()) }
              }
            }
          ) {
            Icon(
              imageVector = Icons.Outlined.Notifications,
              contentDescription = "Notifications"
            )
          }
        }

        IconButton(
          onClick = onOpenMessages,
          modifier = Modifier.testTag("home_messages_button")
        ) {
          BadgedBox(
            badge = {
              if (unreadMsgCount > 0) {
                Badge { Text(unreadMsgCount.coerceAtMost(99).toString()) }
              }
            }
          ) {
            Icon(
              imageVector = Icons.Outlined.Forum,
              contentDescription = "Direct Messages"
            )
          }
        }
      }
    }

    PullToRefreshBox(
      isRefreshing = isRefreshing,
      onRefresh = {
        isRefreshing = true
        viewModel.refreshFeed()
        scope.launch {
          delay(600)
          isRefreshing = false
        }
      },
      modifier = Modifier.fillMaxSize()
    ) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
          state = listState,
          modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 620.dp),
          contentPadding = PaddingValues(bottom = 24.dp)
        ) {
          // 1. Stories Tray
          item(key = "stories_tray") {
            val distinctStoryAuthors = remember(stories, blockedSet) {
              stories
                .filter { it.authorId !in blockedSet }
                .groupBy { it.authorId }
                .values
                .map { it.first() }
            }
            val myHasStory = distinctStoryAuthors.any { it.authorId == viewModel.currentUserId }

            LazyRow(
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              item(key = "your_story") {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier
                    .width(72.dp)
                    .clickable {
                      if (myHasStory) onOpenStoryViewer(viewModel.currentUserId)
                      else onOpenCreateStory()
                    }
                    .testTag("your_story_button")
                ) {
                  Box(contentAlignment = Alignment.BottomEnd) {
                    VynzoAvatar(
                      avatarUrl = myProfile?.avatarUrl.orEmpty(),
                      displayName = myProfile?.displayName ?: "You",
                      size = 60.dp,
                      hasStoryRing = myHasStory
                    )
                    Surface(
                      shape = CircleShape,
                      color = MaterialTheme.colorScheme.primary,
                      border = BorderStroke(2.dp, MaterialTheme.colorScheme.background),
                      modifier = Modifier
                        .size(22.dp)
                        .clickable { onOpenCreateStory() }
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(
                          imageVector = Icons.Rounded.Add,
                          contentDescription = "Add story",
                          tint = MaterialTheme.colorScheme.onPrimary,
                          modifier = Modifier.size(14.dp)
                        )
                      }
                    }
                  }
                  Spacer(Modifier.height(5.dp))
                  Text(
                    text = if (myHasStory) "Your story" else "Add story",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }

              items(
                items = distinctStoryAuthors.filter { it.authorId != viewModel.currentUserId },
                key = { it.authorId }
              ) { story ->
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier
                    .width(72.dp)
                    .clickable { onOpenStoryViewer(story.authorId) }
                    .testTag("story_item_${story.authorId}")
                ) {
                  VynzoAvatar(
                    avatarUrl = story.authorAvatarUrl,
                    displayName = story.authorUsername,
                    size = 60.dp,
                    hasStoryRing = true
                  )
                  Spacer(Modifier.height(5.dp))
                  Text(
                    text = story.authorUsername,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }

          // 2. Feed Scope Chips
          item(key = "feed_tabs") {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              FilterChip(
                selected = feedTab == 0,
                onClick = { feedTab = 0 },
                label = { Text("For You") },
                leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("feed_tab_foryou")
              )
              FilterChip(
                selected = feedTab == 1,
                onClick = { feedTab = 1 },
                label = { Text("Following (${followingMap.size})") },
                modifier = Modifier.testTag("feed_tab_following")
              )
            }
          }

          // 3. Feed Content
          when (val state = feedState) {
            UiState.Loading -> {
              item(key = "loading_skeleton") { VynzoFeedSkeleton() }
            }
            is UiState.Error -> {
              item(key = "feed_error") {
                Surface(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                  shape = RoundedCornerShape(18.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                  Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                  ) {
                    Text(state.message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { viewModel.refreshFeed() }) {
                      Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(Modifier.width(6.dp))
                      Text("Retry")
                    }
                  }
                }
              }
            }
            is UiState.Success -> {
              val filteredPosts = state.data.filter { post ->
                post.authorId !in blockedSet &&
                  (feedTab == 0 || post.authorId == viewModel.currentUserId || followingMap.containsKey(post.authorId))
              }

              // Suggested creators strip if user isn't following many people yet
              val suggestedCreators = creators.filter {
                it.userId != viewModel.currentUserId &&
                  it.userId !in blockedSet &&
                  !followingMap.containsKey(it.userId)
              }.take(8)

              if (suggestedCreators.isNotEmpty() && (feedTab == 1 || filteredPosts.size < 3)) {
                item(key = "suggested_creators") {
                  SuggestedCreatorsStrip(
                    creators = suggestedCreators,
                    onOpenProfile = onOpenUserProfile,
                    onFollow = { target -> viewModel.toggleFollow(target, null) }
                  )
                }
              }

              if (filteredPosts.isEmpty()) {
                item(key = "empty_feed") {
                  Surface(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(16.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, colors.hairlineBorder)
                  ) {
                    Column(
                      modifier = Modifier.padding(28.dp),
                      horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Explore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                      )
                      Spacer(Modifier.height(12.dp))
                      Text(
                        text = if (feedTab == 1) "Your Following feed is quiet" else "Start the conversation on Vynzo",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                      )
                      Spacer(Modifier.height(6.dp))
                      Text(
                        text = if (feedTab == 1) {
                          "Follow creators from For You or Explore to see their latest posts and Reels here."
                        } else {
                          "Share your first photo, multi-image carousel, or Reel with the Vynzo community."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.mutedText,
                        textAlign = TextAlign.Center
                      )
                      Spacer(Modifier.height(16.dp))
                      Button(
                        onClick = onOpenCreatePost,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("empty_feed_create_button")
                      ) {
                        Text("Create First Post")
                      }
                    }
                  }
                }
              } else {
                val interval = adsConfig.nativeFeedInterval.toInt().coerceAtLeast(3)
                itemsIndexed(
                  items = filteredPosts,
                  key = { _, post -> post.id }
                ) { index, post ->
                  val isLiked by viewModel.postRepository
                    .observeIsPostLiked(post.id, viewModel.currentUserId)
                    .collectAsStateWithLifecycle(initialValue = false)

                  val authorProfile = creatorMap[post.authorId]
                  val followRelation = followingMap[post.authorId]

                  VynzoPostCard(
                    post = post,
                    isOwnPost = post.authorId == viewModel.currentUserId,
                    isLiked = isLiked,
                    isSaved = post.id in bookmarkedSet,
                    isFollowingAuthor = followRelation != null,
                    isVerifiedAuthor = authorProfile?.verified == true,
                    onAuthorClick = { onOpenUserProfile(post.authorId) },
                    onToggleFollow = {
                      val target = authorProfile ?: UserProfile(
                        userId = post.authorId,
                        username = post.authorUsername,
                        displayName = post.authorDisplayName.ifBlank { post.authorUsername },
                        avatarUrl = post.authorAvatarUrl
                      )
                      viewModel.toggleFollow(target, followRelation)
                    },
                    onToggleLike = { forceLike ->
                      if (!forceLike || !isLiked) {
                        viewModel.toggleLike(post, isLiked)
                      }
                    },
                    onCommentClick = { onOpenComments(post.id, post.authorId) },
                    onShareClick = { viewModel.sharePost(post) },
                    onToggleSave = { viewModel.toggleBookmark(post.id, post.id in bookmarkedSet) },
                    onHashtagClick = onOpenExploreWithTag,
                    onDeletePost = {
                      scope.launch {
                        viewModel.postRepository.deletePost(post.id, post.visibility == "public")
                          .onSuccess { viewModel.notifyMessage("Post deleted") }
                      }
                    },
                    onReportPost = { reportTargetPost = post }
                  )

                  if (adsConfig.adsEnabled && (index + 1) % interval == 0) {
                    VynzoNativeFeedAdCard(
                      adsEnabled = adsConfig.adsEnabled,
                      personalizedConsent = privateInfo?.adsConsentAccepted ?: true
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  reportTargetPost?.let { target ->
    ReportContentDialog(
      title = "Report @${target.authorUsername}'s post",
      onDismiss = { reportTargetPost = null },
      onSubmit = { reason, details ->
        viewModel.reportContent(
          targetType = if (target.mediaType == "reel") "reel" else "post",
          targetId = target.id,
          reason = reason,
          details = details
        )
        reportTargetPost = null
      }
    )
  }
}

@Composable
private fun SuggestedCreatorsStrip(
  creators: List<UserProfile>,
  onOpenProfile: (String) -> Unit,
  onFollow: (UserProfile) -> Unit
) {
  val colors = LocalVynzoColors.current
  Column(modifier = Modifier.padding(vertical = 8.dp)) {
    Text(
      text = "Suggested Creators",
      style = MaterialTheme.typography.titleSmall,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
    LazyRow(
      contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(creators, key = { it.userId }) { creator ->
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, colors.hairlineBorder),
          modifier = Modifier
            .width(148.dp)
            .clickable { onOpenProfile(creator.userId) }
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            VynzoAvatar(
              avatarUrl = creator.avatarUrl,
              displayName = creator.displayName,
              size = 54.dp,
              isOnline = creator.online
            )
            Spacer(Modifier.height(8.dp))
            Text(
              text = creator.displayName,
              style = MaterialTheme.typography.labelLarge,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "@${creator.username}",
              style = MaterialTheme.typography.bodySmall,
              color = colors.mutedText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            Button(
              onClick = { onFollow(creator) },
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Follow", style = MaterialTheme.typography.labelMedium)
            }
          }
        }
      }
    }
  }
}
