package com.ritesh.cashiro.presentation.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

/**
 * RemoteViews cannot tint shape drawables dynamically without losing their shape,
 * so circles (accent plus, bank dots) are drawn into bitmaps instead.
 */
object WidgetDrawer {

    fun plus(accentArgb: Int, sizePx: Int): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentArgb }
        canvas.drawOval(RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()), paint)

        // White plus, centered
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        val center = sizePx / 2f
        val arm = sizePx * 0.10f
        val len = sizePx * 0.44f
        val radius = arm / 2f
        // horizontal bar
        canvas.drawRoundRect(center - len / 2f, center - radius, center + len / 2f, center + radius, radius, radius, paint)
        // vertical bar
        canvas.drawRoundRect(center - radius, center - len / 2f, center + radius, center + len / 2f, radius, radius, paint)
        return bmp
    }

    fun dot(letter: String, bgArgb: Int, sizePx: Int): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgArgb }
        canvas.drawOval(RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()), paint)

        if (letter.isNotEmpty()) {
            paint.color = Color.WHITE
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = sizePx * 0.46f
            paint.isFakeBoldText = true
            val baseline = sizePx / 2f - (paint.ascent() + paint.descent()) / 2f
            canvas.drawText(letter.take(1), sizePx / 2f, baseline, paint)
        }
        return bmp
    }
}
