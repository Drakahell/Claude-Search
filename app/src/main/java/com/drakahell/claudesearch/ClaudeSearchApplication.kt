package com.drakahell.claudesearch

import android.app.Application
import com.drakahell.claudesearch.data.ai.GeminiVisualSearch
import com.drakahell.claudesearch.data.settings.SettingsRepository
import com.google.firebase.Firebase
import com.google.firebase.initialize

/**
 * Holds app-wide singletons (simple manual dependency injection).
 * ViewModel factories get dependencies from here via `APPLICATION_KEY`.
 */
class ClaudeSearchApplication : Application() {

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }

    val geminiVisualSearch: GeminiVisualSearch by lazy { GeminiVisualSearch() }

    override fun onCreate() {
        super.onCreate()
        Firebase.initialize(this)
        // Firebase AI Logic rejects requests without an App Check token.
        // The implementation differs per build type (see src/debug and src/release).
        AppCheckSetup.install()
    }
}
