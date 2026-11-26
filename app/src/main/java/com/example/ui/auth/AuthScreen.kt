package com.example.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.firebase.VynzoAuthManager
import com.example.data.repository.UserRepository
import com.example.media.MediaUtils
import com.example.ui.components.VynzoAvatar
import com.example.ui.components.VynzoWordmark
import com.example.ui.theme.LocalVynzoColors
import com.example.ui.theme.VynzoBrandGradient
import com.example.ui.theme.VynzoEmerald
import com.example.ui.theme.VynzoIndigo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class OnboardingSlide(
  val title: String,
  val subtitle: String,
  val drawableRes: Int
)

@Composable
fun AuthFlowScreen(
  onboardingCompleted: Boolean,
  onMarkOnboardingCompleted: () -> Unit,
  onAuthSuccess: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var showSplash by remember { mutableStateOf(true) }
  var showOnboarding by remember(onboardingCompleted) { mutableStateOf(!onboardingCompleted) }
  var isSigningIn by remember { mutableStateOf(false) }
  var authError by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    VynzoAuthManager.attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      scope = scope,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = { /* stay on onboarding/auth */ }
    )
    delay(1100)
    showSplash = false
  }

  AnimatedContent(
    targetState = when {
      showSplash -> "splash"
      showOnboarding -> "onboarding"
      else -> "signin"
    },
    transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(240)) },
    label = "auth_stage"
  ) { stage ->
    when (stage) {
      "splash" -> VynzoSplashView()
      "onboarding" -> VynzoOnboardingView(
        onFinish = {
          showOnboarding = false
          onMarkOnboardingCompleted()
        }
      )
      else -> VynzoSignInView(
        isLoading = isSigningIn,
        errorMessage = authError,
        onGoogleSignIn = {
          authError = null
          isSigningIn = true
          VynzoAuthManager.onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            scope = scope,
            onAuthSuccess = {
              isSigningIn = false
              onAuthSuccess()
            },
            onAuthError = { msg ->
              isSigningIn = false
              authError = msg
            },
            onAuthCancelled = {
              isSigningIn = false
            }
          )
        },
        onReplayOnboarding = { showOnboarding = true }
      )
    }
  }
}

@Composable
private fun VynzoSplashView() {
  Box(modifier = Modifier.fillMaxSize()) {
    Image(
      painter = painterResource(id = R.drawable.img_splash_bg),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize()
    )
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.28f))
    )
    Column(
      modifier = Modifier
        .align(Alignment.Center)
        .padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_vynzo_logo),
        contentDescription = "Vynzo Emblem",
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .size(92.dp)
          .clip(RoundedCornerShape(24.dp))
      )
      Spacer(Modifier.height(20.dp))
      Text(
        text = "VYNZO",
        style = MaterialTheme.typography.displayLarge.copy(brush = VynzoBrandGradient),
        fontWeight = FontWeight.Bold
      )
      Spacer(Modifier.height(6.dp))
      Text(
        text = "Connect. Share. Create.",
        style = MaterialTheme.typography.titleMedium,
        color = Color.White.copy(alpha = 0.85f)
      )
    }
  }
}

@Composable
private fun VynzoOnboardingView(onFinish: () -> Unit) {
  val slides = remember {
    listOf(
      OnboardingSlide(
        title = "Connect with Your Circle",
        subtitle = "Follow creators, share ephemeral 24-hour stories, and chat in real time with voice, photos, and reactions.",
        drawableRes = R.drawable.img_onboarding_hero
      ),
      OnboardingSlide(
        title = "Share Moments in Style",
        subtitle = "Publish multi-photo carousels, tag friends and locations, and curate permanent story highlights on your profile.",
        drawableRes = R.drawable.img_featured_city
      ),
      OnboardingSlide(
        title = "Create Vynzo Reels",
        subtitle = "Trim and preview vertical short videos, discover trending hashtags, and grow your audience across Vynzo.",
        drawableRes = R.drawable.img_featured_studio
      )
    )
  }
  val pagerState = rememberPagerState(pageCount = { slides.size })
  val scope = rememberCoroutineScope()
  val colors = LocalVynzoColors.current

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      VynzoWordmark(fontSize = 20)
      TextButton(
        onClick = onFinish,
        modifier = Modifier.testTag("onboarding_skip_button")
      ) {
        Text("Skip")
      }
    }

    HorizontalPager(
      state = pagerState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .widthIn(max = 540.dp)
    ) { page ->
      val slide = slides[page]
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Image(
          painter = painterResource(id = slide.drawableRes),
          contentDescription = slide.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .fillMaxWidth()
            .height(290.dp)
            .clip(RoundedCornerShape(28.dp))
        )
        Spacer(Modifier.height(28.dp))
        Text(
          text = slide.title,
          style = MaterialTheme.typography.headlineLarge,
          textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
          text = slide.subtitle,
          style = MaterialTheme.typography.bodyLarge,
          color = colors.mutedText,
          textAlign = TextAlign.Center
        )
      }
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      repeat(slides.size) { idx ->
        val active = pagerState.currentPage == idx
        Box(
          modifier = Modifier
            .width(if (active) 24.dp else 8.dp)
            .height(8.dp)
            .clip(CircleShape)
            .background(if (active) VynzoIndigo else colors.hairlineBorder)
        )
      }
    }

    Spacer(Modifier.height(24.dp))
    Button(
      onClick = {
        if (pagerState.currentPage < slides.lastIndex) {
          scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
        } else {
          onFinish()
        }
      },
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 460.dp)
        .height(54.dp)
        .testTag("onboarding_next_button")
    ) {
      Text(
        text = if (pagerState.currentPage < slides.lastIndex) "Next" else "Get Started",
        style = MaterialTheme.typography.titleMedium
      )
    }
  }
}

@Composable
private fun VynzoSignInView(
  isLoading: Boolean,
  errorMessage: String?,
  onGoogleSignIn: () -> Unit,
  onReplayOnboarding: () -> Unit
) {
  val colors = LocalVynzoColors.current

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    Image(
      painter = painterResource(id = R.drawable.img_splash_bg),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .fillMaxWidth()
        .height(340.dp)
    )
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(340.dp)
        .background(
          Brush.verticalGradient(
            colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
          )
        )
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Spacer(Modifier.height(24.dp))

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(max = 460.dp)
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_vynzo_logo),
          contentDescription = "Vynzo logo",
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(76.dp)
            .clip(RoundedCornerShape(20.dp))
        )
        Spacer(Modifier.height(16.dp))
        Text(
          text = "Welcome to Vynzo",
          style = MaterialTheme.typography.headlineLarge,
          textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
          text = "Connect. Share. Create.",
          style = MaterialTheme.typography.titleMedium.copy(brush = VynzoBrandGradient),
          textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
          text = "Sign in with your Google account to sync your feed, stories, Reels, and real-time messages securely across devices.",
          style = MaterialTheme.typography.bodyMedium,
          color = colors.mutedText,
          textAlign = TextAlign.Center
        )
      }

      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 460.dp)
          .padding(vertical = 24.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, colors.hairlineBorder),
        tonalElevation = 3.dp
      ) {
        Column(
          modifier = Modifier.padding(22.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          if (errorMessage != null) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Rounded.ErrorOutline,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(8.dp))
                Text(
                  text = errorMessage,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.error
                )
              }
            }
          }

          Button(
            onClick = onGoogleSignIn,
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
              .fillMaxWidth()
              .height(54.dp)
              .testTag("google_sign_in_button")
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.5.dp
              )
            } else {
              Text(
                text = "Continue with Google",
                style = MaterialTheme.typography.titleMedium
              )
            }
          }

          Spacer(Modifier.height(14.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.Security,
              contentDescription = null,
              tint = VynzoEmerald,
              modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
              text = "Protected by Firebase Authentication & Encrypted Cloud Rules",
              style = MaterialTheme.typography.bodySmall,
              color = colors.mutedText
            )
          }

          Spacer(Modifier.height(10.dp))
          OutlinedButton(
            onClick = onReplayOnboarding,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Preview Vynzo Tour")
          }
        }
      }
    }
  }
}

@Composable
fun ProfileSetupScreen(
  defaultDisplayName: String,
  defaultEmail: String,
  defaultAvatarUrl: String,
  userId: String,
  userRepository: UserRepository,
  onProfileCreated: () -> Unit,
  onSignOut: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val colors = LocalVynzoColors.current

  var username by remember {
    mutableStateOf(
      defaultEmail.substringBefore("@")
        .lowercase()
        .replace(Regex("[^a-z0-9_.]"), "")
        .ifBlank { "vynzo_${userId.take(6)}" }
        .take(24)
    )
  }
  var displayName by remember { mutableStateOf(defaultDisplayName.ifBlank { "Vynzo Creator" }) }
  var bio by remember { mutableStateOf("") }
  var avatarDataUri by remember { mutableStateOf(defaultAvatarUrl) }
  var usernameAvailable by remember { mutableStateOf<Boolean?>(null) }
  var isSubmitting by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  val photoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      scope.launch {
        val bmp = MediaUtils.decodeBitmapFromUri(context, uri, maxDim = 512)
        if (bmp != null) {
          avatarDataUri = MediaUtils.encodeBitmapToDataUri(bmp, maxBytes = 90_000)
        }
      }
    }
  }

  LaunchedEffect(username) {
    val clean = username.trim().lowercase()
    if (clean.length >= 3) {
      delay(350)
      userRepository.checkUsernameAvailable(clean, userId)
        .onSuccess { usernameAvailable = it }
    } else {
      usernameAvailable = false
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding()
      .navigationBarsPadding()
      .verticalScroll(rememberScrollState())
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      VynzoWordmark(fontSize = 20)
      TextButton(onClick = onSignOut) {
        Text("Sign out")
      }
    }

    Spacer(Modifier.height(24.dp))
    Text(
      text = "Claim Your Vynzo Handle",
      style = MaterialTheme.typography.headlineMedium,
      textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(6.dp))
    Text(
      text = "Set up your creator identity so friends can find and follow you.",
      style = MaterialTheme.typography.bodyMedium,
      color = colors.mutedText,
      textAlign = TextAlign.Center
    )

    Spacer(Modifier.height(24.dp))
    Box(
      modifier = Modifier
        .clickable {
          photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        .testTag("setup_avatar_picker"),
      contentAlignment = Alignment.BottomEnd
    ) {
      VynzoAvatar(
        avatarUrl = avatarDataUri,
        displayName = displayName,
        size = 96.dp,
        hasStoryRing = true
      )
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(32.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Rounded.AddAPhoto,
            contentDescription = "Choose profile photo",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }

    Spacer(Modifier.height(24.dp))
    Column(
      modifier = Modifier.widthIn(max = 460.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      OutlinedTextField(
        value = username,
        onValueChange = { username = it.lowercase().replace(" ", "").take(30) },
        label = { Text("Username") },
        prefix = { Text("@") },
        trailingIcon = {
          when (usernameAvailable) {
            true -> Icon(Icons.Rounded.CheckCircle, contentDescription = "Available", tint = VynzoEmerald)
            false -> Icon(Icons.Rounded.ErrorOutline, contentDescription = "Unavailable", tint = MaterialTheme.colorScheme.error)
            null -> Unit
          }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("setup_username_input")
      )

      OutlinedTextField(
        value = displayName,
        onValueChange = { displayName = it.take(50) },
        label = { Text("Display Name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("setup_displayname_input")
      )

      OutlinedTextField(
        value = bio,
        onValueChange = { bio = it.take(200) },
        label = { Text("Bio (optional)") },
        maxLines = 3,
        modifier = Modifier.fillMaxWidth().testTag("setup_bio_input")
      )

      if (errorMsg != null) {
        Text(
          text = errorMsg!!,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error
        )
      }

      Spacer(Modifier.height(6.dp))
      Button(
        onClick = {
          errorMsg = null
          if (usernameAvailable == false) {
            errorMsg = "Please choose an available username (min 3 characters)."
            return@Button
          }
          isSubmitting = true
          scope.launch {
            userRepository.createInitialProfile(
              username = username,
              displayName = displayName,
              bio = bio,
              avatarUrl = avatarDataUri,
              email = defaultEmail
            ).onSuccess {
              isSubmitting = false
              onProfileCreated()
            }.onFailure {
              isSubmitting = false
              errorMsg = it.localizedMessage ?: "Failed to create profile"
            }
          }
        },
        enabled = !isSubmitting,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("setup_complete_button")
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
        } else {
          Text("Launch Vynzo", style = MaterialTheme.typography.titleMedium)
        }
      }
    }
  }
}
