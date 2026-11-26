package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.data.model.Post
import com.example.ui.theme.LocalVynzoColors
import com.example.ui.theme.VynzoIndigo
import com.example.ui.theme.VynzoRose
import kotlinx.coroutines.delay

@Composable
fun VynzoPostCard(
  post: Post,
  isOwnPost: Boolean,
  isLiked: Boolean,
  isSaved: Boolean,
  isFollowingAuthor: Boolean,
  isVerifiedAuthor: Boolean = false,
  onAuthorClick: () -> Unit,
  onToggleFollow: () -> Unit,
  onToggleLike: (forceLike: Boolean) -> Unit,
  onCommentClick: () -> Unit,
  onShareClick: () -> Unit,
  onToggleSave: () -> Unit,
  onHashtagClick: (String) -> Unit,
  onDeletePost: () -> Unit,
  onReportPost: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val colors = LocalVynzoColors.current
  var showDoubleTapHeart by remember { mutableStateOf(false) }
  var menuExpanded by remember { mutableStateOf(false) }
  var isVideoPlaying by remember { mutableStateOf(true) }
  var isMuted by remember { mutableStateOf(true) }

  LaunchedEffect(showDoubleTapHeart) {
    if (showDoubleTapHeart) {
      delay(650)
      showDoubleTapHeart = false
    }
  }

  val heartScale by animateFloatAsState(
    targetValue = if (isLiked) 1.15f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "like_scale"
  )

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp)
      .testTag("post_card_${post.id}"),
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, colors.hairlineBorder),
    tonalElevation = 1.dp
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Author Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        VynzoAvatar(
          avatarUrl = post.authorAvatarUrl,
          displayName = post.authorDisplayName.ifBlank { post.authorUsername },
          size = 40.dp,
          onClick = onAuthorClick
        )
        Spacer(Modifier.width(10.dp))
        Column(
          modifier = Modifier
            .weight(1f)
            .clickable(onClick = onAuthorClick)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = post.authorUsername,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
            if (isVerifiedAuthor) {
              Spacer(Modifier.width(4.dp))
              VerifiedCreatorBadge()
            }
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = formatRelativeTime(post.createdAt?.seconds),
              style = MaterialTheme.typography.bodySmall,
              color = colors.mutedText
            )
            if (post.locationName.isNotBlank()) {
              Text(" · ", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
              Icon(
                imageVector = Icons.Rounded.LocationOn,
                contentDescription = null,
                tint = colors.accentCyan,
                modifier = Modifier.size(12.dp)
              )
              Spacer(Modifier.width(2.dp))
              Text(
                text = post.locationName,
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedText,
                maxLines = 1
              )
            }
          }
        }

        if (!isOwnPost) {
          TextButton(
            onClick = onToggleFollow,
            modifier = Modifier.testTag("follow_post_author_${post.id}")
          ) {
            Text(
              text = if (isFollowingAuthor) "Following" else "Follow",
              style = MaterialTheme.typography.labelLarge,
              color = if (isFollowingAuthor) colors.mutedText else MaterialTheme.colorScheme.primary
            )
          }
        }

        Box {
          IconButton(onClick = { menuExpanded = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "Post options")
          }
          DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
              text = { Text(if (isSaved) "Unsave post" else "Save post") },
              leadingIcon = {
                Icon(if (isSaved) Icons.Rounded.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null)
              },
              onClick = {
                menuExpanded = false
                onToggleSave()
              }
            )
            if (isOwnPost) {
              DropdownMenuItem(
                text = { Text("Delete post", color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                  Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                },
                onClick = {
                  menuExpanded = false
                  onDeletePost()
                }
              )
            } else {
              DropdownMenuItem(
                text = { Text("Report post") },
                leadingIcon = { Icon(Icons.Rounded.Flag, contentDescription = null) },
                onClick = {
                  menuExpanded = false
                  onReportPost()
                }
              )
            }
          }
        }
      }

      // Media Surface (Single Image, Multi-Image Carousel, or Video/Reel)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(if (post.mediaType == "reel") 0.8f else 1.0f)
          .padding(horizontal = 10.dp)
          .clip(RoundedCornerShape(18.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant)
          .pointerInput(post.id) {
            detectTapGestures(
              onDoubleTap = {
                showDoubleTapHeart = true
                if (!isLiked) onToggleLike(true)
              },
              onTap = {
                if (post.mediaType == "video" || post.mediaType == "reel") {
                  isVideoPlaying = !isVideoPlaying
                }
              }
            )
          }
      ) {
        if (post.mediaType == "video" || post.mediaType == "reel") {
          val uri = post.mediaUrls.firstOrNull().orEmpty()
          if (uri.startsWith("http") || uri.startsWith("file") || uri.startsWith("content")) {
            VynzoVideoPlayer(
              videoUri = uri,
              isPlaying = isVideoPlaying,
              isMuted = isMuted,
              modifier = Modifier.fillMaxSize()
            )
          } else {
            VynzoMediaImage(
              source = post.coverUrl.ifBlank { uri },
              contentDescription = post.caption.ifBlank { "Video preview" },
              modifier = Modifier.fillMaxSize()
            )
          }

          Row(
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Surface(
              shape = CircleShape,
              color = Color.Black.copy(alpha = 0.55f),
              modifier = Modifier.clickable { isVideoPlaying = !isVideoPlaying }
            ) {
              Icon(
                imageVector = if (isVideoPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isVideoPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.padding(8.dp).size(18.dp)
              )
            }
            Surface(
              shape = CircleShape,
              color = Color.Black.copy(alpha = 0.55f),
              modifier = Modifier.clickable { isMuted = !isMuted }
            ) {
              Icon(
                imageVector = if (isMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                contentDescription = if (isMuted) "Unmute" else "Mute",
                tint = Color.White,
                modifier = Modifier.padding(8.dp).size(18.dp)
              )
            }
          }

          if (post.musicTitle.isNotBlank()) {
            Surface(
              shape = RoundedCornerShape(20.dp),
              color = Color.Black.copy(alpha = 0.55f),
              modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(post.musicTitle, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1)
              }
            }
          }
        } else if (post.mediaUrls.size > 1) {
          val pagerState = rememberPagerState(pageCount = { post.mediaUrls.size })
          HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            VynzoMediaImage(
              source = post.mediaUrls[page],
              contentDescription = "Photo ${page + 1}",
              modifier = Modifier.fillMaxSize()
            )
          }
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.Black.copy(alpha = 0.6f),
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(10.dp)
          ) {
            Text(
              text = "${pagerState.currentPage + 1}/${post.mediaUrls.size}",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
            )
          }
          Row(
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            repeat(post.mediaUrls.size) { index ->
              val active = index == pagerState.currentPage
              Box(
                modifier = Modifier
                  .size(if (active) 8.dp else 6.dp)
                  .clip(CircleShape)
                  .background(if (active) Color.White else Color.White.copy(alpha = 0.45f))
              )
            }
          }
        } else {
          VynzoMediaImage(
            source = post.mediaUrls.firstOrNull().orEmpty(),
            contentDescription = post.caption.ifBlank { "Post image" },
            modifier = Modifier.fillMaxSize()
          )
        }

        // Double-Tap Heart Animation
        androidx.compose.animation.AnimatedVisibility(
          visible = showDoubleTapHeart,
          enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
          exit = scaleOut() + fadeOut(),
          modifier = Modifier.align(Alignment.Center)
        ) {
          Icon(
            imageVector = Icons.Rounded.Favorite,
            contentDescription = null,
            tint = VynzoRose,
            modifier = Modifier.size(92.dp)
          )
        }
      }

      // Action Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = { onToggleLike(false) },
          modifier = Modifier.testTag("like_button_${post.id}")
        ) {
          Icon(
            imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (isLiked) "Unlike" else "Like",
            tint = if (isLiked) VynzoRose else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.scale(heartScale)
          )
        }
        Text(
          text = "${post.likesCount.coerceAtLeast(0)}",
          style = MaterialTheme.typography.labelLarge
        )

        Spacer(Modifier.width(8.dp))
        IconButton(
          onClick = onCommentClick,
          modifier = Modifier.testTag("comment_button_${post.id}")
        ) {
          Icon(
            imageVector = Icons.Outlined.ChatBubbleOutline,
            contentDescription = "Comments"
          )
        }
        Text(
          text = "${post.commentsCount.coerceAtLeast(0)}",
          style = MaterialTheme.typography.labelLarge
        )

        Spacer(Modifier.width(8.dp))
        IconButton(
          onClick = {
            onShareClick()
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_TEXT, "Check out @${post.authorUsername} on Vynzo: ${post.caption}")
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share via"))
          },
          modifier = Modifier.testTag("share_button_${post.id}")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.Send,
            contentDescription = "Share post"
          )
        }
        if (post.sharesCount > 0) {
          Text(text = "${post.sharesCount}", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.weight(1f))

        IconButton(
          onClick = onToggleSave,
          modifier = Modifier.testTag("save_button_${post.id}")
        ) {
          Icon(
            imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = if (isSaved) "Remove bookmark" else "Save post",
            tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
          )
        }
      }

      // Caption & Hashtags
      if (post.caption.isNotBlank() || post.hashtags.isNotEmpty()) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
          val annotated = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
              append("@${post.authorUsername} ")
            }
            val words = post.caption.split(" ")
            words.forEachIndexed { idx, w ->
              if (w.startsWith("#") || w.startsWith("@")) {
                withStyle(SpanStyle(color = VynzoIndigo, fontWeight = FontWeight.SemiBold)) {
                  append(w)
                }
              } else {
                append(w)
              }
              if (idx < words.lastIndex) append(" ")
            }
          }
          Text(
            text = annotated,
            style = MaterialTheme.typography.bodyMedium
          )
          if (post.hashtags.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              post.hashtags.take(4).forEach { tag ->
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                  modifier = Modifier.clickable { onHashtagClick(tag) }
                ) {
                  Text(
                    text = "#$tag",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              }
            }
          }
        }
      } else {
        Spacer(Modifier.height(6.dp))
      }
    }
  }
}
