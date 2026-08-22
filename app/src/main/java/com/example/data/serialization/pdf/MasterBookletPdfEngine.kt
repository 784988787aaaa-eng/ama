package com.example.data.serialization.pdf

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.util.Log
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.repository.FinanceRepository
import com.example.data.serialization.BusinessProfileLoader
import com.example.ui.state.CustomerUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import kotlin.coroutines.coroutineContext

data class BusinessProfileData(
    val name: String,
    val desc: String,
    val phonesStr: String,
    val scaledLogo: Bitmap?,
    val hasLogo: Boolean,
    val logoW: Float,
    val logoH: Float
)

data class PdfReportMetaData(
    val docDateText: String,
    val docTimeText: String,
    val currencySymbol: String,
    val primaryColorHex: String
)

object MasterBookletPdfEngine {
    private const val TAG = "MasterBookletPdfEngine"
    private const val PREFS_BUSINESS_PROFILE = "business_profile"
    private const val PREF_BIZ_NAME = "biz_name"
    private const val PREF_BIZ_DESC = "biz_desc"
    private const val PREF_BIZ_LOGO_PATH = "biz_logo_path"
    private const val PREF_BIZ_PHONES = "biz_phones"
    private const val DEFAULT_PHONES_JSON = "[]"
    private const val PHONE_DELIMITER = " - "

    suspend fun generateBookletPdfAsync(
        context: Context,
        allCustomers: List<CustomerUiState>,
        selectedIds: List<String>,
        onlySelected: Boolean,
        currencySymbol: String,
        primaryColorHex: String = PdfColors.PRIMARY_EMERALD,
        onProgress: (processed: Int, total: Int) -> Unit,
        onFinished: (File?) -> Unit
    ) = withContext(Dispatchers.IO) {
        var scaledLogoToRecycle: android.graphics.Bitmap? = null
        var rawBitmapToRecycle: android.graphics.Bitmap? = null
        try {
            val targetCustomers = if (onlySelected && selectedIds.isNotEmpty()) {
                allCustomers.filter { selectedIds.contains(it.id) }
            } else {
                allCustomers
            }

            val totalCustomers = targetCustomers.size
            if (totalCustomers == 0) {
                onFinished(null)
                return@withContext
            }

            // Load business profile from shared BusinessProfileLoader
            val header = BusinessProfileLoader.load(context)
            scaledLogoToRecycle = header.scaledLogo
            rawBitmapToRecycle = header.rawBitmap

            val now = Date()
            val docDateText = context.getString(R.string.pdf_doc_date, PdfPageRenderer.formatDayAr(now), PdfPageRenderer.formatDateEn(now))
            val docTimeText = context.getString(R.string.pdf_doc_time, PdfPageRenderer.formatTimeAr(now))

            val businessProfile = BusinessProfileData(
                name = header.displayedName,
                desc = header.displayedDesc,
                phonesStr = header.phonesStr,
                scaledLogo = header.scaledLogo,
                hasLogo = header.hasLogo,
                logoW = header.logoW,
                logoH = header.logoH
            )

            val reportMetaData = PdfReportMetaData(
                docDateText = docDateText,
                docTimeText = docTimeText,
                currencySymbol = currencySymbol,
                primaryColorHex = primaryColorHex
            )

            // Initialize DB & Repository inside for streaming
            val database = AppDatabase.getDatabase(context)
            val repository = FinanceRepository(database, context.applicationContext as Application)

            // Memory Cache Map to avoid querying database twice during dry run and real pass
            val txCacheMap = mutableMapOf<String, List<com.example.data.local.entities.HabayebTransaction>>()

            // Let's compute Overall System Balances
            val summary = PdfReportCalculator.calculateComprehensiveReport(targetCustomers)

            // First Pass: DRY RUN to compute total pages accurately
            var totalPagesInDryRun = 1
            run {
                val dryDoc = PdfDocument()
                try {
                    val drawingCtx = BookletDrawingContext(
                        context = context,
                        pdfDocument = dryDoc,
                        isDryRun = true,
                        totalPagesInDryRun = 1,
                        businessProfile = businessProfile,
                        reportMetaData = reportMetaData
                    )

                    // Start detailed ledger sheets directly below the business header on Page 1
                    drawingCtx.currentY = 78f

                    // Render detail sections for each customer in chunks
                    val customerChunks = targetCustomers.chunked(50)
                    var processedCount = 0
                    for (chunk in customerChunks) {
                        coroutineContext.ensureActive()
                        for (customer in chunk) {
                            coroutineContext.ensureActive()
                            val transactions = txCacheMap.getOrPut(customer.id) {
                                repository.getTransactionsForCustomerDirect(customer.id)
                            }
                            val singleSummary = PdfReportCalculator.calculateSingleCustomerReport(transactions, currencySymbol)
                            drawCustomerLedgerSheet(drawingCtx, customer, singleSummary)
                            processedCount++
                        }
                    }
                    drawingCtx.finishLastPage()
                    totalPagesInDryRun = drawingCtx.currentPageNumber - 1
                } finally {
                    dryDoc.close()
                }
            }

            coroutineContext.ensureActive()

            // Second Pass: REAL PASS
            val pdfDocument = PdfDocument()
            val outputFile = try {
                val drawingCtx = BookletDrawingContext(
                    context = context,
                    pdfDocument = pdfDocument,
                    isDryRun = false,
                    totalPagesInDryRun = totalPagesInDryRun,
                    businessProfile = businessProfile,
                    reportMetaData = reportMetaData
                )

                // Draw Business Header on Page 1
                val canvas = drawingCtx.currentPageCanvas
                if (canvas != null) {
                    PdfPageRenderer.drawBusinessHeader(
                        canvas, drawingCtx.displayedName, drawingCtx.displayedDesc, drawingCtx.phonesStr,
                        drawingCtx.hasLogo, drawingCtx.scaledLogo, drawingCtx.logoW, drawingCtx.logoH, drawingCtx.docDateText, drawingCtx.docTimeText
                    )
                }
                drawingCtx.currentY = 78f

                // 2. Customers detailed ledger sheets in chunks
                val customerChunks = targetCustomers.chunked(50)
                var processedCount = 0
                for (chunk in customerChunks) {
                    coroutineContext.ensureActive()
                    for (customer in chunk) {
                        coroutineContext.ensureActive()
                        val transactions = txCacheMap[customer.id]
                            ?: repository.getTransactionsForCustomerDirect(customer.id)
                        val singleSummary = PdfReportCalculator.calculateSingleCustomerReport(transactions, currencySymbol)
                        drawCustomerLedgerSheet(drawingCtx, customer, singleSummary)
                        processedCount++
                        onProgress(processedCount, totalCustomers)
                    }
                }

                drawingCtx.finishLastPage()

                // Save PDF to cache file
                val outputDir = File(context.cacheDir, "pdf_reports")
                if (!outputDir.exists()) outputDir.mkdirs()
                val file = File(outputDir, "MasterBookletReport_${System.currentTimeMillis()}.pdf")
                FileOutputStream(file).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                    outputStream.flush()
                }
                file
            } finally {
                pdfDocument.close()
            }

            onFinished(outputFile)

        } catch (e: Exception) {
            Log.e(TAG, "Error generating booklet PDF", e)
            onFinished(null)
        } finally {
            try {
                if (rawBitmapToRecycle != null && !rawBitmapToRecycle.isRecycled) {
                    if (scaledLogoToRecycle != null && scaledLogoToRecycle != rawBitmapToRecycle && !scaledLogoToRecycle.isRecycled) {
                        scaledLogoToRecycle.recycle()
                    }
                    rawBitmapToRecycle.recycle()
                } else if (scaledLogoToRecycle != null && !scaledLogoToRecycle.isRecycled) {
                    scaledLogoToRecycle.recycle()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error recycling bitmaps", e)
            }
        }
    }

    private fun drawCoverAndIndexDryRun(ctx: BookletDrawingContext, customers: List<CustomerUiState>) {
        // Business header space
        ctx.currentY = 78f
        // Title space
        ctx.currentY += 22f
        // Subtitle space
        ctx.currentY += 18f

        // Index Title
        ctx.currentY += 18f
        // Index Header
        ctx.currentY += 24f

        // Index Rows
        customers.forEachIndexed { _, customer ->
            val rowHeight = PdfRowRenderer.calculateBookletIndexRowHeight(customer)
            if (ctx.currentY + rowHeight > 780f) {
                ctx.startNewPage()
                // Subsequent page header + index header space on new page
                ctx.currentY = 69f
            }
            ctx.currentY += rowHeight
        }
    }

    private fun drawCoverAndIndexReal(
        ctx: BookletDrawingContext,
        customers: List<CustomerUiState>,
        summary: com.example.data.serialization.pdf.ComprehensivePdfSummary
    ) {
        val context = ctx.context
        val canvas = ctx.currentPageCanvas ?: return

        // 1. Draw Business Header
        PdfPageRenderer.drawBusinessHeader(
            canvas, ctx.displayedName, ctx.displayedDesc, ctx.phonesStr,
            ctx.hasLogo, ctx.scaledLogo, ctx.logoW, ctx.logoH, ctx.docDateText, ctx.docTimeText
        )
        ctx.currentY = 78f

        // 2. Draw Title
        val paintTitle = Paint().apply {
            color = Color.parseColor(ctx.primaryColorHex)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        PdfDrawingUtils.drawArabicText(
            canvas, context.getString(R.string.pdf_booklet_title),
            25f, ctx.currentY, 545, paintTitle, Layout.Alignment.ALIGN_CENTER
        )
        ctx.currentY += 22f

        // 3. Draw Subtitle
        val paintSub = Paint().apply {
            color = Color.parseColor(PdfColors.TEXT_MEDIUM)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        PdfDrawingUtils.drawArabicText(
            canvas, context.getString(R.string.pdf_booklet_subtitle),
            25f, ctx.currentY, 545, paintSub, Layout.Alignment.ALIGN_CENTER
        )
        ctx.currentY += 18f

        // 4. Draw Index Title
        val paintIndexTitle = Paint().apply {
            color = Color.parseColor(ctx.primaryColorHex)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        PdfDrawingUtils.drawArabicText(
            canvas, context.getString(R.string.pdf_index_title),
            25f, ctx.currentY, 545, paintIndexTitle, Layout.Alignment.ALIGN_NORMAL
        )
        ctx.currentY += 18f

        // 5. Draw Index Table Header
        PdfRowRenderer.drawBookletIndexHeader(canvas, ctx.currentY, context)
        ctx.currentY += 24f

        // 6. Draw Index Rows
        customers.forEachIndexed { index, customer ->
            val rowHeight = PdfRowRenderer.calculateBookletIndexRowHeight(customer)
            if (ctx.currentY + rowHeight > 780f) {
                ctx.startNewPage()
                val nextCanvas = ctx.currentPageCanvas ?: return@forEachIndexed
                // Redraw table header on next page
                PdfRowRenderer.drawBookletIndexHeader(nextCanvas, 45f, context)
                ctx.currentY = 69f
            }

            val currentCanvas = ctx.currentPageCanvas ?: return@forEachIndexed
            PdfRowRenderer.drawBookletIndexRow(
                canvas = currentCanvas,
                context = context,
                index = index,
                customer = customer,
                currentY = ctx.currentY,
                rowHeight = rowHeight,
                currencySymbol = ctx.currencySymbol
            )

            ctx.currentY += rowHeight
        }
    }

    private fun drawCustomerLedgerSheet(
        ctx: BookletDrawingContext,
        customer: CustomerUiState,
        summary: com.example.data.serialization.pdf.SingleCustomerPdfSummary
    ) {
        ctx.currentY = PdfPageRenderer.drawCustomerStatementSheet(
            canvas = ctx.currentPageCanvas,
            context = ctx.context,
            customer = customer,
            summary = summary,
            startY = ctx.currentY,
            primaryColorHex = ctx.primaryColorHex,
            currencySymbol = ctx.currencySymbol,
            isDryRun = ctx.isDryRun,
            includeCustomerHeaderBanner = true,
            onPageBreakNeeded = { newHeader ->
                ctx.startNewPage()
                if (newHeader && !ctx.isDryRun) {
                    ctx.currentPageCanvas?.let { currentCanvas ->
                        PdfPageRenderer.drawSubsequentPageHeader(currentCanvas, customer.name, ctx.primaryColorHex, ctx.context)
                        PdfPageRenderer.drawTableHeader(currentCanvas, 45f, ctx.context, customer.originalCustomer.initialType)
                    }
                }
                ctx.currentPageCanvas
            }
        )
    }
}

class BookletDrawingContext(
    val context: Context,
    val pdfDocument: PdfDocument,
    val isDryRun: Boolean,
    val totalPagesInDryRun: Int,
    val businessProfile: BusinessProfileData,
    val reportMetaData: PdfReportMetaData
) {
    val displayedName: String get() = businessProfile.name
    val displayedDesc: String get() = businessProfile.desc
    val phonesStr: String get() = businessProfile.phonesStr
    val scaledLogo: Bitmap? get() = businessProfile.scaledLogo
    val hasLogo: Boolean get() = businessProfile.hasLogo
    val logoW: Float get() = businessProfile.logoW
    val logoH: Float get() = businessProfile.logoH

    val docDateText: String get() = reportMetaData.docDateText
    val docTimeText: String get() = reportMetaData.docTimeText
    val currencySymbol: String get() = reportMetaData.currencySymbol
    val primaryColorHex: String get() = reportMetaData.primaryColorHex
    var currentPageNumber = 1
    var currentY = 42f
    var currentPageCanvas: Canvas? = null
    var currentPageObject: PdfDocument.Page? = null

    init {
        startNewPage()
    }

    fun startNewPage() {
        if (!isDryRun) {
            drawFooterOnCurrentPage()
            currentPageObject?.let { pdfDocument.finishPage(it) }

            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            currentPageCanvas = page.canvas
            currentPageObject = page
        }
        currentPageNumber++
        currentY = 42f
    }

    private fun drawFooterOnCurrentPage() {
        val canvas = currentPageCanvas ?: return
        PdfPageRenderer.drawFooter(
            canvas,
            currentPageNumber - 1,
            totalPagesInDryRun,
            primaryColorHex,
            context
        )
    }

    fun finishLastPage() {
        if (!isDryRun) {
            drawFooterOnCurrentPage()
            currentPageObject?.let { pdfDocument.finishPage(it) }
        }
    }
}
