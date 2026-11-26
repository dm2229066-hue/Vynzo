package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val username: String = "",
  val displayName: String = "",
  val bio: String = "",
  val website: String = "",
  val avatarUrl: String = "",
  val coverUrl: String = "",
  val privateAccount: Boolean = false,
  val status: String = "active",
  val commentControl: String = "everyone",
  val messagePrivacy: String = "everyone",
  val followersCount: Long = 0L,
  val followingCount: Long = 0L,
  val postsCount: Long = 0L,
  val verified: Boolean = false,
  val online: Boolean = true,
  @ServerTimestamp val lastActiveAt: Timestamp? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun toCreateMap(): Map<String, Any> = buildMap {
    put("userId", userId)
    put("username", username.trim().lowercase().take(30))
    put("displayName", displayName.trim().take(50))
    if (bio.isNotBlank()) put("bio", bio.trim().take(200))
    if (website.isNotBlank()) put("website", website.trim().take(200))
    if (avatarUrl.isNotBlank()) put("avatarUrl", avatarUrl)
    if (coverUrl.isNotBlank()) put("coverUrl", coverUrl)
    put("privateAccount", privateAccount)
    put("status", "active")
    put("commentControl", commentControl)
    put("messagePrivacy", messagePrivacy)
    put("followersCount", followersCount.coerceAtLeast(0L))
    put("followingCount", followingCount.coerceAtLeast(0L))
    put("postsCount", postsCount.coerceAtLeast(0L))
    put("verified", false)
    put("online", true)
    put("lastActiveAt", FieldValue.serverTimestamp())
    put("createdAt", FieldValue.serverTimestamp())
    put("updatedAt", FieldValue.serverTimestamp())
  }
}

@Keep
@IgnoreExtraProperties
data class UserPrivateInfo(
  @DocumentId val id: String = "",
  val userId: String = "",
  val email: String = "",
  val fcmToken: String = "",
  val themeMode: String = "dark",
  val notifyLikes: Boolean = true,
  val notifyComments: Boolean = true,
  val notifyFollows: Boolean = true,
  val notifyMessages: Boolean = true,
  val adsConsentAccepted: Boolean = true,
  val blockedUids: List<String> = emptyList(),
  val restrictedUids: List<String> = emptyList(),
  val recentSearches: List<String> = emptyList(),
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Post(
  @DocumentId val id: String = "",
  val authorId: String = "",
  val authorUsername: String = "",
  val authorDisplayName: String = "",
  val authorAvatarUrl: String = "",
  val mediaType: String = "image", // image, carousel, video, reel
  val mediaUrls: List<String> = emptyList(),
  val coverUrl: String = "",
  val caption: String = "",
  val hashtags: List<String> = emptyList(),
  val taggedUserIds: List<String> = emptyList(),
  val locationName: String = "",
  val musicTitle: String = "",
  val visibility: String = "public", // public, followers, draft, removed
  val commentsDisabled: Boolean = false,
  val likesCount: Long = 0L,
  val commentsCount: Long = 0L,
  val sharesCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun toCreateMap(): Map<String, Any> = buildMap {
    put("authorId", authorId)
    put("authorUsername", authorUsername.take(30))
    if (authorDisplayName.isNotBlank()) put("authorDisplayName", authorDisplayName.take(50))
    if (authorAvatarUrl.isNotBlank()) put("authorAvatarUrl", authorAvatarUrl)
    put("mediaType", mediaType)
    put("mediaUrls", mediaUrls.take(10))
    if (coverUrl.isNotBlank()) put("coverUrl", coverUrl)
    if (caption.isNotBlank()) put("caption", caption.take(2200))
    put("hashtags", hashtags.take(30))
    put("taggedUserIds", taggedUserIds.take(20))
    if (locationName.isNotBlank()) put("locationName", locationName.take(120))
    if (musicTitle.isNotBlank()) put("musicTitle", musicTitle.take(120))
    put("visibility", visibility)
    put("commentsDisabled", commentsDisabled)
    put("likesCount", likesCount.coerceAtLeast(0L))
    put("commentsCount", commentsCount.coerceAtLeast(0L))
    put("sharesCount", sharesCount.coerceAtLeast(0L))
    put("createdAt", FieldValue.serverTimestamp())
    put("updatedAt", FieldValue.serverTimestamp())
  }
}

@Keep
@IgnoreExtraProperties
data class PostComment(
  @DocumentId val id: String = "",
  val postId: String = "",
  val postAuthorId: String = "",
  val authorId: String = "",
  val authorUsername: String = "",
  val authorAvatarUrl: String = "",
  val text: String = "",
  val hidden: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Story(
  @DocumentId val id: String = "",
  val authorId: String = "",
  val authorUsername: String = "",
  val authorAvatarUrl: String = "",
  val mediaType: String = "image",
  val mediaUrl: String = "",
  val caption: String = "",
  val visibility: String = "public",
  val viewsCount: Long = 0L,
  val expiresAt: Timestamp? = null,
  @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StoryView(
  @DocumentId val id: String = "",
  val viewerId: String = "",
  val viewerUsername: String = "",
  val viewerAvatarUrl: String = "",
  val reaction: String = "",
  @ServerTimestamp val viewedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StoryHighlight(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val coverUrl: String = "",
  val storyMediaUrls: List<String> = emptyList(),
  val visibility: String = "public",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Bookmark(
  @DocumentId val id: String = "",
  val userId: String = "",
  val postId: String = "",
  val collectionName: String = "All Saved",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FollowRelation(
  @DocumentId val id: String = "",
  val followerId: String = "",
  val followerUsername: String = "",
  val followerAvatarUrl: String = "",
  val targetId: String = "",
  val targetUsername: String = "",
  val targetAvatarUrl: String = "",
  val status: String = "accepted", // accepted, pending
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatThread(
  @DocumentId val id: String = "",
  val participantUids: List<String> = emptyList(),
  val participantNames: Map<String, String> = emptyMap(),
  val participantAvatars: Map<String, String> = emptyMap(),
  val lastMessageText: String = "",
  val lastSenderId: String = "",
  val typingUid: String = "",
  val unreadCounts: Map<String, Long> = emptyMap(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
  @DocumentId val id: String = "",
  val chatId: String = "",
  val participantUids: List<String> = emptyList(),
  val senderId: String = "",
  val messageType: String = "text", // text, image, video, voice
  val text: String = "",
  val mediaUrl: String = "",
  val replyToMessageId: String = "",
  val replyToPreview: String = "",
  val reactionEmoji: String = "",
  val readBy: List<String> = emptyList(),
  val deleted: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
  @DocumentId val id: String = "",
  val recipientId: String = "",
  val actorId: String = "",
  val actorUsername: String = "",
  val actorAvatarUrl: String = "",
  val type: String = "like",
  val targetId: String = "",
  val message: String = "",
  val read: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ModerationReport(
  @DocumentId val id: String = "",
  val reporterId: String = "",
  val targetType: String = "post",
  val targetId: String = "",
  val reason: String = "",
  val details: String = "",
  val status: String = "open", // open, resolved, dismissed
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HashtagEntry(
  @DocumentId val id: String = "",
  val tag: String = "",
  val postsCount: Long = 0L,
  val status: String = "active", // active, blocked
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminConfig(
  @DocumentId val id: String = "",
  val active: Boolean = true,
  val adsEnabled: Boolean = true,
  val nativeFeedInterval: Long = 5L,
  val interstitialCooldownSec: Long = 120L,
  @ServerTimestamp val updatedAt: Timestamp? = null
)
