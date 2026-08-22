package com.example.data.serialization.pdf

import android.content.Context
import android.graphics.Canvas
import com.example.domain.model.TransactionType
import com.example.ui.state.CustomerUiState
import java.math.BigDecimal

/**
 * Unified Facade for rendering PDF rows, summaries, banners, and booklet tables.
 * Delegates actual drawing logic to specialized renderers for optimal modularity and memory efficiency.
 */
object PdfRowRenderer {

    // -------------------------------------------------------------------------
    // 1. Single Customer Transaction Rows & Details
    // -------------------------------------------------------------------------

    fun buildTransactionDescriptionText(
        context: Context,
        pt: ProcessedTransaction,
        initialType: String = TransactionType.OWED_BY_THEM.value
    ): String = PdfTransactionRowRenderer.buildTransactionDescriptionText(context, pt, initialType)

    fun calculateTransactionRowHeight(
        context: Context,
        pt: ProcessedTransaction,
        initialType: String = TransactionType.OWED_BY_THEM.value,
        availableWidth: Int = 190
    ): Float = PdfTransactionRowRenderer.calculateTransactionRowHeight(context, pt, initialType, availableWidth)

    fun drawSingleTransactionRow(
        canvas: Canvas,
        context: Context,
        index: Int,
        pt: ProcessedTransaction,
        currentY: Float,
        rowHeight: Float,
        runningBal: BigDecimal,
        initialType: String = TransactionType.OWED_BY_THEM.value
    ) = PdfTransactionRowRenderer.drawSingleTransactionRow(
        canvas, context, index, pt, currentY, rowHeight, runningBal, initialType
    )

    // -------------------------------------------------------------------------
    // 2. Statement Totals, Net Banner & Foreign Currencies
    // -------------------------------------------------------------------------

    fun drawTotalsRow(
        canvas: Canvas,
        context: Context,
        currentY: Float,
        totalDebts: BigDecimal,
        totalPayments: BigDecimal,
        currencySymbol: String,
        initialType: String = TransactionType.OWED_BY_THEM.value
    ): Float = PdfStatementTotalsRenderer.drawTotalsRow(
        canvas, context, currentY, totalDebts, totalPayments, currencySymbol, initialType
    )

    fun drawFinalNetBanner(
        canvas: Canvas,
        context: Context,
        currentY: Float,
        netBalance: BigDecimal,
        currencySymbol: String,
        initialType: String = TransactionType.OWED_BY_THEM.value
    ): Float = PdfStatementTotalsRenderer.drawFinalNetBanner(
        canvas, context, currentY, netBalance, currencySymbol, initialType
    )

    fun drawForeignCurrenciesSummary(
        canvas: Canvas,
        context: Context,
        currentY: Float,
        uncalculatedForeignSums: Map<String, BigDecimal>,
        currencySymbol: String
    ): Float = PdfStatementTotalsRenderer.drawForeignCurrenciesSummary(
        canvas, context, currentY, uncalculatedForeignSums, currencySymbol
    )

    // -------------------------------------------------------------------------
    // 3. Customer Summary Rows (Comprehensive & All-Customers Report)
    // -------------------------------------------------------------------------

    fun calculateCustomerSummaryRowHeight(
        context: Context,
        c: CustomerUiState,
        nameWidth: Int = 175,
        foreignWidth: Int = 125
    ): Float = PdfCustomerSummaryRenderer.calculateCustomerSummaryRowHeight(context, c, nameWidth, foreignWidth)

    fun drawCustomerSummaryRow(
        canvas: Canvas,
        context: Context,
        index: Int,
        c: CustomerUiState,
        currentY: Float,
        rowHeight: Float,
        currencySymbol: String
    ) = PdfCustomerSummaryRenderer.drawCustomerSummaryRow(
        canvas, context, index, c, currentY, rowHeight, currencySymbol
    )

    // -------------------------------------------------------------------------
    // 4. Booklet Index Table Header & Rows
    // -------------------------------------------------------------------------

    fun drawBookletIndexHeader(canvas: Canvas, y: Float, context: Context) =
        PdfCustomerSummaryRenderer.drawBookletIndexHeader(canvas, y, context)

    fun calculateBookletIndexRowHeight(customer: CustomerUiState, availableWidth: Int = 225): Float =
        PdfCustomerSummaryRenderer.calculateBookletIndexRowHeight(customer, availableWidth)

    fun drawBookletIndexRow(
        canvas: Canvas,
        context: Context,
        index: Int,
        customer: CustomerUiState,
        currentY: Float,
        rowHeight: Float,
        currencySymbol: String
    ) = PdfCustomerSummaryRenderer.drawBookletIndexRow(
        canvas, context, index, customer, currentY, rowHeight, currencySymbol
    )

    // -------------------------------------------------------------------------
    // 5. Comprehensive Summary Card
    // -------------------------------------------------------------------------

    fun drawComprehensiveSummaryCard(
        canvas: Canvas,
        context: Context,
        primaryColorHex: String,
        summary: ComprehensivePdfSummary,
        totalItems: Int,
        currencySymbol: String,
        startY: Float = 98f
    ) = PdfCustomerSummaryRenderer.drawComprehensiveSummaryCard(
        canvas, context, primaryColorHex, summary, totalItems, currencySymbol, startY
    )
}
