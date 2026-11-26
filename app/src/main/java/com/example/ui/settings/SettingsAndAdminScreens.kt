package com.example.ui.settings

import android.app.Activity
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AdsClick
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.VynzoAdManager
import com.example.data.firebase.VynzoAuthManager
import com.example.data.model.HashtagEntry
import com.example.data.model.ModerationReport
import com.example.data.model.Post
import com.example.data.model.UserProfile
import com.example.ui.components.VynzoAvatar
import com.example.ui.theme.LocalVynzoColors
import com.example.ui.theme.VynzoEmerald
import com.example.ui.theme.VynzoThemeMode
import com.example.ui.viewmodel.VynzoSessionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  viewModel: VynzoSessionViewModel,
  currentThemeMode: VynzoThemeMode,
  adsConsentAccepted: Boolean,
  onBack: () -> Unit,
  onOpenEditProfile: () -> Unit,
  onOpenSavedPosts: () -> Unit,
  onOpenAdminDashboard: () -> Unit,
  onOpenInfoPage: (String) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val colors = LocalVynzoColors.current
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val myProfile = viewModel.currentProfileOrNull()
  val privateInfo by viewModel.myPrivateInfo.collectAsStateWithLifecycle()
  val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
  val creators by viewModel.activeCreators.collectAsStateWithLifecycle()
  val creatorMap = remember(creators) { creators.associateBy { it.userId } }

  var showBlockedDialog by remember { mutableStateOf(false) }
  var showRestrictedDialog by remember { mutableStateOf(false) }
  var showProblemDialog by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }
  var showThemeDialog by remember { mutableStateOf(false) }
  var showCommentControlDialog by remember { mutableStateOf(false) }
  var showMessagePrivacyDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Settings & Privacy") },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
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
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      if (isAdmin) {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .clickable(onClick = onOpenAdminDashboard)
            .testTag("admin_dashboard_entry"),
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("Vynzo Admin Console", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              Text("Moderate reports, creators, posts, tags & AdMob controls", style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
          }
        }
      }

      SettingsGroupCard(title = "Account") {
        SettingsRowItem(
          icon = Icons.Rounded.Edit,
          title = "Edit Profile",
          subtitle = "Photo, cover, bio, website & @username",
          onClick = onOpenEditProfile
        )
        HorizontalDivider(color = colors.hairlineBorder)
        SettingsRowItem(
          icon = Icons.Rounded.Bookmark,
          title = "Saved Bookmarks",
          subtitle = "View your saved posts & collections",
          onClick = onOpenSavedPosts
        )
      }

      SettingsGroupCard(title = "Privacy & Security") {
        SettingsSwitchItem(
          icon = Icons.Rounded.Lock,
          title = "Private Account",
          subtitle = "Only approved followers can follow and view private updates",
          checked = myProfile?.privateAccount == true,
          onCheckedChange = { enabled ->
            scope.launch {
              viewModel.userRepository.updatePrivacySettings(
                privateAccount = enabled,
                commentControl = myProfile?.commentControl ?: "everyone",
                messagePrivacy = myProfile?.messagePrivacy ?: "everyone"
              ).onSuccess {
                viewModel.notifyMessage(if (enabled) "Account is now private" else "Account is now public")
              }
            }
          }
        )
        HorizontalDivider(color = colors.hairlineBorder)
        SettingsRowItem(
          icon = Icons.Rounded.Shield,
          title = "Comment Controls",
          subtitle = "Current: ${(myProfile?.commentControl ?: "everyone").replaceFirstChar { it.uppercase() }}",
          onClick = { showCommentControlDialog = true }
        )
        HorizontalDivider(color = colors.hairlineBorder)
        SettingsRowItem(
          icon = Icons.Rounded.PrivacyTip,
          title = "Message Privacy",
          subtitle = "Current: ${(myProfile?.messagePrivacy ?: "everyone").replaceFirstChar { it.uppercase() }}",
          onClick = { showMessagePrivacyDialog = true }
        )
        HorizontalDivider(color = colors.hairlineBorder)
        SettingsRowItem(
          icon = Icons.Rounded.Block,
          title = "Blocked Accounts",
          subtitle = "${privateInfo?.blockedUids?.size ?: 0} blocked user(s)",
          onClick = { showBlockedDialog = true }
        )
        HorizontalDivider(color = colors.hairlineBorder)
        SettingsRowItem(
          icon = Icons.Rounded.VisibilityOff,
          title = "Restricted Accounts",
          subtitle = "${privateInfo?.restrictedUids?.size ?: 0} restricted user(s)",
          onClick = { showRestrictedDialog = true }
        )
      }

      SettingsGroupCard(title = "Notifications") {
        SettingsSwitchItem(
          icon = Icons.Rounded.Notifications,
          title = "Likes & Reactions",
          subtitle = "Alerts for post, Reel, and story reactions",
          checked = privateInfo?.notifyLikes ?: true,
          onCheckedChange = { v ->
            scope.launch { viewModel.userRepository.updatePrivatePreferences(mapOf("notifyLikes" to v)) }
          }
        )
        HorizontalDivider(color = colors.hairlineBorder)
        SettingsSwitchItem(
          icon = Icons.Rounded.Notifications,
          title = "Comments & Mentions",
          subtitle = "Alerts when someone comments or tags you",
          checked = privateInfo?.notifyComments ?: true,
          onCheckedChange = { v ->
            scope.launch { viewModel.userRepository.updatePrivatePreferences(mapOf("notifyComments" to v)) }
          }
        )
        HorizontalDivider(color = colors.hairlineBorder)
        SettingsSwitchItem(
          icon = Icons.Rounded.Notifications,
          title = "New Followers & Requests",
          subtitle = "Alerts for new followers and requests",
          checked = privateInfo?.notifyFollows ?: true,
          onCheckedChange = { v ->
            scope.launch { viewModel.userRepository.updatePrivatePreferences(mapOf("notifyFollows" to v)) }
          }
        )
        HorizontalDivider(color = colors.hairlineBorder)
        SettingsSwitchItem(
          icon = Icons.Rounded.Notifications,
          title = "Direct Messages",
          subtitle = "Alerts for new messages and voice notes",
          checked = privateInfo?.notifyMessages ?: true,
          onCheckedChange = { v ->
            scope.launch { viewModel.userRepository.updatePrivatePreferences(mapOf("notifyMessages" to v)) }
          }
        )
      }

      SettingsGroupCard(title = "Appearance & Monetization") {
        SettingsRowItem(
          icon = Icons.Rounded.DarkMode,
          title = "Theme Mode",
          subtitle = currentThemeMode.label,
          onClick = { showThemeDialog = true }
        )
        HorizontalDivider(color = colors.hairlineBorder)
        SettingsSwitchItem(
          icon = Icons.Rounded.AdsClick,
          title = "Personalized Ads Consent",
          subtitle = "Tailor sponsored AdMob cards inside Vynzo",
          checked = adsConsentAccepted,
          onCheckedChange = { accepted ->
            viewModel.setAdsConsent(accepted)
            viewModel.notifyMessage(if (accepted) "Personalized ads enabled" else "Switched to non-personalized ads")
          }
        )
        HorizontalDivider(color = colors.hairlineBorder)
        SettingsRowItem(
          icon = Icons.Rounded.PlayCircleOutline,
          title = "Creator Spotlight Reward",
          subtitle = "Watch a rewarded ad to unlock a creator spotlight boost",
          onClick = {
            VynzoAdManager.showRewardedAd(
              activity = context as? Activity,
              personalizedConsent = adsConsentAccepted,
              onRewardEarned = {
                viewModel.notifyMessage("Spotlight boost unlocked! Your next post is prioritized.")
              },
              onUnavailable = { msg -> viewModel.notifyMessage(msg) }
            )
          }
        )
      }

      SettingsGroupCard(title = "Support & Legal") {
        SettingsRowItem(
          icon = Icons.AutoMirrored.Rounded.HelpOutline,
          title = "Help Center",
          subtitle = "FAQs, creator tips, and account security",
          onClick = { onOpenInfoPage("help") }
        )
        HorizontalDivider(color = colors.hairlineBorder)
        SettingsRowItem(
          icon = Icons.Rounded.BugReport,
          title = "Report a Problem",
          subtitle = "Send feedback or bug reports to the Vynzo team",
          onClick = { showProblemDialog = true }
        )
        HorizontalDivider(color = colors.hairlineBorder)
        SettingsRowItem(
          icon = Icons.Rounded.Info,
          title = "About Vynzo",
          subtitle = "Connect. Share. Create. v1.0",
          onClick = { onOpenInfoPage("about") }
        )
        HorizontalDivider(color = colors.hairlineBorder)
        SettingsRowItem(
          icon = Icons.Rounded.Description,
          title = "Terms & Conditions",
          subtitle = "Community rules and service terms",
          onClick = { onOpenInfoPage("terms") }
        )
        HorizontalDivider(color = colors.hairlineBorder)
        SettingsRowItem(
          icon = Icons.Rounded.Gavel,
          title = "Privacy Policy",
          subtitle = "How Vynzo protects your data",
          onClick = { onOpenInfoPage("privacy") }
        )
      }

      SettingsGroupCard(title = "Session") {
        SettingsRowItem(
          icon = Icons.AutoMirrored.Rounded.Logout,
          title = "Log Out",
          subtitle = "Sign out of @${myProfile?.username ?: "vynzo"}",
          onClick = {
            VynzoAuthManager.signOut(
              credentialManager = credentialManager,
              scope = scope,
              onCleanupBeforeSignOut = { viewModel.userRepository.setOnlineStatus(false) },
              onSignOutComplete = {}
            )
          }
        )
        HorizontalDivider(color = colors.hairlineBorder)
        SettingsRowItem(
          icon = Icons.Rounded.DeleteForever,
          title = "Delete Account",
          subtitle = "Permanently remove your profile and sign-in account",
          destructive = true,
          onClick = { showDeleteDialog = true }
        )
      }

      Spacer(Modifier.height(16.dp))
    }
  }

  if (showThemeDialog) {
    AlertDialog(
      onDismissRequest = { showThemeDialog = false },
      title = { Text("Choose Vynzo Theme") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          VynzoThemeMode.entries.forEach { mode ->
            FilterChip(
              selected = currentThemeMode == mode,
              onClick = {
                viewModel.setThemeMode(mode)
                showThemeDialog = false
              },
              label = { Text(mode.label) },
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showThemeDialog = false }) { Text("Close") }
      }
    )
  }

  if (showCommentControlDialog) {
    val options = listOf("everyone" to "Everyone", "followers" to "Followers Only", "off" to "Turned Off")
    AlertDialog(
      onDismissRequest = { showCommentControlDialog = false },
      title = { Text("Who can comment?") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          options.forEach { (key, label) ->
            FilterChip(
              selected = (myProfile?.commentControl ?: "everyone") == key,
              onClick = {
                scope.launch {
                  viewModel.userRepository.updatePrivacySettings(
                    privateAccount = myProfile?.privateAccount ?: false,
                    commentControl = key,
                    messagePrivacy = myProfile?.messagePrivacy ?: "everyone"
                  )
                }
                showCommentControlDialog = false
              },
              label = { Text(label) },
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showCommentControlDialog = false }) { Text("Done") }
      }
    )
  }

  if (showMessagePrivacyDialog) {
    val options = listOf("everyone" to "Everyone", "followers" to "Followers Only")
    AlertDialog(
      onDismissRequest = { showMessagePrivacyDialog = false },
      title = { Text("Message Privacy") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          options.forEach { (key, label) ->
            FilterChip(
              selected = (myProfile?.messagePrivacy ?: "everyone") == key,
              onClick = {
                scope.launch {
                  viewModel.userRepository.updatePrivacySettings(
                    privateAccount = myProfile?.privateAccount ?: false,
                    commentControl = myProfile?.commentControl ?: "everyone",
                    messagePrivacy = key
                  )
                }
                showMessagePrivacyDialog = false
              },
              label = { Text(label) },
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showMessagePrivacyDialog = false }) { Text("Done") }
      }
    )
  }

  if (showBlockedDialog) {
    val blocked = privateInfo?.blockedUids.orEmpty()
    AlertDialog(
      onDismissRequest = { showBlockedDialog = false },
      title = { Text("Blocked Accounts (${blocked.size})") },
      text = {
        if (blocked.isEmpty()) {
          Text("You haven't blocked any accounts.")
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            blocked.forEach { uid ->
              val u = creatorMap[uid]
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(u?.let { "@${it.username}" } ?: uid.take(12))
                TextButton(
                  onClick = {
                    scope.launch {
                      viewModel.userRepository.toggleBlockUser(uid, blocked)
                      viewModel.notifyMessage("Account unblocked")
                    }
                  }
                ) { Text("Unblock") }
              }
            }
          }
        }
      },
      confirmButton = { TextButton(onClick = { showBlockedDialog = false }) { Text("Done") } }
    )
  }

  if (showRestrictedDialog) {
    val restricted = privateInfo?.restrictedUids.orEmpty()
    AlertDialog(
      onDismissRequest = { showRestrictedDialog = false },
      title = { Text("Restricted Accounts (${restricted.size})") },
      text = {
        if (restricted.isEmpty()) {
          Text("No restricted accounts.")
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            restricted.forEach { uid ->
              val u = creatorMap[uid]
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(u?.let { "@${it.username}" } ?: uid.take(12))
                TextButton(
                  onClick = {
                    scope.launch {
                      viewModel.userRepository.toggleRestrictUser(uid, restricted)
                      viewModel.notifyMessage("Restriction removed")
                    }
                  }
                ) { Text("Unrestrict") }
              }
            }
          }
        }
      },
      confirmButton = { TextButton(onClick = { showRestrictedDialog = false }) { Text("Done") } }
    )
  }

  if (showProblemDialog) {
    var issueText by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showProblemDialog = false },
      title = { Text("Report a Problem") },
      text = {
        OutlinedTextField(
          value = issueText,
          onValueChange = { issueText = it.take(400) },
          label = { Text("Describe what happened") },
          minLines = 3,
          maxLines = 5,
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(
          enabled = issueText.isNotBlank(),
          onClick = {
            viewModel.reportContent("app_bug", viewModel.currentUserId, "App Issue", issueText)
            showProblemDialog = false
          }
        ) { Text("Send") }
      },
      dismissButton = { TextButton(onClick = { showProblemDialog = false }) { Text("Cancel") } }
    )
  }

  if (showDeleteDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteDialog = false },
      title = { Text("Delete Account Permanently?") },
      text = { Text("This permanently deletes your Vynzo profile and Firebase account. This action cannot be undone.") },
      confirmButton = {
        Button(
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          onClick = {
            showDeleteDialog = false
            scope.launch {
              viewModel.userRepository.deleteCurrentUserAccount()
                .onFailure {
                  viewModel.notifyMessage(it.localizedMessage ?: "Recent login required before deleting account")
                }
            }
          }
        ) { Text("Delete") }
      },
      dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
    )
  }
}

@Composable
private fun SettingsGroupCard(
  title: String,
  content: @Composable () -> Unit
) {
  val colors = LocalVynzoColors.current
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .widthIn(max = 600.dp)
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelLarge,
      color = colors.mutedText,
      modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
    )
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(18.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, colors.hairlineBorder)
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        content()
      }
    }
  }
}

@Composable
private fun SettingsRowItem(
  icon: ImageVector,
  title: String,
  subtitle: String,
  destructive: Boolean = false,
  onClick: () -> Unit
) {
  val colors = LocalVynzoColors.current
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    )
    Spacer(Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
      )
      Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
    }
    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.mutedText)
  }
}

@Composable
private fun SettingsSwitchItem(
  icon: ImageVector,
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  val colors = LocalVynzoColors.current
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.titleSmall)
      Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
    }
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
  viewModel: VynzoSessionViewModel,
  onBack: () -> Unit,
  onOpenUserProfile: (String) -> Unit
) {
  val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
  val users by viewModel.adminRepository.observeAllUsersAdmin().collectAsStateWithLifecycle(initialValue = emptyList())
  val posts by viewModel.adminRepository.observeAllPostsAdmin().collectAsStateWithLifecycle(initialValue = emptyList())
  val reports by viewModel.adminRepository.observeReportsAdmin().collectAsStateWithLifecycle(initialValue = emptyList())
  val hashtags by viewModel.adminRepository.observeAllHashtagsAdmin().collectAsStateWithLifecycle(initialValue = emptyList())
  val adsConfig by viewModel.adsConfig.collectAsStateWithLifecycle()

  val scope = rememberCoroutineScope()
  val colors = LocalVynzoColors.current
  var selectedTab by remember { mutableIntStateOf(0) } // 0=Overview/Ads, 1=Reports, 2=Users, 3=Posts, 4=Tags

  var editAdsEnabled by remember(adsConfig) { mutableStateOf(adsConfig.adsEnabled) }
  var editInterval by remember(adsConfig) { mutableStateOf(adsConfig.nativeFeedInterval.toString()) }
  var editCooldown by remember(adsConfig) { mutableStateOf(adsConfig.interstitialCooldownSec.toString()) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Vynzo Admin Dashboard") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { innerPadding ->
    if (!isAdmin) {
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
        Text("Admin access denied.")
      }
      return@Scaffold
    }

    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      TabRow(selectedTabIndex = selectedTab) {
        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Overview") })
        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Reports (${reports.count { it.status == "open" }})") })
        Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Users") })
        Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Posts") })
        Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Tags") })
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        when (selectedTab) {
          0 -> {
            item {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminKpiCard("Users", users.size.toString(), Modifier.weight(1f))
                AdminKpiCard("Posts", posts.size.toString(), Modifier.weight(1f))
                AdminKpiCard("Open Reports", reports.count { it.status == "open" }.toString(), Modifier.weight(1f))
              }
            }
            item {
              Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, colors.hairlineBorder)
              ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                  Text("AdMob Monetization Controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text("Enable Ads App-Wide")
                    Switch(checked = editAdsEnabled, onCheckedChange = { editAdsEnabled = it })
                  }
                  OutlinedTextField(
                    value = editInterval,
                    onValueChange = { editInterval = it.filter(Char::isDigit).take(2) },
                    label = { Text("Native Feed Ad Interval (posts)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                  )
                  OutlinedTextField(
                    value = editCooldown,
                    onValueChange = { editCooldown = it.filter(Char::isDigit).take(3) },
                    label = { Text("Interstitial Cooldown (seconds)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                  )
                  Button(
                    onClick = {
                      scope.launch {
                        viewModel.adminRepository.updateAdsConfig(
                          adsEnabled = editAdsEnabled,
                          nativeInterval = editInterval.toLongOrNull() ?: 5L,
                          interstitialCooldownSec = editCooldown.toLongOrNull() ?: 120L
                        ).onSuccess {
                          viewModel.notifyMessage("AdMob configuration saved!")
                        }
                      }
                    },
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text("Save AdMob Settings")
                  }
                }
              }
            }
          }

          1 -> {
            items(reports, key = { it.id }) { rep ->
              AdminReportCard(
                report = rep,
                onResolve = {
                  scope.launch {
                    viewModel.adminRepository.resolveReport(rep.id, "resolved")
                    viewModel.notifyMessage("Report marked resolved")
                  }
                },
                onDismiss = {
                  scope.launch {
                    viewModel.adminRepository.resolveReport(rep.id, "dismissed")
                    viewModel.notifyMessage("Report dismissed")
                  }
                }
              )
            }
          }

          2 -> {
            items(users, key = { it.userId }) { user ->
              AdminUserCard(
                user = user,
                onOpenProfile = { onOpenUserProfile(user.userId) },
                onToggleSuspend = {
                  scope.launch {
                    val next = if (user.status == "active") "suspended" else "active"
                    viewModel.adminRepository.setUserStatus(user.userId, next)
                  }
                },
                onToggleVerify = {
                  scope.launch {
                    viewModel.adminRepository.setUserVerified(user.userId, !user.verified)
                  }
                },
                onDelete = {
                  scope.launch {
                    viewModel.adminRepository.deleteUserRecord(user.userId)
                  }
                }
              )
            }
          }

          3 -> {
            items(posts, key = { it.id }) { post ->
              AdminPostCard(
                post = post,
                onToggleRemove = {
                  scope.launch {
                    val next = if (post.visibility == "removed") "public" else "removed"
                    viewModel.adminRepository.setPostVisibility(post.id, next)
                  }
                }
              )
            }
          }

          4 -> {
            items(hashtags, key = { it.id }) { tag ->
              AdminTagCard(
                entry = tag,
                onToggleBlock = {
                  scope.launch {
                    val next = if (tag.status == "active") "blocked" else "active"
                    viewModel.adminRepository.setHashtagStatus(tag.id, next)
                  }
                }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AdminKpiCard(title: String, value: String, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, LocalVynzoColors.current.hairlineBorder)
  ) {
    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
      Text(title, style = MaterialTheme.typography.labelMedium, color = LocalVynzoColors.current.mutedText)
    }
  }
}

@Composable
private fun AdminReportCard(
  report: ModerationReport,
  onResolve: () -> Unit,
  onDismiss: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, LocalVynzoColors.current.hairlineBorder)
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text("${report.targetType.uppercase()} • ${report.reason}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
      Text("Target ID: ${report.targetId}", style = MaterialTheme.typography.bodySmall)
      if (report.details.isNotBlank()) Text(report.details, style = MaterialTheme.typography.bodyMedium)
      Text("Status: ${report.status}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
      if (report.status == "open") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(onClick = onResolve) { Text("Resolve") }
          OutlinedButton(onClick = onDismiss) { Text("Dismiss") }
        }
      }
    }
  }
}

@Composable
private fun AdminUserCard(
  user: UserProfile,
  onOpenProfile: () -> Unit,
  onToggleSuspend: () -> Unit,
  onToggleVerify: () -> Unit,
  onDelete: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, LocalVynzoColors.current.hairlineBorder)
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenProfile),
        verticalAlignment = Alignment.CenterVertically
      ) {
        VynzoAvatar(avatarUrl = user.avatarUrl, displayName = user.displayName, size = 40.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("@${user.username}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (user.verified) {
              Spacer(Modifier.width(4.dp))
              Icon(Icons.Rounded.Verified, contentDescription = null, tint = VynzoEmerald, modifier = Modifier.size(16.dp))
            }
          }
          Text("Status: ${user.status} • ${user.followersCount} followers", style = MaterialTheme.typography.bodySmall)
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onToggleVerify) {
          Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text(if (user.verified) "Unverify" else "Verify")
        }
        OutlinedButton(onClick = onToggleSuspend) {
          Text(if (user.status == "active") "Suspend" else "Activate")
        }
        TextButton(onClick = onDelete) {
          Text("Remove", color = MaterialTheme.colorScheme.error)
        }
      }
    }
  }
}

@Composable
private fun AdminPostCard(post: Post, onToggleRemove: () -> Unit) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, LocalVynzoColors.current.hairlineBorder)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text("@${post.authorUsername} • ${post.mediaType}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(post.caption.ifBlank { "(No caption)" }, maxLines = 2, style = MaterialTheme.typography.bodySmall)
        Text("Visibility: ${post.visibility}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
      }
      OutlinedButton(onClick = onToggleRemove) {
        Text(if (post.visibility == "removed") "Restore" else "Remove")
      }
    }
  }
}

@Composable
private fun AdminTagCard(entry: HashtagEntry, onToggleBlock: () -> Unit) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, LocalVynzoColors.current.hairlineBorder)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text("#${entry.tag}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text("${entry.postsCount} posts • ${entry.status}", style = MaterialTheme.typography.bodySmall)
      }
      OutlinedButton(onClick = onToggleBlock) {
        Text(if (entry.status == "active") "Block" else "Unblock")
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoPageScreen(
  pageType: String,
  onBack: () -> Unit
) {
  val (title, sections) = when (pageType) {
    "help" -> "Vynzo Help Center" to listOf(
      "Creating & Editing Media" to "Tap the + tab to upload multi-image carousels, Reels with custom cover images and trim ranges, or 24-hour Stories. You can crop and rotate images directly before publishing.",
      "Privacy & Safety" to "Switch your profile to Private under Settings > Privacy & Security so new followers require approval. You can also restrict, block, or report content anytime.",
      "Direct Messaging" to "Send real-time text, photo, video, and voice messages, react with emojis, reply inline, or delete your sent messages."
    )
    "terms" -> "Terms & Conditions" to listOf(
      "Community Conduct" to "Creators on Vynzo agree to share original or licensed content and refrain from harassment, spam, hate speech, or impersonation.",
      "Content Moderation" to "Reported posts, comments, Reels, and profiles are reviewed by Vynzo Trust & Safety administrators and may be removed if they violate community standards.",
      "Monetization & Ads" to "Vynzo integrates Google AdMob banners, feed native placements, frequency-capped interstitials, and opt-in rewarded creator boosts."
    )
    "privacy" -> "Privacy Policy" to listOf(
      "Data Protection" to "Authentication is secured by Firebase Authentication (Google Sign-In). Private preferences, notification settings, blocked lists, and FCM tokens are isolated inside owner-only Firestore subcollections.",
      "Media Access" to "Vynzo uses Android's zero-permission Photo Picker for gallery images and videos, and requests Camera, Microphone, Coarse Location, or Notifications strictly at runtime when used.",
      "Account Control" to "You may export preferences, adjust personalized ad consent, or permanently delete your Vynzo account anytime in Settings."
    )
    else -> "About Vynzo" to listOf(
      "Connect. Share. Create." to "Vynzo is a modern, original social media platform engineered with Jetpack Compose, Material 3, Media3 video playback, real-time Firestore synchronization, and Google AdMob monetization.",
      "Version & Platform" to "Version 1.0 • Built for Android mobile, foldable, and tablet screens."
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(title) },
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
        .verticalScroll(rememberScrollState())
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      sections.forEach { (heading, body) ->
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, LocalVynzoColors.current.hairlineBorder)
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(heading, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = LocalVynzoColors.current.mutedText)
          }
        }
      }
    }
  }
}
