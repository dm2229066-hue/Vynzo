package com.example.data.repository

import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.firebase.unwrapFirestoreError
import com.example.data.model.FollowRelation
import com.example.data.model.UserPrivateInfo
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserRepository(
  private val db: FirebaseFirestore
) {
  private val auth get() = Firebase.auth

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
    if (userId.isBlank() || auth.currentUser == null) {
      emit(null)
      return@flow
    }
    val path = "users/$userId"
    emitAll(
      db.collection("users").document(userId)
        .snapshots()
        .map { snap -> snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { err ->
          val root = unwrapFirestoreError(err)
          if (auth.currentUser != null) handleFirestoreError(root, OperationType.GET, path)
          throw root
        }
    )
  }

  fun observePrivateInfo(userId: String): Flow<UserPrivateInfo?> = flow {
    if (userId.isBlank() || auth.currentUser?.uid != userId) {
      emit(null)
      return@flow
    }
    val path = "users/$userId/private/info"
    emitAll(
      db.collection("users").document(userId).collection("private").document("info")
        .snapshots()
        .map { snap -> snap.toObject(UserPrivateInfo::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { err ->
          val root = unwrapFirestoreError(err)
          if (auth.currentUser?.uid == userId) handleFirestoreError(root, OperationType.GET, path)
          throw root
        }
    )
  }

  fun observeActiveCreators(limitCount: Long = 40L): Flow<List<UserProfile>> = flow {
    if (auth.currentUser == null) {
      emit(emptyList())
      return@flow
    }
    val path = "users"
    emitAll(
      db.collection("users")
        .whereEqualTo("status", "active")
        .limit(limitCount)
        .snapshots()
        .map { snap -> snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { err ->
          val root = unwrapFirestoreError(err)
          if (auth.currentUser != null) handleFirestoreError(root, OperationType.LIST, path)
          throw root
        }
    )
  }

  suspend fun checkUsernameAvailable(username: String, excludeUid: String): Result<Boolean> {
    val normalized = username.trim().lowercase()
    if (normalized.length < 3) return Result.success(false)
    return try {
      requireUserId()
      val snap = db.collection("users")
        .whereEqualTo("status", "active")
        .whereEqualTo("username", normalized)
        .limit(2)
        .get()
        .await()
      val takenByOther = snap.documents.any { it.id != excludeUid }
      Result.success(!takenByOther)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "users")
      Result.failure(e)
    }
  }

  suspend fun createInitialProfile(
    username: String,
    displayName: String,
    bio: String,
    avatarUrl: String,
    email: String
  ): Result<Unit> {
    val cleanUsername = username.trim().lowercase().replace(Regex("[^a-z0-9_.]"), "").take(30)
    val cleanDisplay = displayName.trim().take(50)
    if (cleanUsername.length < 3) return Result.failure(IllegalArgumentException("Username must be at least 3 characters."))
    if (cleanDisplay.isBlank()) return Result.failure(IllegalArgumentException("Display name cannot be empty."))

    return try {
      val uid = requireUserId()
      val profile = UserProfile(
        userId = uid,
        username = cleanUsername,
        displayName = cleanDisplay,
        bio = bio.trim().take(200),
        avatarUrl = avatarUrl,
        status = "active"
      )
      val batch = db.batch()
      batch.set(db.collection("users").document(uid), profile.toCreateMap())
      batch.set(
        db.collection("users").document(uid).collection("private").document("info"),
        mapOf(
          "userId" to uid,
          "email" to email.take(200),
          "themeMode" to "dark",
          "notifyLikes" to true,
          "notifyComments" to true,
          "notifyFollows" to true,
          "notifyMessages" to true,
          "adsConsentAccepted" to true,
          "blockedUids" to emptyList<String>(),
          "restrictedUids" to emptyList<String>(),
          "recentSearches" to emptyList<String>(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "users")
      Result.failure(e)
    }
  }

  suspend fun updateProfile(
    username: String,
    displayName: String,
    bio: String,
    website: String,
    avatarUrl: String?,
    coverUrl: String?
  ): Result<Unit> {
    val cleanUsername = username.trim().lowercase().replace(Regex("[^a-z0-9_.]"), "").take(30)
    val cleanDisplay = displayName.trim().take(50)
    if (cleanUsername.length < 3) return Result.failure(IllegalArgumentException("Username must be at least 3 characters."))
    if (cleanDisplay.isBlank()) return Result.failure(IllegalArgumentException("Display name is required."))

    var path = "users"
    return try {
      val uid = requireUserId()
      path = "users/$uid"
      val updates = buildMap<String, Any> {
        put("username", cleanUsername)
        put("displayName", cleanDisplay)
        put("bio", bio.trim().take(200))
        put("website", website.trim().take(200))
        if (!avatarUrl.isNullOrBlank()) put("avatarUrl", avatarUrl)
        if (!coverUrl.isNullOrBlank()) put("coverUrl", coverUrl)
        put("updatedAt", FieldValue.serverTimestamp())
      }
      db.collection("users").document(uid).update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun updatePrivacySettings(
    privateAccount: Boolean,
    commentControl: String,
    messagePrivacy: String
  ): Result<Unit> {
    var path = "users"
    return try {
      val uid = requireUserId()
      path = "users/$uid"
      db.collection("users").document(uid).update(
        mapOf(
          "privateAccount" to privateAccount,
          "commentControl" to commentControl,
          "messagePrivacy" to messagePrivacy,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun updatePrivatePreferences(updates: Map<String, Any>): Result<Unit> {
    var path = "users"
    return try {
      val uid = requireUserId()
      path = "users/$uid/private/info"
      val payload = updates.toMutableMap().apply {
        put("userId", uid)
        put("updatedAt", FieldValue.serverTimestamp())
      }
      db.collection("users").document(uid).collection("private").document("info")
        .set(payload, SetOptions.merge()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun setOnlineStatus(online: Boolean): Result<Unit> {
    return try {
      val uid = auth.currentUser?.uid ?: return Result.success(Unit)
      val docRef = db.collection("users").document(uid)
      val snap = docRef.get().await()
      if (snap.exists()) {
        docRef.update(
          mapOf(
            "online" to online,
            "lastActiveAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun observeFollowRelation(followerId: String, targetId: String): Flow<FollowRelation?> = flow {
    if (followerId.isBlank() || targetId.isBlank() || auth.currentUser == null) {
      emit(null)
      return@flow
    }
    val docId = "${followerId}_${targetId}"
    emitAll(
      db.collection("follows").document(docId)
        .snapshots()
        .map { it.toObject(FollowRelation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { err ->
          val root = unwrapFirestoreError(err)
          if (auth.currentUser != null) handleFirestoreError(root, OperationType.GET, "follows/$docId")
          throw root
        }
    )
  }

  fun observeFollowers(targetId: String): Flow<List<FollowRelation>> = flow {
    if (targetId.isBlank() || auth.currentUser == null) {
      emit(emptyList())
      return@flow
    }
    emitAll(
      db.collection("follows")
        .whereEqualTo("targetId", targetId)
        .whereEqualTo("status", "accepted")
        .snapshots()
        .map { it.toObjects(FollowRelation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { err ->
          val root = unwrapFirestoreError(err)
          if (auth.currentUser != null) handleFirestoreError(root, OperationType.LIST, "follows")
          throw root
        }
    )
  }

  fun observeFollowing(followerId: String): Flow<List<FollowRelation>> = flow {
    if (followerId.isBlank() || auth.currentUser == null) {
      emit(emptyList())
      return@flow
    }
    emitAll(
      db.collection("follows")
        .whereEqualTo("followerId", followerId)
        .whereEqualTo("status", "accepted")
        .snapshots()
        .map { it.toObjects(FollowRelation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { err ->
          val root = unwrapFirestoreError(err)
          if (auth.currentUser != null) handleFirestoreError(root, OperationType.LIST, "follows")
          throw root
        }
    )
  }

  fun observeIncomingFollowRequests(targetId: String): Flow<List<FollowRelation>> = flow {
    if (targetId.isBlank() || auth.currentUser?.uid != targetId) {
      emit(emptyList())
      return@flow
    }
    emitAll(
      db.collection("follows")
        .whereEqualTo("targetId", targetId)
        .whereEqualTo("status", "pending")
        .snapshots()
        .map { it.toObjects(FollowRelation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { err ->
          val root = unwrapFirestoreError(err)
          if (auth.currentUser?.uid == targetId) handleFirestoreError(root, OperationType.LIST, "follows")
          throw root
        }
    )
  }

  suspend fun followOrRequestUser(currentUser: UserProfile, target: UserProfile): Result<String> {
    return try {
      val uid = requireUserId()
      if (uid == target.userId) return Result.failure(IllegalArgumentException("Cannot follow yourself."))
      val docId = "${uid}_${target.userId}"
      val status = if (target.privateAccount) "pending" else "accepted"
      val payload = buildMap<String, Any> {
        put("followerId", uid)
        put("followerUsername", currentUser.username)
        if (currentUser.avatarUrl.isNotBlank()) put("followerAvatarUrl", currentUser.avatarUrl)
        put("targetId", target.userId)
        put("targetUsername", target.username)
        if (target.avatarUrl.isNotBlank()) put("targetAvatarUrl", target.avatarUrl)
        put("status", status)
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
      }
      val batch = db.batch()
      batch.set(db.collection("follows").document(docId), payload)
      if (status == "accepted") {
        batch.update(
          db.collection("users").document(uid),
          mapOf("followingCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
        )
        batch.update(
          db.collection("users").document(target.userId),
          mapOf("followersCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
        )
      }
      val notifRef = db.collection("notifications").document()
      val notifPayload = buildMap<String, Any> {
        put("recipientId", target.userId)
        put("actorId", uid)
        put("actorUsername", currentUser.username)
        if (currentUser.avatarUrl.isNotBlank()) put("actorAvatarUrl", currentUser.avatarUrl)
        put("type", if (status == "pending") "follow_request" else "follow")
        put("targetId", uid)
        put("message", if (status == "pending") "requested to follow you" else "started following you")
        put("read", false)
        put("createdAt", FieldValue.serverTimestamp())
      }
      batch.set(notifRef, notifPayload)
      batch.commit().await()
      Result.success(status)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "follows")
      Result.failure(e)
    }
  }

  suspend fun unfollowUser(targetId: String, wasAccepted: Boolean): Result<Unit> {
    return try {
      val uid = requireUserId()
      val docId = "${uid}_${targetId}"
      val batch = db.batch()
      batch.delete(db.collection("follows").document(docId))
      if (wasAccepted) {
        batch.update(
          db.collection("users").document(uid),
          mapOf("followingCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp())
        )
        batch.update(
          db.collection("users").document(targetId),
          mapOf("followersCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp())
        )
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, "follows")
      Result.failure(e)
    }
  }

  suspend fun respondToFollowRequest(relation: FollowRelation, accept: Boolean): Result<Unit> {
    return try {
      val uid = requireUserId()
      val docId = "${relation.followerId}_${uid}"
      val batch = db.batch()
      if (accept) {
        batch.update(
          db.collection("follows").document(docId),
          mapOf("status" to "accepted", "updatedAt" to FieldValue.serverTimestamp())
        )
        batch.update(
          db.collection("users").document(uid),
          mapOf("followersCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
        )
      } else {
        batch.delete(db.collection("follows").document(docId))
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "follows")
      Result.failure(e)
    }
  }

  suspend fun toggleBlockUser(targetUid: String, currentBlocked: List<String>): Result<Boolean> {
    return try {
      val uid = requireUserId()
      val nowBlocked = if (targetUid in currentBlocked) {
        currentBlocked - targetUid
      } else {
        (currentBlocked + targetUid).distinct().take(200)
      }
      updatePrivatePreferences(mapOf("blockedUids" to nowBlocked)).getOrThrow()
      Result.success(targetUid in nowBlocked)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun toggleRestrictUser(targetUid: String, currentRestricted: List<String>): Result<Boolean> {
    return try {
      requireUserId()
      val nowRestricted = if (targetUid in currentRestricted) {
        currentRestricted - targetUid
      } else {
        (currentRestricted + targetUid).distinct().take(200)
      }
      updatePrivatePreferences(mapOf("restrictedUids" to nowRestricted)).getOrThrow()
      Result.success(targetUid in nowRestricted)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun saveRecentSearch(query: String, currentSearches: List<String>): Result<Unit> {
    val clean = query.trim().take(60)
    if (clean.isBlank()) return Result.success(Unit)
    val updated = (listOf(clean) + currentSearches.filterNot { it.equals(clean, ignoreCase = true) }).take(15)
    return updatePrivatePreferences(mapOf("recentSearches" to updated))
  }

  suspend fun deleteCurrentUserAccount(): Result<Unit> {
    return try {
      val user = auth.currentUser ?: throw IllegalStateException("Not signed in")
      val uid = user.uid
      db.collection("users").document(uid).collection("private").document("info").delete().await()
      db.collection("users").document(uid).delete().await()
      user.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, "users")
      Result.failure(e)
    }
  }
}
