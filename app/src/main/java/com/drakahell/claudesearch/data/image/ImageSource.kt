package com.drakahell.claudesearch.data.image

import android.net.Uri

/** Where the image to trace on comes from. */
sealed interface ImageSource {
    /** Built-in generated picture for quick testing. */
    data object Sample : ImageSource

    /** Image picked by the user (e.g. via the Photo Picker). */
    data class Picked(val uri: Uri) : ImageSource
}
