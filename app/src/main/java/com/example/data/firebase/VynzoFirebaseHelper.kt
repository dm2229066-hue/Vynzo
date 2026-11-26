package com.example.data.firebase

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

data class FirebaseConfigStatus(
  val packageName: String,
  val hasGoogleAppId: Boolean,
  val hasGoogleApiKey: Boolean,
  val hasProjectId: Boolean,
  val hasFirestoreDatabaseId: Boolean,
  val hasWebClientId: Boolean,
  val isFirebaseInitialized: Boolean
)

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

  fun diagnoseConfig(context: Context): FirebaseConfigStatus {
    val appContext = context.applicationContext ?: context
    val initialized = ensureInitialized(appContext)

    fun hasGeneratedString(name: String): Boolean {
      val resId = appContext.resources.getIdentifier(name, "string", appContext.packageName)
      if (resId == 0) return false
      return runCatching { appContext.getString(resId).trim().isNotEmpty() }.getOrDefault(false)
    }

    val hasDbId = runCatching {
      appContext.getString(com.example.R.string.firestore_database_id).trim().isNotEmpty()
    }.getOrDefault(false)

    val hasWebClient = runCatching {
      appContext.getString(com.example.R.string.default_web_client_id).trim().isNotEmpty()
    }.getOrDefault(false)

    return FirebaseConfigStatus(
      packageName = appContext.packageName,
      hasGoogleAppId = !forceUninitializedForTest && hasGeneratedString("google_app_id"),
      hasGoogleApiKey = !forceUninitializedForTest && hasGeneratedString("google_api_key"),
      hasProjectId = !forceUninitializedForTest && hasGeneratedString("project_id"),
      hasFirestoreDatabaseId = hasDbId,
      hasWebClientId = hasWebClient,
      isFirebaseInitialized = initialized
    )
  }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext?.findActivity()
  else -> null
}
