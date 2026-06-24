package com.emoji.app.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.emoji.app.domain.cache.FontMetricsCache

/**
 * TextRenderer — renders text onto exported frames using the same
 * NativeTypeface mapping as the UI preview overlay, so exported GIFs
 * match what the user sees in the editor.
 */
class TextRenderer {

    private val metricsCache = FontMetricsCache()

    fun renderText(
        bitmap: Bitmap,
        text: String,
        fontName: String,
        textColor: Int,
        weight: String,
        fontSizePx: Int,
        textX: Float,
        textY: Float,
        baseCanvasWidth: Int = AppConfig.BASE_CANVAS_WIDTH_PX,
    ) {
        if (text.isEmpty()) return

        val canvas = Canvas(bitmap)
        val exportSize = fontSizePx * (bitmap.width.toFloat() / baseCanvasWidth)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = exportSize
            this.color = textColor
            this.textAlign = Paint.Align.CENTER
            this.isFakeBoldText = NativeTypeface.isBold(fontName)
            this.typeface = NativeTypeface.forLabel(fontName)
            setShadowLayer(4f, 2f, 2f, 0x77000000.toInt())
        }

        val x = bitmap.width * textX
        val y = bitmap.height * textY + exportSize * 0.35f

        canvas.drawText(text, x, y, paint)
        paint.clearShadowLayer()
        canvas.drawText(text, x, y, paint)
    }

    fun measureText(
        text: String,
        fontName: String,
        fontSizePx: Float,
        weight: String,
    ): Pair<Float, Float> {
        val cached = metricsCache.get(text, fontName, fontSizePx)
        if (cached != null) return cached

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.textSize = fontSizePx }
        val bounds = android.graphics.Rect()
        paint.getTextBounds(text, 0, text.length, bounds)

        val result = Pair(bounds.width().toFloat(), bounds.height().toFloat())
        metricsCache.put(text, fontName, fontSizePx, result.first, result.second)
        return result
    }

    fun clearMetricsCache() { metricsCache.clear() }
}
