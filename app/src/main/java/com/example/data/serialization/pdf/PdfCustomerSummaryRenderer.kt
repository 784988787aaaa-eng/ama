package com.example.data.serialization.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import com.example.R
import com.example.data.serialization.pdf.PdfDrawingUtils.drawArabicText
import com.example.ui.helper.HabayebMathHelper
import com.example.ui.state.CustomerUiState
import java.math.BigDecimal

object PdfCustomerSummaryRenderer {

    private val paintCardBg = Paint().apply {
        color = Color.parseColor(PdfColors.CARD_BG)
        style = Paint.Style.FILL
    }
    private val paintCardBorder = Paint().apply {
        color = Color.parseColor(PdfColors.HEADER_BORDER)
        strokeWidth = 0.5f
        style = Paint.Style.STROKE
    }

    fun calculateCustomerSummaryRowHeight(
        context: Context,
        c: CustomerUiState,
        nameWidth: Int = 175,
        foreignWidth: Int = 125
    ): Float {
        val nameHeight = PdfDrawingUtils.measureTextHeight(c.name, PdfPaints.paintCellBold, nameWidth)
        val phoneHeight = if (c.phone.isNotBlank()) 14 else 0
        val colNameTotal = nameHeight + phoneHeight

        val foreignList = c.foreignDebts.filter { it.value.compareTo(BigDecimal.ZERO) != 0 }
        val foreignStr = if (foreignList.isEmpty()) "-" else foreignList.entries.joinToString("\n") { (curr, bd) ->
            val formatted = HabayebMathHelper.formatSmart(bd.abs())
            val prefix = if (bd.compareTo(BigDecimal.ZERO) > 0) "+" else "-"
            "$prefix$formatted $curr"
        }
        val foreignHeight = PdfDrawingUtils.measureTextHeight(foreignStr, PdfPaints.paintCellNormal, foreignWidth)

        return maxOf(colNameTotal + 14f, foreignHeight + 14f, 34f)
    }

    fun drawCustomerSummaryRow(
        canvas: Canvas,
        context: Context,
        index: Int,
        c: CustomerUiState,
        currentY: Float,
        rowHeight: Float,
        currencySymbol: String
    ) {
        val hasForeign = c.foreignDebts.any { it.value.compareTo(BigDecimal.ZERO) != 0 }
        if (hasForeign) {
            canvas.drawRect(25f, currentY, 570f, currentY + rowHeight, PdfPaints.paintForeignBg)
        }

        canvas.drawLine(25f, currentY + rowHeight, 570f, currentY + rowHeight, PdfPaints.paintRowDivider)

        // Vertical dividers
        canvas.drawLine(535f, currentY, 535f, currentY + rowHeight, PdfPaints.paintRowDivider)
        canvas.drawLine(360f, currentY, 360f, currentY + rowHeight, PdfPaints.paintRowDivider)
        canvas.drawLine(230f, currentY, 230f, currentY + rowHeight, PdfPaints.paintRowDivider)
        canvas.drawLine(105f, currentY, 105f, currentY + rowHeight, PdfPaints.paintRowDivider)

        val textYOffset = (rowHeight - 12f) / 2f

        // Col 1: Index
        drawArabicText(canvas, (index + 1).toString(), 535f, currentY + textYOffset, 35, PdfPaints.paintCellNormal, Layout.Alignment.ALIGN_CENTER)

        // Col 2: Name & Phone with Dynamic Layout
        val nameLayout = PdfDrawingUtils.createStaticLayout(c.name, PdfPaints.paintCellBold, 170, Layout.Alignment.ALIGN_NORMAL)
        val nameTotalH = nameLayout.height + if (c.phone.isNotBlank()) 14f else 0f
        val nameYOffset = ((rowHeight - nameTotalH) / 2f).coerceAtLeast(3f)

        PdfDrawingUtils.drawStaticLayout(canvas, nameLayout, 365f, currentY + nameYOffset)
        if (c.phone.isNotBlank()) {
            drawArabicText(canvas, c.phone, 365f, currentY + nameYOffset + nameLayout.height + 1f, 170, PdfPaints.paintMutedText, Layout.Alignment.ALIGN_NORMAL)
        }

        // Col 3: Primary Balance
        val totalBd = c.defaultCurrencyTotal
        val isPositive = totalBd.compareTo(BigDecimal.ZERO) > 0
        val isNegative = totalBd.compareTo(BigDecimal.ZERO) < 0
        val formattedPrimary = HabayebMathHelper.formatSmart(totalBd.abs()) + " " + currencySymbol
        val balancePaint = if (isPositive) PdfPaints.paintOwedText else if (isNegative) PdfPaints.paintPaymentText else PdfPaints.paintCellNormal
        drawArabicText(canvas, formattedPrimary, 230f, currentY + textYOffset, 130, balancePaint, Layout.Alignment.ALIGN_CENTER)

        // Col 4: Foreign Currencies with Dynamic Layout
        val foreignList = c.foreignDebts.filter { it.value.compareTo(BigDecimal.ZERO) != 0 }
        val foreignStr = if (foreignList.isEmpty()) {
            "-"
        } else {
            foreignList.entries.joinToString("\n") { (curr, bd) ->
                val formatted = HabayebMathHelper.formatSmart(bd.abs())
                val prefix = if (bd.compareTo(BigDecimal.ZERO) > 0) "+" else "-"
                "$prefix$formatted $curr"
            }
        }
        val foreignLayout = PdfDrawingUtils.createStaticLayout(foreignStr, PdfPaints.paintCellNormal, 120, Layout.Alignment.ALIGN_CENTER)
        val foreignYOffset = ((rowHeight - foreignLayout.height) / 2f).coerceAtLeast(3f)
        PdfDrawingUtils.drawStaticLayout(canvas, foreignLayout, 105f, currentY + foreignYOffset)

        // Col 5: Status
        val statusStr = if (isPositive) {
            context.getString(R.string.pdf_status_owed_word)
        } else if (isNegative) {
            context.getString(R.string.pdf_status_to_him_word)
        } else {
            context.getString(R.string.pdf_status_balanced_word)
        }
        val statusPaint = if (isPositive) PdfPaints.paintOwedText else if (isNegative) PdfPaints.paintPaymentText else PdfPaints.paintMutedText
        drawArabicText(canvas, statusStr, 25f, currentY + textYOffset, 80, statusPaint, Layout.Alignment.ALIGN_CENTER)
    }

    fun drawBookletIndexHeader(canvas: Canvas, y: Float, context: Context) {
        val paintHeaderBg = Paint().apply {
            color = Color.parseColor(PdfColors.HEADER_BG)
            style = Paint.Style.FILL
        }
        val paintHeaderBorder = Paint().apply {
            color = Color.parseColor(PdfColors.HEADER_BORDER)
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
        }
        canvas.drawRect(25f, y, 570f, y + 24f, paintHeaderBg)
        canvas.drawLine(25f, y, 570f, y, paintHeaderBorder)
        canvas.drawLine(25f, y + 24f, 570f, y + 24f, paintHeaderBorder)

        // Vertical dividers
        canvas.drawLine(535f, y, 535f, y + 24f, paintHeaderBorder)
        canvas.drawLine(305f, y, 305f, y + 24f, paintHeaderBorder)
        canvas.drawLine(205f, y, 205f, y + 24f, paintHeaderBorder)
        canvas.drawLine(105f, y, 105f, y + 24f, paintHeaderBorder)

        val paintHeaderText = Paint().apply {
            color = Color.parseColor(PdfColors.HEADER_TEXT)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        drawArabicText(canvas, context.getString(R.string.pdf_col_m), 535f, y + 6f, 35, paintHeaderText, Layout.Alignment.ALIGN_CENTER)
        drawArabicText(canvas, context.getString(R.string.pdf_index_col_name), 305f, y + 6f, 230, paintHeaderText, Layout.Alignment.ALIGN_NORMAL)
        drawArabicText(canvas, context.getString(R.string.pdf_index_col_phone), 205f, y + 6f, 100, paintHeaderText, Layout.Alignment.ALIGN_NORMAL)
        drawArabicText(canvas, context.getString(R.string.pdf_index_col_balance), 105f, y + 6f, 100, paintHeaderText, Layout.Alignment.ALIGN_CENTER)
        drawArabicText(canvas, context.getString(R.string.pdf_index_col_status), 25f, y + 6f, 80, paintHeaderText, Layout.Alignment.ALIGN_CENTER)
    }

    fun calculateBookletIndexRowHeight(customer: CustomerUiState, availableWidth: Int = 225): Float {
        val nameHeight = PdfDrawingUtils.measureTextHeight(customer.name, PdfPaints.paintCellBold, availableWidth)
        return (nameHeight + 10f).coerceAtLeast(24f)
    }

    fun drawBookletIndexRow(
        canvas: Canvas,
        context: Context,
        index: Int,
        customer: CustomerUiState,
        currentY: Float,
        rowHeight: Float,
        currencySymbol: String
    ) {
        canvas.drawLine(25f, currentY + rowHeight, 570f, currentY + rowHeight, PdfPaints.paintRowDivider)

        // Vertical dividers
        canvas.drawLine(535f, currentY, 535f, currentY + rowHeight, PdfPaints.paintRowDivider)
        canvas.drawLine(305f, currentY, 305f, currentY + rowHeight, PdfPaints.paintRowDivider)
        canvas.drawLine(205f, currentY, 205f, currentY + rowHeight, PdfPaints.paintRowDivider)
        canvas.drawLine(105f, currentY, 105f, currentY + rowHeight, PdfPaints.paintRowDivider)

        val textYOffset = (rowHeight - 12f) / 2f

        // Column: No (م)
        drawArabicText(canvas, (index + 1).toString(), 535f, currentY + textYOffset, 35, PdfPaints.paintCellNormal, Layout.Alignment.ALIGN_CENTER)

        // Column: Name with Dynamic Layout
        val nameLayout = PdfDrawingUtils.createStaticLayout(customer.name, PdfPaints.paintCellBold, 225, Layout.Alignment.ALIGN_NORMAL)
        val nameYOffset = ((rowHeight - nameLayout.height) / 2f).coerceAtLeast(2f)
        PdfDrawingUtils.drawStaticLayout(canvas, nameLayout, 310f, currentY + nameYOffset)

        // Column: Phone
        drawArabicText(canvas, customer.phone.ifEmpty { "-" }, 205f, currentY + textYOffset, 100, PdfPaints.paintCellNormal, Layout.Alignment.ALIGN_NORMAL)

        // Column: Final Balance
        val balText = "${HabayebMathHelper.formatSmart(customer.defaultCurrencyTotal.abs())} $currencySymbol"
        drawArabicText(canvas, balText, 105f, currentY + textYOffset, 100, PdfPaints.paintCellBold, Layout.Alignment.ALIGN_CENTER)

        // Column: Status
        val statusStr = when {
            customer.defaultCurrencyTotal > BigDecimal.ZERO -> context.getString(R.string.pdf_status_for_us)
            customer.defaultCurrencyTotal < BigDecimal.ZERO -> context.getString(R.string.pdf_status_on_us)
            else -> context.getString(R.string.pdf_status_balanced)
        }
        val statusColor = when {
            customer.defaultCurrencyTotal > BigDecimal.ZERO -> PdfColors.PAYMENT_TEXT
            customer.defaultCurrencyTotal < BigDecimal.ZERO -> PdfColors.OWED_TEXT
            else -> PdfColors.TEXT_LIGHT
        }
        val paintStatus = Paint().apply {
            color = Color.parseColor(statusColor)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        drawArabicText(canvas, statusStr, 25f, currentY + textYOffset, 80, paintStatus, Layout.Alignment.ALIGN_CENTER)
    }

    fun drawComprehensiveSummaryCard(
        canvas: Canvas,
        context: Context,
        primaryColorHex: String,
        summary: ComprehensivePdfSummary,
        totalItems: Int,
        currencySymbol: String,
        startY: Float = 98f
    ) {
        val nonZeroForeign = summary.foreignTotalsMap.filter { it.value.compareTo(BigDecimal.ZERO) != 0 }
        val cardHeight = if (nonZeroForeign.isNotEmpty()) 54f else 46f
        val endY = startY + cardHeight
        canvas.drawRoundRect(25f, startY, 570f, endY, 6f, 6f, paintCardBg)
        canvas.drawRoundRect(25f, startY, 570f, endY, 6f, 6f, paintCardBorder)

        val netPrimary = summary.netPrimary
        val netPrimaryFormatted = HabayebMathHelper.formatSmart(netPrimary.abs()) + " " + currencySymbol
        val netPrimaryStatus = if (netPrimary.compareTo(BigDecimal.ZERO) > 0) {
            context.getString(R.string.pdf_status_for_us)
        } else if (netPrimary.compareTo(BigDecimal.ZERO) < 0) {
            context.getString(R.string.pdf_status_on_us)
        } else {
            context.getString(R.string.pdf_status_balanced_word)
        }

        val primarySummary = context.getString(
            R.string.pdf_comprehensive_accounts_summary,
            totalItems,
            currencySymbol,
            HabayebMathHelper.formatSmart(summary.totalOwedByThem),
            HabayebMathHelper.formatSmart(summary.totalOwedToThem),
            netPrimaryFormatted,
            netPrimaryStatus
        )

        val paintMainSummary = Paint().apply {
            color = Color.parseColor(PdfColors.TEXT_CHARCOAL)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        if (nonZeroForeign.isEmpty()) {
            drawArabicText(canvas, primarySummary, 30f, startY + 16f, 535, paintMainSummary, Layout.Alignment.ALIGN_CENTER)
        } else {
            drawArabicText(canvas, primarySummary, 30f, startY + 10f, 535, paintMainSummary, Layout.Alignment.ALIGN_CENTER)
            val foreignSummary = context.getString(R.string.pdf_other_currencies_balances) + " " + nonZeroForeign.entries.joinToString("   |   ") { (curr, bd) ->
                val status = if (bd.compareTo(BigDecimal.ZERO) > 0) {
                    context.getString(R.string.pdf_status_for_us)
                } else {
                    context.getString(R.string.pdf_status_on_us)
                }
                "$curr: " + HabayebMathHelper.formatSmart(bd.abs()) + " ($status)"
            }
            val paintForeignSummary = Paint().apply {
                color = Color.parseColor(primaryColorHex)
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            drawArabicText(canvas, foreignSummary, 30f, startY + 30f, 535, paintForeignSummary, Layout.Alignment.ALIGN_CENTER)
        }
    }
}
