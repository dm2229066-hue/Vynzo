package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.VynzoThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.vynzoDataStore by preferencesDataStore(name = "vynzo_prefs")

class VynzoPreferencesRepository(context: Context) {
  private val appContext = context.applicationContext ?: context
  private val onboardingKey = booleanPreferencesKey("onboarding_completed")
  private val themeModeKey = stringPreferencesKey("theme_mode")
  private val adsConsentKey = booleanPreferencesKey("ads_consent")

  private val safeData = appContext.vynzoDataStore.data.catch { err ->
    if (err is IOException) emit(emptyPreferences()) else throw err
  }

  val onboardingCompleted: Flow<Boolean> =
    safeData.map { it[onboardingKey] ?: false }

  val themeMode: Flow<VynzoThemeMode> =
    safeData.map { VynzoThemeMode.fromWire(it[themeModeKey]) }

  val adsConsentAccepted: Flow<Boolean> =
    safeData.map { it[adsConsentKey] ?: true }

  suspend fun setOnboardingCompleted(completed: Boolean) {
    appContext.vynzoDataStore.edit { it[onboardingKey] = completed }
  }

  suspend fun setThemeMode(mode: VynzoThemeMode) {
    appContext.vynzoDataStore.edit { it[themeModeKey] = mode.wireValue }
  }

  suspend fun setAdsConsentAccepted(accepted: Boolean) {
    appContext.vynzoDataStore.edit { it[adsConsentKey] = accepted }
  }
}
