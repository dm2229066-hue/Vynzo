package com.example.data.firebase

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

object VynzoFirebaseHelper {
  private const val TAG = "VynzoFirebase"

  @androidx.annotation.VisibleForTesting
  internal var forceUninitializedForTest: Boolean = false

  fun ensureInitialized(context: Context): Boolean {
    if (forceUninitializedForTest) return false
    val appContext = context.applicationContext ?: context
    return runCatching {
      if (FirebaseApp.getApps(appContext).isEmpty()) {
        FirebaseApp.initializeApp(appContext)
      }
      if (FirebaseApp.getApps(appContext).isEmpty()) {
        return false
      }
      Firebase.auth
      true
    }.onFailure { err ->
      Log.w(TAG, "Firebase initialization unavailable: ${err.message}")
    }.getOrDefault(false)
  }

  fun isFirebaseReady(context: Context): Boolean = ensureInitialized(context)

  fun currentUserOrNull(context: Context): FirebaseUser? {
    if (!ensureInitialized(context)) return null
    return runCatching { Firebase.auth.currentUser }.getOrNull()
  }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext?.findActivity()
  else -> null
}
