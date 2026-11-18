package com.drakahell.claudesearch.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI state of the home screen. Everything the screen shows is described here;
 * the screen itself makes no decisions and only renders this state.
 */
data class HomeUiState(
    /** Whether the trace-on-image demo is available (step 2). */
    val isDemoAvailable: Boolean = false,
    /** Whether the app is set as the default assistant (step 4). */
    val isAssistantEnabled: Boolean = false,
    /** Whether Gemini is configured (step 3). */
    val isAiReady: Boolean = false,
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onTryDemoClick() {
        // Step 2: open the trace screen with a sample image.
    }
}
