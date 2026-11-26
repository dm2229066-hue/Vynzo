package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdsClick
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.BuildConfig
import com.example.ui.theme.LocalVynzoColors
import com.example.ui.theme.VynzoBrandGradient
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object VynzoAdManager {
  private const val TAG = "VynzoAds"

  private var initialized = false
  private var lastInterstitialShownAt = 0L
  private var sessionInterstitialCount = 0
  private const val MAX_INTERSTITIALS_PER_SESSION = 3

  private var cachedInterstitial: InterstitialAd? = null
  private var cachedRewarded: RewardedAd? = null

  fun bannerUnitId(): String = BuildConfig.ADMOB_BANNER_ID.ifBlank {
    "ca-app-pub-3940256099942544/6300978111"
  }

  fun interstitialUnitId(): String = BuildConfig.ADMOB_INTERSTITIAL_ID.ifBlank {
    "ca-app-pub-3940256099942544/1033173712"
  }

  fun rewardedUnitId(): String = BuildConfig.ADMOB_REWARDED_ID.ifBlank {
    "ca-app-pub-3940256099942544/5224354917"
  }

  fun nativeUnitId(): String = BuildConfig.ADMOB_NATIVE_ID.ifBlank {
    "ca-app-pub-3940256099942544/2247696110"
  }

  fun initialize(context: Context, personalizedConsent: Boolean) {
    if (!initialized) {
      runCatching {
        val config = RequestConfiguration.Builder()
          .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
          .build()
        MobileAds.setRequestConfiguration(config)
        MobileAds.initialize(context) { status ->
          Log.d(TAG, "AdMob initialized: ${status.adapterStatusMap.keys}")
        }
        initialized = true
      }.onFailure {
        Log.w(TAG, "AdMob init skipped or unavailable: ${it.message}")
      }
    }
    preloadInterstitial(context, personalizedConsent)
    preloadRewarded(context, personalizedConsent)
  }

  fun buildAdRequest(personalizedConsent: Boolean): AdRequest {
    val builder = AdRequest.Builder()
    if (!personalizedConsent) {
      val extras = android.os.Bundle().apply { putString("npa", "1") }
      builder.addNetworkExtrasBundle(com.google.ads.mediation.admob.AdMobAdapter::class.java, extras)
    }
    return builder.build()
  }

  fun preloadInterstitial(context: Context, personalizedConsent: Boolean) {
    if (cachedInterstitial != null) return
    runCatching {
      InterstitialAd.load(
        context,
        interstitialUnitId(),
        buildAdRequest(personalizedConsent),
        object : InterstitialAdLoadCallback() {
          override fun onAdLoaded(ad: InterstitialAd) {
            cachedInterstitial = ad
          }

          override fun onAdFailedToLoad(error: LoadAdError) {
            Log.w(TAG, "Interstitial failed to load: ${error.message}")
            cachedInterstitial = null
          }
        }
      )
    }
  }

  fun showInterstitialIfEligible(
    activity: Activity?,
    adsEnabled: Boolean,
    cooldownSec: Long,
    personalizedConsent: Boolean
  ): Boolean {
    if (activity == null || !adsEnabled) return false
    if (sessionInterstitialCount >= MAX_INTERSTITIALS_PER_SESSION) return false
    val now = System.currentTimeMillis()
    if (now - lastInterstitialShownAt < cooldownSec * 1000L) return false
    val ad = cachedInterstitial ?: run {
      preloadInterstitial(activity, personalizedConsent)
      return false
    }
    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
      override fun onAdDismissedFullScreenContent() {
        cachedInterstitial = null
        preloadInterstitial(activity, personalizedConsent)
      }

      override fun onAdFailedToShowFullScreenContent(adError: AdError) {
        cachedInterstitial = null
      }
    }
    lastInterstitialShownAt = now
    sessionInterstitialCount += 1
    ad.show(activity)
    return true
  }

  fun preloadRewarded(context: Context, personalizedConsent: Boolean) {
    if (cachedRewarded != null) return
    runCatching {
      RewardedAd.load(
        context,
        rewardedUnitId(),
        buildAdRequest(personalizedConsent),
        object : RewardedAdLoadCallback() {
          override fun onAdLoaded(ad: RewardedAd) {
            cachedRewarded = ad
          }

          override fun onAdFailedToLoad(error: LoadAdError) {
            Log.w(TAG, "Rewarded failed to load: ${error.message}")
            cachedRewarded = null
          }
        }
      )
    }
  }

  fun showRewardedAd(
    activity: Activity?,
    personalizedConsent: Boolean,
    onRewardEarned: () -> Unit,
    onUnavailable: (String) -> Unit
  ) {
    if (activity == null) {
      onUnavailable("Activity unavailable for ad display")
      return
    }
    val ad = cachedRewarded
    if (ad == null) {
      preloadRewarded(activity, personalizedConsent)
      onUnavailable("Rewarded ad is still loading. Please try again in a moment.")
      return
    }
    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
      override fun onAdDismissedFullScreenContent() {
        cachedRewarded = null
        preloadRewarded(activity, personalizedConsent)
      }

      override fun onAdFailedToShowFullScreenContent(error: AdError) {
        cachedRewarded = null
        onUnavailable(error.message)
      }
    }
    ad.show(activity) {
      onRewardEarned()
    }
  }
}

@Composable
fun VynzoBannerAd(
  adsEnabled: Boolean,
  personalizedConsent: Boolean,
  modifier: Modifier = Modifier
) {
  if (!adsEnabled) return
  val context = LocalContext.current
  val colors = LocalVynzoColors.current
  var isLoading by remember { mutableStateOf(true) }
  var hasError by remember { mutableStateOf(false) }

  if (hasError) return

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
    border = BorderStroke(1.dp, colors.hairlineBorder)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp, horizontal = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "SPONSORED",
          style = MaterialTheme.typography.labelSmall,
          color = colors.mutedText
        )
        if (isLoading) {
          CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
        }
      }
      AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { ctx ->
          AdView(ctx).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = VynzoAdManager.bannerUnitId()
            adListener = object : AdListener() {
              override fun onAdLoaded() {
                isLoading = false
                hasError = false
              }

              override fun onAdFailedToLoad(error: LoadAdError) {
                isLoading = false
                hasError = true
              }
            }
            loadAd(VynzoAdManager.buildAdRequest(personalizedConsent))
          }
        }
      )
    }
  }
}

@Composable
fun VynzoNativeFeedAdCard(
  adsEnabled: Boolean,
  personalizedConsent: Boolean,
  modifier: Modifier = Modifier
) {
  if (!adsEnabled) return
  val context = LocalContext.current
  val colors = LocalVynzoColors.current
  var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
  var isLoading by remember { mutableStateOf(true) }
  var loadError by remember { mutableStateOf<String?>(null) }

  DisposableEffect(personalizedConsent) {
    var active = true
    val loader = AdLoader.Builder(context, VynzoAdManager.nativeUnitId())
      .forNativeAd { ad ->
        if (active) {
          nativeAd?.destroy()
          nativeAd = ad
          isLoading = false
          loadError = null
        } else {
          ad.destroy()
        }
      }
      .withAdListener(object : AdListener() {
        override fun onAdFailedToLoad(error: LoadAdError) {
          if (active) {
            isLoading = false
            loadError = error.message
          }
        }
      })
      .build()
    runCatching { loader.loadAd(VynzoAdManager.buildAdRequest(personalizedConsent)) }
      .onFailure {
        isLoading = false
        loadError = it.message
      }

    onDispose {
      active = false
      nativeAd?.destroy()
    }
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, colors.hairlineBorder),
    tonalElevation = 2.dp
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(VynzoBrandGradient),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Rounded.AutoAwesome,
              contentDescription = "Sponsored partner",
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(18.dp)
            )
          }
          Spacer(Modifier.width(10.dp))
          Column {
            Text(
              text = nativeAd?.headline ?: "Vynzo Partner Spotlight",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = nativeAd?.advertiser ?: "Sponsored",
              style = MaterialTheme.typography.labelSmall,
              color = colors.mutedText
            )
          }
        }
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.primaryContainer
        ) {
          Text(
            text = "AD",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }
      }

      Spacer(Modifier.height(10.dp))
      when {
        isLoading -> {
          Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("Loading sponsored content...", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
          }
        }
        nativeAd != null -> {
          val ad = nativeAd!!
          Text(
            text = ad.body ?: "Discover curated creator tools and brand experiences on Vynzo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(Modifier.height(12.dp))
          Button(
            onClick = { runCatching { ad.performClick(android.os.Bundle()) } },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Rounded.AdsClick, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(ad.callToAction ?: "Learn More")
          }
        }
        else -> {
          Text(
            text = loadError?.let { "Partner slot unavailable right now." } ?: "Partner slot ready.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.mutedText
          )
        }
      }
    }
  }
}
