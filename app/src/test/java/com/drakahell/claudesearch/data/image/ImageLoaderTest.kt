package com.drakahell.claudesearch.data.image

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageLoaderTest {

    @Test
    fun sampleSize_keepsSmallImagesAsIs() {
        assertEquals(1, ImageLoader.sampleSizeFor(1080, 2400, 2560))
    }

    @Test
    fun sampleSize_halvesUntilFits() {
        assertEquals(2, ImageLoader.sampleSizeFor(4000, 3000, 2560))
        assertEquals(4, ImageLoader.sampleSizeFor(8000, 6000, 2560))
    }

    @Test
    fun sampleSize_handlesUnknownSize() {
        assertEquals(1, ImageLoader.sampleSizeFor(-1, -1, 2560))
    }
}
