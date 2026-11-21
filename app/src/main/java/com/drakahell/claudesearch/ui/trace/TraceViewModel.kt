package com.drakahell.claudesearch.ui.trace

import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.drakahell.claudesearch.ClaudeSearchApplication
import com.drakahell.claudesearch.R
import com.drakahell.claudesearch.data.ai.GeminiVisualSearch
import com.drakahell.claudesearch.data.ai.QuotaPolicy
import com.drakahell.claudesearch.data.ai.SearchFailure
import com.drakahell.claudesearch.data.image.ImageLoader
import com.drakahell.claudesearch.data.image.ImageSource
import com.drakahell.claudesearch.data.settings.SearchProvider
import com.drakahell.claudesearch.data.settings.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One question/answer pair in the conversation about a selection. */
data class ChatTurn(
    /** Follow-up question; null for the first, automatic "what is this?" request. */
    val question: String?,
    val answer: String,
)

enum class LensReason {
    /** The user picked Google Lens in settings. */
    USER_CHOICE,
    /** Gemini is paused because its quota is exhausted. */
    GEMINI_QUOTA,
    /** The user switched to Lens from the Gemini panel (e.g. after an error). */
    MANUAL,
    /** Gemini can't be used right now (region or access error). */
    GEMINI_UNAVAILABLE,
}

/** What the bottom panel shows for the current selection. */
sealed interface SearchUiState {
    data object Idle : SearchUiState

    data class Gemini(
        val turns: List<ChatTurn> = emptyList(),
        val isStreaming: Boolean = false,
        val failure: SearchFailure? = null,
    ) : SearchUiState

    data class Lens(
        val reason: LensReason,
        /** Until when Gemini is paused (only for [LensReason.GEMINI_QUOTA]). */
        val geminiPausedUntilMillis: Long = 0L,
    ) : SearchUiState
}

/** The part of the image chosen by the user. */
data class TraceSelection(
    /** Selection bounds in image pixel coordinates. */
    val bounds: Rect,
    /** Cropped fragment that is sent to Gemini / Google Lens. */
    val crop: ImageBitmap,
    val search: SearchUiState = SearchUiState.Idle,
)

sealed interface TraceUiState {
    data object Loading : TraceUiState
    data class Error(@param:StringRes val messageRes: Int) : TraceUiState
    data class Ready(
        val image: ImageBitmap,
        val selection: TraceSelection? = null,
    ) : TraceUiState
}

class TraceViewModel(
    private val imageLoader: ImageLoader,
    private val source: ImageSource,
    private val settingsRepository: SettingsRepository,
    private val gemini: GeminiVisualSearch,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TraceUiState>(TraceUiState.Loading)
    val uiState: StateFlow<TraceUiState> = _uiState.asStateFlow()

    /** Original bitmap, kept to crop selections from it. */
    private var sourceBitmap: Bitmap? = null
    private var cropBitmap: Bitmap? = null
    private var geminiSession: GeminiVisualSearch.Session? = null

    /** Cropping + searching for the current selection; cancelled on a new selection. */
    private var selectionJob: Job? = null

    init {
        loadImage()
    }

    private fun loadImage() {
        viewModelScope.launch {
            _uiState.value = try {
                val bitmap = imageLoader.load(source)
                sourceBitmap = bitmap
                TraceUiState.Ready(image = bitmap.asImageBitmap())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load image from $source", e)
                TraceUiState.Error(R.string.trace_error_load)
            }
        }
    }

    /** Called when the user finishes a trace or tap. [bounds] are in image coordinates. */
    fun onSelectionMade(bounds: Rect) {
        val bitmap = sourceBitmap ?: return
        selectionJob?.cancel()
        geminiSession = null
        selectionJob = viewModelScope.launch {
            val pixels = SelectionMath.toPixelRect(bounds, IntSize(bitmap.width, bitmap.height))
            val crop = withContext(Dispatchers.Default) {
                Bitmap.createBitmap(bitmap, pixels.left, pixels.top, pixels.width, pixels.height)
            }
            cropBitmap = crop
            updateReady { it.copy(selection = TraceSelection(bounds, crop.asImageBitmap())) }
            startSearch()
        }
    }

    fun clearSelection() {
        selectionJob?.cancel()
        geminiSession = null
        cropBitmap = null
        updateReady { it.copy(selection = null) }
    }

    /** Sends a follow-up question about the current selection to Gemini. */
    fun askFollowUp(question: String) {
        val text = question.trim()
        val session = geminiSession ?: return
        val search = currentSearch() as? SearchUiState.Gemini ?: return
        if (text.isEmpty() || search.isStreaming) return
        selectionJob = viewModelScope.launch {
            runGeminiTurn(question = text) { session.ask(text) }
        }
    }

    /** Shows Google Lens results for the current selection instead of Gemini. */
    fun showLens() {
        if (currentSearch() == null) return
        selectionJob?.cancel()
        updateSearch { SearchUiState.Lens(LensReason.MANUAL) }
    }

    /** Repeats the last failed Gemini request. */
    fun retry() {
        val search = currentSearch() as? SearchUiState.Gemini ?: return
        val lastQuestion = search.turns.lastOrNull()?.question
        selectionJob?.cancel()
        if (lastQuestion == null) {
            // The very first request failed: start over.
            selectionJob = viewModelScope.launch { startSearch() }
        } else {
            updateSearch { search.copy(turns = search.turns.dropLast(1), failure = null) }
            askFollowUp(lastQuestion)
        }
    }

    private suspend fun startSearch() {
        val crop = cropBitmap ?: return
        val settings = settingsRepository.current()
        val now = clock()
        when (settings.effectiveProvider(now)) {
            SearchProvider.GOOGLE_LENS -> updateSearch {
                if (settings.preferredProvider == SearchProvider.GEMINI) {
                    SearchUiState.Lens(LensReason.GEMINI_QUOTA, settings.geminiPausedUntilMillis)
                } else {
                    SearchUiState.Lens(LensReason.USER_CHOICE)
                }
            }

            SearchProvider.GEMINI -> {
                val session = gemini.newSession().also { geminiSession = it }
                updateSearch { SearchUiState.Gemini() }
                runGeminiTurn(question = null) { session.describe(crop, sourceBitmap) }
            }
        }
    }

    /** Streams one answer into the conversation and handles errors. */
    private suspend fun runGeminiTurn(question: String?, request: () -> Flow<String>) {
        updateSearch { old ->
            val turns = (old as? SearchUiState.Gemini)?.turns.orEmpty()
            SearchUiState.Gemini(turns = turns + ChatTurn(question, ""), isStreaming = true)
        }
        try {
            request().collect { chunk -> appendToLastAnswer(chunk) }
            updateSearch { (it as? SearchUiState.Gemini)?.copy(isStreaming = false) ?: it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Gemini request failed", e)
            onGeminiFailure(SearchFailure.from(e), e.message)
        }
    }

    private suspend fun onGeminiFailure(failure: SearchFailure, message: String?) {
        val search = currentSearch() as? SearchUiState.Gemini
        val hasAnswers = search?.turns?.any { it.answer.isNotBlank() } == true
        when (failure) {
            SearchFailure.QUOTA -> {
                val until = QuotaPolicy.pausedUntil(message, clock())
                settingsRepository.pauseGeminiUntil(until)
                if (!hasAnswers) {
                    // Nothing useful from Gemini yet: switch straight to Google Lens.
                    updateSearch { SearchUiState.Lens(LensReason.GEMINI_QUOTA, until) }
                    return
                }
            }
            SearchFailure.REGION, SearchFailure.ACCESS_DENIED -> if (!hasAnswers) {
                updateSearch { SearchUiState.Lens(LensReason.GEMINI_UNAVAILABLE) }
                return
            }
            else -> Unit
        }
        updateSearch {
            (it as? SearchUiState.Gemini)?.copy(isStreaming = false, failure = failure) ?: it
        }
    }

    private fun appendToLastAnswer(chunk: String) {
        if (chunk.isEmpty()) return
        updateSearch { search ->
            if (search !is SearchUiState.Gemini || search.turns.isEmpty()) return@updateSearch search
            val last = search.turns.last()
            search.copy(turns = search.turns.dropLast(1) + last.copy(answer = last.answer + chunk))
        }
    }

    private fun currentSearch(): SearchUiState? =
        (_uiState.value as? TraceUiState.Ready)?.selection?.search

    private inline fun updateReady(crossinline transform: (TraceUiState.Ready) -> TraceUiState.Ready) {
        _uiState.update { state -> if (state is TraceUiState.Ready) transform(state) else state }
    }

    private inline fun updateSearch(crossinline transform: (SearchUiState) -> SearchUiState) {
        updateReady { ready ->
            val selection = ready.selection ?: return@updateReady ready
            ready.copy(selection = selection.copy(search = transform(selection.search)))
        }
    }

    companion object {
        private const val TAG = "TraceViewModel"

        fun factory(source: ImageSource): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as ClaudeSearchApplication
                TraceViewModel(
                    imageLoader = ImageLoader(app.contentResolver),
                    source = source,
                    settingsRepository = app.settingsRepository,
                    gemini = app.geminiVisualSearch,
                )
            }
        }
    }
}
