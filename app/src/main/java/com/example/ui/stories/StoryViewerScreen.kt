package com.example.ui.stories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserProfile
import com.example.ui.components.VynzoAvatar
import com.example.ui.components.VynzoMediaImage
import com.example.ui.components.VynzoVideoPlayer
import com.example.ui.components.formatRelativeTime
import com.example.ui.viewmodel.VynzoSessionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StoryViewerScreen(
  authorId: String,
  viewModel: VynzoSessionViewModel,
  onClose: () -> Unit,
  onOpenProfile: (String) -> Unit
) {
  val scope = rememberCoroutineScope()
  val allStories by viewModel.storiesState.collectAsStateWithLifecycle()
  val authorStories = remember(allStories, authorId) {
    allStories.filter { it.authorId == authorId }.sortedBy { it.createdAt?.seconds ?: 0L }
  }
  val myProfile = viewModel.currentProfileOrNull()
  val creators by viewModel.activeCreators.collectAsStateWithLifecycle()

  var currentIndex by remember { mutableIntStateOf(0) }
  var progress by remember { mutableFloatStateOf(0f) }
  var isPaused by remember { mutableStateOf(false) }
  var replyText by remember { mutableStateOf("") }
  var showViewersDialog by remember { mutableStateOf(false) }
  var showHighlightDialog by remember { mutableStateOf(false) }
  var highlightTitle by remember { mutableStateOf("Highlights") }

  if (authorStories.isEmpty()) {
    Box(
      modifier = Modifier.fillMaxSize().background(Color.Black),
      contentAlignment = Alignment.Center
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No active stories", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onClose) { Text("Close", color = Color.White) }
      }
    }
    return
  }

  val safeIndex = currentIndex.coerceIn(0, authorStories.lastIndex)
  val story = authorStories[safeIndex]
  val isOwnStory = story.authorId == viewModel.currentUserId

  val viewers by viewModel.storyRepository.observeStoryViews(story.id)
    .collectAsStateWithLifecycle(initialValue = emptyList())

  // Record view when story appears
  LaunchedEffect(story.id) {
    progress = 0f
    myProfile?.let { viewModel.storyRepository.recordStoryView(story, it) }
  }

  // Story timer loop
  LaunchedEffect(safeIndex, isPaused, showViewersDialog, showHighlightDialog) {
    while (progress < 1f) {
      delay(50)
      if (!isPaused && !showViewersDialog && !showHighlightDialog) {
        progress += 0.01f // ~5 seconds per story
      }
    }
    if (safeIndex < authorStories.lastIndex) {
      currentIndex = safeIndex + 1
      progress = 0f
    } else {
      onClose()
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
      .statusBarsPadding()
      .navigationBarsPadding()
      .imePadding()
      .pointerInput(safeIndex, authorStories.size) {
        detectTapGestures(
          onPress = {
            isPaused = true
            tryAwaitRelease()
            isPaused = false
          },
          onTap = { offset ->
            if (offset.x < size.width * 0.35f) {
              if (safeIndex > 0) {
                currentIndex = safeIndex - 1
                progress = 0f
              }
            } else {
              if (safeIndex < authorStories.lastIndex) {
                currentIndex = safeIndex + 1
                progress = 0f
              } else {
                onClose()
              }
            }
          }
        )
      }
  ) {
    // Story Media
    if (story.mediaType == "video") {
      VynzoVideoPlayer(
        videoUri = story.mediaUrl,
        isPlaying = !isPaused,
        isMuted = false,
        modifier = Modifier.fillMaxSize()
      )
    } else {
      VynzoMediaImage(
        source = story.mediaUrl,
        contentDescription = story.caption.ifBlank { "Story" },
        modifier = Modifier.fillMaxSize()
      )
    }

    // Top Progress & Header
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
        .align(Alignment.TopCenter)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        authorStories.forEachIndexed { idx, _ ->
          val segProgress = when {
            idx < safeIndex -> 1f
            idx == safeIndex -> progress.coerceIn(0f, 1f)
            else -> 0f
          }
          LinearProgressIndicator(
            progress = { segProgress },
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.35f),
            modifier = Modifier
              .weight(1f)
              .height(3.dp)
              .clip(RoundedCornerShape(2.dp))
          )
        }
      }

      Spacer(Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        VynzoAvatar(
          avatarUrl = story.authorAvatarUrl,
          displayName = story.authorUsername,
          size = 36.dp,
          onClick = { onOpenProfile(story.authorId) }
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "@${story.authorUsername}",
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = formatRelativeTime(story.createdAt?.seconds),
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.bodySmall
          )
        }
        if (isOwnStory) {
          IconButton(onClick = { showHighlightDialog = true }) {
            Icon(Icons.Rounded.BookmarkAdd, contentDescription = "Pin to Highlight", tint = Color.White)
          }
        }
        IconButton(onClick = onClose) {
          Icon(Icons.Rounded.Close, contentDescription = "Close story", tint = Color.White)
        }
      }
    }

    // Bottom Controls (Caption + Reactions + Reply / Viewers)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
        .background(Color.Black.copy(alpha = 0.45f))
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      if (story.caption.isNotBlank()) {
        Text(
          text = story.caption,
          color = Color.White,
          style = MaterialTheme.typography.bodyLarge
        )
      }

      if (isOwnStory) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color.White.copy(alpha = 0.16f),
          modifier = Modifier.clickable { showViewersDialog = true }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Rounded.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("${viewers.size} viewers", color = Color.White, style = MaterialTheme.typography.labelLarge)
          }
        }
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          listOf("❤️", "🔥", "😂", "😍", "👏").forEach { emoji ->
            Text(
              text = emoji,
              fontSize = 24.sp,
              modifier = Modifier
                .clickable {
                  val viewer = myProfile ?: return@clickable
                  scope.launch {
                    viewModel.storyRepository.recordStoryView(story, viewer, reaction = emoji)
                    viewModel.notifyMessage("Reacted $emoji to @${story.authorUsername}'s story")
                  }
                }
                .padding(4.dp)
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = replyText,
            onValueChange = { replyText = it.take(300) },
            placeholder = { Text("Reply to @${story.authorUsername}...", color = Color.White.copy(alpha = 0.7f)) },
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedBorderColor = Color.White,
              unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(24.dp),
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          Spacer(Modifier.width(8.dp))
          IconButton(
            enabled = replyText.isNotBlank() && myProfile != null,
            onClick = {
              val me = myProfile ?: return@IconButton
              val peer = creators.firstOrNull { it.userId == story.authorId }
                ?: UserProfile(userId = story.authorId, username = story.authorUsername, displayName = story.authorUsername)
              scope.launch {
                val chatId = viewModel.chatRepository.getOrCreateChatThread(me, peer).getOrNull()
                if (chatId != null) {
                  val sortedUids = listOf(me.userId, peer.userId).sorted()
                  viewModel.chatRepository.sendMessage(
                    chatId = chatId,
                    participantUids = sortedUids,
                    sender = me,
                    peerId = peer.userId,
                    messageType = "text",
                    text = "Replied to your story: $replyText"
                  )
                  replyText = ""
                  viewModel.notifyMessage("Reply sent!")
                }
              }
            }
          ) {
            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send reply", tint = Color.White)
          }
        }
      }
    }
  }

  if (showViewersDialog) {
    AlertDialog(
      onDismissRequest = { showViewersDialog = false },
      title = { Text("Story Viewers (${viewers.size})") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          if (viewers.isEmpty()) {
            Text("No views yet.")
          } else {
            viewers.forEach { v ->
              Row(verticalAlignment = Alignment.CenterVertically) {
                VynzoAvatar(avatarUrl = v.viewerAvatarUrl, displayName = v.viewerUsername, size = 32.dp)
                Spacer(Modifier.width(8.dp))
                Text("@${v.viewerUsername}", modifier = Modifier.weight(1f))
                if (v.reaction.isNotBlank()) Text(v.reaction, fontSize = 18.sp)
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showViewersDialog = false }) { Text("Close") }
      }
    )
  }

  if (showHighlightDialog) {
    AlertDialog(
      onDismissRequest = { showHighlightDialog = false },
      title = { Text("Pin to Story Highlight") },
      text = {
        OutlinedTextField(
          value = highlightTitle,
          onValueChange = { highlightTitle = it.take(40) },
          label = { Text("Highlight Title") },
          singleLine = true
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            scope.launch {
              viewModel.storyRepository.createOrUpdateHighlight(
                highlightId = null,
                title = highlightTitle,
                coverUrl = story.mediaUrl,
                storyMediaUrls = listOf(story.mediaUrl)
              ).onSuccess {
                viewModel.notifyMessage("Added to profile highlights!")
                showHighlightDialog = false
              }
            }
          }
        ) { Text("Save Highlight") }
      },
      dismissButton = {
        TextButton(onClick = { showHighlightDialog = false }) { Text("Cancel") }
      }
    )
  }
}
