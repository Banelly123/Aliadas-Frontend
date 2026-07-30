package com.aliadas.profile

import android.graphics.*
import android.graphics.drawable.Drawable

class GeometricAvatarDrawable(
    var animalType: String,
    var backgroundColor: Int
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    // Color Palette for animals
    private val colWhite = Color.WHITE
    private val colBlack = Color.parseColor("#2D2D2D")
    private val colPink = Color.parseColor("#FFD1DC")
    private val colOrange = Color.parseColor("#FF9800")
    private val colCream = Color.parseColor("#FFFDD0")
    private val colGrey = Color.parseColor("#9E9E9E")
    private val colBlue = Color.parseColor("#64B5F6")
    private val colYellow = Color.parseColor("#FFF176")

    override fun draw(canvas: Canvas) {
        val b = bounds
        val size = b.width().toFloat()
        val centerX = b.centerX().toFloat()
        val centerY = b.centerY().toFloat()

        if (size <= 0) return

        // Draw background circle
        paint.color = backgroundColor
        paint.style = Paint.Style.FILL
        canvas.drawCircle(centerX, centerY, size / 2, paint)

        // Limpiar el nombre por si viene con "avatar_"
        val type = if (animalType.startsWith("avatar_")) animalType.substring(7) else animalType

        when (type) {
            "cat" -> drawCat(canvas, centerX, centerY, size)
            "bird" -> drawBird(canvas, centerX, centerY, size)
            "butterfly" -> drawButterfly(canvas, centerX, centerY, size)
            "fox" -> drawFox(canvas, centerX, centerY, size)
            "panda" -> drawPanda(canvas, centerX, centerY, size)
            "rabbit" -> drawRabbit(canvas, centerX, centerY, size)
            else -> drawCat(canvas, centerX, centerY, size)
        }
    }

    private fun drawCat(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val s = size * 0.5f
        // Ears
        paint.color = colGrey
        path.reset()
        path.moveTo(cx - s * 0.45f, cy - s * 0.1f)
        path.lineTo(cx - s * 0.55f, cy - s * 0.65f)
        path.lineTo(cx - s * 0.15f, cy - s * 0.35f)
        path.close()
        path.moveTo(cx + s * 0.45f, cy - s * 0.1f)
        path.lineTo(cx + s * 0.55f, cy - s * 0.65f)
        path.lineTo(cx + s * 0.15f, cy - s * 0.35f)
        path.close()
        canvas.drawPath(path, paint)

        // Inner ears
        paint.color = colPink
        path.reset()
        path.moveTo(cx - s * 0.38f, cy - s * 0.15f)
        path.lineTo(cx - s * 0.45f, cy - s * 0.45f)
        path.lineTo(cx - s * 0.22f, cy - s * 0.28f)
        path.close()
        path.moveTo(cx + s * 0.38f, cy - s * 0.15f)
        path.lineTo(cx + s * 0.45f, cy - s * 0.45f)
        path.lineTo(cx + s * 0.22f, cy - s * 0.28f)
        path.close()
        canvas.drawPath(path, paint)

        // Head
        paint.color = colGrey
        canvas.drawCircle(cx, cy + s * 0.1f, s * 0.5f, paint)

        // Eyes
        paint.color = colBlack
        canvas.drawCircle(cx - s * 0.18f, cy + s * 0.05f, s * 0.06f, paint)
        canvas.drawCircle(cx + s * 0.18f, cy + s * 0.05f, s * 0.06f, paint)
        
        // Nose
        paint.color = colPink
        canvas.drawCircle(cx, cy + s * 0.18f, s * 0.04f, paint)

        // Whiskers
        paint.color = colWhite
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.02f
        canvas.drawLine(cx - s * 0.3f, cy + s * 0.15f, cx - s * 0.55f, cy + s * 0.12f, paint)
        canvas.drawLine(cx - s * 0.3f, cy + s * 0.22f, cx - s * 0.55f, cy + s * 0.25f, paint)
        canvas.drawLine(cx + s * 0.3f, cy + s * 0.15f, cx + s * 0.55f, cy + s * 0.12f, paint)
        canvas.drawLine(cx + s * 0.3f, cy + s * 0.22f, cx + s * 0.55f, cy + s * 0.25f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawBird(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val s = size * 0.5f
        // Tail
        paint.color = colBlue
        path.reset()
        path.moveTo(cx - s * 0.4f, cy + s * 0.2f)
        path.lineTo(cx - s * 0.7f, cy + s * 0.1f)
        path.lineTo(cx - s * 0.7f, cy + s * 0.4f)
        path.close()
        canvas.drawPath(path, paint)

        // Body
        paint.color = colBlue
        canvas.drawCircle(cx - s * 0.1f, cy + s * 0.15f, s * 0.45f, paint)
        
        // Chest detail (lighter blue)
        paint.color = Color.parseColor("#90CAF9")
        canvas.drawArc(RectF(cx - s * 0.55f, cy - s * 0.3f, cx + s * 0.35f, cy + s * 0.6f), 90f, 180f, true, paint)

        // Head
        paint.color = colBlue
        canvas.drawCircle(cx + s * 0.15f, cy - s * 0.15f, s * 0.32f, paint)
        
        // Beak
        paint.color = colOrange
        path.reset()
        path.moveTo(cx + s * 0.38f, cy - s * 0.2f)
        path.lineTo(cx + s * 0.75f, cy - s * 0.1f)
        path.lineTo(cx + s * 0.42f, cy)
        path.close()
        canvas.drawPath(path, paint)
        
        // Eye
        paint.color = colBlack
        canvas.drawCircle(cx + s * 0.25f, cy - s * 0.22f, s * 0.05f, paint)
    }

    private fun drawButterfly(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val s = size * 0.5f
        
        // Wings base
        paint.color = Color.parseColor("#BA68C8") // Purple
        val rectLT = RectF(cx - s * 0.7f, cy - s * 0.6f, cx - s * 0.05f, cy)
        val rectLB = RectF(cx - s * 0.6f, cy, cx - s * 0.05f, cy + s * 0.5f)
        val rectRT = RectF(cx + s * 0.05f, cy - s * 0.6f, cx + s * 0.7f, cy)
        val rectRB = RectF(cx + s * 0.05f, cy, cx + s * 0.6f, cy + s * 0.5f)
        
        canvas.drawRoundRect(rectLT, s * 0.35f, s * 0.35f, paint)
        canvas.drawRoundRect(rectLB, s * 0.25f, s * 0.25f, paint)
        canvas.drawRoundRect(rectRT, s * 0.35f, s * 0.35f, paint)
        canvas.drawRoundRect(rectRB, s * 0.25f, s * 0.25f, paint)
        
        // Inner Wing patterns
        paint.color = colPink
        canvas.drawCircle(cx - s * 0.4f, cy - s * 0.3f, s * 0.15f, paint)
        canvas.drawCircle(cx + s * 0.4f, cy - s * 0.3f, s * 0.15f, paint)
        paint.color = colYellow
        canvas.drawCircle(cx - s * 0.35f, cy + s * 0.25f, s * 0.1f, paint)
        canvas.drawCircle(cx + s * 0.35f, cy + s * 0.25f, s * 0.1f, paint)

        // Body
        paint.color = colBlack
        canvas.drawRoundRect(RectF(cx - s * 0.08f, cy - s * 0.65f, cx + s * 0.08f, cy + s * 0.6f), s * 0.08f, s * 0.08f, paint)
        
        // Antennae
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.03f
        path.reset()
        path.moveTo(cx - s * 0.05f, cy - s * 0.5f)
        path.quadTo(cx - s * 0.15f, cy - s * 0.7f, cx - s * 0.3f, cy - s * 0.75f)
        path.moveTo(cx + s * 0.05f, cy - s * 0.5f)
        path.quadTo(cx + s * 0.15f, cy - s * 0.7f, cx + s * 0.3f, cy - s * 0.75f)
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawFox(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val s = size * 0.5f
        // Ears
        paint.color = colOrange
        path.reset()
        path.moveTo(cx - s * 0.55f, cy - s * 0.2f)
        path.lineTo(cx - s * 0.6f, cy - s * 0.8f)
        path.lineTo(cx - s * 0.2f, cy - s * 0.2f)
        path.close()
        path.moveTo(cx + s * 0.55f, cy - s * 0.2f)
        path.lineTo(cx + s * 0.6f, cy - s * 0.8f)
        path.lineTo(cx + s * 0.2f, cy - s * 0.2f)
        path.close()
        canvas.drawPath(path, paint)

        // Black Ear tips
        paint.color = colBlack
        path.reset()
        path.moveTo(cx - s * 0.57f, cy - s * 0.6f)
        path.lineTo(cx - s * 0.6f, cy - s * 0.8f)
        path.lineTo(cx - s * 0.45f, cy - s * 0.55f)
        path.close()
        path.moveTo(cx + s * 0.57f, cy - s * 0.6f)
        path.lineTo(cx + s * 0.6f, cy - s * 0.8f)
        path.lineTo(cx + s * 0.45f, cy - s * 0.55f)
        path.close()
        canvas.drawPath(path, paint)

        // Face Main
        paint.color = colOrange
        path.reset()
        path.moveTo(cx, cy + s * 0.7f)
        path.lineTo(cx - s * 0.65f, cy - s * 0.3f)
        path.lineTo(cx + s * 0.65f, cy - s * 0.3f)
        path.close()
        canvas.drawPath(path, paint)
        
        // White muzzle area
        paint.color = colWhite
        path.reset()
        path.moveTo(cx, cy + s * 0.7f)
        path.lineTo(cx - s * 0.35f, cy + s * 0.1f)
        path.quadTo(cx, cy + s * 0.2f, cx + s * 0.35f, cy + s * 0.1f)
        path.close()
        canvas.drawPath(path, paint)
        
        // Eyes
        paint.color = colBlack
        canvas.drawCircle(cx - s * 0.22f, cy, s * 0.05f, paint)
        canvas.drawCircle(cx + s * 0.22f, cy, s * 0.05f, paint)
        
        // Nose
        canvas.drawCircle(cx, cy + s * 0.6f, s * 0.08f, paint)
    }

    private fun drawPanda(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val s = size * 0.5f
        // Ears
        paint.color = colBlack
        canvas.drawCircle(cx - s * 0.45f, cy - s * 0.45f, s * 0.2f, paint)
        canvas.drawCircle(cx + s * 0.45f, cy - s * 0.45f, s * 0.2f, paint)
        
        // Head
        paint.color = colWhite
        canvas.drawCircle(cx, cy + s * 0.1f, s * 0.6f, paint)
        
        // Eye patches
        paint.color = colBlack
        canvas.save()
        canvas.rotate(-20f, cx - s * 0.25f, cy + s * 0.05f)
        canvas.drawOval(RectF(cx - s * 0.45f, cy - s * 0.1f, cx - s * 0.05f, cy + s * 0.25f), paint)
        canvas.restore()
        canvas.save()
        canvas.rotate(20f, cx + s * 0.25f, cy + s * 0.05f)
        canvas.drawOval(RectF(cx + s * 0.05f, cy - s * 0.1f, cx + s * 0.45f, cy + s * 0.25f), paint)
        canvas.restore()
        
        // Nose
        canvas.drawCircle(cx, cy + s * 0.3f, s * 0.08f, paint)
        
        // Eyes
        paint.color = colWhite
        canvas.drawCircle(cx - s * 0.25f, cy + s * 0.05f, s * 0.04f, paint)
        canvas.drawCircle(cx + s * 0.25f, cy + s * 0.05f, s * 0.04f, paint)
        
        // Cheeks
        paint.color = colPink
        paint.alpha = 150
        canvas.drawCircle(cx - s * 0.4f, cy + s * 0.3f, s * 0.1f, paint)
        canvas.drawCircle(cx + s * 0.4f, cy + s * 0.3f, s * 0.1f, paint)
        paint.alpha = 255
    }

    private fun drawRabbit(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val s = size * 0.5f
        // Ears
        paint.color = colCream
        canvas.drawRoundRect(RectF(cx - s * 0.38f, cy - s * 0.85f, cx - s * 0.08f, cy), s * 0.2f, s * 0.2f, paint)
        canvas.drawRoundRect(RectF(cx + s * 0.08f, cy - s * 0.85f, cx + s * 0.38f, cy), s * 0.2f, s * 0.2f, paint)
        
        // Inner ears
        paint.color = colPink
        canvas.drawRoundRect(RectF(cx - s * 0.3f, cy - s * 0.75f, cx - s * 0.16f, cy - s * 0.1f), s * 0.1f, s * 0.1f, paint)
        canvas.drawRoundRect(RectF(cx + s * 0.16f, cy - s * 0.75f, cx + s * 0.3f, cy - s * 0.1f), s * 0.1f, s * 0.1f, paint)

        // Head
        paint.color = colCream
        canvas.drawCircle(cx, cy + s * 0.15f, s * 0.55f, paint)
        
        // Cheeks
        paint.color = colPink
        paint.alpha = 120
        canvas.drawCircle(cx - s * 0.3f, cy + s * 0.35f, s * 0.12f, paint)
        canvas.drawCircle(cx + s * 0.3f, cy + s * 0.35f, s * 0.12f, paint)
        paint.alpha = 255

        // Eyes
        paint.color = colBlack
        canvas.drawCircle(cx - s * 0.2f, cy + s * 0.15f, s * 0.06f, paint)
        canvas.drawCircle(cx + s * 0.2f, cy + s * 0.15f, s * 0.06f, paint)
        
        // Nose
        paint.color = colPink
        canvas.drawCircle(cx, cy + s * 0.28f, s * 0.05f, paint)
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        fun getDefaultColorFor(rawType: String): String {
            val type = if (rawType.startsWith("avatar_")) rawType.substring(7) else rawType
            return when (type.lowercase()) {
                "cat" -> "#FF80AB"
                "bird" -> "#80D8FF"
                "butterfly" -> "#E1BEE7"
                "fox" -> "#FFAB91"
                "panda" -> "#B2DFDB"
                "rabbit" -> "#FFF59D"
                else -> "#FF80AB"
            }
        }
    }
}
