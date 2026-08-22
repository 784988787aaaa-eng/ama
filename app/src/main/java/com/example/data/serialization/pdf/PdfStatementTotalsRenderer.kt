package com.example.data.serialization.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import com.example.R
import com.example.data.serialization.pdf.PdfDrawingUtils.drawArabicText
import com.example.domain.model.TransactionType
import com.example.ui.helper.HabayebMathHelper
import java.math.BigDecimal

object PdfStatementTotalsRenderer {

    fun drawTotalsRow(
        canvas: Canvas,
        context: Context,
        currentY: Float,
        totalDebts: BigDecimal,
        totalPayments: BigDecimal,
        currencySymbol: String,
        initialType: String = TransactionType.OWED_BY_THEM.value
    ): Float {
        val rowHeight = 24f
        val paintBg = Paint().apply {
            color = Color.parseColor(PdfColors.TOTALS_ROW_BG)
            style = Paint.Style.FILL
        }
        canvas.drawRect(25f, currentY, 570f, currentY + rowHeight, paintBg)
        canvas.drawLine(25f, currentY, 570f, currentY, PdfPaints.paintRowDivider)
        canvas.drawLine(25f, currentY + rowHeight, 570f, currentY + rowHeight, PdfPaints.paintRowDivider)

        // Draw vertical column dividers
        canvas.drawLine(545f, currentY, 545f, currentY + rowHeight, PdfPaints.paintRowDivider)
        canvas.drawLine(455f, currentY, 455f, currentY + rowHeight, PdfPaints.paintRowDivider)
        canvas.drawLine(260f, currentY, 260f, currentY + rowHeight, PdfPaints.paintRowDivider)
        canvas.drawLine(180f, currentY, 180f, currentY + rowHeight, PdfPaints.paintRowDivider)
        canvas.drawLine(100f, currentY, 100f, currentY + rowHeight, PdfPaints.paintRowDivider)

        val paintTitle = Paint().apply {
            color = Color.parseColor(PdfColors.TEXT_DARK)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        drawArabicText(canvas, "-", 545f, currentY + 5f, 25, PdfPaints.paintEmptyDash, Layout.Alignment.ALIGN_CENTER)
        drawArabicText(canvas, "-", 455f, currentY + 5f, 90, PdfPaints.paintEmptyDash, Layout.Alignment.ALIGN_CENTER)
        drawArabicText(canvas, context.getString(R.string.pdf_totals_operations_title), 260f, currentY + 5f, 195, paintTitle, Layout.Alignment.ALIGN_NORMAL)

        val isOwedToThemAccount = initialType == TransactionType.OWED_TO_THEM.value
        val col4Color = if (isOwedToThemAccount) PdfColors.PAYMENT_TEXT else PdfColors.OWED_TEXT
        val col5Color = if (isOwedToThemAccount) PdfColors.OWED_TEXT else PdfColors.PAYMENT_TEXT

        val formattedDebts = HabayebMathHelper.formatSmart(totalDebts)
        val paintDebts = Paint(PdfPaints.paintCellBold).apply {
            color = Color.parseColor(col4Color)
        }
        drawArabicText(canvas, formattedDebts, 180f, currentY + 5f, 80, paintDebts, Layout.Alignment.ALIGN_CENTER)

        val formattedPayments = HabayebMathHelper.formatSmart(totalPayments)
        val paintPayments = Paint(PdfPaints.paintCellBold).apply {
            color = Color.parseColor(col5Color)
        }
        drawArabicText(canvas, formattedPayments, 100f, currentY + 5f, 80, paintPayments, Layout.Alignment.ALIGN_CENTER)

        drawArabicText(canvas, "-", 25f, currentY + 5f, 75, PdfPaints.paintEmptyDash, Layout.Alignment.ALIGN_CENTER)

        return currentY + rowHeight
    }

    fun drawFinalNetBanner(
        canvas: Canvas,
        context: Context,
        currentY: Float,
        netBalance: BigDecimal,
        currencySymbol: String,
        initialType: String = TransactionType.OWED_BY_THEM.value
    ): Float {
        val bannerHeight = 30f
        val isOwedToThemAccount = initialType == TransactionType.OWED_TO_THEM.value
        val rawPositive = netBalance.compareTo(BigDecimal.ZERO) > 0
        val rawNegative = netBalance.compareTo(BigDecimal.ZERO) < 0

        val isOwedToThemStatus = if (isOwedToThemAccount) rawPositive else rawNegative
        val isOwedByThemStatus = if (isOwedToThemAccount) rawNegative else rawPositive

        val bannerBgColor = when {
            isOwedByThemStatus -> PdfColors.BANNER_OWED_BG
            isOwedToThemStatus -> PdfColors.BANNER_PAYMENT_BG
            else -> PdfColors.TOTALS_ROW_BG
        }

        val paintBannerBg = Paint().apply {
            color = Color.parseColor(bannerBgColor)
            style = Paint.Style.FILL
        }
        val paintBannerBorder = Paint().apply {
            color = Color.parseColor(if (isOwedByThemStatus) "#FCA5A5" else if (isOwedToThemStatus) "#86EFAC" else PdfColors.HEADER_BORDER)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        canvas.drawRoundRect(25f, currentY, 570f, currentY + bannerHeight, 4f, 4f, paintBannerBg)
        canvas.drawRoundRect(25f, currentY, 570f, currentY + bannerHeight, 4f, 4f, paintBannerBorder)

        val statusText = when {
            isOwedByThemStatus -> context.getString(R.string.pdf_net_banner_owed_by)
            isOwedToThemStatus -> context.getString(R.string.pdf_net_banner_owed_to)
            else -> context.getString(R.string.pdf_net_banner_balanced)
        }

        val textColor = when {
            isOwedByThemStatus -> PdfColors.OWED_TEXT
            isOwedToThemStatus -> PdfColors.PAYMENT_TEXT
            else -> PdfColors.TEXT_DARK
        }

        val paintTextLabel = Paint().apply {
            color = Color.parseColor(textColor)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val formattedAmount = "${HabayebMathHelper.formatSmart(netBalance.abs())} $currencySymbol"

        drawArabicText(canvas, statusText, 250f, currentY + 7f, 310, paintTextLabel, Layout.Alignment.ALIGN_NORMAL)
        drawArabicText(canvas, formattedAmount, 35f, currentY + 7f, 200, paintTextLabel, Layout.Alignment.ALIGN_OPPOSITE)

        return currentY + bannerHeight + 8f
    }

    fun drawForeignCurrenciesSummary(
        canvas: Canvas,
        context: Context,
        currentY: Float,
        uncalculatedForeignSums: Map<String, BigDecimal>,
        currencySymbol: String
    ): Float {
        if (uncalculatedForeignSums.isEmpty()) return currentY

        var y = currentY + 4f
        val itemHeight = 20f
        val boxHeight = 24f + (uncalculatedForeignSums.size * itemHeight)

        val paintBg = Paint().apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }
        val paintBorder = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val paintTitle = Paint().apply {
            color = Color.parseColor(PdfColors.PRIMARY_EMERALD)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintItem = Paint().apply {
            color = Color.parseColor(PdfColors.TEXT_CHARCOAL)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        canvas.drawRoundRect(25f, y, 570f, y + boxHeight, 4f, 4f, paintBg)
        canvas.drawRoundRect(25f, y, 570f, y + boxHeight, 4f, 4f, paintBorder)

        drawArabicText(canvas, context.getString(R.string.pdf_independent_totals_uncalculated), 35f, y + 6f, 520, paintTitle, Layout.Alignment.ALIGN_NORMAL)

        var itemY = y + 24f
        for ((curr, amount) in uncalculatedForeignSums) {
            val isPositive = amount.compareTo(BigDecimal.ZERO) > 0
            val isNegative = amount.compareTo(BigDecimal.ZERO) < 0
            val statusText = if (isPositive) context.getString(R.string.pdf_status_owed_word) else if (isNegative) context.getString(R.string.pdf_status_to_him_word) else context.getString(R.string.pdf_status_balanced_word)
            val foreignTag = context.getString(R.string.pdf_foreign_currency_tag)
            val lineStr = "• ${context.getString(R.string.pdf_total_currency_prefix, curr)}: ${HabayebMathHelper.formatSmart(amount.abs())} $curr ($statusText - $foreignTag)"
            drawArabicText(canvas, lineStr, 40f, itemY, 510, paintItem, Layout.Alignment.ALIGN_NORMAL)
            itemY += itemHeight
        }

        return y + boxHeight + 6f
    }
}
