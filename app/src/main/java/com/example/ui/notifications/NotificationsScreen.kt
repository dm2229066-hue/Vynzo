package com.example.ui.notifications

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppNotification
import com.example.ui.components.VynzoAvatar
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.LocalVynzoColors
import com.example.ui.theme.VynzoRose
import com.example.ui.viewmodel.VynzoSessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
  viewModel: VynzoSessionViewModel,
  onBack: () -> Unit,
  onOpenUserProfile: (String) -> Unit,
  onOpenComments: (postId: String, postAuthorId: String) -> Unit,
  onOpenChat: () -> Unit,
  onOpenStory: (authorId: String) -> Unit
) {
  val colors = LocalVynzoColors.current
  val notifications by viewModel.notifications.collectAsStateWithLifecycle()
  val followRequests by viewModel.incomingFollowRequests.collectAsStateWithLifecycle()

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Activity") },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("notifications_back_button")) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          if (notifications.any { !it.read }) {
            TextButton(
              onClick = { viewModel.markAllNotificationsRead() },
              modifier = Modifier.testTag("mark_all_read_button")
            ) {
              Icon(Icons.Rounded.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(4.dp))
              Text("Mark Read")
            }
          }
        }
      )
    }
  ) { innerPadding ->
    if (notifications.isEmpty() && followRequests.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Rounded.NotificationsNone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
          )
          Spacer(Modifier.height(12.dp))
          Text("All caught up", style = MaterialTheme.typography.titleLarge)
          Spacer(Modifier.height(6.dp))
          Text(
            text = "Likes, comments, new followers, story reactions, and mentions will appear here in real time.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.mutedText,
            textAlign = TextAlign.Center
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        if (followRequests.isNotEmpty()) {
          item(key = "requests_header") {
            Text(
              text = "Follow Requests (${followRequests.size})",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .padding(horizontal = 4.dp)
            )
          }
          items(followRequests, key = { "req_${it.id}" }) { rel ->
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, colors.hairlineBorder)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                VynzoAvatar(
                  avatarUrl = rel.followerAvatarUrl,
                  displayName = rel.followerUsername,
                  size = 44.dp,
                  onClick = { onOpenUserProfile(rel.followerId) }
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text("@${rel.followerUsername}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                  Text("Requested to follow you", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                }
                Button(
                  onClick = { viewModel.respondToFollowRequest(rel, accept = true) },
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  modifier = Modifier.testTag("accept_follow_${rel.followerId}")
                ) {
                  Text("Accept")
                }
                Spacer(Modifier.width(6.dp))
                OutlinedButton(
                  onClick = { viewModel.respondToFollowRequest(rel, accept = false) },
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                  Text("Decline")
                }
              }
            }
          }
        }

        if (notifications.isNotEmpty()) {
          item(key = "activity_header") {
            Text(
              text = "Recent Activity",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .padding(horizontal = 4.dp, vertical = 2.dp)
            )
          }
          items(notifications, key = { it.id }) { item ->
            NotificationRow(
              item = item,
              onClick = {
                when (item.type) {
                  "like", "comment", "share", "mention", "reel_like" ->
                    if (item.targetId.isNotBlank()) onOpenComments(item.targetId, viewModel.currentUserId)
                    else onOpenUserProfile(item.actorId)
                  "message" -> onOpenChat()
                  "story_reaction", "story_reply" -> onOpenStory(viewModel.currentUserId)
                  else -> if (item.actorId.isNotBlank()) onOpenUserProfile(item.actorId)
                }
              },
              onAvatarClick = {
                if (item.actorId.isNotBlank()) onOpenUserProfile(item.actorId)
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun NotificationRow(
  item: AppNotification,
  onClick: () -> Unit,
  onAvatarClick: () -> Unit
) {
  val colors = LocalVynzoColors.current
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .widthIn(max = 600.dp)
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(16.dp),
    color = if (!item.read) {
      MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
    } else {
      MaterialTheme.colorScheme.surface
    },
    border = BorderStroke(1.dp, colors.hairlineBorder)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box {
        VynzoAvatar(
          avatarUrl = item.actorAvatarUrl,
          displayName = item.actorUsername,
          size = 44.dp,
          onClick = onAvatarClick
        )
        val badgeIcon = when (item.type) {
          "like", "reel_like", "story_reaction" -> Icons.Rounded.Favorite
          "comment", "message", "story_reply" -> Icons.Rounded.ChatBubble
          "follow", "follow_request" -> Icons.Rounded.PersonAdd
          "share" -> Icons.Rounded.Share
          else -> Icons.Rounded.AutoAwesome
        }
        val badgeTint = when (item.type) {
          "like", "reel_like", "story_reaction" -> VynzoRose
          "follow", "follow_request" -> MaterialTheme.colorScheme.primary
          else -> colors.accentCyan
        }
        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .size(18.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = badgeIcon,
            contentDescription = null,
            tint = badgeTint,
            modifier = Modifier.size(12.dp)
          )
        }
      }

      Spacer(Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "@${item.actorUsername} ${item.message}",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = if (!item.read) FontWeight.SemiBold else FontWeight.Normal
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = formatRelativeTime(item.createdAt?.seconds),
          style = MaterialTheme.typography.labelSmall,
          color = colors.mutedText
        )
      }

      if (!item.read) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
        )
      }
    }
  }
}
