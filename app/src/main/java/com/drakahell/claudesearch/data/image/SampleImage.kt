package com.drakahell.claudesearch.data.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface

/**
 * Draws a simple test picture (landscape with a house, a tree and a text card),
 * so the trace screen can be tried without any photos on the device.
 */
object SampleImage {

    /** Default size matches a typical phone screen, like a real screenshot would. */
    fun create(width: Int = 1080, height: Int = 2400): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = width.toFloat()
        val h = height.toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Sky
        paint.shader = LinearGradient(
            0f, 0f, 0f, h * 0.55f,
            0xFF4FC3F7.toInt(), 0xFFFFF3E0.toInt(), Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h * 0.55f, paint)
        paint.shader = null

        // Sun
        paint.color = 0xFFFFD54F.toInt()
        canvas.drawCircle(w * 0.78f, h * 0.12f, w * 0.09f, paint)

        // Mountains
        paint.color = 0xFF90A4AE.toInt()
        canvas.drawPath(triangle(0f, h * 0.55f, w * 0.25f, h * 0.33f, w * 0.5f, h * 0.55f), paint)
        paint.color = 0xFF78909C.toInt()
        canvas.drawPath(triangle(w * 0.35f, h * 0.55f, w * 0.7f, h * 0.28f, w * 1.05f, h * 0.55f), paint)

        // Ground
        paint.color = 0xFF7CB342.toInt()
        canvas.drawRect(0f, h * 0.55f, w, h, paint)

        // House
        paint.color = 0xFFFFCC80.toInt()
        canvas.drawRect(w * 0.12f, h * 0.47f, w * 0.45f, h * 0.64f, paint)
        paint.color = 0xFFD84315.toInt()
        canvas.drawPath(triangle(w * 0.08f, h * 0.47f, w * 0.285f, h * 0.39f, w * 0.49f, h * 0.47f), paint)
        paint.color = 0xFF6D4C41.toInt()
        canvas.drawRect(w * 0.25f, h * 0.55f, w * 0.32f, h * 0.64f, paint)
        paint.color = 0xFF81D4FA.toInt()
        canvas.drawRect(w * 0.15f, h * 0.50f, w * 0.22f, h * 0.54f, paint)
        canvas.drawRect(w * 0.35f, h * 0.50f, w * 0.42f, h * 0.54f, paint)

        // Tree
        paint.color = 0xFF5D4037.toInt()
        canvas.drawRect(w * 0.68f, h * 0.52f, w * 0.72f, h * 0.64f, paint)
        paint.color = 0xFF2E7D32.toInt()
        canvas.drawCircle(w * 0.70f, h * 0.49f, w * 0.11f, paint)

        // Text card
        paint.color = 0xFFFFFFFF.toInt()
        canvas.drawRoundRect(RectF(w * 0.08f, h * 0.70f, w * 0.92f, h * 0.92f), 40f, 40f, paint)

        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF212121.toInt()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = w * 0.075f
        }
        canvas.drawText("Claude Search", w * 0.13f, h * 0.775f, text)
        text.typeface = Typeface.DEFAULT
        text.textSize = w * 0.05f
        canvas.drawText("Обведите этот текст", w * 0.13f, h * 0.835f, text)
        text.color = 0xFF757575.toInt()
        canvas.drawText("или дом, дерево, солнце", w * 0.13f, h * 0.885f, text)

        return bitmap
    }

    private fun triangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) =
        Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
            lineTo(x3, y3)
            close()
        }
}
