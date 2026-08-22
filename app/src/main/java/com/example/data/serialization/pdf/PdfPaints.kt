package com.example.data.serialization.pdf

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint

object PdfPaints {
    private val TYPEFACE_NORMAL = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    private val TYPEFACE_BOLD = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

    private fun createTextPaint(
        colorHex: String,
        textSizePt: Float,
        typeface: Typeface = TYPEFACE_NORMAL
    ): Paint = Paint().apply {
        color = Color.parseColor(colorHex)
        textSize = textSizePt
        this.typeface = typeface
        isAntiAlias = true
    }

    val paintCellNormal = createTextPaint(PdfColors.TEXT_CHARCOAL, 9.5f, TYPEFACE_NORMAL)
    val paintCellBold = createTextPaint(PdfColors.TEXT_DARK, 9.5f, TYPEFACE_BOLD)
    val paintMutedText = createTextPaint(PdfColors.TEXT_MUTED_GREY, 8.5f, TYPEFACE_NORMAL)
    val paintEmptyDash = createTextPaint(PdfColors.TEXT_MUTED_GREY, 10f, TYPEFACE_NORMAL)

    val paintOwedBg = Paint().apply {
        color = Color.parseColor(PdfColors.OWED_BG)
        style = Paint.Style.FILL
    }
    val paintOwedText = createTextPaint(PdfColors.OWED_TEXT, 9.5f, TYPEFACE_BOLD)

    val paintPaymentBg = Paint().apply {
        color = Color.parseColor(PdfColors.PAYMENT_BG)
        style = Paint.Style.FILL
    }
    val paintPaymentText = createTextPaint(PdfColors.PAYMENT_TEXT, 9.5f, TYPEFACE_BOLD)

    val paintDayText = createTextPaint(PdfColors.TEXT_MEDIUM, 8.5f, TYPEFACE_NORMAL)
    val paintDateText = createTextPaint(PdfColors.TEXT_DARK, 9f, TYPEFACE_NORMAL)

    val paintForeignBg = Paint().apply {
        color = Color.parseColor(PdfColors.FOREIGN_ROW_BG)
        style = Paint.Style.FILL
    }
    val paintRowDivider = Paint().apply {
        color = Color.parseColor(PdfColors.ROW_DIVIDER)
        strokeWidth = 0.5f
        style = Paint.Style.STROKE
    }
    val textPaintDesc = TextPaint(paintCellNormal)
}

