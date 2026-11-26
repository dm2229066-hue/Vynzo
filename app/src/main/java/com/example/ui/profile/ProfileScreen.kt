package com.example.ui.profile

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Post
import com.example.data.model.StoryHighlight
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.media.MediaUtils
import com.example.ui.components.ReportContentDialog
import com.example.ui.components.VerifiedCreatorBadge
import com.example.ui.components.VynzoAvatar
import com.example.ui.components.VynzoMediaImage
import com.example.ui.theme.LocalVynzoColors
import com.example.ui.theme.VynzoBrandGradient
import com.example.ui.viewmodel.VynzoSessionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
  targetUserId: String,
  isOwnTab: Boolean,
  viewModel: VynzoSessionViewModel,
  onBack: (() -> Unit)? = null,
  onOpenSettings: () -> Unit,
  onOpenEditProfile: () -> Unit,
  onOpenConnections: (userId: String, tab: Int) -> Unit,
  onOpenChat: (chatId: String, peerId: String) -> Unit,
  onOpenComments: (postId: String, postAuthorId: String) -> Unit,
  onOpenSaved: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val colors = LocalVynzoColors.current

  val profile by if (isOwnTab) {
    val state by viewModel.myProfileState.collectAsStateWithLifecycle()
    remember(state) { mutableStateOf((state as? UiState.Success)?.data) }
  } else {
    val loaded by viewModel.userRepository.observeUserProfile(targetUserId)
      .collectAsStateWithLifecycle(initialValue = null)
    remember(loaded) { mutableStateOf(loaded) }
  }

  val posts by if (isOwnTab) {
    viewModel.myPosts.collectAsStateWithLifecycle()
  } else {
    viewModel.postRepository.observeUserPosts(targetUserId).collectAsStateWithLifecycle(initialValue = emptyList())
  }

  val taggedPosts by if (isOwnTab) {
    viewModel.myTaggedPosts.collectAsStateWithLifecycle()
  } else {
    viewModel.postRepository.observeTaggedPosts(targetUserId).collectAsStateWithLifecycle(initialValue = emptyList())
  }

  val highlights by if (isOwnTab) {
    viewModel.myHighlights.collectAsStateWithLifecycle()
  } else {
    viewModel.storyRepository.observeHighlights(targetUserId).collectAsStateWithLifecycle(initialValue = emptyList())
  }

  val bookmarks by viewModel.myBookmarks.collectAsStateWithLifecycle()
  val publicFeedState by viewModel.publicFeedState.collectAsStateWithLifecycle()
  val followRequests by viewModel.incomingFollowRequests.collectAsStateWithLifecycle()
  val followRelation by viewModel.userRepository
    .observeFollowRelation(viewModel.currentUserId, targetUserId)
    .collectAsStateWithLifecycle(initialValue = null)
  val privateInfo by viewModel.myPrivateInfo.collectAsStateWithLifecycle()

  val isBlocked = targetUserId in (privateInfo?.blockedUids ?: emptyList())
  val isRestricted = targetUserId in (privateInfo?.restrictedUids ?: emptyList())

  var selectedTab by remember { mutableIntStateOf(0) } // 0=Posts, 1=Reels, 2=Tagged, 3=Saved/Drafts
  var showMenu by remember { mutableStateOf(false) }
  var showReportUser by remember { mutableStateOf(false) }
  var showNewHighlightDialog by remember { mutableStateOf(false) }
  var selectedHighlight by remember { mutableStateOf<StoryHighlight?>(null) }

  val user = profile
  if (user == null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator()
    }
    return
  }

  val canViewContent = isOwnTab || !user.privateAccount || followRelation?.status == "accepted"
  val publicPosts = posts.filter { it.visibility != "draft" }
  val draftPosts = posts.filter { it.visibility == "draft" }
  val reelsOnly = publicPosts.filter { it.mediaType == "reel" || it.mediaType == "video" }
  val allPublicFeed = (publicFeedState as? UiState.Success)?.data ?: emptyList()
  val savedPostList = allPublicFeed.filter { p -> bookmarks.any { it.postId == p.id } }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    TopAppBar(
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (user.privateAccount) {
            Icon(Icons.Rounded.Lock, contentDescription = "Private account", modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
          }
          Text("@${user.username}", maxLines = 1, overflow = TextOverflow.Ellipsis)
          if (user.verified) {
            Spacer(Modifier.width(4.dp))
            VerifiedCreatorBadge()
          }
        }
      },
      navigationIcon = {
        if (onBack != null) {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        }
      },
      actions = {
        IconButton(
          onClick = {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_TEXT, "Follow @${user.username} on Vynzo!")
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Profile"))
          }
        ) {
          Icon(Icons.Rounded.Share, contentDescription = "Share profile")
        }
        if (isOwnTab) {
          IconButton(onClick = onOpenSaved) {
            Icon(Icons.Rounded.Bookmark, contentDescription = "Saved posts")
          }
          IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.testTag("profile_settings_button")
          ) {
            Icon(Icons.Rounded.Settings, contentDescription = "Settings")
          }
        } else {
          Box {
            IconButton(onClick = { showMenu = true }) {
              Icon(Icons.Rounded.MoreVert, contentDescription = "User actions")
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
              DropdownMenuItem(
                text = { Text(if (isBlocked) "Unblock user" else "Block user") },
                onClick = {
                  showMenu = false
                  scope.launch {
                    viewModel.userRepository.toggleBlockUser(user.userId, privateInfo?.blockedUids ?: emptyList())
                      .onSuccess { blocked -> viewModel.notifyMessage(if (blocked) "Blocked @${user.username}" else "Unblocked @${user.username}") }
                  }
                }
              )
              DropdownMenuItem(
                text = { Text(if (isRestricted) "Unrestrict user" else "Restrict user") },
                onClick = {
                  showMenu = false
                  scope.launch {
                    viewModel.userRepository.toggleRestrictUser(user.userId, privateInfo?.restrictedUids ?: emptyList())
                      .onSuccess { res -> viewModel.notifyMessage(if (res) "Restricted @${user.username}" else "Unrestricted @${user.username}") }
                  }
                }
              )
              DropdownMenuItem(
                text = { Text("Report user", color = MaterialTheme.colorScheme.error) },
                onClick = {
                  showMenu = false
                  showReportUser = true
                }
              )
            }
          }
        }
      }
    )

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 640.dp),
      contentPadding = PaddingValues(bottom = 28.dp)
    ) {
      // Cover Banner + Avatar + Bio + Stats
      item(key = "profile_header") {
        Column(modifier = Modifier.fillMaxWidth()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(120.dp)
              .background(VynzoBrandGradient)
          ) {
            if (user.coverUrl.isNotBlank()) {
              VynzoMediaImage(
                source = user.coverUrl,
                contentDescription = "Profile cover",
                modifier = Modifier.fillMaxSize()
              )
            }
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            VynzoAvatar(
              avatarUrl = user.avatarUrl,
              displayName = user.displayName,
              size = 80.dp,
              hasStoryRing = true,
              isOnline = user.online
            )
            Spacer(Modifier.width(16.dp))
            Row(
              modifier = Modifier.weight(1f),
              horizontalArrangement = Arrangement.SpaceEvenly
            ) {
              StatMetric(value = publicPosts.size.toLong().coerceAtLeast(user.postsCount), label = "Posts") {}
              StatMetric(value = user.followersCount, label = "Followers") {
                onOpenConnections(user.userId, 0)
              }
              StatMetric(value = user.followingCount, label = "Following") {
                onOpenConnections(user.userId, 1)
              }
            }
          }

          Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(user.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (user.bio.isNotBlank()) {
              Spacer(Modifier.height(4.dp))
              Text(user.bio, style = MaterialTheme.typography.bodyMedium)
            }
            if (user.website.isNotBlank()) {
              Spacer(Modifier.height(4.dp))
              Text(user.website, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
          }

          Spacer(Modifier.height(14.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            if (isOwnTab) {
              OutlinedButton(
                onClick = onOpenEditProfile,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).testTag("edit_profile_button")
              ) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Edit Profile")
              }
              OutlinedButton(
                onClick = onOpenSaved,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Rounded.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Saved (${bookmarks.size})")
              }
            } else {
              Button(
                onClick = { viewModel.toggleFollow(user, followRelation) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
              ) {
                Text(
                  when (followRelation?.status) {
                    "accepted" -> "Following"
                    "pending" -> "Requested"
                    else -> if (user.privateAccount) "Request Follow" else "Follow"
                  }
                )
              }
              OutlinedButton(
                onClick = {
                  val me = viewModel.currentProfileOrNull() ?: return@OutlinedButton
                  scope.launch {
                    viewModel.chatRepository.getOrCreateChatThread(me, user)
                      .onSuccess { chatId -> onOpenChat(chatId, user.userId) }
                  }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
              ) {
                Text("Message")
              }
            }
          }
        }
      }

      // Follow Requests Banner on own profile
      if (isOwnTab && followRequests.isNotEmpty()) {
        item(key = "follow_requests_banner") {
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text("Follow Requests (${followRequests.size})", style = MaterialTheme.typography.titleSmall)
              followRequests.take(3).forEach { req ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text("@${req.followerUsername}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                  IconButton(onClick = { viewModel.respondToFollowRequest(req, true) }) {
                    Icon(Icons.Rounded.Check, contentDescription = "Accept")
                  }
                  IconButton(onClick = { viewModel.respondToFollowRequest(req, false) }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Decline")
                  }
                }
              }
            }
          }
        }
      }

      // Story Highlights Row
      if (canViewContent && (isOwnTab || highlights.isNotEmpty())) {
        item(key = "highlights_row") {
          LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            if (isOwnTab) {
              item(key = "new_highlight") {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier.clickable { showNewHighlightDialog = true }
                ) {
                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, colors.hairlineBorder),
                    modifier = Modifier.size(60.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(Icons.Rounded.Add, contentDescription = "New Highlight")
                    }
                  }
                  Spacer(Modifier.height(4.dp))
                  Text("New", style = MaterialTheme.typography.bodySmall)
                }
              }
            }
            items(highlights, key = { it.id }) { h ->
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(68.dp).clickable { selectedHighlight = h }
              ) {
                VynzoAvatar(
                  avatarUrl = h.coverUrl,
                  displayName = h.title,
                  size = 60.dp,
                  hasStoryRing = true
                )
                Spacer(Modifier.height(4.dp))
                Text(h.title, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
              }
            }
          }
        }
      }

      if (!canViewContent) {
        item(key = "private_lock") {
          Surface(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
          ) {
            Column(
              modifier = Modifier.padding(28.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(40.dp))
              Spacer(Modifier.height(10.dp))
              Text("This Account is Private", style = MaterialTheme.typography.titleMedium)
              Text(
                "Follow @${user.username} to see their photos, Reels, and highlights.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedText
              )
            }
          }
        }
      } else {
        // Profile Content Tabs
        item(key = "profile_tabs") {
          val tabTitles = if (isOwnTab) {
            listOf("Posts", "Reels", "Tagged", "Drafts")
          } else {
            listOf("Posts", "Reels", "Tagged")
          }
          TabRow(selectedTabIndex = selectedTab.coerceAtMost(tabTitles.lastIndex)) {
            tabTitles.forEachIndexed { idx, label ->
              Tab(
                selected = selectedTab == idx,
                onClick = { selectedTab = idx },
                text = { Text(label, maxLines = 1) }
              )
            }
          }
        }

        val gridList: List<Post> = when (selectedTab) {
          1 -> reelsOnly
          2 -> taggedPosts
          3 -> draftPosts
          else -> publicPosts
        }

        if (gridList.isEmpty()) {
          item(key = "empty_tab_state") {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = when (selectedTab) {
                  1 -> "No Reels shared yet."
                  2 -> "No tagged posts yet."
                  3 -> "No saved drafts."
                  else -> "No posts yet."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.mutedText
              )
            }
          }
        } else if (selectedTab == 3 && isOwnTab) {
          items(draftPosts, key = { it.id }) { draft ->
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, colors.hairlineBorder)
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                VynzoMediaImage(
                  source = draft.coverUrl.ifBlank { draft.mediaUrls.firstOrNull().orEmpty() },
                  contentDescription = "Draft",
                  modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(draft.caption.ifBlank { "Untitled draft" }, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
                  Text(draft.mediaType.uppercase(), style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                }
                Button(
                  onClick = {
                    scope.launch {
                      viewModel.postRepository.publishDraft(draft.id)
                        .onSuccess { viewModel.notifyMessage("Draft published!") }
                    }
                  }
                ) {
                  Icon(Icons.Rounded.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Publish")
                }
              }
            }
          }
        } else {
          val rows = gridList.chunked(3)
          items(rows, key = { r -> r.joinToString("_") { it.id } }) { row ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              row.forEach { post ->
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onOpenComments(post.id, post.authorId) }
                ) {
                  VynzoMediaImage(
                    source = post.coverUrl.ifBlank { post.mediaUrls.firstOrNull().orEmpty() },
                    contentDescription = post.caption,
                    modifier = Modifier.fillMaxSize()
                  )
                  if (post.mediaType == "reel" || post.mediaType == "video") {
                    Icon(
                      imageVector = Icons.Rounded.PlayCircleOutline,
                      contentDescription = "Video",
                      tint = Color.White,
                      modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(18.dp)
                    )
                  }
                }
              }
              repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
          }
        }
      }
    }
  }

  if (showReportUser) {
    ReportContentDialog(
      title = "Report @${user.username}",
      onDismiss = { showReportUser = false },
      onSubmit = { reason, details ->
        viewModel.reportContent("user", user.userId, reason, details)
        showReportUser = false
      }
    )
  }

  if (showNewHighlightDialog) {
    CreateHighlightDialog(
      posts = publicPosts,
      onDismiss = { showNewHighlightDialog = false },
      onSave = { title, cover, mediaList ->
        scope.launch {
          viewModel.storyRepository.createOrUpdateHighlight(null, title, cover, mediaList)
            .onSuccess {
              viewModel.notifyMessage("Highlight created!")
              showNewHighlightDialog = false
            }
        }
      }
    )
  }

  selectedHighlight?.let { h ->
    AlertDialog(
      onDismissRequest = { selectedHighlight = null },
      title = { Text(h.title) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          val img = h.storyMediaUrls.firstOrNull() ?: h.coverUrl
          if (img.isNotBlank()) {
            VynzoMediaImage(
              source = img,
              contentDescription = h.title,
              modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(14.dp))
            )
          }
          Text("${h.storyMediaUrls.size.coerceAtLeast(1)} highlight item(s)")
        }
      },
      confirmButton = {
        TextButton(onClick = { selectedHighlight = null }) { Text("Close") }
      },
      dismissButton = {
        if (isOwnTab) {
          TextButton(
            onClick = {
              scope.launch {
                viewModel.storyRepository.deleteHighlight(h.id)
                  .onSuccess {
                    viewModel.notifyMessage("Highlight removed")
                    selectedHighlight = null
                  }
              }
            }
          ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        }
      }
    )
  }
}

@Composable
private fun StatMetric(value: Long, label: String, onClick: () -> Unit) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.clickable(onClick = onClick).padding(4.dp)
  ) {
    Text(text = value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text(text = label, style = MaterialTheme.typography.bodySmall, color = LocalVynzoColors.current.mutedText)
  }
}

@Composable
private fun CreateHighlightDialog(
  posts: List<Post>,
  onDismiss: () -> Unit,
  onSave: (title: String, coverUrl: String, mediaUrls: List<String>) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var title by remember { mutableStateOf("") }
  var coverUrl by remember { mutableStateOf(posts.firstOrNull()?.mediaUrls?.firstOrNull().orEmpty()) }
  val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null) {
      scope.launch {
        MediaUtils.decodeBitmapFromUri(context, uri, 600)?.let {
          coverUrl = MediaUtils.encodeBitmapToDataUri(it, 100_000)
        }
      }
    }
  }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("New Story Highlight") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it.take(40) },
          label = { Text("Highlight Name") },
          singleLine = true
        )
        OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
          Text("Choose Custom Cover Photo")
        }
      }
    },
    confirmButton = {
      TextButton(
        enabled = title.isNotBlank(),
        onClick = { onSave(title, coverUrl, listOfNotNull(coverUrl.takeIf { it.isNotBlank() })) }
      ) { Text("Create") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
  viewModel: VynzoSessionViewModel,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val myProfile = viewModel.currentProfileOrNull()

  var username by remember(myProfile) { mutableStateOf(myProfile?.username.orEmpty()) }
  var displayName by remember(myProfile) { mutableStateOf(myProfile?.displayName.orEmpty()) }
  var bio by remember(myProfile) { mutableStateOf(myProfile?.bio.orEmpty()) }
  var website by remember(myProfile) { mutableStateOf(myProfile?.website.orEmpty()) }
  var avatarUrl by remember(myProfile) { mutableStateOf(myProfile?.avatarUrl.orEmpty()) }
  var coverUrl by remember(myProfile) { mutableStateOf(myProfile?.coverUrl.orEmpty()) }
  var isSaving by remember { mutableStateOf(false) }

  val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null) {
      scope.launch {
        MediaUtils.decodeBitmapFromUri(context, uri, 512)?.let {
          avatarUrl = MediaUtils.encodeBitmapToDataUri(it, 95_000)
        }
      }
    }
  }

  val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null) {
      scope.launch {
        MediaUtils.decodeBitmapFromUri(context, uri, 720)?.let {
          coverUrl = MediaUtils.encodeBitmapToDataUri(it, 110_000)
        }
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Edit Profile") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      VynzoAvatar(
        avatarUrl = avatarUrl,
        displayName = displayName,
        size = 86.dp,
        hasStoryRing = true,
        onClick = { avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
      )
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = { avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
          Text("Change Photo")
        }
        OutlinedButton(onClick = { coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
          Text("Change Cover")
        }
      }

      OutlinedTextField(
        value = username,
        onValueChange = { username = it.lowercase().replace(" ", "").take(30) },
        label = { Text("Username") },
        prefix = { Text("@") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )
      OutlinedTextField(
        value = displayName,
        onValueChange = { displayName = it.take(50) },
        label = { Text("Display Name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )
      OutlinedTextField(
        value = bio,
        onValueChange = { bio = it.take(200) },
        label = { Text("Bio") },
        maxLines = 3,
        modifier = Modifier.fillMaxWidth()
      )
      OutlinedTextField(
        value = website,
        onValueChange = { website = it.take(200) },
        label = { Text("Website Link") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )

      Button(
        enabled = !isSaving,
        onClick = {
          isSaving = true
          scope.launch {
            viewModel.userRepository.updateProfile(
              username = username,
              displayName = displayName,
              bio = bio,
              website = website,
              avatarUrl = avatarUrl.takeIf { it.isNotBlank() },
              coverUrl = coverUrl.takeIf { it.isNotBlank() }
            ).onSuccess {
              viewModel.notifyMessage("Profile updated!")
              onBack()
            }.onFailure {
              viewModel.notifyMessage(it.localizedMessage ?: "Update failed")
            }
            isSaving = false
          }
        },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().height(52.dp)
      ) {
        Text(if (isSaving) "Saving..." else "Save Changes")
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionsScreen(
  userId: String,
  initialTab: Int,
  viewModel: VynzoSessionViewModel,
  onBack: () -> Unit,
  onOpenUserProfile: (String) -> Unit
) {
  var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
  val followers by viewModel.userRepository.observeFollowers(userId).collectAsStateWithLifecycle(initialValue = emptyList())
  val following by viewModel.userRepository.observeFollowing(userId).collectAsStateWithLifecycle(initialValue = emptyList())
  val myFollowing by viewModel.myFollowing.collectAsStateWithLifecycle()
  val myFollowingIds = remember(myFollowing) { myFollowing.map { it.targetId }.toSet() }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Connections") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { innerPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      TabRow(selectedTabIndex = selectedTab) {
        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Followers (${followers.size})") })
        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Following (${following.size})") })
      }
      val list = if (selectedTab == 0) followers else following
      LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(list, key = { it.id }) { rel ->
          val targetUid = if (selectedTab == 0) rel.followerId else rel.targetId
          val uname = if (selectedTab == 0) rel.followerUsername else rel.targetUsername
          val avatar = if (selectedTab == 0) rel.followerAvatarUrl else rel.targetAvatarUrl
          val isMutual = targetUid in myFollowingIds
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onOpenUserProfile(targetUid) }
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            VynzoAvatar(avatarUrl = avatar, displayName = uname, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("@$uname", style = MaterialTheme.typography.titleSmall)
              if (isMutual && targetUid != viewModel.currentUserId) {
                Text("Mutual connection", style = MaterialTheme.typography.bodySmall, color = LocalVynzoColors.current.accentCyan)
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPostsScreen(
  viewModel: VynzoSessionViewModel,
  onBack: () -> Unit,
  onOpenComments: (postId: String, postAuthorId: String) -> Unit
) {
  val bookmarks by viewModel.myBookmarks.collectAsStateWithLifecycle()
  val feedState by viewModel.publicFeedState.collectAsStateWithLifecycle()
  val allPosts = (feedState as? UiState.Success)?.data ?: emptyList()
  val savedPosts = allPosts.filter { p -> bookmarks.any { it.postId == p.id } }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Saved Bookmarks (${savedPosts.size})") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { innerPadding ->
    if (savedPosts.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
        Text("No saved posts yet. Tap the bookmark icon on any post!")
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        val rows = savedPosts.chunked(3)
        items(rows) { row ->
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row.forEach { post ->
              Box(
                modifier = Modifier
                  .weight(1f)
                  .aspectRatio(1f)
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { onOpenComments(post.id, post.authorId) }
              ) {
                VynzoMediaImage(
                  source = post.coverUrl.ifBlank { post.mediaUrls.firstOrNull().orEmpty() },
                  contentDescription = post.caption,
                  modifier = Modifier.fillMaxSize()
                )
              }
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
          }
        }
      }
    }
  }
}
