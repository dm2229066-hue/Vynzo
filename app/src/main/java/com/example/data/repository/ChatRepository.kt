package com.example.data.repository

import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.firebase.unwrapFirestoreError
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ChatRepository(
  private val db: FirebaseFirestore
) {
  private val auth get() = Firebase.auth

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  fun chatIdFor(uidA: String, uidB: String): String =
    listOf(uidA, uidB).sorted().joinToString("_")

  fun observeChatThreads(userId: String): Flow<List<ChatThread>> = flow {
    if (userId.isBlank() || auth.currentUser?.uid != userId) {
      emit(emptyList())
      return@flow
    }
    emitAll(
      db.collection("chats")
        .whereArrayContains("participantUids", userId)
        .snapshots()
        .map { snap ->
          snap.toObjects(ChatThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.updatedAt?.seconds ?: 0L }
        }
        .catch { err ->
          val root = unwrapFirestoreError(err)
          if (auth.currentUser?.uid == userId) handleFirestoreError(root, OperationType.LIST, "chats")
          throw root
        }
    )
  }

  fun observeChatThread(chatId: String): Flow<ChatThread?> = flow {
    if (chatId.isBlank() || auth.currentUser == null) {
      emit(null)
      return@flow
    }
    emitAll(
      db.collection("chats").document(chatId)
        .snapshots()
        .map { it.toObject(ChatThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { err ->
          val root = unwrapFirestoreError(err)
          if (auth.currentUser != null) handleFirestoreError(root, OperationType.GET, "chats/$chatId")
          throw root
        }
    )
  }

  fun observeMessages(chatId: String): Flow<List<ChatMessage>> = flow {
    if (chatId.isBlank() || auth.currentUser == null) {
      emit(emptyList())
      return@flow
    }
    emitAll(
      db.collection("chats").document(chatId).collection("messages")
        .snapshots()
        .map { snap ->
          snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.createdAt?.seconds ?: 0L }
        }
        .catch { err ->
          val root = unwrapFirestoreError(err)
          if (auth.currentUser != null) handleFirestoreError(root, OperationType.LIST, "chats/$chatId/messages")
          throw root
        }
    )
  }

  suspend fun getOrCreateChatThread(me: UserProfile, peer: UserProfile): Result<String> {
    return try {
      val uid = requireUserId()
      val chatId = chatIdFor(uid, peer.userId)
      val ref = db.collection("chats").document(chatId)
      val snap = ref.get().await()
      if (!snap.exists()) {
        val sortedUids = listOf(uid, peer.userId).sorted()
        val payload = mapOf(
          "participantUids" to sortedUids,
          "participantNames" to mapOf(uid to me.displayName, peer.userId to peer.displayName),
          "participantAvatars" to mapOf(uid to me.avatarUrl, peer.userId to peer.avatarUrl),
          "lastMessageText" to "",
          "lastSenderId" to "",
          "typingUid" to "",
          "unreadCounts" to mapOf(uid to 0L, peer.userId to 0L),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        ref.set(payload).await()
      }
      Result.success(chatId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "chats")
      Result.failure(e)
    }
  }

  suspend fun sendMessage(
    chatId: String,
    participantUids: List<String>,
    sender: UserProfile,
    peerId: String,
    messageType: String,
    text: String,
    mediaUrl: String = "",
    replyToMessageId: String = "",
    replyToPreview: String = ""
  ): Result<Unit> {
    val cleanText = text.trim().take(2000)
    if (messageType == "text" && cleanText.isBlank()) {
      return Result.failure(IllegalArgumentException("Message cannot be empty."))
    }
    return try {
      val uid = requireUserId()
      val chatRef = db.collection("chats").document(chatId)
      val msgRef = chatRef.collection("messages").document()
      val preview = when (messageType) {
        "image" -> "📷 Photo"
        "video" -> "🎬 Video"
        "voice" -> "🎤 Voice note"
        else -> cleanText.take(120)
      }
      val msgPayload = buildMap<String, Any> {
        put("chatId", chatId)
        put("participantUids", participantUids)
        put("senderId", uid)
        put("messageType", messageType)
        if (cleanText.isNotBlank()) put("text", cleanText)
        if (mediaUrl.isNotBlank()) put("mediaUrl", mediaUrl)
        if (replyToMessageId.isNotBlank()) put("replyToMessageId", replyToMessageId)
        if (replyToPreview.isNotBlank()) put("replyToPreview", replyToPreview.take(160))
        put("readBy", listOf(uid))
        put("deleted", false)
        put("createdAt", FieldValue.serverTimestamp())
      }
      val batch = db.batch()
      batch.set(msgRef, msgPayload)
      batch.update(
        chatRef,
        mapOf(
          "lastMessageText" to preview,
          "lastSenderId" to uid,
          "typingUid" to "",
          "unreadCounts.$peerId" to FieldValue.increment(1L),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      val notifPayload = buildMap<String, Any> {
        put("recipientId", peerId)
        put("actorId", uid)
        put("actorUsername", sender.username)
        if (sender.avatarUrl.isNotBlank()) put("actorAvatarUrl", sender.avatarUrl)
        put("type", "message")
        put("targetId", chatId)
        put("message", preview)
        put("read", false)
        put("createdAt", FieldValue.serverTimestamp())
      }
      batch.set(db.collection("notifications").document(), notifPayload)
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "chats/$chatId/messages")
      Result.failure(e)
    }
  }

  suspend fun markThreadRead(chatId: String, unreadMessages: List<ChatMessage>): Result<Unit> {
    return try {
      val uid = requireUserId()
      val chatRef = db.collection("chats").document(chatId)
      val batch = db.batch()
      batch.update(chatRef, mapOf("unreadCounts.$uid" to 0L, "updatedAt" to FieldValue.serverTimestamp()))
      unreadMessages.filter { uid !in it.readBy }.take(20).forEach { msg ->
        val updatedRead = (msg.readBy + uid).distinct().take(2)
        batch.update(chatRef.collection("messages").document(msg.id), mapOf("readBy" to updatedRead))
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun setTypingStatus(chatId: String, isTyping: Boolean): Result<Unit> {
    return try {
      val uid = requireUserId()
      db.collection("chats").document(chatId).update(
        mapOf(
          "typingUid" to if (isTyping) uid else "",
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun reactToMessage(chatId: String, messageId: String, emoji: String): Result<Unit> {
    return try {
      requireUserId()
      db.collection("chats").document(chatId).collection("messages").document(messageId)
        .update(mapOf("reactionEmoji" to emoji.take(16))).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId/messages/$messageId")
      Result.failure(e)
    }
  }

  suspend fun softDeleteMessage(chatId: String, messageId: String): Result<Unit> {
    return try {
      requireUserId()
      db.collection("chats").document(chatId).collection("messages").document(messageId)
        .update(mapOf("deleted" to true, "text" to "Message deleted")).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId/messages/$messageId")
      Result.failure(e)
    }
  }
}
