package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.R
import com.example.media.MediaUtils
import com.example.ui.theme.LocalVynzoColors
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.VynzoBrandGradient
import com.example.ui.theme.VynzoCyan
import com.example.ui.theme.VynzoEmerald
import com.example.ui.theme.VynzoStoryRingGradient

@Composable
fun VynzoWordmark(
  modifier: Modifier = Modifier,
  fontSize: Int = 24
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Image(
      painter = painterResource(id = R.drawable.img_vynzo_logo),
      contentDescription = "Vynzo logo",
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .size((fontSize + 6).dp)
        .clip(RoundedCornerShape(8.dp))
    )
    Spacer(Modifier.width(8.dp))
    Text(
      text = "VYNZO",
      style = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize.sp,
        letterSpacing = 1.4.sp,
        brush = VynzoBrandGradient
      )
    )
  }
}

@Composable
fun VynzoMediaImage(
  source: String,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  contentScale: ContentScale = ContentScale.Crop
) {
  when {
    source.startsWith("res://") -> {
      val resId = when (source.removePrefix("res://")) {
        "img_featured_city" -> R.drawable.img_featured_city
        "img_featured_studio" -> R.drawable.img_featured_studio
        "img_onboarding_hero" -> R.drawable.img_onboarding_hero
        "img_splash_bg" -> R.drawable.img_splash_bg
        else -> R.drawable.img_vynzo_logo
      }
      Image(
        painter = painterResource(id = resId),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
      )
    }
    source.startsWith("data:image") -> {
      val bitmap = remember(source) { MediaUtils.decodeDataUriBitmap(source) }
      if (bitmap != null) {
        Image(
          bitmap = bitmap.asImageBitmap(),
          contentDescription = contentDescription,
          contentScale = contentScale,
          modifier = modifier
        )
      } else {
        Box(
          modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Rounded.Person, contentDescription = null, tint = LocalVynzoColors.current.mutedText)
        }
      }
    }
    source.isNotBlank() -> {
      AsyncImage(
        model = source,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
      )
    }
    else -> {
      Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = LocalVynzoColors.current.mutedText)
      }
    }
  }
}

@Composable
fun VynzoAvatar(
  avatarUrl: String,
  displayName: String,
  size: Dp = 44.dp,
  hasStoryRing: Boolean = false,
  isOnline: Boolean = false,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null
) {
  val colors = LocalVynzoColors.current
  val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
  Box(
    modifier = modifier
      .size(size + if (hasStoryRing) 6.dp else 0.dp)
      .then(clickMod),
    contentAlignment = Alignment.Center
  ) {
    if (hasStoryRing) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clip(CircleShape)
          .border(2.5.dp, VynzoStoryRingGradient, CircleShape)
          .padding(3.5.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.surface)
      )
    }
    Box(
      modifier = Modifier
        .size(size)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center
    ) {
      if (avatarUrl.isNotBlank()) {
        VynzoMediaImage(
          source = avatarUrl,
          contentDescription = "$displayName avatar",
          modifier = Modifier.fillMaxSize()
        )
      } else {
        val initials = displayName.trim().take(1).uppercase().ifBlank { "V" }
        Text(
          text = initials,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onPrimaryContainer
        )
      }
    }
    if (isOnline) {
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .offset(x = (-1).dp, y = (-1).dp)
          .size((size.value * 0.27f).dp.coerceAtLeast(10.dp))
          .clip(CircleShape)
          .background(VynzoEmerald)
          .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
      )
    }
  }
}

@Composable
fun VerifiedCreatorBadge(modifier: Modifier = Modifier) {
  Icon(
    imageVector = Icons.Rounded.Verified,
    contentDescription = "Verified creator",
    tint = VynzoCyan,
    modifier = modifier.size(16.dp)
  )
}

@OptIn(UnstableApi::class)
@Composable
fun VynzoVideoPlayer(
  videoUri: String,
  isPlaying: Boolean,
  isMuted: Boolean,
  modifier: Modifier = Modifier,
  resizeModeCrop: Boolean = true
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  val exoPlayer = remember(videoUri) {
    ExoPlayer.Builder(context).build().apply {
      repeatMode = Player.REPEAT_MODE_ONE
      if (videoUri.isNotBlank()) {
        setMediaItem(MediaItem.fromUri(Uri.parse(videoUri)))
        prepare()
      }
    }
  }

  LaunchedEffect(isPlaying) {
    exoPlayer.playWhenReady = isPlaying
  }

  LaunchedEffect(isMuted) {
    exoPlayer.volume = if (isMuted) 0f else 1f
  }

  DisposableEffect(lifecycleOwner, exoPlayer) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
        Lifecycle.Event.ON_RESUME -> if (isPlaying) exoPlayer.play()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      exoPlayer.release()
    }
  }

  AndroidView(
    modifier = modifier,
    factory = { ctx ->
      PlayerView(ctx).apply {
        useController = false
        resizeMode = if (resizeModeCrop) {
          AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        } else {
          AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        layoutParams = FrameLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT
        )
        player = exoPlayer
      }
    },
    update = { view ->
      if (view.player !== exoPlayer) {
        view.player = exoPlayer
      }
    }
  )
}

@Composable
fun VynzoFeedSkeleton(modifier: Modifier = Modifier) {
  val transition = rememberInfiniteTransition(label = "skeleton")
  val shift by transition.animateFloat(
    initialValue = 0f,
    targetValue = 600f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200),
      repeatMode = RepeatMode.Restart
    ),
    label = "shimmer"
  )
  val base = MaterialTheme.colorScheme.surfaceVariant
  val highlight = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
  val brush = Brush.linearGradient(
    colors = listOf(base, highlight, base),
    start = Offset(shift - 300f, 0f),
    end = Offset(shift, 300f)
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    repeat(2) {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, LocalVynzoColors.current.hairlineBorder)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(brush))
            Spacer(Modifier.width(10.dp))
            Column {
              Box(modifier = Modifier.width(120.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).background(brush))
              Spacer(Modifier.height(6.dp))
              Box(modifier = Modifier.width(80.dp).height(10.dp).clip(RoundedCornerShape(5.dp)).background(brush))
            }
          }
          Spacer(Modifier.height(12.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .aspectRatio(1.1f)
              .clip(RoundedCornerShape(16.dp))
              .background(brush)
          )
        }
      }
    }
  }
}

fun formatRelativeTime(epochSeconds: Long?): String {
  if (epochSeconds == null || epochSeconds <= 0L) return "Just now"
  val diff = (System.currentTimeMillis() / 1000L - epochSeconds).coerceAtLeast(0L)
  return when {
    diff < 60 -> "Just now"
    diff < 3600 -> "${diff / 60}m ago"
    diff < 86_400 -> "${diff / 3600}h ago"
    diff < 604_800 -> "${diff / 86_400}d ago"
    else -> "${diff / 604_800}w ago"
  }
}
