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
import com.drakahell.claudesearch.R
import com.drakahell.claudesearch.data.image.ImageLoader
import com.drakahell.claudesearch.data.image.ImageSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The part of the image chosen by the user. */
data class TraceSelection(
    /** Selection bounds in image pixel coordinates. */
    val bounds: Rect,
    /** Cropped fragment; this is what will be sent to the AI in step 3. */
    val crop: ImageBitmap,
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
) : ViewModel() {

    private val _uiState = MutableStateFlow<TraceUiState>(TraceUiState.Loading)
    val uiState: StateFlow<TraceUiState> = _uiState.asStateFlow()

    /** Original bitmap, kept to crop selections from it. */
    private var sourceBitmap: Bitmap? = null
    private var cropJob: Job? = null

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
        cropJob?.cancel()
        cropJob = viewModelScope.launch {
            val pixels = SelectionMath.toPixelRect(bounds, IntSize(bitmap.width, bitmap.height))
            val crop = withContext(Dispatchers.Default) {
                Bitmap.createBitmap(bitmap, pixels.left, pixels.top, pixels.width, pixels.height)
            }
            _uiState.update { state ->
                if (state is TraceUiState.Ready) {
                    state.copy(selection = TraceSelection(bounds, crop.asImageBitmap()))
                } else {
                    state
                }
            }
        }
    }

    fun clearSelection() {
        cropJob?.cancel()
        _uiState.update { state ->
            if (state is TraceUiState.Ready) state.copy(selection = null) else state
        }
    }

    companion object {
        private const val TAG = "TraceViewModel"

        fun factory(source: ImageSource): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])
                TraceViewModel(ImageLoader(application.contentResolver), source)
            }
        }
    }
}
