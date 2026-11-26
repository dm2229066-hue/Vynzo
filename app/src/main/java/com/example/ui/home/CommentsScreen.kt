package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.UiState
import com.example.ui.components.ReportContentDialog
import com.example.ui.components.VynzoAvatar
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.LocalVynzoColors
import com.example.ui.viewmodel.VynzoSessionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsScreen(
  postId: String,
  postAuthorId: String,
  viewModel: VynzoSessionViewModel,
  onBack: () -> Unit,
  onOpenUserProfile: (String) -> Unit
) {
  val colors = LocalVynzoColors.current
  val scope = rememberCoroutineScope()
  val comments by viewModel.postRepository.observeComments(postId)
    .collectAsStateWithLifecycle(initialValue = emptyList())
  val feedState by viewModel.publicFeedState.collectAsStateWithLifecycle()
  val post = (feedState as? UiState.Success)?.data?.firstOrNull { it.id == postId }
    ?: Post(id = postId, authorId = postAuthorId)
  val myProfile = viewModel.currentProfileOrNull()
  val isPostAuthor = postAuthorId == viewModel.currentUserId

  var draftText by remember { mutableStateOf("") }
  var isSending by remember { mutableStateOf(false) }
  var reportComment by remember { mutableStateOf<PostComment?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Comments (${comments.size})") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        }
      )
    },
    bottomBar = {
      if (!post.commentsDisabled) {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 4.dp,
          modifier = Modifier.imePadding()
        ) {
          Column {
            HorizontalDivider(color = colors.hairlineBorder)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              VynzoAvatar(
                avatarUrl = myProfile?.avatarUrl.orEmpty(),
                displayName = myProfile?.displayName ?: "You",
                size = 34.dp
              )
              Spacer(Modifier.width(10.dp))
              OutlinedTextField(
                value = draftText,
                onValueChange = { draftText = it.take(1000) },
                placeholder = { Text("Add a thoughtful comment...") },
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                modifier = Modifier
                  .weight(1f)
                  .testTag("comment_input")
              )
              Spacer(Modifier.width(6.dp))
              IconButton(
                enabled = draftText.isNotBlank() && !isSending && myProfile != null,
                onClick = {
                  val actor = myProfile ?: return@IconButton
                  isSending = true
                  scope.launch {
                    viewModel.postRepository.addComment(post, actor, draftText)
                      .onSuccess { draftText = "" }
                      .onFailure { viewModel.notifyMessage(it.localizedMessage ?: "Unable to comment") }
                    isSending = false
                  }
                },
                modifier = Modifier.testTag("comment_send_button")
              ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Post comment", tint = MaterialTheme.colorScheme.primary)
              }
            }
          }
        }
      }
    }
  ) { innerPadding ->
    if (comments.isEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = if (post.commentsDisabled) "Comments are disabled for this post" else "No comments yet",
          style = MaterialTheme.typography.titleMedium
        )
        Text(
          text = if (post.commentsDisabled) "The creator turned off commenting." else "Start the conversation below!",
          style = MaterialTheme.typography.bodyMedium,
          color = colors.mutedText
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(comments, key = { it.id }) { comment ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
          ) {
            VynzoAvatar(
              avatarUrl = comment.authorAvatarUrl,
              displayName = comment.authorUsername,
              size = 36.dp,
              onClick = { onOpenUserProfile(comment.authorId) }
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "@${comment.authorUsername}",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                  text = formatRelativeTime(comment.createdAt?.seconds),
                  style = MaterialTheme.typography.bodySmall,
                  color = colors.mutedText
                )
              }
              Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp)
              )
            }
            if (isPostAuthor || comment.authorId == viewModel.currentUserId) {
              IconButton(
                onClick = {
                  scope.launch {
                    viewModel.postRepository.hideComment(postId, comment.id, true)
                      .onSuccess { viewModel.notifyMessage("Comment hidden") }
                  }
                }
              ) {
                Icon(Icons.Rounded.VisibilityOff, contentDescription = "Hide comment", modifier = Modifier.size(18.dp))
              }
            } else {
              IconButton(onClick = { reportComment = comment }) {
                Icon(Icons.Rounded.Flag, contentDescription = "Report comment", modifier = Modifier.size(18.dp))
              }
            }
          }
        }
      }
    }
  }

  reportComment?.let { target ->
    ReportContentDialog(
      title = "Report @${target.authorUsername}'s comment",
      onDismiss = { reportComment = null },
      onSubmit = { reason, details ->
        viewModel.reportContent("comment", target.id, reason, details)
        reportComment = null
      }
    )
  }
}
