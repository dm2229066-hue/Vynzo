package com.example.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatMessage
import com.example.media.MediaUtils
import com.example.media.VoiceNotePlayer
import com.example.media.VoiceNoteRecorder
import com.example.ui.components.VynzoAvatar
import com.example.ui.components.VynzoMediaImage
import com.example.ui.components.VynzoVideoPlayer
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.LocalVynzoColors
import com.example.ui.theme.VynzoCoral
import com.example.ui.viewmodel.VynzoSessionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
  viewModel: VynzoSessionViewModel,
  onBack: () -> Unit,
  onOpenChat: (chatId: String, peerId: String) -> Unit
) {
  val scope = rememberCoroutineScope()
  val colors = LocalVynzoColors.current
  val threads by viewModel.chatThreads.collectAsStateWithLifecycle()
  val creators by viewModel.activeCreators.collectAsStateWithLifecycle()
  val privateInfo by viewModel.myPrivateInfo.collectAsStateWithLifecycle()
  val blockedSet = remember(privateInfo) { privateInfo?.blockedUids?.toSet() ?: emptySet() }
  val creatorMap = remember(creators) { creators.associateBy { it.userId } }
  val myProfile = viewModel.currentProfileOrNull()

  var searchQuery by remember { mutableStateOf("") }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Direct Messages") },
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
    ) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search messages or start a new chat...") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
      )

      // Quick Start Chat Row with Active Creators
      val otherCreators = creators.filter {
        it.userId != viewModel.currentUserId &&
          it.userId !in blockedSet &&
          (searchQuery.isBlank() || it.displayName.contains(searchQuery, true) || it.username.contains(searchQuery, true))
      }
      if (otherCreators.isNotEmpty()) {
        Text(
          text = "Start a Chat",
          style = MaterialTheme.typography.labelLarge,
          color = colors.mutedText,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(otherCreators.take(15), key = { it.userId }) { peer ->
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .width(64.dp)
                .clickable {
                  val me = myProfile ?: return@clickable
                  scope.launch {
                    viewModel.chatRepository.getOrCreateChatThread(me, peer)
                      .onSuccess { chatId -> onOpenChat(chatId, peer.userId) }
                  }
                }
            ) {
              VynzoAvatar(
                avatarUrl = peer.avatarUrl,
                displayName = peer.displayName,
                size = 50.dp,
                isOnline = peer.online
              )
              Spacer(Modifier.height(4.dp))
              Text(peer.username, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
        }
      }

      HorizontalDivider(color = colors.hairlineBorder, modifier = Modifier.padding(vertical = 4.dp))

      val filteredThreads = threads.filter { thread ->
        val peerId = thread.participantUids.firstOrNull { it != viewModel.currentUserId }.orEmpty()
        peerId !in blockedSet && (
          searchQuery.isBlank() ||
            thread.lastMessageText.contains(searchQuery, true) ||
            (thread.participantNames[peerId] ?: "").contains(searchQuery, true)
          )
      }

      if (filteredThreads.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text("No conversations yet. Tap any creator above to say hello!", color = colors.mutedText)
        }
      } else {
        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
          items(filteredThreads, key = { it.id }) { thread ->
            val peerId = thread.participantUids.firstOrNull { it != viewModel.currentUserId }.orEmpty()
            val peerProfile = creatorMap[peerId]
            val peerName = peerProfile?.displayName ?: thread.participantNames[peerId] ?: "Vynzo User"
            val peerAvatar = peerProfile?.avatarUrl ?: thread.participantAvatars[peerId].orEmpty()
            val unread = (thread.unreadCounts[viewModel.currentUserId] ?: 0L).toInt()

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenChat(thread.id, peerId) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              VynzoAvatar(
                avatarUrl = peerAvatar,
                displayName = peerName,
                size = 52.dp,
                isOnline = peerProfile?.online == true
              )
              Spacer(Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(peerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                  text = if (thread.typingUid == peerId) "Typing..." else thread.lastMessageText.ifBlank { "Tap to chat" },
                  style = MaterialTheme.typography.bodySmall,
                  color = if (thread.typingUid == peerId) MaterialTheme.colorScheme.primary else colors.mutedText,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              Column(horizontalAlignment = Alignment.End) {
                Text(formatRelativeTime(thread.updatedAt?.seconds), style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                if (unread > 0) {
                  Spacer(Modifier.height(4.dp))
                  Badge { Text(unread.toString()) }
                }
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatDetailScreen(
  chatId: String,
  peerId: String,
  viewModel: VynzoSessionViewModel,
  onBack: () -> Unit,
  onOpenPeerProfile: (String) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val colors = LocalVynzoColors.current

  val thread by viewModel.chatRepository.observeChatThread(chatId).collectAsStateWithLifecycle(initialValue = null)
  val messages by viewModel.chatRepository.observeMessages(chatId).collectAsStateWithLifecycle(initialValue = emptyList())
  val peerProfile by viewModel.userRepository.observeUserProfile(peerId).collectAsStateWithLifecycle(initialValue = null)
  val privateInfo by viewModel.myPrivateInfo.collectAsStateWithLifecycle()
  val myProfile = viewModel.currentProfileOrNull()

  val participantUids = thread?.participantUids?.takeIf { it.size == 2 }
    ?: listOf(viewModel.currentUserId, peerId).sorted()

  var draft by remember { mutableStateOf("") }
  var replyTo by remember { mutableStateOf<ChatMessage?>(null) }
  var actionMsg by remember { mutableStateOf<ChatMessage?>(null) }
  var isRecording by remember { mutableStateOf(false) }
  var playingMsgId by remember { mutableStateOf<String?>(null) }

  val voiceRecorder = remember(context) { VoiceNoteRecorder(context) }
  val voicePlayer = remember { VoiceNotePlayer() }

  DisposableEffect(Unit) {
    onDispose {
      voicePlayer.stop()
      scope.launch { viewModel.chatRepository.setTypingStatus(chatId, false) }
    }
  }

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      viewModel.chatRepository.markThreadRead(chatId, messages)
    }
  }

  val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    val me = myProfile ?: return@rememberLauncherForActivityResult
    if (uri != null) {
      scope.launch {
        val bmp = MediaUtils.decodeBitmapFromUri(context, uri, 800)
        if (bmp != null) {
          val dataUri = MediaUtils.encodeBitmapToDataUri(bmp, 140_000)
          viewModel.chatRepository.sendMessage(
            chatId = chatId,
            participantUids = participantUids,
            sender = me,
            peerId = peerId,
            messageType = "image",
            text = "",
            mediaUrl = dataUri,
            replyToMessageId = replyTo?.id.orEmpty(),
            replyToPreview = replyTo?.text.orEmpty()
          )
          replyTo = null
        }
      }
    }
  }

  val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    val me = myProfile ?: return@rememberLauncherForActivityResult
    if (uri != null) {
      scope.launch {
        val persisted = MediaUtils.persistMediaFile(context, uri, "mp4")
        if (persisted != null) {
          viewModel.chatRepository.sendMessage(
            chatId = chatId,
            participantUids = participantUids,
            sender = me,
            peerId = peerId,
            messageType = "video",
            text = "",
            mediaUrl = persisted
          )
        }
      }
    }
  }

  val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    if (granted) {
      isRecording = voiceRecorder.startRecording()
    } else {
      viewModel.notifyMessage("Microphone permission is needed to record voice messages.")
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onOpenPeerProfile(peerId) }
          ) {
            VynzoAvatar(
              avatarUrl = peerProfile?.avatarUrl.orEmpty(),
              displayName = peerProfile?.displayName ?: "User",
              size = 36.dp,
              isOnline = peerProfile?.online == true
            )
            Spacer(Modifier.width(10.dp))
            Column {
              Text(peerProfile?.displayName ?: "Chat", style = MaterialTheme.typography.titleSmall)
              Text(
                text = when {
                  thread?.typingUid == peerId -> "Typing..."
                  peerProfile?.online == true -> "Online"
                  else -> "Offline"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (thread?.typingUid == peerId) MaterialTheme.colorScheme.primary else colors.mutedText
              )
            }
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(
            onClick = {
              scope.launch {
                viewModel.userRepository.toggleBlockUser(peerId, privateInfo?.blockedUids ?: emptyList())
                  .onSuccess { blocked ->
                    viewModel.notifyMessage(if (blocked) "User blocked" else "User unblocked")
                    if (blocked) onBack()
                  }
              }
            }
          ) {
            Icon(Icons.Rounded.Block, contentDescription = "Block user")
          }
        }
      )
    },
    bottomBar = {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.imePadding()
      ) {
        Column {
          HorizontalDivider(color = colors.hairlineBorder)
          replyTo?.let { r ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.AutoMirrored.Rounded.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(8.dp))
              Text("Replying to: ${r.text.ifBlank { r.messageType }}", maxLines = 1, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
              IconButton(onClick = { replyTo = null }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Cancel reply", modifier = Modifier.size(16.dp))
              }
            }
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            ) {
              Icon(Icons.Rounded.Photo, contentDescription = "Send photo")
            }
            IconButton(
              onClick = { videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }
            ) {
              Icon(Icons.Rounded.Videocam, contentDescription = "Send video")
            }
            OutlinedTextField(
              value = draft,
              onValueChange = {
                draft = it.take(2000)
                scope.launch { viewModel.chatRepository.setTypingStatus(chatId, draft.isNotBlank()) }
              },
              placeholder = { Text(if (isRecording) "Recording voice..." else "Message...") },
              shape = RoundedCornerShape(24.dp),
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("chat_message_input")
            )
            if (draft.isBlank()) {
              IconButton(
                onClick = {
                  if (isRecording) {
                    isRecording = false
                    val uri = voiceRecorder.stopRecording()
                    val me = myProfile
                    if (uri != null && me != null) {
                      scope.launch {
                        viewModel.chatRepository.sendMessage(
                          chatId = chatId,
                          participantUids = participantUids,
                          sender = me,
                          peerId = peerId,
                          messageType = "voice",
                          text = "Voice note",
                          mediaUrl = uri
                        )
                      }
                    }
                  } else {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                      isRecording = voiceRecorder.startRecording()
                    } else {
                      micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                  }
                }
              ) {
                Icon(
                  imageVector = if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                  contentDescription = if (isRecording) "Stop recording" else "Record voice message",
                  tint = if (isRecording) VynzoCoral else MaterialTheme.colorScheme.onSurface
                )
              }
            } else {
              IconButton(
                enabled = myProfile != null,
                onClick = {
                  val me = myProfile ?: return@IconButton
                  val textToSend = draft
                  val reply = replyTo
                  draft = ""
                  replyTo = null
                  scope.launch {
                    viewModel.chatRepository.sendMessage(
                      chatId = chatId,
                      participantUids = participantUids,
                      sender = me,
                      peerId = peerId,
                      messageType = "text",
                      text = textToSend,
                      replyToMessageId = reply?.id.orEmpty(),
                      replyToPreview = reply?.text.orEmpty()
                    )
                  }
                },
                modifier = Modifier.testTag("chat_send_button")
              ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
              }
            }
          }
        }
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(messages, key = { it.id }) { msg ->
        val isMine = msg.senderId == viewModel.currentUserId
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
        ) {
          Surface(
            shape = RoundedCornerShape(
              topStart = 18.dp,
              topEnd = 18.dp,
              bottomStart = if (isMine) 18.dp else 4.dp,
              bottomEnd = if (isMine) 4.dp else 18.dp
            ),
            color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
              .widthIn(max = 290.dp)
              .combinedClickable(
                onClick = {},
                onLongClick = { if (!msg.deleted) actionMsg = msg }
              )
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              if (msg.replyToPreview.isNotBlank()) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = Color.Black.copy(alpha = 0.16f),
                  modifier = Modifier.padding(bottom = 6.dp)
                ) {
                  Text(
                    text = msg.replyToPreview,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isMine) Color.White.copy(alpha = 0.85f) else colors.mutedText,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              }

              when {
                msg.deleted -> {
                  Text(
                    text = "Message deleted",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isMine) Color.White.copy(alpha = 0.7f) else colors.mutedText
                  )
                }
                msg.messageType == "image" && msg.mediaUrl.isNotBlank() -> {
                  VynzoMediaImage(
                    source = msg.mediaUrl,
                    contentDescription = "Photo message",
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp))
                  )
                }
                msg.messageType == "video" && msg.mediaUrl.isNotBlank() -> {
                  Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp))) {
                    VynzoVideoPlayer(videoUri = msg.mediaUrl, isPlaying = false, isMuted = true, modifier = Modifier.fillMaxSize())
                  }
                }
                msg.messageType == "voice" && msg.mediaUrl.isNotBlank() -> {
                  val playingThis = playingMsgId == msg.id
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                      if (playingThis) {
                        voicePlayer.stop()
                        playingMsgId = null
                      } else {
                        playingMsgId = msg.id
                        voicePlayer.play(msg.mediaUrl) { playingMsgId = null }
                      }
                    }
                  ) {
                    Icon(
                      imageVector = if (playingThis) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                      contentDescription = "Play voice note",
                      tint = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                      imageVector = Icons.Rounded.GraphicEq,
                      contentDescription = null,
                      tint = if (isMine) Color.White else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                      text = if (playingThis) "Playing..." else "Voice note",
                      color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                  }
                }
                else -> {
                  Text(
                    text = msg.text,
                    color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                  )
                }
              }

              Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                if (msg.reactionEmoji.isNotBlank()) {
                  Text(msg.reactionEmoji, fontSize = 14.sp)
                }
                Text(
                  text = formatRelativeTime(msg.createdAt?.seconds),
                  style = MaterialTheme.typography.labelSmall,
                  color = if (isMine) Color.White.copy(alpha = 0.72f) else colors.mutedText
                )
                if (isMine && peerId in msg.readBy) {
                  Icon(
                    imageVector = Icons.Rounded.DoneAll,
                    contentDescription = "Read",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  actionMsg?.let { target ->
    AlertDialog(
      onDismissRequest = { actionMsg = null },
      title = { Text("Message Options") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
          ) {
            listOf("❤️", "👍", "😂", "🔥", "😢").forEach { emoji ->
              Text(
                text = emoji,
                fontSize = 24.sp,
                modifier = Modifier.clickable {
                  scope.launch {
                    viewModel.chatRepository.reactToMessage(chatId, target.id, emoji)
                  }
                  actionMsg = null
                }
              )
            }
          }
          TextButton(
            onClick = {
              replyTo = target
              actionMsg = null
            }
          ) {
            Icon(Icons.AutoMirrored.Rounded.Reply, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Reply")
          }
          if (target.senderId == viewModel.currentUserId) {
            TextButton(
              onClick = {
                scope.launch { viewModel.chatRepository.softDeleteMessage(chatId, target.id) }
                actionMsg = null
              }
            ) {
              Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
              Spacer(Modifier.width(8.dp))
              Text("Delete Message", color = MaterialTheme.colorScheme.error)
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { actionMsg = null }) { Text("Close") }
      }
    )
  }
}
