package com.example.data.serialization.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Layout
import com.example.R
import com.example.data.serialization.pdf.PdfDrawingUtils.drawArabicText
import com.example.domain.model.TransactionType
import com.example.ui.helper.HabayebMathHelper
import com.example.ui.screens.habayeb.utils.CurrencyConfig
import java.math.BigDecimal
import java.util.Date

object PdfTransactionRowRenderer {

    fun buildTransactionDescriptionText(
        context: Context,
        pt: ProcessedTransaction,
        initialType: String = TransactionType.OWED_BY_THEM.value
    ): String {
        val tx = pt.tx
        val isOwedToThemAccount = initialType == TransactionType.OWED_TO_THEM.value

        val txTypeStr = when (tx.type) {
            TransactionType.OWED_BY_THEM.value -> context.getString(R.string.pdf_tx_type_owed_by_them)
            TransactionType.PAYMENT_BY_THEM.value -> if (isOwedToThemAccount) context.getString(R.string.pdf_tx_type_payment_to_them) else context.getString(R.string.pdf_tx_type_payment_by_them)
            TransactionType.OWED_TO_THEM.value -> context.getString(R.string.pdf_tx_type_owed_to_them)
            TransactionType.PAYMENT_TO_THEM.value -> context.getString(R.string.pdf_tx_type_payment_to_them)
            else -> context.getString(R.string.pdf_tx_type_new)
        }
        val cleanDetails = CurrencyConfig.getCleanDetails(tx.description)

        return buildString {
            append(txTypeStr)
            if (cleanDetails.isNotEmpty()) {
                append(" - ")
                append(cleanDetails)
            }
            if (tx.isRateCalculated) {
                val origCurrency = CurrencyConfig.getBySymbol(tx.currencyCode)?.symbol ?: tx.currencyCode
                val origAmount = if (tx.foreignAmount.compareTo(BigDecimal.ZERO) > 0) tx.foreignAmount else tx.amount
                val formattedAmount = HabayebMathHelper.formatSmart(origAmount)
                val formattedRate = HabayebMathHelper.formatRate(tx.exchangeRate)
                append("\n[ صرف: $formattedAmount $origCurrency × $formattedRate ]")
            } else if (pt.isTxForeign) {
                val origCurrency = CurrencyConfig.getBySymbol(tx.currencyCode)?.symbol ?: tx.currencyCode
                val origAmount = if (tx.foreignAmount.compareTo(BigDecimal.ZERO) > 0) tx.foreignAmount else tx.amount
                val formattedAmount = HabayebMathHelper.formatSmart(origAmount)
                append("\n[ $formattedAmount $origCurrency - نقد أجنبي ]")
            }
        }
    }

    fun calculateTransactionRowHeight(
        context: Context,
        pt: ProcessedTransaction,
        initialType: String = TransactionType.OWED_BY_THEM.value,
        availableWidth: Int = 190
    ): Float {
        val descText = buildTransactionDescriptionText(context, pt, initialType)
        val textHeight = PdfDrawingUtils.measureTextHeight(descText, PdfPaints.textPaintDesc, availableWidth)
        return (textHeight + 14f).coerceAtLeast(32f)
    }

    fun drawSingleTransactionRow(
        canvas: Canvas,
        context: Context,
        index: Int,
        pt: ProcessedTransaction,
        currentY: Float,
        rowHeight: Float,
        runningBal: BigDecimal,
        initialType: String = TransactionType.OWED_BY_THEM.value
    ) {
        val tx = pt.tx
        val isTxForeign = pt.isTxForeign
        val hasBaseAmount = pt.baseCurrencyAmount.compareTo(BigDecimal.ZERO) > 0

        if (isTxForeign) {
            canvas.drawRect(25f, currentY, 570f, currentY + rowHeight, PdfPaints.paintForeignBg)
        }

        // Horizontal bottom divider
        canvas.drawLine(25f, currentY + rowHeight, 570f, currentY + rowHeight, PdfPaints.paintRowDivider)

        // Vertical grid lines between columns
        canvas.drawLine(545f, currentY, 545f, currentY + rowHeight, PdfPaints.paintRowDivider)
        canvas.drawLine(455f, currentY, 455f, currentY + rowHeight, PdfPaints.paintRowDivider)
        canvas.drawLine(260f, currentY, 260f, currentY + rowHeight, PdfPaints.paintRowDivider)
        canvas.drawLine(180f, currentY, 180f, currentY + rowHeight, PdfPaints.paintRowDivider)
        canvas.drawLine(100f, currentY, 100f, currentY + rowHeight, PdfPaints.paintRowDivider)

        val textYOffset = (rowHeight - 12f) / 2f

        // Col 1: Sequence Number (#)
        val seqNo = (index + 1).toString()
        drawArabicText(canvas, seqNo, 545f, currentY + textYOffset, 25, PdfPaints.paintCellNormal, Layout.Alignment.ALIGN_CENTER)

        // Col 2: Date & Day together in a single row
        val txTimestampMs = if (tx.timestamp > 1000000000000L) tx.timestamp else tx.timestamp * 1000
        val txDate = Date(txTimestampMs)
        val dayName = try { PdfPageRenderer.formatDayAr(txDate) } catch (e: Exception) { "" }
        val formattedDate = try { PdfPageRenderer.formatDateEn(txDate) } catch (e: Exception) { "" }
        val fullDateStr = if (dayName.isNotBlank()) "$dayName $formattedDate" else formattedDate

        val layoutDate = PdfDrawingUtils.createStaticLayout(fullDateStr, PdfPaints.paintDateText, 90, Layout.Alignment.ALIGN_CENTER)
        val dateYOffset = ((rowHeight - layoutDate.height) / 2f).coerceAtLeast(2f)
        PdfDrawingUtils.drawStaticLayout(canvas, layoutDate, 455f, currentY + dateYOffset)

        // Col 3: Details with Dynamic StaticLayout
        val txLabel = buildTransactionDescriptionText(context, pt, initialType)
        val layoutDesc = PdfDrawingUtils.createStaticLayout(txLabel, PdfPaints.textPaintDesc, 190, Layout.Alignment.ALIGN_NORMAL)
        val descYOffset = ((rowHeight - layoutDesc.height) / 2f).coerceAtLeast(2f)
        PdfDrawingUtils.drawStaticLayout(canvas, layoutDesc, 262f, currentY + descYOffset)

        // Amounts
        val formattedAmount = if (hasBaseAmount) {
            HabayebMathHelper.formatSmart(pt.baseCurrencyAmount)
        } else "-"

        // Col 4 & Col 5
        val isOwedToThemAccount = initialType == TransactionType.OWED_TO_THEM.value
        val isCol4 = if (isOwedToThemAccount) {
            tx.type == TransactionType.OWED_TO_THEM.value || tx.type == TransactionType.PAYMENT_BY_THEM.value
        } else {
            tx.type == TransactionType.OWED_BY_THEM.value || tx.type == TransactionType.PAYMENT_TO_THEM.value
        }

        if (hasBaseAmount) {
            if (isCol4) {
                // Col 4 Badge & Text (180f)
                val badgeLeft = 184f
                val badgeTop = currentY + ((rowHeight - 18f) / 2f)
                val badgeRight = 256f
                val badgeBottom = badgeTop + 18f
                val badgePaint = if (isOwedToThemAccount) PdfPaints.paintPaymentBg else PdfPaints.paintOwedBg
                val textPaint = if (isOwedToThemAccount) PdfPaints.paintPaymentText else PdfPaints.paintOwedText

                canvas.drawRoundRect(badgeLeft, badgeTop, badgeRight, badgeBottom, 3f, 3f, badgePaint)
                drawArabicText(canvas, formattedAmount, 180f, currentY + textYOffset, 80, textPaint, Layout.Alignment.ALIGN_CENTER)

                // Col 5: empty dash (-)
                drawArabicText(canvas, "-", 100f, currentY + textYOffset, 80, PdfPaints.paintEmptyDash, Layout.Alignment.ALIGN_CENTER)
            } else {
                // Col 4: empty dash (-)
                drawArabicText(canvas, "-", 180f, currentY + textYOffset, 80, PdfPaints.paintEmptyDash, Layout.Alignment.ALIGN_CENTER)

                // Col 5 Badge & Text (100f)
                val badgeLeft = 104f
                val badgeTop = currentY + ((rowHeight - 18f) / 2f)
                val badgeRight = 176f
                val badgeBottom = badgeTop + 18f
                val badgePaint = if (isOwedToThemAccount) PdfPaints.paintOwedBg else PdfPaints.paintPaymentBg
                val textPaint = if (isOwedToThemAccount) PdfPaints.paintOwedText else PdfPaints.paintPaymentText

                canvas.drawRoundRect(badgeLeft, badgeTop, badgeRight, badgeBottom, 3f, 3f, badgePaint)
                drawArabicText(canvas, formattedAmount, 100f, currentY + textYOffset, 80, textPaint, Layout.Alignment.ALIGN_CENTER)
            }

            // Col 6: Running Balance (الرصيد)
            val formattedRunning = HabayebMathHelper.formatSmart(runningBal.abs())
            val isBalanced = runningBal.compareTo(BigDecimal.ZERO) == 0
            val isPositive = runningBal.compareTo(BigDecimal.ZERO) > 0
            val runningBalColor = when {
                isBalanced -> PdfColors.TEXT_DARK
                isOwedToThemAccount -> if (isPositive) PdfColors.PAYMENT_TEXT else PdfColors.OWED_TEXT
                else -> if (isPositive) PdfColors.OWED_TEXT else PdfColors.PAYMENT_TEXT
            }
            val paintRunning = Paint(PdfPaints.paintCellBold).apply { color = Color.parseColor(runningBalColor) }
            val balText = if (isBalanced) "-" else formattedRunning
            drawArabicText(canvas, balText, 25f, currentY + textYOffset, 75, paintRunning, Layout.Alignment.ALIGN_CENTER)
        } else {
            // Transaction belongs to another currency ledger, so it doesn't affect this ledger's amounts
            drawArabicText(canvas, "-", 180f, currentY + textYOffset, 80, PdfPaints.paintEmptyDash, Layout.Alignment.ALIGN_CENTER)
            drawArabicText(canvas, "-", 100f, currentY + textYOffset, 80, PdfPaints.paintEmptyDash, Layout.Alignment.ALIGN_CENTER)
            drawArabicText(canvas, "-", 25f, currentY + textYOffset, 75, PdfPaints.paintEmptyDash, Layout.Alignment.ALIGN_CENTER)
        }
    }
}
