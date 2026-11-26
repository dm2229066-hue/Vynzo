package com.example.data.repository

import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.firebase.unwrapFirestoreError
import com.example.data.model.Bookmark
import com.example.data.model.HashtagEntry
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PostRepository(
  private val db: FirebaseFirestore
) {
  private val auth get() = Firebase.auth

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  fun observePublicFeed(limitCount: Long = 30L): Flow<List<Post>> = flow {
    if (auth.currentUser == null) {
      emit(emptyList())
      return@flow
    }
    emitAll(
      db.collection("posts")
        .whereEqualTo("visibility", "public")
        .limit(limitCount)
        .snapshots()
        .map { snap ->
          snap.toObjects(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { err ->
          val root = unwrapFirestoreError(err)
          if (auth.currentUser != null) handleFirestoreError(root, OperationType.LIST, "posts")
          throw root
        }
    )
  }

  fun observeReelsFeed(limitCount: Long = 30L): Flow<List<Post>> = flow {
    if (auth.currentUser == null) {
      emit(emptyList())
      return@flow
    }
    emitAll(
      db.collection("posts")
        .whereEqualTo("visibility", "public")
        .whereEqualTo("mediaType", "reel")
        .limit(limitCount)
        .snapshots()
        .map { snap ->
          snap.toObjects(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { err ->
          val root = unwrapFirestoreError(err)
          if (auth.currentUser != null) handleFirestoreError(root, OperationType.LIST, "posts")
          throw root
        }
    )
  }

  fun observeUserPosts(authorId: String): Flow<List<Post>> = flow {
    if (authorId.isBlank() || auth.currentUser == null) {
      emit(emptyList())
      return@flow
    }
    val query = if (auth.currentUser?.uid == authorId) {
      db.collection("posts").whereEqualTo("authorId", authorId)
    } else {
      db.collection("posts").whereEqualTo("authorId", authorId).whereEqualTo("visibility", "public")
    }
    emitAll(
      query.snapshots()
        .map { snap ->
          snap.toObjects(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .filter { it.visibility != "removed" }
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { err ->
          val root = unwrapFirestoreError(err)
          if (auth.currentUser != null) handleFirestoreError(root, OperationType.LIST, "posts")
          throw root
        }
    )
  }

  fun observeTaggedPosts(userId: String): Flow<List<Post>> = flow {
    if (userId.isBlank() || auth.currentUser == null) {
      emit(emptyList())
      return@flow
    }
    emitAll(
      db.collection("posts")
        .whereEqualTo("visibility", "public")
        .whereArrayContains("taggedUserIds", userId)
        .snapshots()
        .map { snap ->
          snap.toObjects(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { err ->
          val root = unwrapFirestoreError(err)
          if (auth.currentUser != null) handleFirestoreError(root, OperationType.LIST, "posts")
          throw root
        }
    )
  }

  fun observeBookmarks(userId: String): Flow<List<Bookmark>> = flow {
    if (userId.isBlank() || auth.currentUser?.uid != userId) {
      emit(emptyList())
      return@flow
    }
    emitAll(
      db.collection("users").document(userId).collection("bookmarks")
        .snapshots()
        .map { snap ->
          snap.toObjects(Bookmark::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { err ->
          val root = unwrapFirestoreError(err)
          if (auth.currentUser?.uid == userId) handleFirestoreError(root, OperationType.LIST, "users/$userId/bookmarks")
          throw root
        }
    )
  }

  fun observeIsPostLiked(postId: String, userId: String): Flow<Boolean> = flow {
    if (postId.isBlank() || userId.isBlank() || auth.currentUser == null) {
      emit(false)
      return@flow
    }
    emitAll(
      db.collection("posts").document(postId).collection("likes").document(userId)
        .snapshots()
        .map { it.exists() }
        .catch { err ->
          val root = unwrapFirestoreError(err)
          if (auth.currentUser != null) handleFirestoreError(root, OperationType.GET, "posts/$postId/likes/$userId")
          throw root
        }
    )
  }

  fun observeComments(postId: String): Flow<List<PostComment>> = flow {
    if (postId.isBlank() || auth.currentUser == null) {
      emit(emptyList())
      return@flow
    }
    emitAll(
      db.collection("posts").document(postId).collection("comments")
        .whereEqualTo("hidden", false)
        .snapshots()
        .map { snap ->
          snap.toObjects(PostComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.createdAt?.seconds ?: 0L }
        }
        .catch { err ->
          val root = unwrapFirestoreError(err)
          if (auth.currentUser != null) handleFirestoreError(root, OperationType.LIST, "posts/$postId/comments")
          throw root
        }
    )
  }

  fun observeTrendingHashtags(): Flow<List<HashtagEntry>> = flow {
    if (auth.currentUser == null) {
      emit(emptyList())
      return@flow
    }
    emitAll(
      db.collection("hashtags")
        .whereEqualTo("status", "active")
        .limit(20)
        .snapshots()
        .map { snap ->
          snap.toObjects(HashtagEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.postsCount }
        }
        .catch { err ->
          val root = unwrapFirestoreError(err)
          if (auth.currentUser != null) handleFirestoreError(root, OperationType.LIST, "hashtags")
          throw root
        }
    )
  }

  suspend fun createPost(
    author: UserProfile,
    mediaType: String,
    mediaUrls: List<String>,
    coverUrl: String,
    caption: String,
    hashtags: List<String>,
    taggedUserIds: List<String>,
    locationName: String,
    musicTitle: String,
    isDraft: Boolean,
    commentsDisabled: Boolean
  ): Result<String> {
    if (mediaUrls.isEmpty()) return Result.failure(IllegalArgumentException("Select at least one photo or video."))
    return try {
      val uid = requireUserId()
      val postRef = db.collection("posts").document()
      val visibility = if (isDraft) "draft" else "public"
      val cleanTags = hashtags
        .map { it.trim().removePrefix("#").lowercase().replace(Regex("[^a-z0-9_]"), "") }
        .filter { it.isNotBlank() }
        .distinct()
        .take(30)

      val post = Post(
        authorId = uid,
        authorUsername = author.username,
        authorDisplayName = author.displayName,
        authorAvatarUrl = author.avatarUrl,
        mediaType = mediaType,
        mediaUrls = mediaUrls.take(10),
        coverUrl = coverUrl,
        caption = caption.trim().take(2200),
        hashtags = cleanTags,
        taggedUserIds = taggedUserIds.take(20),
        locationName = locationName.trim().take(120),
        musicTitle = musicTitle.trim().take(120),
        visibility = visibility,
        commentsDisabled = commentsDisabled
      )

      val batch = db.batch()
      batch.set(postRef, post.toCreateMap())
      if (!isDraft) {
        batch.update(
          db.collection("users").document(uid),
          mapOf("postsCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
        )
        cleanTags.take(5).forEach { tag ->
          val tagId = tag.take(60)
          val tagRef = db.collection("hashtags").document(tagId)
          batch.set(
            tagRef,
            mapOf(
              "tag" to tagId,
              "postsCount" to FieldValue.increment(1L),
              "status" to "active",
              "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
          )
        }
      }
      batch.commit().await()
      Result.success(postRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "posts")
      Result.failure(e)
    }
  }

  suspend fun publishDraft(postId: String): Result<Unit> {
    return try {
      val uid = requireUserId()
      val batch = db.batch()
      batch.update(
        db.collection("posts").document(postId),
        mapOf("visibility" to "public", "updatedAt" to FieldValue.serverTimestamp())
      )
      batch.update(
        db.collection("users").document(uid),
        mapOf("postsCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "posts/$postId")
      Result.failure(e)
    }
  }

  suspend fun deletePost(postId: String, wasPublic: Boolean): Result<Unit> {
    return try {
      val uid = requireUserId()
      val batch = db.batch()
      batch.delete(db.collection("posts").document(postId))
      if (wasPublic) {
        batch.update(
          db.collection("users").document(uid),
          mapOf("postsCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp())
        )
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, "posts/$postId")
      Result.failure(e)
    }
  }

  suspend fun toggleLike(post: Post, actor: UserProfile, currentlyLiked: Boolean): Result<Boolean> {
    return try {
      val uid = requireUserId()
      val postRef = db.collection("posts").document(post.id)
      val likeRef = postRef.collection("likes").document(uid)
      val batch = db.batch()
      if (currentlyLiked) {
        batch.delete(likeRef)
        batch.update(
          postRef,
          mapOf("likesCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp())
        )
      } else {
        batch.set(likeRef, mapOf("userId" to uid, "createdAt" to FieldValue.serverTimestamp()))
        batch.update(
          postRef,
          mapOf("likesCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
        )
        if (post.authorId != uid) {
          val notifPayload = buildMap<String, Any> {
            put("recipientId", post.authorId)
            put("actorId", uid)
            put("actorUsername", actor.username)
            if (actor.avatarUrl.isNotBlank()) put("actorAvatarUrl", actor.avatarUrl)
            put("type", if (post.mediaType == "reel") "reel" else "like")
            put("targetId", post.id)
            put("message", if (post.mediaType == "reel") "liked your Reel" else "liked your post")
            put("read", false)
            put("createdAt", FieldValue.serverTimestamp())
          }
          batch.set(db.collection("notifications").document(), notifPayload)
        }
      }
      batch.commit().await()
      Result.success(!currentlyLiked)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "posts/${post.id}")
      Result.failure(e)
    }
  }

  suspend fun toggleBookmark(postId: String, collectionName: String = "All Saved", currentlySaved: Boolean): Result<Boolean> {
    return try {
      val uid = requireUserId()
      val ref = db.collection("users").document(uid).collection("bookmarks").document(postId)
      if (currentlySaved) {
        ref.delete().await()
      } else {
        ref.set(
          mapOf(
            "userId" to uid,
            "postId" to postId,
            "collectionName" to collectionName.take(50),
            "createdAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Result.success(!currentlySaved)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "users/bookmarks/$postId")
      Result.failure(e)
    }
  }

  suspend fun addComment(post: Post, actor: UserProfile, text: String): Result<Unit> {
    val cleanText = text.trim().take(1000)
    if (cleanText.isBlank()) return Result.failure(IllegalArgumentException("Comment cannot be empty."))
    return try {
      val uid = requireUserId()
      val postRef = db.collection("posts").document(post.id)
      val commentRef = postRef.collection("comments").document()
      val payload = buildMap<String, Any> {
        put("postId", post.id)
        put("postAuthorId", post.authorId)
        put("authorId", uid)
        put("authorUsername", actor.username)
        if (actor.avatarUrl.isNotBlank()) put("authorAvatarUrl", actor.avatarUrl)
        put("text", cleanText)
        put("hidden", false)
        put("createdAt", FieldValue.serverTimestamp())
      }
      val batch = db.batch()
      batch.set(commentRef, payload)
      batch.update(
        postRef,
        mapOf("commentsCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
      )
      if (post.authorId != uid) {
        val notifPayload = buildMap<String, Any> {
          put("recipientId", post.authorId)
          put("actorId", uid)
          put("actorUsername", actor.username)
          if (actor.avatarUrl.isNotBlank()) put("actorAvatarUrl", actor.avatarUrl)
          put("type", "comment")
          put("targetId", post.id)
          put("message", "commented: ${cleanText.take(60)}")
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
      handleFirestoreError(e, OperationType.CREATE, "posts/${post.id}/comments")
      Result.failure(e)
    }
  }

  suspend fun hideComment(postId: String, commentId: String, hidden: Boolean): Result<Unit> {
    return try {
      requireUserId()
      db.collection("posts").document(postId).collection("comments").document(commentId)
        .update(mapOf("hidden" to hidden)).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "posts/$postId/comments/$commentId")
      Result.failure(e)
    }
  }

  suspend fun incrementShare(post: Post, actor: UserProfile): Result<Unit> {
    return try {
      val uid = requireUserId()
      val batch = db.batch()
      batch.update(
        db.collection("posts").document(post.id),
        mapOf("sharesCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
      )
      if (post.authorId != uid) {
        val notifPayload = buildMap<String, Any> {
          put("recipientId", post.authorId)
          put("actorId", uid)
          put("actorUsername", actor.username)
          if (actor.avatarUrl.isNotBlank()) put("actorAvatarUrl", actor.avatarUrl)
          put("type", "share")
          put("targetId", post.id)
          put("message", "shared your post")
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
      handleFirestoreError(e, OperationType.UPDATE, "posts/${post.id}")
      Result.failure(e)
    }
  }

  suspend fun submitReport(targetType: String, targetId: String, reason: String, details: String): Result<Unit> {
    return try {
      val uid = requireUserId()
      val payload = buildMap<String, Any> {
        put("reporterId", uid)
        put("targetType", targetType)
        put("targetId", targetId)
        put("reason", reason.trim().take(120))
        if (details.isNotBlank()) put("details", details.trim().take(1000))
        put("status", "open")
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
      }
      db.collection("reports").document().set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "reports")
      Result.failure(e)
    }
  }
}
