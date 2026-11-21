package com.drakahell.claudesearch.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Persists [AppSettings] on disk with Jetpack DataStore. */
class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            preferredProvider = prefs[KEY_PROVIDER]
                ?.let { name -> SearchProvider.entries.firstOrNull { it.name == name } }
                ?: SearchProvider.GEMINI,
            geminiPausedUntilMillis = prefs[KEY_GEMINI_PAUSED_UNTIL] ?: 0L,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setPreferredProvider(provider: SearchProvider) {
        dataStore.edit { it[KEY_PROVIDER] = provider.name }
    }

    suspend fun pauseGeminiUntil(epochMillis: Long) {
        dataStore.edit { it[KEY_GEMINI_PAUSED_UNTIL] = epochMillis }
    }

    suspend fun resumeGemini() {
        dataStore.edit { it.remove(KEY_GEMINI_PAUSED_UNTIL) }
    }

    private companion object {
        val KEY_PROVIDER = stringPreferencesKey("search_provider")
        val KEY_GEMINI_PAUSED_UNTIL = longPreferencesKey("gemini_paused_until")
    }
}
