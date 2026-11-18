package com.drakahell.claudesearch.ui.trace

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionMathTest {

    // 1000x2000 image shown 1:1 in a container of the same size.
    private val identityFit = ImageFit.fit(Size(1000f, 2000f), IntSize(1000, 2000))

    @Test
    fun bounds_ofEmptyStroke_isNull() {
        assertNull(SelectionMath.bounds(emptyList()))
    }

    @Test
    fun bounds_coversAllPoints() {
        val b = SelectionMath.bounds(listOf(Offset(10f, 50f), Offset(40f, 20f), Offset(25f, 80f)))
        assertEquals(Rect(10f, 20f, 40f, 80f), b)
    }

    @Test
    fun isTap_whenStrokeSmallerThanSlop() {
        assertTrue(SelectionMath.isTap(listOf(Offset(100f, 100f), Offset(103f, 102f)), 8f))
        assertFalse(SelectionMath.isTap(listOf(Offset(100f, 100f), Offset(150f, 102f)), 8f))
    }

    @Test
    fun trace_givesPaddedBoundingBox() {
        val points = listOf(Offset(100f, 100f), Offset(300f, 150f), Offset(200f, 400f))
        val rect = SelectionMath.selectionFromStroke(points, identityFit, 8f, 140f, 10f)
        assertEquals(Rect(90f, 90f, 310f, 410f), rect)
    }

    @Test
    fun tap_givesSquareAroundPoint() {
        val rect = SelectionMath.selectionFromStroke(listOf(Offset(500f, 500f)), identityFit, 8f, 140f, 10f)
        assertEquals(Rect(430f, 430f, 570f, 570f), rect)
    }

    @Test
    fun selection_isClippedToImage() {
        val rect = SelectionMath.selectionFromStroke(listOf(Offset(0f, 0f)), identityFit, 8f, 140f, 10f)
        assertEquals(Rect(0f, 0f, 70f, 70f), rect)
    }

    @Test
    fun selection_outsideImage_isNull() {
        // Image 1000x1000 letterboxed in a 1000x2000 container: it occupies y = 500..1500.
        val fit = ImageFit.fit(Size(1000f, 2000f), IntSize(1000, 1000))
        val points = listOf(Offset(100f, 50f), Offset(400f, 300f))
        assertNull(SelectionMath.selectionFromStroke(points, fit, 8f, 140f, 10f))
    }

    @Test
    fun selection_isConvertedToImageCoordinates() {
        // 2000x2000 image in a 1000x1000 container: scale 0.5, no offset.
        val fit = ImageFit.fit(Size(1000f, 1000f), IntSize(2000, 2000))
        val points = listOf(Offset(100f, 100f), Offset(200f, 300f))
        val rect = SelectionMath.selectionFromStroke(points, fit, 8f, 140f, 0f)
        assertEquals(Rect(200f, 200f, 400f, 600f), rect)
    }

    @Test
    fun toPixelRect_roundsOutwardsAndClamps() {
        val px = SelectionMath.toPixelRect(Rect(10.4f, 20.6f, 99.2f, 2500f), IntSize(1000, 2000))
        assertEquals(IntRect(10, 20, 100, 2000), px)
    }
}
