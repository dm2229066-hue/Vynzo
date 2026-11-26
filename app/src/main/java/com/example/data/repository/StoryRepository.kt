package com.example.data.repository

import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.firebase.unwrapFirestoreError
import com.example.data.model.Story
import com.example.data.model.StoryHighlight
import com.example.data.model.StoryView
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
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
import java.util.Date

class StoryRepository(
  private val db: FirebaseFirestore
) {
  private val auth get() = Firebase.auth

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  fun observeActiveStories(): Flow<List<Story>> = flow {
    if (auth.currentUser == null) {
      emit(emptyList())
      return@flow
    }
    emitAll(
      db.collection("stories")
        .whereEqualTo("visibility", "public")
        .limit(60)
        .snapshots()
        .map { snap ->
          val nowSec = Timestamp.now().seconds
          snap.toObjects(Story::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .filter { (it.expiresAt?.seconds ?: Long.MAX_VALUE) > nowSec }
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { err ->
          val root = unwrapFirestoreError(err)
          if (auth.currentUser != null) handleFirestoreError(root, OperationType.LIST, "stories")
          throw root
        }
    )
  }

  fun observeHighlights(userId: String): Flow<List<StoryHighlight>> = flow {
    if (userId.isBlank() || auth.currentUser == null) {
      emit(emptyList())
      return@flow
    }
    val query = if (auth.currentUser?.uid == userId) {
      db.collection("users").document(userId).collection("highlights")
    } else {
      db.collection("users").document(userId).collection("highlights")
        .whereEqualTo("visibility", "public")
    }
    emitAll(
      query.snapshots()
        .map { snap ->
          snap.toObjects(StoryHighlight::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { err ->
          val root = unwrapFirestoreError(err)
          if (auth.currentUser != null) handleFirestoreError(root, OperationType.LIST, "users/$userId/highlights")
          throw root
        }
    )
  }

  fun observeStoryViews(storyId: String): Flow<List<StoryView>> = flow {
    if (storyId.isBlank() || auth.currentUser == null) {
      emit(emptyList())
      return@flow
    }
    emitAll(
      db.collection("stories").document(storyId).collection("views")
        .snapshots()
        .map { snap ->
          snap.toObjects(StoryView::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.viewedAt?.seconds ?: 0L }
        }
        .catch { err ->
          val root = unwrapFirestoreError(err)
          if (auth.currentUser != null) handleFirestoreError(root, OperationType.LIST, "stories/$storyId/views")
          throw root
        }
    )
  }

  suspend fun createStory(
    author: UserProfile,
    mediaType: String,
    mediaUrl: String,
    caption: String,
    visibility: String = "public"
  ): Result<String> {
    if (mediaUrl.isBlank()) return Result.failure(IllegalArgumentException("Story media is required."))
    return try {
      val uid = requireUserId()
      val storyRef = db.collection("stories").document()
      val expiresAt = Timestamp(Date(System.currentTimeMillis() + 24L * 60L * 60L * 1000L))
      val payload = buildMap<String, Any> {
        put("authorId", uid)
        put("authorUsername", author.username)
        if (author.avatarUrl.isNotBlank()) put("authorAvatarUrl", author.avatarUrl)
        put("mediaType", mediaType)
        put("mediaUrl", mediaUrl)
        if (caption.isNotBlank()) put("caption", caption.trim().take(300))
        put("visibility", visibility)
        put("viewsCount", 0L)
        put("expiresAt", expiresAt)
        put("createdAt", FieldValue.serverTimestamp())
      }
      storyRef.set(payload).await()
      Result.success(storyRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "stories")
      Result.failure(e)
    }
  }

  suspend fun recordStoryView(story: Story, viewer: UserProfile, reaction: String = ""): Result<Unit> {
    return try {
      val uid = requireUserId()
      val storyRef = db.collection("stories").document(story.id)
      val viewRef = storyRef.collection("views").document(uid)
      val existing = viewRef.get().await().exists()
      val payload = buildMap<String, Any> {
        put("viewerId", uid)
        put("viewerUsername", viewer.username)
        if (viewer.avatarUrl.isNotBlank()) put("viewerAvatarUrl", viewer.avatarUrl)
        if (reaction.isNotBlank()) put("reaction", reaction.take(16))
        put("viewedAt", FieldValue.serverTimestamp())
      }
      val batch = db.batch()
      batch.set(viewRef, payload)
      if (!existing) {
        batch.update(storyRef, mapOf("viewsCount" to FieldValue.increment(1L)))
      }
      if (reaction.isNotBlank() && story.authorId != uid) {
        val notifPayload = buildMap<String, Any> {
          put("recipientId", story.authorId)
          put("actorId", uid)
          put("actorUsername", viewer.username)
          if (viewer.avatarUrl.isNotBlank()) put("actorAvatarUrl", viewer.avatarUrl)
          put("type", "story_reaction")
          put("targetId", story.id)
          put("message", "reacted $reaction to your story")
          put("read", false)
          put("createdAt", FieldValue.serverTimestamp())
        }
        batch.set(db.collection("notifications").document(), notifPayload)
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "stories/${story.id}/views")
      Result.failure(e)
    }
  }

  suspend fun createOrUpdateHighlight(
    highlightId: String?,
    title: String,
    coverUrl: String,
    storyMediaUrls: List<String>
  ): Result<Unit> {
    val cleanTitle = title.trim().take(40)
    if (cleanTitle.isBlank()) return Result.failure(IllegalArgumentException("Highlight title required"))
    return try {
      val uid = requireUserId()
      val col = db.collection("users").document(uid).collection("highlights")
      if (highlightId.isNullOrBlank()) {
        val payload = buildMap<String, Any> {
          put("userId", uid)
          put("title", cleanTitle)
          if (coverUrl.isNotBlank()) put("coverUrl", coverUrl)
          put("storyMediaUrls", storyMediaUrls.take(20))
          put("visibility", "public")
          put("createdAt", FieldValue.serverTimestamp())
          put("updatedAt", FieldValue.serverTimestamp())
        }
        col.document().set(payload).await()
      } else {
        val updates = buildMap<String, Any> {
          put("title", cleanTitle)
          if (coverUrl.isNotBlank()) put("coverUrl", coverUrl)
          put("storyMediaUrls", storyMediaUrls.take(20))
          put("updatedAt", FieldValue.serverTimestamp())
        }
        col.document(highlightId).update(updates).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "highlights")
      Result.failure(e)
    }
  }

  suspend fun deleteHighlight(highlightId: String): Result<Unit> {
    return try {
      val uid = requireUserId()
      db.collection("users").document(uid).collection("highlights").document(highlightId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, "highlights/$highlightId")
      Result.failure(e)
    }
  }
}
