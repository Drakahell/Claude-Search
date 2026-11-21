package com.drakahell.claudesearch.ui.trace

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.viewinterop.AndroidView
import com.drakahell.claudesearch.data.lens.LensWeb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/** Shows Google Lens web results for [crop] inside the app. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LensWebView(crop: ImageBitmap, modifier: Modifier = Modifier) {
    val uploadPage by produceState<String?>(initialValue = null, crop) {
        value = withContext(Dispatchers.Default) {
            LensWeb.uploadPage(
                image = crop.asAndroidBitmap(),
                languageTag = Locale.getDefault().toLanguageTag(),
                nowMillis = System.currentTimeMillis(),
            )
        }
    }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    // Index 0 is our upload page, index 1 the first results page: only go back past that.
    var canGoBack by remember { mutableStateOf(false) }

    BackHandler(enabled = canGoBack) { webView?.goBack() }

    Box(modifier) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean {
                            val url = request.url
                            if (LensWeb.isGoogleHost(url.host)) return false
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url)) }
                            return true
                        }

                        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                            canGoBack = view.copyBackForwardList().currentIndex > 1
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView, newProgress: Int) {
                            progress = newProgress
                        }
                    }
                    webView = this
                }
            },
            update = { view ->
                val page = uploadPage
                // Load each upload page only once (tag remembers what is loaded).
                if (page != null && view.tag != page.hashCode()) {
                    view.tag = page.hashCode()
                    view.loadDataWithBaseURL(LensWeb.BASE_URL, page, "text/html", "utf-8", null)
                }
            },
            onRelease = { view ->
                view.stopLoading()
                view.destroy()
                webView = null
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (uploadPage == null || progress < 100) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
            )
        }
    }
}
