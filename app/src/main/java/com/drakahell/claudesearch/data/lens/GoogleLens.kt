package com.drakahell.claudesearch.data.lens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Hands an image over to Google Lens (part of the Google app).
 *
 * There is no public Lens API, so we share the image with the Google app's Lens
 * entry point. If it's missing, we try the Google app in general, then a regular
 * share sheet.
 */
object GoogleLens {

    private const val GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox"
    private const val LENS_SHARE_ACTIVITY =
        "com.google.android.apps.search.lens.LensShareEntryPointActivity"

    /** Result of [open]. */
    enum class Result { OPENED_LENS, OPENED_CHOOSER, FAILED }

    suspend fun open(context: Context, image: Bitmap): Result {
        val uri = saveForSharing(context, image)
        val base = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .apply { clipData = ClipData.newRawUri(null, uri) }

        val attempts = listOf(
            Intent(base).setClassName(GOOGLE_APP_PACKAGE, LENS_SHARE_ACTIVITY),
            Intent(base).setPackage(GOOGLE_APP_PACKAGE),
        )
        for (intent in attempts) {
            if (tryStart(context, intent)) return Result.OPENED_LENS
        }
        return if (tryStart(context, Intent.createChooser(base, null))) {
            Result.OPENED_CHOOSER
        } else {
            Result.FAILED
        }
    }

    private fun tryStart(context: Context, intent: Intent): Boolean = try {
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        // The Lens activity may be non-exported on some Google app versions.
        false
    }

    private suspend fun saveForSharing(context: Context, image: Bitmap): Uri =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
            // A single file is reused so the cache never grows.
            val file = File(dir, "selection.png")
            file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
}
