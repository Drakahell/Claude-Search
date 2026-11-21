package com.drakahell.claudesearch.data.lens

import android.graphics.Bitmap
import android.util.Base64
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Builds a tiny HTML page that uploads an image to the Google Lens website and
 * navigates to the results. It is loaded into a WebView inside our bottom sheet,
 * so Lens results appear in-app instead of switching to the Google app.
 *
 * There is no public Lens API: this relies on the website's upload form
 * (field `encoded_image`). If Google changes it, the "open in Google app"
 * action in the panel remains as a fallback.
 */
object LensWeb {

    const val BASE_URL = "https://lens.google.com/"
    private const val UPLOAD_URL = "https://lens.google.com/v3/upload"
    private const val MAX_SIDE = 1000

    fun uploadPage(image: Bitmap, languageTag: String, nowMillis: Long): String {
        val base64 = Base64.encodeToString(image.toJpeg(), Base64.NO_WRAP)
        val action = "$UPLOAD_URL?hl=$languageTag&re=df&ep=gsbubu&st=$nowMillis"
        return """
            <!DOCTYPE html>
            <html><head><meta name="viewport" content="width=device-width, initial-scale=1"></head>
            <body style="margin:0">
            <form id="f" method="POST" enctype="multipart/form-data" action="$action">
              <input type="file" name="encoded_image" id="i" style="display:none">
            </form>
            <script>
              const raw = atob("$base64");
              const bytes = new Uint8Array(raw.length);
              for (let k = 0; k < raw.length; k++) bytes[k] = raw.charCodeAt(k);
              const transfer = new DataTransfer();
              transfer.items.add(new File([bytes], "selection.jpg", { type: "image/jpeg" }));
              document.getElementById("i").files = transfer.files;
              document.getElementById("f").submit();
            </script>
            </body></html>
        """.trimIndent()
    }

    /** Links to Google pages stay in the panel; anything else opens in the browser. */
    fun isGoogleHost(host: String?): Boolean {
        val h = host?.lowercase() ?: return false
        return GOOGLE_HOST.matches(h) || h.endsWith("gstatic.com") || h.endsWith("googleusercontent.com")
    }

    private val GOOGLE_HOST = Regex("""^(.+\.)?google\.[a-z]{2,3}(\.[a-z]{2})?$""")

    private fun Bitmap.toJpeg(): ByteArray {
        val longest = max(width, height)
        val scaled = if (longest > MAX_SIDE) {
            val f = MAX_SIDE.toFloat() / longest
            scale((width * f).toInt().coerceAtLeast(1), (height * f).toInt().coerceAtLeast(1))
        } else {
            this
        }
        return ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
            out.toByteArray()
        }
    }
}
