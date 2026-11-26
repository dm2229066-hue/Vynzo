package com.example.ui.explore

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.VynzoBannerAd
import com.example.data.model.Post
import com.example.data.model.UiState
import com.example.ui.components.VerifiedCreatorBadge
import com.example.ui.components.VynzoAvatar
import com.example.ui.components.VynzoMediaImage
import com.example.ui.theme.LocalVynzoColors
import com.example.ui.viewmodel.VynzoSessionViewModel
import kotlinx.coroutines.launch

@Composable
fun ExploreScreen(
  viewModel: VynzoSessionViewModel,
  initialTag: String? = null,
  onOpenUserProfile: (String) -> Unit,
  onOpenComments: (postId: String, postAuthorId: String) -> Unit,
  onOpenReels: () -> Unit
) {
  val colors = LocalVynzoColors.current
  val scope = rememberCoroutineScope()

  val feedState by viewModel.publicFeedState.collectAsStateWithLifecycle()
  val creators by viewModel.activeCreators.collectAsStateWithLifecycle()
  val hashtags by viewModel.trendingHashtags.collectAsStateWithLifecycle()
  val following by viewModel.myFollowing.collectAsStateWithLifecycle()
  val privateInfo by viewModel.myPrivateInfo.collectAsStateWithLifecycle()
  val adsConfig by viewModel.adsConfig.collectAsStateWithLifecycle()

  val blockedSet = remember(privateInfo) { privateInfo?.blockedUids?.toSet() ?: emptySet() }
  val followingMap = remember(following) { following.associateBy { it.targetId } }

  var query by remember(initialTag) { mutableStateOf(initialTag?.let { "#$it" } ?: "") }
  var selectedTab by remember { mutableIntStateOf(0) } // 0=All, 1=Accounts, 2=Posts, 3=Reels, 4=Tags
  var selectedCategory by remember { mutableStateOf("Trending") }
  val categories = listOf("Trending", "Photography", "Design", "Music", "Travel", "Tech")

  val allPosts = (feedState as? UiState.Success)?.data?.filter { it.authorId !in blockedSet } ?: emptyList()
  val cleanQ = query.trim().removePrefix("#").lowercase()

  val matchingUsers = remember(creators, cleanQ, blockedSet) {
    creators.filter { u ->
      u.userId !in blockedSet && (
        cleanQ.isBlank() ||
          u.username.lowercase().contains(cleanQ) ||
          u.displayName.lowercase().contains(cleanQ)
        )
    }
  }

  val matchingPosts = remember(allPosts, cleanQ, selectedCategory) {
    allPosts.filter { post ->
      val catMatch = selectedCategory == "Trending" ||
        post.caption.contains(selectedCategory, ignoreCase = true) ||
        post.hashtags.any { it.equals(selectedCategory, ignoreCase = true) }
      val qMatch = cleanQ.isBlank() ||
        post.caption.lowercase().contains(cleanQ) ||
        post.authorUsername.lowercase().contains(cleanQ) ||
        post.hashtags.any { it.lowercase().contains(cleanQ) }
      catMatch && qMatch
    }.sortedByDescending { it.likesCount + it.commentsCount * 2 + (it.createdAt?.seconds ?: 0L) / 10000L }
  }

  val matchingReels = remember(matchingPosts) {
    matchingPosts.filter { it.mediaType == "reel" || it.mediaType == "video" }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Search Bar
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text("Search creators, #hashtags, posts, Reels...") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
          if (query.isNotEmpty()) {
            IconButton(onClick = { query = "" }) {
              Icon(Icons.Rounded.Close, contentDescription = "Clear search")
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("explore_search_input")
      )

      if (query.isNotBlank()) {
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = {
              scope.launch {
                viewModel.userRepository.saveRecentSearch(query, privateInfo?.recentSearches ?: emptyList())
              }
            },
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("Save Search", style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    }

    // Recent Searches Strip
    val recents = privateInfo?.recentSearches.orEmpty()
    if (query.isBlank() && recents.isNotEmpty()) {
      LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(recents) { item ->
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.clickable { query = item }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(14.dp), tint = colors.mutedText)
              Spacer(Modifier.width(4.dp))
              Text(item, style = MaterialTheme.typography.labelMedium)
            }
          }
        }
      }
    }

    // Filter Tabs
    TabRow(selectedTabIndex = selectedTab) {
      listOf("All", "Users", "Posts", "Reels", "Tags").forEachIndexed { idx, label ->
        Tab(
          selected = selectedTab == idx,
          onClick = { selectedTab = idx },
          text = { Text(label, maxLines = 1) }
        )
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(bottom = 24.dp)
    ) {
      // Category Chips
      item(key = "categories") {
        LazyRow(
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(categories) { category ->
            FilterChip(
              selected = selectedCategory == category,
              onClick = { selectedCategory = category },
              label = { Text(category) }
            )
          }
        }
      }

      // Banner Ad
      item(key = "explore_banner_ad") {
        VynzoBannerAd(
          adsEnabled = adsConfig.adsEnabled,
          personalizedConsent = privateInfo?.adsConsentAccepted ?: true,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
        )
      }

      // Accounts Section
      if (selectedTab == 0 || selectedTab == 1) {
        if (matchingUsers.isNotEmpty()) {
          item(key = "accounts_header") {
            Text(
              text = if (cleanQ.isBlank()) "Suggested Accounts" else "Accounts",
              style = MaterialTheme.typography.titleMedium,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
          }
          items(matchingUsers.take(if (selectedTab == 1) 30 else 5), key = { "u_${it.userId}" }) { user ->
            val relation = followingMap[user.userId]
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenUserProfile(user.userId) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              VynzoAvatar(
                avatarUrl = user.avatarUrl,
                displayName = user.displayName,
                size = 46.dp,
                isOnline = user.online
              )
              Spacer(Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(user.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  if (user.verified) {
                    Spacer(Modifier.width(4.dp))
                    VerifiedCreatorBadge()
                  }
                }
                Text(
                  text = "@${user.username} · ${user.followersCount} followers",
                  style = MaterialTheme.typography.bodySmall,
                  color = colors.mutedText
                )
              }
              if (user.userId != viewModel.currentUserId) {
                if (relation != null) {
                  OutlinedButton(onClick = { viewModel.toggleFollow(user, relation) }) {
                    Text(if (relation.status == "pending") "Requested" else "Following")
                  }
                } else {
                  Button(onClick = { viewModel.toggleFollow(user, null) }) {
                    Text("Follow")
                  }
                }
              }
            }
          }
        }
      }

      // Hashtags Section
      if (selectedTab == 0 || selectedTab == 4) {
        val filteredTags = hashtags.filter {
          cleanQ.isBlank() || it.tag.lowercase().contains(cleanQ)
        }
        if (filteredTags.isNotEmpty()) {
          item(key = "tags_header") {
            Text(
              text = "Trending Hashtags",
              style = MaterialTheme.typography.titleMedium,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
          }
          items(filteredTags.take(if (selectedTab == 4) 30 else 6), key = { "t_${it.id}" }) { tag ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { query = "#${tag.tag}"; selectedTab = 2 }
                .padding(horizontal = 16.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Rounded.Tag, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
              }
              Spacer(Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text("#${tag.tag}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${tag.postsCount} posts", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
              }
            }
          }
        }
      }

      // Explore Media Grid (Posts & Reels)
      if (selectedTab == 0 || selectedTab == 2 || selectedTab == 3) {
        val gridItems = if (selectedTab == 3) matchingReels else matchingPosts
        item(key = "grid_header") {
          Text(
            text = if (selectedTab == 3) "Trending Reels" else "Explore Grid",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
          )
        }
        if (gridItems.isEmpty()) {
          item(key = "empty_grid") {
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
              shape = RoundedCornerShape(18.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              border = BorderStroke(1.dp, colors.hairlineBorder)
            ) {
              Text(
                text = "No matching posts found yet. Try another keyword or create a post!",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.mutedText,
                modifier = Modifier.padding(20.dp)
              )
            }
          }
        } else {
          val rows = gridItems.chunked(3)
          items(rows, key = { row -> row.joinToString("_") { it.id } }) { rowPosts ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              rowPosts.forEach { post ->
                ExploreGridTile(
                  post = post,
                  modifier = Modifier.weight(1f),
                  onClick = {
                    if (post.mediaType == "reel") onOpenReels()
                    else onOpenComments(post.id, post.authorId)
                  }
                )
              }
              repeat(3 - rowPosts.size) {
                Spacer(modifier = Modifier.weight(1f))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ExploreGridTile(
  post: Post,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Box(
    modifier = modifier
      .aspectRatio(1f)
      .clip(RoundedCornerShape(10.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant)
      .clickable(onClick = onClick)
  ) {
    VynzoMediaImage(
      source = post.coverUrl.ifBlank { post.mediaUrls.firstOrNull().orEmpty() },
      contentDescription = post.caption.ifBlank { "Explore post" },
      modifier = Modifier.fillMaxSize()
    )
    if (post.mediaType == "reel" || post.mediaType == "video") {
      Icon(
        imageVector = Icons.Rounded.PlayCircleOutline,
        contentDescription = "Video",
        tint = Color.White,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(6.dp)
          .size(20.dp)
      )
    }
  }
}
