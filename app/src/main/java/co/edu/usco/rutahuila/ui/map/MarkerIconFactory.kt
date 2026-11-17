package co.edu.usco.rutahuila.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable


object MarkerIconFactory {

    private const val SIZE_DP = 44
    private const val BORDER_DP = 3
    private const val EMOJI_SCALE = 0.5f

    fun create(context: Context, emoji: String, borderColor: Int): Drawable {
        val density = context.resources.displayMetrics.density
        val size = (SIZE_DP * density).toInt()
        val border = BORDER_DP * density
        val center = size / 2f
        val radius = center - border / 2f

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fondo blanco
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        canvas.drawCircle(center, center, radius, fillPaint)

        // Borde del color de la categoría
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = border
            color = borderColor
        }
        canvas.drawCircle(center, center, radius, borderPaint)

        // Emoji centrado
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size * EMOJI_SCALE
            textAlign = Paint.Align.CENTER
        }
        val textY = center - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(emoji, center, textY, textPaint)

        return BitmapDrawable(context.resources, bitmap)
    }
}
