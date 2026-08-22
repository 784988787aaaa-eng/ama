package com.example.data.serialization.pdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.ByteArrayOutputStream
import java.io.File

data class LogoResult(
    val bitmap: Bitmap?,
    val rawBitmapToRecycle: Bitmap?,
    val width: Float,
    val height: Float,
    val hasLogo: Boolean
)

object PdfDrawingUtils {

    private val EMOJI_CLEANER_REGEX = Regex(
        "[\uD83C-\uD83E][\uDC00-\uDFFF]" +
        "|[\u2600-\u27BF]" +
        "|[\u2300-\u23FF]" +
        "|[\u2B50-\u2B55]" +
        "|[\u200D]" +
        "|[\uFE0F]"
    )

    fun sanitizePdfText(text: CharSequence): CharSequence {
        if (text.isEmpty()) return text
        return if (EMOJI_CLEANER_REGEX.containsMatchIn(text)) {
            text.replace(EMOJI_CLEANER_REGEX, "")
        } else {
            text
        }
    }

    fun createStaticLayout(
        text: CharSequence,
        paint: Paint,
        width: Int,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): StaticLayout {
        val sanitized = sanitizePdfText(text)
        val textPaint = if (paint is TextPaint) paint else TextPaint(paint)
        val safeWidth = width.coerceAtLeast(1)
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(sanitized, 0, sanitized.length, textPaint, safeWidth)
                .setAlignment(alignment)
                .setLineSpacing(0f, 1.05f)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(sanitized, textPaint, safeWidth, alignment, 1.05f, 0f, false)
        }
    }

    fun measureTextHeight(
        text: CharSequence,
        paint: Paint,
        width: Int,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): Int {
        val layout = createStaticLayout(text, paint, width, alignment)
        return layout.height
    }

    fun drawStaticLayout(
        canvas: Canvas,
        layout: StaticLayout,
        x: Float,
        y: Float
    ) {
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    fun drawArabicText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Int,
        paint: Paint,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): Int {
        val layout = createStaticLayout(text, paint, width, alignment)
        drawStaticLayout(canvas, layout, x, y)
        return layout.height
    }

    fun loadAndScaleLogo(context: android.content.Context, logoPath: String, maxW: Float = 70f, maxH: Float = 55f): LogoResult {
        var rawBitmap: Bitmap? = null
        var scaledLogo: Bitmap? = null
        var logoW = 0f
        var logoH = 0f
        var hasLogo = false
        try {
            if (logoPath.isNotEmpty()) {
                val logoFile = File(logoPath)
                if (logoFile.exists()) {
                    if (logoFile.length() > 1024 * 1024) {
                        val original = BitmapFactory.decodeFile(logoFile.absolutePath)
                        if (original != null) {
                            val stream = ByteArrayOutputStream()
                            original.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                            val byteArray = stream.toByteArray()
                            rawBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                            original.recycle()
                        }
                    } else {
                        rawBitmap = BitmapFactory.decodeFile(logoFile.absolutePath)
                    }
                }
            }

            // Fallback to app icon if no custom logo was loaded
            if (rawBitmap == null) {
                rawBitmap = BitmapFactory.decodeResource(context.resources, com.example.R.drawable.img_app_icon)
            }

            if (rawBitmap != null) {
                val originalWidth = rawBitmap.width.toFloat()
                val originalHeight = rawBitmap.height.toFloat()
                val scale = (maxW / originalWidth).coerceAtMost(maxH / originalHeight)
                val finalW = (originalWidth * scale).coerceAtLeast(1f)
                val finalH = (originalHeight * scale).coerceAtLeast(1f)

                scaledLogo = Bitmap.createScaledBitmap(rawBitmap, finalW.toInt(), finalH.toInt(), true)

                logoW = finalW
                logoH = finalH
                hasLogo = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return LogoResult(scaledLogo, rawBitmap, logoW, logoH, hasLogo)
    }

    fun loadAndScaleLogo(logoPath: String, maxW: Float = 70f, maxH: Float = 55f): LogoResult {
        if (logoPath.isEmpty()) {
            return LogoResult(null, null, 0f, 0f, false)
        }
        var rawBitmap: Bitmap? = null
        var scaledLogo: Bitmap? = null
        var logoW = 0f
        var logoH = 0f
        var hasLogo = false
        try {
            val logoFile = File(logoPath)
            if (logoFile.exists()) {
                if (logoFile.length() > 1024 * 1024) {
                    val original = BitmapFactory.decodeFile(logoFile.absolutePath)
                    if (original != null) {
                        val stream = ByteArrayOutputStream()
                        original.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                        val byteArray = stream.toByteArray()
                        rawBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                        original.recycle()
                    }
                } else {
                    rawBitmap = BitmapFactory.decodeFile(logoFile.absolutePath)
                }

                if (rawBitmap != null) {
                    val originalWidth = rawBitmap.width.toFloat()
                    val originalHeight = rawBitmap.height.toFloat()
                    val scale = (maxW / originalWidth).coerceAtMost(maxH / originalHeight)
                    val finalW = (originalWidth * scale).coerceAtLeast(1f)
                    val finalH = (originalHeight * scale).coerceAtLeast(1f)

                    scaledLogo = Bitmap.createScaledBitmap(rawBitmap, finalW.toInt(), finalH.toInt(), true)

                    logoW = finalW
                    logoH = finalH
                    hasLogo = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return LogoResult(scaledLogo, rawBitmap, logoW, logoH, hasLogo)
    }
}
