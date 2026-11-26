package com.example.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.ads.VynzoAdManager
import com.example.data.firebase.VynzoAuthManager
import com.example.data.firebase.VynzoFirebaseHelper
import com.example.data.firebase.findActivity
import com.example.data.model.UiState
import com.example.data.repository.AdminRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.PostRepository
import com.example.data.repository.StoryRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.VynzoPreferencesRepository
import com.example.ui.auth.AuthFlowScreen
import com.example.ui.auth.ProfileSetupScreen
import com.example.ui.auth.VynzoOnboardingView
import com.example.ui.chat.ChatDetailScreen
import com.example.ui.chat.ChatListScreen
import com.example.ui.components.VynzoWordmark
import com.example.ui.create.CreateScreen
import com.example.ui.explore.ExploreScreen
import com.example.ui.home.CommentsScreen
import com.example.ui.home.HomeScreen
import com.example.ui.notifications.NotificationsScreen
import com.example.ui.profile.ConnectionsScreen
import com.example.ui.profile.EditProfileScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.profile.SavedPostsScreen
import com.example.ui.reels.ReelsScreen
import com.example.ui.settings.AdminDashboardScreen
import com.example.ui.settings.InfoPageScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.stories.StoryViewerScreen
import com.example.ui.theme.LocalVynzoColors
import com.example.ui.theme.VynzoTheme
import com.example.ui.theme.VynzoThemeMode
import com.example.ui.viewmodel.VynzoSessionViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private data class VynzoTopTab(
  val route: VynzoRoute,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String
)

private val VynzoBottomTabs = listOf(
  VynzoTopTab(VynzoRoute.Home, "Home", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
  VynzoTopTab(VynzoRoute.Explore, "Explore", Icons.Rounded.Explore, Icons.Outlined.Explore, "nav_explore"),
  VynzoTopTab(VynzoRoute.Create, "Create", Icons.Rounded.AddBox, Icons.Outlined.AddBox, "nav_create"),
  VynzoTopTab(VynzoRoute.Reels, "Reels", Icons.Rounded.Movie, Icons.Outlined.Movie, "nav_reels"),
  VynzoTopTab(VynzoRoute.Profile, "Profile", Icons.Rounded.Person, Icons.Outlined.Person, "nav_profile")
)

@Composable
fun VynzoAppRoot() {
  val context = LocalContext.current
  val appContext = context.applicationContext ?: context
  val scope = rememberCoroutineScope()
  val prefsRepository = remember(appContext) { VynzoPreferencesRepository(appContext) }

  val onboardingCompleted by prefsRepository.onboardingCompleted.collectAsStateWithLifecycle(initialValue = false)
  val themeMode by prefsRepository.themeMode.collectAsStateWithLifecycle(initialValue = VynzoThemeMode.DARK)
  val adsConsent by prefsRepository.adsConsentAccepted.collectAsStateWithLifecycle(initialValue = true)

  var isFirebaseReady by remember(appContext) {
    mutableStateOf(VynzoFirebaseHelper.ensureInitialized(appContext))
  }
  var currentUser by remember(isFirebaseReady) {
    mutableStateOf(if (isFirebaseReady) VynzoFirebaseHelper.currentUserOrNull(appContext) else null)
  }

  DisposableEffect(isFirebaseReady) {
    if (!isFirebaseReady) {
      return@DisposableEffect onDispose {}
    }
    val auth = runCatching { Firebase.auth }.getOrNull() ?: return@DisposableEffect onDispose {}
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
      currentUser = firebaseAuth.currentUser
    }
    auth.addAuthStateListener(listener)
    onDispose { auth.removeAuthStateListener(listener) }
  }

  LaunchedEffect(adsConsent) {
    VynzoAdManager.initialize(appContext, adsConsent)
  }

  VynzoTheme(themeMode = themeMode) {
    if (!isFirebaseReady) {
      FirebaseSetupRequiredScreen(
        onRetry = {
          val ready = VynzoFirebaseHelper.ensureInitialized(appContext)
          isFirebaseReady = ready
          if (ready) {
            currentUser = VynzoFirebaseHelper.currentUserOrNull(appContext)
          }
        }
      )
    } else {
      val user = currentUser
      if (user == null) {
        AuthFlowScreen(
          onboardingCompleted = onboardingCompleted,
          onMarkOnboardingCompleted = {
            scope.launch { prefsRepository.setOnboardingCompleted(true) }
          },
          onAuthSuccess = {
            currentUser = VynzoFirebaseHelper.currentUserOrNull(appContext)
          }
        )
      } else {
        key(user.uid) {
          AuthenticatedVynzoShell(
            firebaseUser = user,
            prefsRepository = prefsRepository,
            themeMode = themeMode,
            adsConsent = adsConsent
          )
        }
      }
    }
  }
}

@Composable
private fun FirebaseSetupRequiredScreen(onRetry: () -> Unit) {
  val colors = LocalVynzoColors.current
  var showTourPreview by remember { mutableStateOf(false) }

  if (showTourPreview) {
    VynzoOnboardingView(onFinish = { showTourPreview = false })
    return
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding()
      .navigationBarsPadding()
      .verticalScroll(rememberScrollState())
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 480.dp)
        .testTag("firebase_setup_required_card"),
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, colors.hairlineBorder),
      tonalElevation = 3.dp
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        VynzoWordmark(fontSize = 24)
        Spacer(Modifier.height(4.dp))
        Icon(
          imageVector = Icons.Rounded.CloudOff,
          contentDescription = "Firebase configuration required",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(44.dp)
        )
        Text(
          text = "Firebase Configuration Missing",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center
        )
        Text(
          text = "This Vynzo APK was compiled without Firebase configuration (app/google-services.json). Add app/google-services.json or set the GOOGLE_SERVICES_JSON secret in GitHub Actions and rebuild to enable Google Sign-In, Feed sync, Stories, Reels, and Chat.",
          style = MaterialTheme.typography.bodyMedium,
          color = colors.mutedText,
          textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Button(
          onClick = onRetry,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("firebase_retry_button")
        ) {
          Text("Retry Firebase Connection")
        }
        OutlinedButton(
          onClick = { showTourPreview = true },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("firebase_preview_tour_button")
        ) {
          Text("Preview Vynzo Tour")
        }
      }
    }
  }
}

@Composable
private fun AuthenticatedVynzoShell(
  firebaseUser: FirebaseUser,
  prefsRepository: VynzoPreferencesRepository,
  themeMode: VynzoThemeMode,
  adsConsent: Boolean
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var retryNonce by remember { mutableIntStateOf(0) }
  val dbResult = remember(context, firebaseUser.uid, retryNonce) {
    runCatching {
      val dbId = context.getString(R.string.firestore_database_id).trim()
      require(dbId.isNotBlank()) { "Firestore database ID is not configured." }
      FirebaseFirestore.getInstance(dbId)
    }
  }
  val db = dbResult.getOrNull()
  if (db == null) {
    val errMsg = dbResult.exceptionOrNull()?.localizedMessage
      ?: "Unable to initialize Cloud Firestore database."
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(24.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        VynzoWordmark(fontSize = 24)
        Text("Database Setup Unavailable", style = MaterialTheme.typography.titleLarge)
        Text(errMsg, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Button(onClick = { retryNonce += 1 }) { Text("Retry") }
        OutlinedButton(
          onClick = { VynzoAuthManager.signOut(credentialManager, scope) }
        ) { Text("Sign Out") }
      }
    }
    return
  }

  val sessionViewModel: VynzoSessionViewModel = viewModel(
    key = "vynzo_session_${firebaseUser.uid}",
    factory = remember(firebaseUser.uid, db, prefsRepository) {
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return VynzoSessionViewModel(
            currentUserId = firebaseUser.uid,
            userRepository = UserRepository(db),
            postRepository = PostRepository(db),
            storyRepository = StoryRepository(db),
            chatRepository = ChatRepository(db),
            notificationRepository = NotificationRepository(db),
            adminRepository = AdminRepository(db),
            prefsRepository = prefsRepository
          ) as T
        }
      }
    }
  )

  val notificationLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { _ -> }

  LaunchedEffect(firebaseUser.uid) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) {
        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
    runCatching {
      val token = FirebaseMessaging.getInstance().token.await()
      if (token.isNotBlank()) {
        sessionViewModel.userRepository.updatePrivatePreferences(mapOf("fcmToken" to token))
      }
    }
  }

  val myProfileState by sessionViewModel.myProfileState.collectAsStateWithLifecycle()
  val adsConfig by sessionViewModel.adsConfig.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(sessionViewModel) {
    sessionViewModel.snackEvents.collect { msg ->
      snackbarHostState.showSnackbar(msg)
    }
  }

  when (val state = myProfileState) {
    is UiState.Loading -> {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          VynzoWordmark(fontSize = 28)
          Spacer(Modifier.height(18.dp))
          CircularProgressIndicator()
        }
      }
    }

    is UiState.Error -> {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Unable to connect to Vynzo", style = MaterialTheme.typography.titleLarge)
          Text(state.message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
          Button(onClick = { sessionViewModel.refreshFeed() }) { Text("Retry") }
          OutlinedButton(
            onClick = {
              VynzoAuthManager.signOut(credentialManager, scope)
            }
          ) { Text("Sign Out") }
        }
      }
    }

    is UiState.Success -> {
      val profile = state.data
      if (profile == null) {
        ProfileSetupScreen(
          defaultDisplayName = firebaseUser.displayName.orEmpty(),
          defaultEmail = firebaseUser.email.orEmpty(),
          defaultAvatarUrl = firebaseUser.photoUrl?.toString().orEmpty(),
          userId = firebaseUser.uid,
          userRepository = sessionViewModel.userRepository,
          onProfileCreated = {},
          onSignOut = {
            VynzoAuthManager.signOut(credentialManager, scope)
          }
        )
      } else {
        VynzoMainNavigator(
          viewModel = sessionViewModel,
          snackbarHostState = snackbarHostState,
          themeMode = themeMode,
          adsConsent = adsConsent,
          adsEnabled = adsConfig.adsEnabled,
          interstitialCooldownSec = adsConfig.interstitialCooldownSec
        )
      }
    }
  }
}

@Composable
private fun VynzoMainNavigator(
  viewModel: VynzoSessionViewModel,
  snackbarHostState: SnackbarHostState,
  themeMode: VynzoThemeMode,
  adsConsent: Boolean,
  adsEnabled: Boolean,
  interstitialCooldownSec: Long
) {
  val context = LocalContext.current
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination

  var createInitialMode by remember { mutableIntStateOf(0) }
  var exploreInitialTag by remember { mutableStateOf<String?>(null) }

  val showBottomOrRail = currentDestination?.let { dest ->
    dest.hasRoute<VynzoRoute.Home>() ||
      dest.hasRoute<VynzoRoute.Explore>() ||
      dest.hasRoute<VynzoRoute.Create>() ||
      dest.hasRoute<VynzoRoute.Reels>() ||
      dest.hasRoute<VynzoRoute.Profile>()
  } ?: true

  fun navigateTopTab(route: VynzoRoute) {
    if (route is VynzoRoute.Explore || route is VynzoRoute.Profile) {
      VynzoAdManager.showInterstitialIfEligible(
        activity = context.findActivity(),
        adsEnabled = adsEnabled,
        cooldownSec = interstitialCooldownSec,
        personalizedConsent = adsConsent
      )
    }
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
      }
      launchSingleTop = true
      restoreState = true
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp

    Scaffold(
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (showBottomOrRail && !useRail) {
          NavigationBar {
            VynzoBottomTabs.forEach { tab ->
              val selected = when (tab.route) {
                VynzoRoute.Home -> currentDestination?.hasRoute<VynzoRoute.Home>() == true
                VynzoRoute.Explore -> currentDestination?.hasRoute<VynzoRoute.Explore>() == true
                VynzoRoute.Create -> currentDestination?.hasRoute<VynzoRoute.Create>() == true
                VynzoRoute.Reels -> currentDestination?.hasRoute<VynzoRoute.Reels>() == true
                VynzoRoute.Profile -> currentDestination?.hasRoute<VynzoRoute.Profile>() == true
                else -> false
              }
              NavigationBarItem(
                selected = selected,
                onClick = {
                  if (tab.route is VynzoRoute.Create) createInitialMode = 0
                  if (tab.route is VynzoRoute.Explore) exploreInitialTag = null
                  navigateTopTab(tab.route)
                },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.label,
                    modifier = Modifier.size(24.dp)
                  )
                },
                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.testTag(tab.testTag)
              )
            }
          }
        }
      }
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        if (showBottomOrRail && useRail) {
          NavigationRail(modifier = Modifier.fillMaxHeight()) {
            Spacer(Modifier.height(12.dp))
            VynzoBottomTabs.forEach { tab ->
              val selected = when (tab.route) {
                VynzoRoute.Home -> currentDestination?.hasRoute<VynzoRoute.Home>() == true
                VynzoRoute.Explore -> currentDestination?.hasRoute<VynzoRoute.Explore>() == true
                VynzoRoute.Create -> currentDestination?.hasRoute<VynzoRoute.Create>() == true
                VynzoRoute.Reels -> currentDestination?.hasRoute<VynzoRoute.Reels>() == true
                VynzoRoute.Profile -> currentDestination?.hasRoute<VynzoRoute.Profile>() == true
                else -> false
              }
              NavigationRailItem(
                selected = selected,
                onClick = {
                  if (tab.route is VynzoRoute.Create) createInitialMode = 0
                  if (tab.route is VynzoRoute.Explore) exploreInitialTag = null
                  navigateTopTab(tab.route)
                },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.label
                  )
                },
                label = { Text(tab.label) },
                modifier = Modifier.testTag("${tab.testTag}_rail")
              )
            }
          }
        }

        NavHost(
          navController = navController,
          startDestination = VynzoRoute.Home,
          modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
          composable<VynzoRoute.Home> {
            HomeScreen(
              viewModel = viewModel,
              onOpenMessages = { navController.navigate(VynzoRoute.ChatList) },
              onOpenNotifications = { navController.navigate(VynzoRoute.Notifications) },
              onOpenCreateStory = {
                createInitialMode = 2
                navigateTopTab(VynzoRoute.Create)
              },
              onOpenCreatePost = {
                createInitialMode = 0
                navigateTopTab(VynzoRoute.Create)
              },
              onOpenStoryViewer = { authorId -> navController.navigate(VynzoRoute.StoryViewer(authorId)) },
              onOpenUserProfile = { uid ->
                if (uid == viewModel.currentUserId) navigateTopTab(VynzoRoute.Profile)
                else navController.navigate(VynzoRoute.UserProfile(uid))
              },
              onOpenComments = { postId, authorId ->
                navController.navigate(VynzoRoute.Comments(postId, authorId))
              },
              onOpenExploreWithTag = { tag ->
                exploreInitialTag = tag
                navigateTopTab(VynzoRoute.Explore)
              }
            )
          }

          composable<VynzoRoute.Explore> {
            ExploreScreen(
              viewModel = viewModel,
              initialTag = exploreInitialTag,
              onOpenUserProfile = { uid ->
                if (uid == viewModel.currentUserId) navigateTopTab(VynzoRoute.Profile)
                else navController.navigate(VynzoRoute.UserProfile(uid))
              },
              onOpenComments = { postId, authorId ->
                navController.navigate(VynzoRoute.Comments(postId, authorId))
              },
              onOpenReels = { navigateTopTab(VynzoRoute.Reels) }
            )
          }

          composable<VynzoRoute.Create> {
            CreateScreen(
              viewModel = viewModel,
              initialMode = createInitialMode,
              onPublishSuccess = { targetTab ->
                when (targetTab) {
                  "reels" -> navigateTopTab(VynzoRoute.Reels)
                  else -> navigateTopTab(VynzoRoute.Home)
                }
              }
            )
          }

          composable<VynzoRoute.Reels> {
            ReelsScreen(
              viewModel = viewModel,
              onOpenCreateReel = {
                createInitialMode = 1
                navigateTopTab(VynzoRoute.Create)
              },
              onOpenUserProfile = { uid ->
                if (uid == viewModel.currentUserId) navigateTopTab(VynzoRoute.Profile)
                else navController.navigate(VynzoRoute.UserProfile(uid))
              },
              onOpenComments = { postId, authorId ->
                navController.navigate(VynzoRoute.Comments(postId, authorId))
              }
            )
          }

          composable<VynzoRoute.Profile> {
            ProfileScreen(
              targetUserId = viewModel.currentUserId,
              isOwnTab = true,
              viewModel = viewModel,
              onBack = null,
              onOpenSettings = { navController.navigate(VynzoRoute.Settings) },
              onOpenEditProfile = { navController.navigate(VynzoRoute.EditProfile) },
              onOpenConnections = { uid, tab -> navController.navigate(VynzoRoute.Connections(uid, tab)) },
              onOpenChat = { chatId, peerId -> navController.navigate(VynzoRoute.ChatDetail(chatId, peerId)) },
              onOpenComments = { postId, authorId -> navController.navigate(VynzoRoute.Comments(postId, authorId)) },
              onOpenSaved = { navController.navigate(VynzoRoute.SavedPosts) }
            )
          }

          composable<VynzoRoute.UserProfile> { backStack ->
            val args = backStack.toRoute<VynzoRoute.UserProfile>()
            ProfileScreen(
              targetUserId = args.userId,
              isOwnTab = false,
              viewModel = viewModel,
              onBack = { navController.popBackStack() },
              onOpenSettings = { navController.navigate(VynzoRoute.Settings) },
              onOpenEditProfile = { navController.navigate(VynzoRoute.EditProfile) },
              onOpenConnections = { uid, tab -> navController.navigate(VynzoRoute.Connections(uid, tab)) },
              onOpenChat = { chatId, peerId -> navController.navigate(VynzoRoute.ChatDetail(chatId, peerId)) },
              onOpenComments = { postId, authorId -> navController.navigate(VynzoRoute.Comments(postId, authorId)) },
              onOpenSaved = { navController.navigate(VynzoRoute.SavedPosts) }
            )
          }

          composable<VynzoRoute.Comments> { backStack ->
            val args = backStack.toRoute<VynzoRoute.Comments>()
            CommentsScreen(
              postId = args.postId,
              postAuthorId = args.postAuthorId,
              viewModel = viewModel,
              onBack = { navController.popBackStack() },
              onOpenUserProfile = { uid ->
                if (uid == viewModel.currentUserId) navigateTopTab(VynzoRoute.Profile)
                else navController.navigate(VynzoRoute.UserProfile(uid))
              }
            )
          }

          composable<VynzoRoute.ChatList> {
            ChatListScreen(
              viewModel = viewModel,
              onBack = { navController.popBackStack() },
              onOpenChat = { chatId, peerId -> navController.navigate(VynzoRoute.ChatDetail(chatId, peerId)) }
            )
          }

          composable<VynzoRoute.ChatDetail> { backStack ->
            val args = backStack.toRoute<VynzoRoute.ChatDetail>()
            ChatDetailScreen(
              chatId = args.chatId,
              peerId = args.peerId,
              viewModel = viewModel,
              onBack = { navController.popBackStack() },
              onOpenPeerProfile = { uid -> navController.navigate(VynzoRoute.UserProfile(uid)) }
            )
          }

          composable<VynzoRoute.Notifications> {
            NotificationsScreen(
              viewModel = viewModel,
              onBack = { navController.popBackStack() },
              onOpenUserProfile = { uid ->
                if (uid == viewModel.currentUserId) navigateTopTab(VynzoRoute.Profile)
                else navController.navigate(VynzoRoute.UserProfile(uid))
              },
              onOpenComments = { postId, authorId -> navController.navigate(VynzoRoute.Comments(postId, authorId)) },
              onOpenChat = { navController.navigate(VynzoRoute.ChatList) },
              onOpenStory = { authorId -> navController.navigate(VynzoRoute.StoryViewer(authorId)) }
            )
          }

          composable<VynzoRoute.Settings> {
            SettingsScreen(
              viewModel = viewModel,
              currentThemeMode = themeMode,
              adsConsentAccepted = adsConsent,
              onBack = { navController.popBackStack() },
              onOpenEditProfile = { navController.navigate(VynzoRoute.EditProfile) },
              onOpenSavedPosts = { navController.navigate(VynzoRoute.SavedPosts) },
              onOpenAdminDashboard = { navController.navigate(VynzoRoute.AdminDashboard) },
              onOpenInfoPage = { pageType -> navController.navigate(VynzoRoute.InfoPage(pageType)) }
            )
          }

          composable<VynzoRoute.EditProfile> {
            EditProfileScreen(
              viewModel = viewModel,
              onBack = { navController.popBackStack() }
            )
          }

          composable<VynzoRoute.Connections> { backStack ->
            val args = backStack.toRoute<VynzoRoute.Connections>()
            ConnectionsScreen(
              userId = args.userId,
              initialTab = args.initialTab,
              viewModel = viewModel,
              onBack = { navController.popBackStack() },
              onOpenUserProfile = { uid ->
                if (uid == viewModel.currentUserId) navigateTopTab(VynzoRoute.Profile)
                else navController.navigate(VynzoRoute.UserProfile(uid))
              }
            )
          }

          composable<VynzoRoute.SavedPosts> {
            SavedPostsScreen(
              viewModel = viewModel,
              onBack = { navController.popBackStack() },
              onOpenComments = { postId, authorId -> navController.navigate(VynzoRoute.Comments(postId, authorId)) }
            )
          }

          composable<VynzoRoute.StoryViewer> { backStack ->
            val args = backStack.toRoute<VynzoRoute.StoryViewer>()
            StoryViewerScreen(
              authorId = args.authorId,
              viewModel = viewModel,
              onClose = { navController.popBackStack() },
              onOpenProfile = { uid ->
                navController.popBackStack()
                if (uid == viewModel.currentUserId) navigateTopTab(VynzoRoute.Profile)
                else navController.navigate(VynzoRoute.UserProfile(uid))
              }
            )
          }

          composable<VynzoRoute.AdminDashboard> {
            AdminDashboardScreen(
              viewModel = viewModel,
              onBack = { navController.popBackStack() },
              onOpenUserProfile = { uid -> navController.navigate(VynzoRoute.UserProfile(uid)) }
            )
          }

          composable<VynzoRoute.InfoPage> { backStack ->
            val args = backStack.toRoute<VynzoRoute.InfoPage>()
            InfoPageScreen(
              pageType = args.pageType,
              onBack = { navController.popBackStack() }
            )
          }
        }
      }
    }
  }
}
