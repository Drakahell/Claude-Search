package com.drakahell.claudesearch.ui.trace

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageFitTest {

    @Test
    fun wideImage_isLetterboxedVertically() {
        val fit = ImageFit.fit(Size(1000f, 2000f), IntSize(2000, 1000))
        assertEquals(0.5f, fit.scale)
        assertEquals(Offset(0f, 750f), fit.offset)
        assertEquals(Rect(0f, 750f, 1000f, 1250f), fit.displayedRect)
    }

    @Test
    fun viewAndImageCoordinates_roundTrip() {
        val fit = ImageFit.fit(Size(1080f, 2400f), IntSize(3000, 4000))
        val imageRect = Rect(300f, 400f, 1500f, 2000f)
        val back = fit.viewToImage(fit.imageToView(imageRect))
        assertEquals(imageRect.left, back.left, 0.01f)
        assertEquals(imageRect.top, back.top, 0.01f)
        assertEquals(imageRect.right, back.right, 0.01f)
        assertEquals(imageRect.bottom, back.bottom, 0.01f)
    }
}
