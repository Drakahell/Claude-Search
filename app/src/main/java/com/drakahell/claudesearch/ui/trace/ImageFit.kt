package com.drakahell.claudesearch.ui.trace

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import kotlin.math.min

/**
 * Describes how an image of [imageSize] is placed inside a container ("fit" + centered),
 * and converts coordinates between the screen (view) and the image pixels.
 */
data class ImageFit(
    val scale: Float,
    val offset: Offset,
    val imageSize: IntSize,
) {
    /** Area of the container actually covered by the image. */
    val displayedRect: Rect
        get() = Rect(offset, Size(imageSize.width * scale, imageSize.height * scale))

    fun viewToImage(point: Offset): Offset =
        Offset((point.x - offset.x) / scale, (point.y - offset.y) / scale)

    fun viewToImage(rect: Rect): Rect =
        Rect(viewToImage(rect.topLeft), viewToImage(rect.bottomRight))

    fun imageToView(rect: Rect): Rect = Rect(
        left = rect.left * scale + offset.x,
        top = rect.top * scale + offset.y,
        right = rect.right * scale + offset.x,
        bottom = rect.bottom * scale + offset.y,
    )

    companion object {
        fun fit(container: Size, imageSize: IntSize): ImageFit {
            require(imageSize.width > 0 && imageSize.height > 0) { "Image must not be empty" }
            val scale = min(container.width / imageSize.width, container.height / imageSize.height)
            val offset = Offset(
                x = (container.width - imageSize.width * scale) / 2f,
                y = (container.height - imageSize.height * scale) / 2f,
            )
            return ImageFit(scale, offset, imageSize)
        }
    }
}
