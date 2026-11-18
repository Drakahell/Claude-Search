package com.drakahell.claudesearch.ui.trace

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.drakahell.claudesearch.data.image.ImageSource
import com.drakahell.claudesearch.ui.theme.ClaudeSearchTheme

/**
 * Full-screen trace mode for a single image. In step 4 the same [TraceScreen]
 * will be shown over other apps with a real screenshot instead of a picked image.
 */
class TraceActivity : ComponentActivity() {

    private val viewModel: TraceViewModel by viewModels {
        TraceViewModel.factory(sourceFrom(intent))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        setContent {
            ClaudeSearchTheme(darkTheme = true) {
                TraceRoute(viewModel = viewModel, onClose = ::finish)
            }
        }
    }

    /** Immersive mode: the image takes the whole screen, bars appear on swipe. */
    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    companion object {
        fun sampleIntent(context: Context): Intent = Intent(context, TraceActivity::class.java)

        fun imageIntent(context: Context, uri: Uri): Intent =
            Intent(context, TraceActivity::class.java)
                .setData(uri)
                // Pass on the read access we got from the Photo Picker.
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        private fun sourceFrom(intent: Intent): ImageSource =
            intent.data?.let { ImageSource.Picked(it) } ?: ImageSource.Sample
    }
}
