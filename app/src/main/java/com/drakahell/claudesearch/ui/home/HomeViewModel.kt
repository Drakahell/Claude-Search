package com.drakahell.claudesearch.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.drakahell.claudesearch.ClaudeSearchApplication
import com.drakahell.claudesearch.data.settings.SearchProvider
import com.drakahell.claudesearch.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * UI state of the home screen. Everything the screen shows is described here;
 * the screen itself makes no decisions and only renders this state.
 */
data class HomeUiState(
    /** Whether the app is set as the default assistant (step 4). */
    val isAssistantEnabled: Boolean = false,
    val preferredProvider: SearchProvider = SearchProvider.GEMINI,
    /** Set when Gemini is paused because of the quota; null otherwise. */
    val geminiPausedUntilMillis: Long? = null,
)

class HomeViewModel(
    settingsRepository: SettingsRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = settingsRepository.settings
        .map { settings ->
            HomeUiState(
                preferredProvider = settings.preferredProvider,
                geminiPausedUntilMillis = settings.geminiPausedUntilMillis
                    .takeIf { settings.isGeminiPaused(clock()) },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as ClaudeSearchApplication
                HomeViewModel(app.settingsRepository)
            }
        }
    }
}
