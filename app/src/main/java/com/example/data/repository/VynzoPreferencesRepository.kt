package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.VynzoThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.vynzoDataStore by preferencesDataStore(name = "vynzo_prefs")

class VynzoPreferencesRepository(private val context: Context) {
  private val onboardingKey = booleanPreferencesKey("onboarding_completed")
  private val themeModeKey = stringPreferencesKey("theme_mode")
  private val adsConsentKey = booleanPreferencesKey("ads_consent")

  val onboardingCompleted: Flow<Boolean> =
    context.vynzoDataStore.data.map { it[onboardingKey] ?: false }

  val themeMode: Flow<VynzoThemeMode> =
    context.vynzoDataStore.data.map { VynzoThemeMode.fromWire(it[themeModeKey]) }

  val adsConsentAccepted: Flow<Boolean> =
    context.vynzoDataStore.data.map { it[adsConsentKey] ?: true }

  suspend fun setOnboardingCompleted(completed: Boolean) {
    context.vynzoDataStore.edit { it[onboardingKey] = completed }
  }

  suspend fun setThemeMode(mode: VynzoThemeMode) {
    context.vynzoDataStore.edit { it[themeModeKey] = mode.wireValue }
  }

  suspend fun setAdsConsentAccepted(accepted: Boolean) {
    context.vynzoDataStore.edit { it[adsConsentKey] = accepted }
  }
}
