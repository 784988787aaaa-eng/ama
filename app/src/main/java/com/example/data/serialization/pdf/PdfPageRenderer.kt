package com.example.data.serialization.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import com.example.R
import com.example.data.serialization.pdf.PdfDrawingUtils.drawArabicText
import com.example.domain.model.TransactionType
import com.example.ui.state.CustomerUiState
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PdfBusinessHeaderData(
    val displayedName: String,
    val displayedDesc: String,
    val phonesStr: String,
    val hasLogo: Boolean,
    val scaledLogo: Bitmap?,
    val logoW: Float,
    val logoH: Float,
    val docDateText: String,
    val docTimeText: String
)

object PdfPageRenderer {

    private val dayFormatAr = SimpleDateFormat("EEEE", Locale("ar"))
    private val dateFormatEn = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)
    private val timeFormatAr = SimpleDateFormat("hh:mm a", Locale("ar"))

    private val paintHeaderBg = Paint().apply {
        color = Color.parseColor(PdfColors.HEADER_BG)
        style = Paint.Style.FILL
    }
    private val paintHeaderBorder = Paint().apply {
        color = Color.parseColor(PdfColors.HEADER_BORDER)
        strokeWidth = 0.5f
        style = Paint.Style.STROKE
    }
    private val paintHeaderText = Paint().apply {
        color = Color.parseColor(PdfColors.HEADER_TEXT)
        textSize = 10f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    private val paintAllCustomersHeaderText = Paint().apply {
        color = Color.parseColor(PdfColors.HEADER_TEXT)
        textSize = 9.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    private val paintMiniLine = Paint().apply {
        color = Color.parseColor(PdfColors.HEADER_BORDER)
        strokeWidth = 0.5f
        style = Paint.Style.STROKE
    }
    private val paintMiniHeader = Paint().apply {
        textSize = 9.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    private val paintFooterText = Paint().apply {
        color = Color.parseColor(PdfColors.TEXT_MUTED_GREY)
        textSize = 8.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        isAntiAlias = true
    }
    private val paintBizName = Paint().apply {
        color = Color.parseColor(PdfColors.TEXT_DARK)
        textSize = 14.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    private val paintBizDesc = Paint().apply {
        color = Color.parseColor(PdfColors.TEXT_MEDIUM)
        textSize = 9.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        isAntiAlias = true
    }
    private val paintBizPhones = Paint().apply {
        color = Color.parseColor(PdfColors.TEXT_LIGHT)
        textSize = 8.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        isAntiAlias = true
    }
    private val paintLeft1 = Paint().apply {
        color = Color.parseColor(PdfColors.TEXT_MEDIUM)
        textSize = 9f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        isAntiAlias = true
    }
    private val paintLeft2 = Paint().apply {
        color = Color.parseColor(PdfColors.TEXT_LIGHT)
        textSize = 8.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        isAntiAlias = true
    }
    private val paintDivider = Paint().apply {
        color = Color.parseColor(PdfColors.HEADER_BORDER)
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    @Synchronized
    fun formatDayAr(date: Date): String = dayFormatAr.format(date)

    @Synchronized
    fun formatDateEn(date: Date): String = dateFormatEn.format(date)

    @Synchronized
    fun formatTimeAr(date: Date): String = timeFormatAr.format(date)

    fun drawTableHeader(canvas: Canvas, y: Float, context: Context, initialType: String = TransactionType.OWED_BY_THEM.value) {
        canvas.drawRect(25f, y, 570f, y + 25f, paintHeaderBg)
        canvas.drawLine(25f, y, 570f, y, paintHeaderBorder)
        canvas.drawLine(25f, y + 25f, 570f, y + 25f, paintHeaderBorder)

        // Draw vertical grid lines in table header
        canvas.drawLine(545f, y, 545f, y + 25f, paintHeaderBorder)
        canvas.drawLine(455f, y, 455f, y + 25f, paintHeaderBorder)
        canvas.drawLine(260f, y, 260f, y + 25f, paintHeaderBorder)
        canvas.drawLine(180f, y, 180f, y + 25f, paintHeaderBorder)
        canvas.drawLine(100f, y, 100f, y + 25f, paintHeaderBorder)

        val isOwedToThem = initialType == TransactionType.OWED_TO_THEM.value
        val col4Text = if (isOwedToThem) context.getString(R.string.pdf_col_owed_to) else context.getString(R.string.pdf_col_owed_by)
        val col5Text = if (isOwedToThem) context.getString(R.string.pdf_col_paid) else context.getString(R.string.pdf_col_received)

        drawArabicText(canvas, context.getString(R.string.pdf_col_m), 545f, y + 6f, 25, paintHeaderText, Layout.Alignment.ALIGN_CENTER)
        drawArabicText(canvas, context.getString(R.string.pdf_col_date), 455f, y + 6f, 90, paintHeaderText, Layout.Alignment.ALIGN_CENTER)
        drawArabicText(canvas, context.getString(R.string.pdf_col_description), 260f, y + 6f, 195, paintHeaderText, Layout.Alignment.ALIGN_NORMAL)
        drawArabicText(canvas, col4Text, 180f, y + 6f, 80, paintHeaderText, Layout.Alignment.ALIGN_CENTER)
        drawArabicText(canvas, col5Text, 100f, y + 6f, 80, paintHeaderText, Layout.Alignment.ALIGN_CENTER)
        drawArabicText(canvas, context.getString(R.string.pdf_col_remaining), 25f, y + 6f, 75, paintHeaderText, Layout.Alignment.ALIGN_CENTER)
    }

    fun drawSubsequentPageHeader(canvas: Canvas, customerName: String, primaryColorHex: String, context: Context) {
        paintMiniHeader.color = Color.parseColor(primaryColorHex)
        val miniHeaderText = context.getString(R.string.pdf_mini_header_text, customerName)
        drawArabicText(canvas, miniHeaderText, 25f, 16f, 545, paintMiniHeader, Layout.Alignment.ALIGN_CENTER)
        canvas.drawLine(25f, 28f, 570f, 28f, paintMiniLine)
    }

    fun drawFooter(canvas: Canvas, pageNum: Int, totalPages: Int, primaryColorHex: String, context: Context) {
        val footerTextLeft = context.getString(R.string.pdf_footer_page, pageNum, totalPages)
        val footerTextRight = context.getString(R.string.pdf_footer_certified)

        drawArabicText(canvas, footerTextLeft, 25f, 815f, 150, paintFooterText, Layout.Alignment.ALIGN_OPPOSITE)
        drawArabicText(canvas, footerTextRight, 200f, 815f, 370, paintFooterText, Layout.Alignment.ALIGN_NORMAL)
    }

    fun drawBusinessHeader(canvas: Canvas, headerData: PdfBusinessHeaderData) {
        drawBusinessHeader(
            canvas = canvas,
            displayedName = headerData.displayedName,
            displayedDesc = headerData.displayedDesc,
            phonesStr = headerData.phonesStr,
            hasLogo = headerData.hasLogo,
            scaledLogo = headerData.scaledLogo,
            logoW = headerData.logoW,
            logoH = headerData.logoH,
            docDateText = headerData.docDateText,
            docTimeText = headerData.docTimeText
        )
    }

    fun drawBusinessHeader(
        canvas: Canvas,
        displayedName: String,
        displayedDesc: String,
        phonesStr: String,
        hasLogo: Boolean,
        scaledLogo: Bitmap?,
        logoW: Float,
        logoH: Float,
        docDateText: String,
        docTimeText: String
    ) {
        val rightColX = 360f
        val maxColWidth = 210f

        val namePaint = Paint(paintBizName)
        var nameSize = 14.5f
        namePaint.textSize = nameSize
        while (namePaint.measureText(displayedName) > maxColWidth && nameSize > 9.0f) {
            nameSize -= 0.4f
            namePaint.textSize = nameSize
        }
        drawArabicText(canvas, displayedName, rightColX, 20f, 210, namePaint, Layout.Alignment.ALIGN_NORMAL)

        val descPaint = Paint(paintBizDesc)
        var descSize = 9.5f
        descPaint.textSize = descSize
        while (descPaint.measureText(displayedDesc) > maxColWidth && descSize > 6.0f) {
            descSize -= 0.3f
            descPaint.textSize = descSize
        }
        drawArabicText(canvas, displayedDesc, rightColX, 38f, 210, descPaint, Layout.Alignment.ALIGN_NORMAL)

        val phonePaint = Paint(paintBizPhones)
        var phoneSize = 8.5f
        phonePaint.textSize = phoneSize
        while (phonePaint.measureText(phonesStr) > maxColWidth && phoneSize > 5.0f) {
            phoneSize -= 0.3f
            phonePaint.textSize = phoneSize
        }
        drawArabicText(canvas, phonesStr, rightColX, 52f, 210, phonePaint, Layout.Alignment.ALIGN_NORMAL)

        if (hasLogo && scaledLogo != null) {
            val logoX = 297.5f - (logoW / 2f)
            val logoY = 20f + ((45f - logoH) / 2f)
            canvas.drawBitmap(scaledLogo, logoX, logoY, null)
        }

        drawArabicText(canvas, docDateText, 25f, 22f, 180, paintLeft1, Layout.Alignment.ALIGN_OPPOSITE)
        drawArabicText(canvas, docTimeText, 25f, 36f, 180, paintLeft2, Layout.Alignment.ALIGN_OPPOSITE)

        canvas.drawLine(25f, 68f, 570f, 68f, paintDivider)
    }

    fun drawAllCustomersTableHeader(canvas: Canvas, y: Float, context: Context) {
        canvas.drawRect(25f, y, 570f, y + 26f, paintHeaderBg)

        canvas.drawLine(25f, y, 570f, y, paintHeaderBorder)
        canvas.drawLine(25f, y + 26f, 570f, y + 26f, paintHeaderBorder)

        // Vertical dividers
        canvas.drawLine(535f, y, 535f, y + 26f, paintHeaderBorder)
        canvas.drawLine(360f, y, 360f, y + 26f, paintHeaderBorder)
        canvas.drawLine(230f, y, 230f, y + 26f, paintHeaderBorder)
        canvas.drawLine(105f, y, 105f, y + 26f, paintHeaderBorder)

        drawArabicText(canvas, context.getString(R.string.pdf_col_m), 535f, y + 7f, 35, paintAllCustomersHeaderText, Layout.Alignment.ALIGN_CENTER)
        drawArabicText(canvas, context.getString(R.string.pdf_col_account_name), 360f, y + 7f, 175, paintAllCustomersHeaderText, Layout.Alignment.ALIGN_NORMAL)
        drawArabicText(canvas, context.getString(R.string.pdf_col_primary_balance), 230f, y + 7f, 130, paintAllCustomersHeaderText, Layout.Alignment.ALIGN_CENTER)
        drawArabicText(canvas, context.getString(R.string.pdf_col_other_currencies), 105f, y + 7f, 125, paintAllCustomersHeaderText, Layout.Alignment.ALIGN_CENTER)
        drawArabicText(canvas, context.getString(R.string.pdf_col_status), 25f, y + 7f, 80, paintAllCustomersHeaderText, Layout.Alignment.ALIGN_CENTER)
    }

    fun drawSingleTransactionRow(
        canvas: Canvas,
        context: Context,
        index: Int,
        pt: ProcessedTransaction,
        currentY: Float,
        rowHeight: Float,
        runningBal: BigDecimal
    ) = PdfRowRenderer.drawSingleTransactionRow(canvas, context, index, pt, currentY, rowHeight, runningBal)

    fun drawCustomerSummaryRow(
        canvas: Canvas,
        context: Context,
        index: Int,
        c: CustomerUiState,
        currentY: Float,
        rowHeight: Float,
        currencySymbol: String
    ) = PdfRowRenderer.drawCustomerSummaryRow(canvas, context, index, c, currentY, rowHeight, currencySymbol)

    fun drawComprehensiveSummaryCard(
        canvas: Canvas,
        context: Context,
        primaryColorHex: String,
        summary: ComprehensivePdfSummary,
        totalItems: Int,
        currencySymbol: String
    ) = PdfRowRenderer.drawComprehensiveSummaryCard(canvas, context, primaryColorHex, summary, totalItems, currencySymbol)

    fun drawCustomerStatementSheet(
        canvas: Canvas?,
        context: Context,
        customer: CustomerUiState,
        summary: SingleCustomerPdfSummary,
        startY: Float,
        primaryColorHex: String,
        currencySymbol: String,
        isDryRun: Boolean = false,
        includeCustomerHeaderBanner: Boolean = true,
        onPageBreakNeeded: ((newHeader: Boolean) -> Canvas?)? = null
    ): Float {
        var workingY = startY
        var currentCanvas = canvas

        // 1. Customer Header Banner (Optional)
        if (includeCustomerHeaderBanner) {
            if (workingY > 42f) {
                if (workingY + 100f > 780f) {
                    currentCanvas = onPageBreakNeeded?.invoke(false) ?: currentCanvas
                    workingY = 42f
                } else {
                    workingY += 15f
                }
            }

            if (!isDryRun && currentCanvas != null) {
                val bannerBg = Paint().apply {
                    color = Color.parseColor(primaryColorHex)
                    alpha = 15
                    style = Paint.Style.FILL
                }
                val accentBar = Paint().apply {
                    color = Color.parseColor(primaryColorHex)
                    style = Paint.Style.FILL
                }
                currentCanvas.drawRect(25f, workingY, 570f, workingY + 32f, bannerBg)
                currentCanvas.drawRect(566f, workingY, 570f, workingY + 32f, accentBar)

                val paintBannerText = Paint().apply {
                    color = Color.parseColor(PdfColors.TEXT_DARK)
                    textSize = 9.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                val bannerText = context.getString(
                    R.string.pdf_customer_banner_text,
                    customer.name,
                    customer.phone.ifEmpty { "-" }
                )
                drawArabicText(currentCanvas, bannerText, 25f, workingY + 9f, 545, paintBannerText, Layout.Alignment.ALIGN_CENTER)
            }
            workingY += 32f
        }

        // 2. Table Header
        if (!isDryRun && currentCanvas != null) {
            drawTableHeader(currentCanvas, workingY, context, customer.originalCustomer.initialType)
        }
        workingY += 28f

        // 3. Transaction Rows
        val sortedTxs = summary.sortedProcessedTxs
        if (sortedTxs.isEmpty()) {
            if (workingY + 25f > 780f) {
                currentCanvas = onPageBreakNeeded?.invoke(true) ?: currentCanvas
                workingY = 75f
            }
            if (!isDryRun && currentCanvas != null) {
                val paintEmptyText = Paint().apply {
                    color = Color.parseColor(PdfColors.TEXT_MUTED_GREY)
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    isAntiAlias = true
                }
                currentCanvas.drawLine(25f, workingY + 25f, 570f, workingY + 25f, PdfPaints.paintRowDivider)
                drawArabicText(
                    currentCanvas,
                    context.getString(R.string.pdf_no_transactions),
                    25f,
                    workingY + 7f,
                    545,
                    paintEmptyText,
                    Layout.Alignment.ALIGN_CENTER
                )
            }
            workingY += 25f
        } else {
            var runningBal = BigDecimal.ZERO
            var totalCol4 = BigDecimal.ZERO
            var totalCol5 = BigDecimal.ZERO
            val isOwedToThemAccount = customer.originalCustomer.initialType == TransactionType.OWED_TO_THEM.value

            for ((txIndex, pt) in sortedTxs.withIndex()) {
                val tx = pt.tx

                val isCol4 = if (isOwedToThemAccount) {
                    tx.type == TransactionType.OWED_TO_THEM.value || tx.type == TransactionType.PAYMENT_BY_THEM.value
                } else {
                    tx.type == TransactionType.OWED_BY_THEM.value || tx.type == TransactionType.PAYMENT_TO_THEM.value
                }

                if (isCol4) {
                    runningBal = runningBal.add(pt.baseCurrencyAmount)
                    totalCol4 = totalCol4.add(pt.baseCurrencyAmount)
                } else {
                    runningBal = runningBal.subtract(pt.baseCurrencyAmount)
                    totalCol5 = totalCol5.add(pt.baseCurrencyAmount)
                }

                val calculatedHeight = PdfRowRenderer.calculateTransactionRowHeight(
                    context = context,
                    pt = pt,
                    initialType = customer.originalCustomer.initialType,
                    availableWidth = 190
                )

                if (workingY + calculatedHeight > 780f) {
                    currentCanvas = onPageBreakNeeded?.invoke(true) ?: currentCanvas
                    workingY = 75f
                }

                if (!isDryRun && currentCanvas != null) {
                    PdfRowRenderer.drawSingleTransactionRow(
                        currentCanvas,
                        context,
                        txIndex,
                        pt,
                        workingY,
                        calculatedHeight,
                        runningBal,
                        customer.originalCustomer.initialType
                    )
                }
                workingY += calculatedHeight
            }

            // 4. Totals & Final Net Banner
            val extraSummaryHeight = 60f + (if (summary.uncalculatedForeignSums.isNotEmpty()) 24f + summary.uncalculatedForeignSums.size * 20f else 0f)
            if (workingY + extraSummaryHeight > 780f) {
                currentCanvas = onPageBreakNeeded?.invoke(false) ?: currentCanvas
                workingY = 42f
            }

            if (!isDryRun && currentCanvas != null) {
                workingY = PdfRowRenderer.drawTotalsRow(
                    currentCanvas,
                    context,
                    workingY,
                    totalCol4,
                    totalCol5,
                    currencySymbol,
                    customer.originalCustomer.initialType
                )
                workingY += 4f
                val netBalance = totalCol4.subtract(totalCol5)
                workingY = PdfRowRenderer.drawFinalNetBanner(
                    currentCanvas,
                    context,
                    workingY,
                    netBalance,
                    currencySymbol,
                    customer.originalCustomer.initialType
                )
                workingY = PdfRowRenderer.drawForeignCurrenciesSummary(
                    currentCanvas,
                    context,
                    workingY,
                    summary.uncalculatedForeignSums,
                    currencySymbol
                )
            } else {
                workingY += 25f + 4f + 30f + 8f
                if (summary.uncalculatedForeignSums.isNotEmpty()) {
                    workingY += 4f + 24f + (summary.uncalculatedForeignSums.size * 20f)
                }
            }
        }

        return workingY
    }
}
