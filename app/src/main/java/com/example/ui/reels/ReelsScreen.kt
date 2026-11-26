package com.example.ui.reels

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Post
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.ReportContentDialog
import com.example.ui.components.VynzoAvatar
import com.example.ui.components.VynzoMediaImage
import com.example.ui.components.VynzoVideoPlayer
import com.example.ui.theme.VynzoRose
import com.example.ui.viewmodel.VynzoSessionViewModel
import kotlinx.coroutines.delay

@Composable
fun ReelsScreen(
  viewModel: VynzoSessionViewModel,
  onOpenCreateReel: () -> Unit,
  onOpenUserProfile: (String) -> Unit,
  onOpenComments: (postId: String, postAuthorId: String) -> Unit
) {
  val context = LocalContext.current
  val reelsState by viewModel.reelsFeedState.collectAsStateWithLifecycle()
  val following by viewModel.myFollowing.collectAsStateWithLifecycle()
  val bookmarks by viewModel.myBookmarks.collectAsStateWithLifecycle()
  val creators by viewModel.activeCreators.collectAsStateWithLifecycle()

  val followingMap = remember(following) { following.associateBy { it.targetId } }
  val bookmarkedSet = remember(bookmarks) { bookmarks.map { it.postId }.toSet() }
  val creatorMap = remember(creators) { creators.associateBy { it.userId } }

  var isMuted by remember { mutableStateOf(false) }
  var reportTarget by remember { mutableStateOf<Post?>(null) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    when (val state = reelsState) {
      UiState.Loading -> {
        CircularProgressIndicator(
          modifier = Modifier.align(Alignment.Center),
          color = Color.White
        )
      }
      is UiState.Error -> {
        Column(
          modifier = Modifier.align(Alignment.Center).padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(state.message, color = Color.White, textAlign = TextAlign.Center)
        }
      }
      is UiState.Success -> {
        val reels = state.data
        if (reels.isEmpty()) {
          Column(
            modifier = Modifier
              .align(Alignment.Center)
              .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Rounded.Movie,
              contentDescription = null,
              tint = Color.White.copy(alpha = 0.85f),
              modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
              text = "No Vynzo Reels yet",
              style = MaterialTheme.typography.headlineMedium,
              color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
              text = "Be the first creator to publish a vertical short video Reel!",
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White.copy(alpha = 0.75f),
              textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Button(
              onClick = onOpenCreateReel,
              modifier = Modifier.testTag("empty_reels_create_button")
            ) {
              Text("Upload Reel")
            }
          }
        } else {
          val pagerState = rememberPagerState(pageCount = { reels.size })
          VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
          ) { page ->
            val reel = reels[page]
            val isCurrentPage = pagerState.currentPage == page
            var isPaused by remember(reel.id) { mutableStateOf(false) }
            var showHeart by remember { mutableStateOf(false) }

            LaunchedEffect(showHeart) {
              if (showHeart) {
                delay(650)
                showHeart = false
              }
            }

            val isLiked by viewModel.postRepository
              .observeIsPostLiked(reel.id, viewModel.currentUserId)
              .collectAsStateWithLifecycle(initialValue = false)

            Box(
              modifier = Modifier
                .fillMaxSize()
                .pointerInput(reel.id) {
                  detectTapGestures(
                    onDoubleTap = {
                      showHeart = true
                      if (!isLiked) viewModel.toggleLike(reel, false)
                    },
                    onTap = { isPaused = !isPaused }
                  )
                }
            ) {
              val uri = reel.mediaUrls.firstOrNull().orEmpty()
              if (uri.startsWith("http") || uri.startsWith("file") || uri.startsWith("content")) {
                VynzoVideoPlayer(
                  videoUri = uri,
                  isPlaying = isCurrentPage && !isPaused,
                  isMuted = isMuted,
                  modifier = Modifier.fillMaxSize()
                )
              } else {
                VynzoMediaImage(
                  source = reel.coverUrl.ifBlank { uri },
                  contentDescription = reel.caption,
                  modifier = Modifier.fillMaxSize()
                )
              }

              // Bottom scrim gradient
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(260.dp)
                  .align(Alignment.BottomCenter)
                  .background(
                    Brush.verticalGradient(
                      colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                    )
                  )
              )

              // Top Bar Overlay
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 14.dp)
                  .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Vynzo Reels",
                  style = MaterialTheme.typography.titleLarge,
                  color = Color.White,
                  fontWeight = FontWeight.Bold
                )
                IconButton(
                  onClick = { isMuted = !isMuted },
                  modifier = Modifier.background(Color.Black.copy(alpha = 0.45f), CircleShape)
                ) {
                  Icon(
                    imageVector = if (isMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = Color.White
                  )
                }
              }

              if (isPaused) {
                Surface(
                  shape = CircleShape,
                  color = Color.Black.copy(alpha = 0.55f),
                  modifier = Modifier.align(Alignment.Center)
                ) {
                  Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Paused",
                    tint = Color.White,
                    modifier = Modifier.padding(16.dp).size(42.dp)
                  )
                }
              }

              AnimatedVisibility(
                visible = showHeart,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier.align(Alignment.Center)
              ) {
                Icon(
                  imageVector = Icons.Rounded.Favorite,
                  contentDescription = null,
                  tint = VynzoRose,
                  modifier = Modifier.size(96.dp)
                )
              }

              // Right Action Rail
              Column(
                modifier = Modifier
                  .align(Alignment.BottomEnd)
                  .padding(end = 12.dp, bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  IconButton(onClick = { viewModel.toggleLike(reel, isLiked) }) {
                    Icon(
                      imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                      contentDescription = "Like Reel",
                      tint = if (isLiked) VynzoRose else Color.White,
                      modifier = Modifier.size(30.dp)
                    )
                  }
                  Text("${reel.likesCount}", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  IconButton(onClick = { onOpenComments(reel.id, reel.authorId) }) {
                    Icon(
                      imageVector = Icons.Outlined.ChatBubbleOutline,
                      contentDescription = "Comments",
                      tint = Color.White,
                      modifier = Modifier.size(28.dp)
                    )
                  }
                  Text("${reel.commentsCount}", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }

                IconButton(
                  onClick = {
                    viewModel.sharePost(reel)
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                      type = "text/plain"
                      putExtra(Intent.EXTRA_TEXT, "Watch @${reel.authorUsername}'s Reel on Vynzo!")
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Reel"))
                  }
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Share Reel",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                  )
                }

                val isSaved = reel.id in bookmarkedSet
                IconButton(onClick = { viewModel.toggleBookmark(reel.id, isSaved) }) {
                  Icon(
                    imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Save Reel",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                  )
                }

                IconButton(onClick = { reportTarget = reel }) {
                  Icon(
                    imageVector = Icons.Rounded.Flag,
                    contentDescription = "Report Reel",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(24.dp)
                  )
                }
              }

              // Bottom-Left Creator Info & Caption
              Column(
                modifier = Modifier
                  .align(Alignment.BottomStart)
                  .padding(start = 16.dp, end = 76.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  VynzoAvatar(
                    avatarUrl = reel.authorAvatarUrl,
                    displayName = reel.authorUsername,
                    size = 38.dp,
                    onClick = { onOpenUserProfile(reel.authorId) }
                  )
                  Spacer(Modifier.width(10.dp))
                  Text(
                    text = "@${reel.authorUsername}",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onOpenUserProfile(reel.authorId) }
                  )
                  if (reel.authorId != viewModel.currentUserId) {
                    Spacer(Modifier.width(10.dp))
                    val rel = followingMap[reel.authorId]
                    OutlinedButton(
                      onClick = {
                        val target = creatorMap[reel.authorId] ?: UserProfile(
                          userId = reel.authorId,
                          username = reel.authorUsername,
                          displayName = reel.authorDisplayName.ifBlank { reel.authorUsername }
                        )
                        viewModel.toggleFollow(target, rel)
                      },
                      shape = RoundedCornerShape(10.dp)
                    ) {
                      Text(
                        text = if (rel != null) "Following" else "Follow",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                      )
                    }
                  }
                }

                if (reel.caption.isNotBlank()) {
                  Text(
                    text = reel.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 3
                  )
                }

                if (reel.musicTitle.isNotBlank()) {
                  Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.16f)
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                      Spacer(Modifier.width(4.dp))
                      Text(reel.musicTitle, style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  reportTarget?.let { target ->
    ReportContentDialog(
      title = "Report @${target.authorUsername}'s Reel",
      onDismiss = { reportTarget = null },
      onSubmit = { reason, details ->
        viewModel.reportContent("reel", target.id, reason, details)
        reportTarget = null
      }
    )
  }
}
