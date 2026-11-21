package com.drakahell.claudesearch.ui.settings

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
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferredProvider: SearchProvider = SearchProvider.GEMINI,
    /** Set when Gemini is paused because of the quota; null otherwise. */
    val geminiPausedUntilMillis: Long? = null,
)

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = repository.settings
        .map { settings ->
            SettingsUiState(
                preferredProvider = settings.preferredProvider,
                geminiPausedUntilMillis = settings.geminiPausedUntilMillis
                    .takeIf { settings.isGeminiPaused(clock()) },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onProviderSelected(provider: SearchProvider) {
        viewModelScope.launch { repository.setPreferredProvider(provider) }
    }

    fun onResumeGemini() {
        viewModelScope.launch { repository.resumeGemini() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as ClaudeSearchApplication
                SettingsViewModel(app.settingsRepository)
            }
        }
    }
}
