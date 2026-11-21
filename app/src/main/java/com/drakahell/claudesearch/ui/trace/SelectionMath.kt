package com.drakahell.claudesearch.ui.trace

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil
import kotlin.math.floor

/** Pure geometry helpers that turn a finger stroke into a selection rectangle. */
object SelectionMath {

    /** Selections smaller than this (in image pixels) are ignored as accidental. */
    const val MIN_SELECTION_SIDE_PX = 16f

    /** Bounding box of the stroke, or null if there are no points. */
    fun bounds(points: List<Offset>): Rect? {
        if (points.isEmpty()) return null
        var left = Float.POSITIVE_INFINITY
        var top = Float.POSITIVE_INFINITY
        var right = Float.NEGATIVE_INFINITY
        var bottom = Float.NEGATIVE_INFINITY
        for (p in points) {
            if (p.x < left) left = p.x
            if (p.y < top) top = p.y
            if (p.x > right) right = p.x
            if (p.y > bottom) bottom = p.y
        }
        return Rect(left, top, right, bottom)
    }

    /** A stroke that barely moved is treated as a tap. */
    fun isTap(points: List<Offset>, touchSlopPx: Float): Boolean {
        val b = bounds(points) ?: return true
        return b.width < touchSlopPx && b.height < touchSlopPx
    }

    /**
     * Converts a stroke in view coordinates into a selection in image coordinates.
     *
     * - Tap: a square of [tapSizePx] around the touch point.
     * - Trace: the stroke bounding box expanded by [paddingPx].
     *
     * The result is clipped to the image. Returns null if the selection is too small
     * or lies completely outside the image.
     */
    fun selectionFromStroke(
        points: List<Offset>,
        fit: ImageFit,
        touchSlopPx: Float,
        tapSizePx: Float,
        paddingPx: Float,
    ): Rect? {
        val strokeBounds = bounds(points) ?: return null
        val viewRect = if (isTap(points, touchSlopPx)) {
            Rect(center = strokeBounds.center, radius = tapSizePx / 2f)
        } else {
            strokeBounds.inflate(paddingPx)
        }
        val imageBounds = Rect(
            0f, 0f, fit.imageSize.width.toFloat(), fit.imageSize.height.toFloat(),
        )
        val clipped = fit.viewToImage(viewRect).intersect(imageBounds)
        if (clipped.width < MIN_SELECTION_SIDE_PX || clipped.height < MIN_SELECTION_SIDE_PX) {
            return null
        }
        return clipped
    }

    /**
     * How far (negative = up) to move the image so that [selectionInView] is not hidden
     * behind a bottom sheet of height [bottomInset]. The selection is never pushed above
     * [topReserved] (space for the close button).
     */
    fun verticalShiftToReveal(
        selectionInView: Rect,
        containerHeight: Float,
        bottomInset: Float,
        topReserved: Float,
    ): Float {
        val visibleBottom = containerHeight - bottomInset
        val overlap = selectionInView.bottom - visibleBottom
        if (overlap <= 0f) return 0f
        val maxUp = (selectionInView.top - topReserved).coerceAtLeast(0f)
        return -minOf(overlap, maxUp)
    }

    /** Rounds a selection outwards to whole pixels that are guaranteed to fit in [imageSize]. */
    fun toPixelRect(rect: Rect, imageSize: IntSize): IntRect {
        val left = floor(rect.left).toInt().coerceIn(0, imageSize.width - 1)
        val top = floor(rect.top).toInt().coerceIn(0, imageSize.height - 1)
        val right = ceil(rect.right).toInt().coerceIn(left + 1, imageSize.width)
        val bottom = ceil(rect.bottom).toInt().coerceIn(top + 1, imageSize.height)
        return IntRect(left, top, right, bottom)
    }
}
