package com.example.data.repository

import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.firebase.unwrapFirestoreError
import com.example.data.model.AdminConfig
import com.example.data.model.AppNotification
import com.example.data.model.HashtagEntry
import com.example.data.model.ModerationReport
import com.example.data.model.Post
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

class NotificationRepository(
  private val db: FirebaseFirestore
) {
  private val auth get() = Firebase.auth

  private fun requireUserId(): String =
    auth.currentUser?.uid ?: throw IllegalStateException("Not authenticated")

  fun observeNotifications(userId: String): Flow<List<AppNotification>> = flow {
    if (userId.isBlank() || auth.currentUser?.uid != userId) {
      emit(emptyList())
      return@flow
    }
    emitAll(
      db.collection("notifications")
        .whereEqualTo("recipientId", userId)
        .limit(60)
        .snapshots()
        .map { snap ->
          snap.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { err ->
          val root = unwrapFirestoreError(err)
          if (auth.currentUser?.uid == userId) handleFirestoreError(root, OperationType.LIST, "notifications")
          throw root
        }
    )
  }

  suspend fun markAllRead(notifications: List<AppNotification>): Result<Unit> {
    return try {
      requireUserId()
      val batch = db.batch()
      notifications.filter { !it.read }.take(40).forEach { notif ->
        batch.update(db.collection("notifications").document(notif.id), mapOf("read" to true))
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "notifications")
      Result.failure(e)
    }
  }
}

class AdminRepository(
  private val db: FirebaseFirestore
) {
  private val auth get() = Firebase.auth

  private fun requireUserId(): String =
    auth.currentUser?.uid ?: throw IllegalStateException("Not authenticated")

  suspend fun checkIsAdmin(): Boolean {
    val user = auth.currentUser ?: return false
    if (user.isEmailVerified && user.email == "rahul11223344d@gmail.com") return true
    return runCatching {
      db.collection("admins").document(user.uid).get().await().exists()
    }.getOrDefault(false)
  }

  fun observeAdsConfig(): Flow<AdminConfig> = flow {
    if (auth.currentUser == null) {
      emit(AdminConfig())
      return@flow
    }
    emitAll(
      db.collection("app_config").document("ads_settings")
        .snapshots()
        .map { snap ->
          snap.toObject(AdminConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: AdminConfig()
        }
        .catch { err ->
          val root = unwrapFirestoreError(err)
          if (auth.currentUser != null) handleFirestoreError(root, OperationType.GET, "app_config/ads_settings")
          emit(AdminConfig())
        }
    )
  }

  fun observeAllUsersAdmin(): Flow<List<UserProfile>> = flow {
    if (auth.currentUser == null) {
      emit(emptyList())
      return@flow
    }
    emitAll(
      db.collection("users").limit(80).snapshots()
        .map { it.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { err ->
          val root = unwrapFirestoreError(err)
          if (auth.currentUser != null) handleFirestoreError(root, OperationType.LIST, "users")
          throw root
        }
    )
  }

  fun observeAllPostsAdmin(): Flow<List<Post>> = flow {
    if (auth.currentUser == null) {
      emit(emptyList())
      return@flow
    }
    emitAll(
      db.collection("posts").limit(80).snapshots()
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

  fun observeReportsAdmin(): Flow<List<ModerationReport>> = flow {
    if (auth.currentUser == null) {
      emit(emptyList())
      return@flow
    }
    emitAll(
      db.collection("reports").limit(60).snapshots()
        .map { snap ->
          snap.toObjects(ModerationReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { err ->
          val root = unwrapFirestoreError(err)
          if (auth.currentUser != null) handleFirestoreError(root, OperationType.LIST, "reports")
          throw root
        }
    )
  }

  fun observeAllHashtagsAdmin(): Flow<List<HashtagEntry>> = flow {
    if (auth.currentUser == null) {
      emit(emptyList())
      return@flow
    }
    emitAll(
      db.collection("hashtags").limit(60).snapshots()
        .map { it.toObjects(HashtagEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { err ->
          val root = unwrapFirestoreError(err)
          if (auth.currentUser != null) handleFirestoreError(root, OperationType.LIST, "hashtags")
          throw root
        }
    )
  }

  suspend fun setUserStatus(targetUid: String, status: String): Result<Unit> {
    return try {
      requireUserId()
      db.collection("users").document(targetUid)
        .update(mapOf("status" to status, "updatedAt" to FieldValue.serverTimestamp())).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "users/$targetUid")
      Result.failure(e)
    }
  }

  suspend fun setUserVerified(targetUid: String, verified: Boolean): Result<Unit> {
    return try {
      requireUserId()
      db.collection("users").document(targetUid)
        .update(mapOf("verified" to verified, "updatedAt" to FieldValue.serverTimestamp())).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "users/$targetUid")
      Result.failure(e)
    }
  }

  suspend fun deleteUserRecord(targetUid: String): Result<Unit> {
    return try {
      requireUserId()
      db.collection("users").document(targetUid).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, "users/$targetUid")
      Result.failure(e)
    }
  }

  suspend fun setPostVisibility(postId: String, visibility: String): Result<Unit> {
    return try {
      requireUserId()
      db.collection("posts").document(postId)
        .update(mapOf("visibility" to visibility, "updatedAt" to FieldValue.serverTimestamp())).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "posts/$postId")
      Result.failure(e)
    }
  }

  suspend fun resolveReport(reportId: String, status: String): Result<Unit> {
    return try {
      requireUserId()
      db.collection("reports").document(reportId)
        .update(mapOf("status" to status, "updatedAt" to FieldValue.serverTimestamp())).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "reports/$reportId")
      Result.failure(e)
    }
  }

  suspend fun setHashtagStatus(tagId: String, status: String): Result<Unit> {
    return try {
      requireUserId()
      db.collection("hashtags").document(tagId)
        .update(mapOf("status" to status, "updatedAt" to FieldValue.serverTimestamp())).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "hashtags/$tagId")
      Result.failure(e)
    }
  }

  suspend fun updateAdsConfig(
    adsEnabled: Boolean,
    nativeInterval: Long,
    interstitialCooldownSec: Long
  ): Result<Unit> {
    return try {
      requireUserId()
      db.collection("app_config").document("ads_settings").set(
        mapOf(
          "active" to true,
          "adsEnabled" to adsEnabled,
          "nativeFeedInterval" to nativeInterval.coerceIn(2L, 20L),
          "interstitialCooldownSec" to interstitialCooldownSec.coerceIn(30L, 600L),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "app_config/ads_settings")
      Result.failure(e)
    }
  }
}
