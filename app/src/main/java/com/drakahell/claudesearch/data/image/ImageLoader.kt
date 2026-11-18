package com.drakahell.claudesearch.data.image

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.math.max

/**
 * Loads images as software bitmaps, downscaled so the longest side does not exceed [maxSide].
 * Downscaling keeps memory usage predictable for huge camera photos.
 */
class ImageLoader(
    private val contentResolver: ContentResolver,
    private val maxSide: Int = DEFAULT_MAX_SIDE,
) {

    suspend fun load(source: ImageSource): Bitmap = when (source) {
        ImageSource.Sample -> withContext(Dispatchers.Default) { SampleImage.create() }
        is ImageSource.Picked -> withContext(Dispatchers.IO) { decode(source.uri) }
    }

    private fun decode(uri: Uri): Bitmap =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val imageSource = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(imageSource) { decoder, info, _ ->
                // Software bitmaps can be cropped and read pixel by pixel; hardware ones cannot.
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.setTargetSampleSize(sampleSizeFor(info.size.width, info.size.height, maxSide))
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxSide)
            }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: throw IOException("Cannot decode image: $uri")
        }

    companion object {
        const val DEFAULT_MAX_SIDE = 2560

        /** Smallest power-of-two sample size that brings the longest side down to [maxSide]. */
        fun sampleSizeFor(width: Int, height: Int, maxSide: Int): Int {
            val longest = max(width, height)
            if (longest <= 0 || maxSide <= 0) return 1
            var sample = 1
            while (longest / sample > maxSide) sample *= 2
            return sample
        }
    }
}
