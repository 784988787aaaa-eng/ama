package com.example.data.serialization.excel

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.entities.HabayebCustomer
import com.example.data.local.entities.HabayebTransaction
import com.example.data.serialization.BusinessProfileLoader
import com.example.data.serialization.pdf.PdfReportCalculator
import com.example.domain.model.TransactionType
import com.example.ui.helper.HabayebMathHelper
import com.example.ui.screens.habayeb.utils.CurrencyConfig
import java.io.File
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Specialized engine responsible for generating customer statement Excel (.xlsx) files.
 */
object SingleCustomerExcelEngine {
    private const val TAG = "SingleCustomerExcel"
    private const val LOCALE_AR = "ar"
    private const val LOCALE_EN = "en"
    private const val FILE_PREFIX = "statement_"

    private val DATE_FORMATTER_EN = ThreadLocal.withInitial { SimpleDateFormat("yyyy/MM/dd", Locale(LOCALE_EN)) }
    private val TIME_FORMATTER_AR = ThreadLocal.withInitial { SimpleDateFormat("hh:mm a", Locale(LOCALE_AR)) }
    private val DAY_FORMATTER_AR = ThreadLocal.withInitial { SimpleDateFormat("EEEE", Locale(LOCALE_AR)) }

    fun generate(
        context: Context,
        customer: HabayebCustomer,
        transactions: List<HabayebTransaction>,
        currencySymbol: String,
        exchangeRatesJson: String = "{}"
    ): File? {
        val sanitizedName = customer.name.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
        val fileName = "${FILE_PREFIX}${sanitizedName}_${System.currentTimeMillis() % 100000}.xlsx"
        val file = File(context.cacheDir, fileName)

        try {
            val bizHeader = BusinessProfileLoader.load(context)
            val now = Date()
            val dayName = try { DAY_FORMATTER_AR.get().format(now) } catch (e: Exception) { "" }
            val dateFormatted = try { DATE_FORMATTER_EN.get().format(now) } catch (e: Exception) { "" }
            val timeFormatted = try { TIME_FORMATTER_AR.get().format(now) } catch (e: Exception) { "" }
            val docDateText = "$dayName $dateFormatted"

            val isOwedToThemAccount = customer.initialType == TransactionType.OWED_TO_THEM.value
            val col4HeaderText = if (isOwedToThemAccount) context.getString(R.string.pdf_col_owed_to) else context.getString(R.string.pdf_col_owed_by)
            val col5HeaderText = if (isOwedToThemAccount) context.getString(R.string.pdf_col_paid) else context.getString(R.string.pdf_col_received)
            val accountTypeDesc = if (isOwedToThemAccount) context.getString(R.string.excel_type_supplier) else context.getString(R.string.excel_type_customer)

            val summary = PdfReportCalculator.calculateSingleCustomerReport(transactions, currencySymbol)

            val columns = listOf(
                XlsxOpenXmlBuilder.SheetColumn(1, 1, 6.0),   // م (Sequence)
                XlsxOpenXmlBuilder.SheetColumn(2, 2, 16.0),  // التاريخ (Date)
                XlsxOpenXmlBuilder.SheetColumn(3, 3, 44.0),  // البيان والتفاصيل (Desc)
                XlsxOpenXmlBuilder.SheetColumn(4, 4, 16.0),  // مدين (Debit)
                XlsxOpenXmlBuilder.SheetColumn(5, 5, 16.0),  // دائن (Credit)
                XlsxOpenXmlBuilder.SheetColumn(6, 6, 18.0)   // الرصيد (Running balance)
            )

            val rowsList = mutableListOf<XlsxOpenXmlBuilder.Row>()
            val mergesList = mutableListOf<XlsxOpenXmlBuilder.MergeRange>()

            // 1. Header Row
            val rTitle = XlsxOpenXmlBuilder.Row(1, ht = 32)
            rTitle.cell(0, context.getString(R.string.excel_single_title), 15)
            rowsList.add(rTitle)
            mergesList.add(XlsxOpenXmlBuilder.MergeRange("A1:F1"))

            // 2. Biz Profile
            val rBiz = XlsxOpenXmlBuilder.Row(2, ht = 22)
            rBiz.cell(0, bizHeader.displayedName + " - " + bizHeader.displayedDesc, 16)
            rBiz.cell(3, context.getString(R.string.excel_date_format, docDateText), 17)
            rowsList.add(rBiz)
            mergesList.add(XlsxOpenXmlBuilder.MergeRange("A2:C2"))
            mergesList.add(XlsxOpenXmlBuilder.MergeRange("D2:F2"))

            // 3. Sub Biz / Meta
            val rBizSub = XlsxOpenXmlBuilder.Row(3, ht = 22)
            rBizSub.cell(0, context.getString(R.string.excel_phone_format, bizHeader.phonesStr), 16)
            rBizSub.cell(3, "", 17)
            rowsList.add(rBizSub)
            mergesList.add(XlsxOpenXmlBuilder.MergeRange("A3:C3"))
            mergesList.add(XlsxOpenXmlBuilder.MergeRange("D3:F3"))

            // Spacer
            rowsList.add(XlsxOpenXmlBuilder.Row(4, ht = 12))

            // 4. Customer Card Row
            val rCard = XlsxOpenXmlBuilder.Row(5, ht = 28)
            val phoneText = customer.phone.ifEmpty { context.getString(R.string.csv_not_registered) }
            val cardText = context.getString(R.string.excel_account_card_format, customer.name, phoneText, accountTypeDesc)
            rCard.cell(0, cardText, 7)
            rowsList.add(rCard)
            mergesList.add(XlsxOpenXmlBuilder.MergeRange("A5:F5"))

            // Spacer
            rowsList.add(XlsxOpenXmlBuilder.Row(6, ht = 12))

            // 5. Table Headers Row
            val rTableHeader = XlsxOpenXmlBuilder.Row(7, ht = 28)
            rTableHeader.cell(0, context.getString(R.string.excel_col_seq), 1)
            rTableHeader.cell(1, context.getString(R.string.pdf_col_date), 1)
            rTableHeader.cell(2, context.getString(R.string.pdf_col_description), 1)
            rTableHeader.cell(3, col4HeaderText, 1)
            rTableHeader.cell(4, col5HeaderText, 1)
            rTableHeader.cell(5, context.getString(R.string.pdf_col_remaining) + " ($currencySymbol)", 1)
            rowsList.add(rTableHeader)

            // 6. Transactions loop
            var rIdx = 8
            val sortedTxs = summary.sortedProcessedTxs
            if (sortedTxs.isEmpty()) {
                val rEmpty = XlsxOpenXmlBuilder.Row(rIdx, ht = 28)
                rEmpty.cell(0, context.getString(R.string.pdf_no_transactions), 6)
                rowsList.add(rEmpty)
                mergesList.add(XlsxOpenXmlBuilder.MergeRange("A$rIdx:F$rIdx"))
                rIdx++
            } else {
                var runningBal = BigDecimal.ZERO
                sortedTxs.forEachIndexed { index, pt ->
                    val tx = pt.tx
                    val isTxForeign = pt.isTxForeign
                    val hasBaseAmount = pt.baseCurrencyAmount.compareTo(BigDecimal.ZERO) > 0

                    val txType = TransactionType.fromValue(tx.type)
                    val isCol4 = if (isOwedToThemAccount) {
                        txType == TransactionType.OWED_TO_THEM || txType == TransactionType.PAYMENT_BY_THEM
                    } else {
                        txType == TransactionType.OWED_BY_THEM || txType == TransactionType.PAYMENT_TO_THEM
                    }

                    val col4Amount = if (hasBaseAmount && isCol4) pt.baseCurrencyAmount else BigDecimal.ZERO
                    val col5Amount = if (hasBaseAmount && !isCol4) pt.baseCurrencyAmount else BigDecimal.ZERO

                    if (hasBaseAmount) {
                        if (isCol4) {
                            runningBal = runningBal.add(pt.baseCurrencyAmount)
                        } else {
                            runningBal = runningBal.subtract(pt.baseCurrencyAmount)
                        }
                    }

                    val txDate = Date(if (tx.timestamp > 1000000000000L) tx.timestamp else tx.timestamp * 1000)
                    val rowDay = try { DAY_FORMATTER_AR.get().format(txDate) } catch (e: Exception) { "" }
                    val rowDate = try { DATE_FORMATTER_EN.get().format(txDate) } catch (e: Exception) { "" }
                    val fullDateStr = "$rowDay $rowDate"

                    val typeName = when (txType) {
                        TransactionType.OWED_BY_THEM -> context.getString(R.string.pdf_tx_type_owed_by_them)
                        TransactionType.PAYMENT_BY_THEM -> if (isOwedToThemAccount) context.getString(R.string.pdf_tx_type_payment_to_them) else context.getString(R.string.pdf_tx_type_payment_by_them)
                        TransactionType.OWED_TO_THEM -> context.getString(R.string.pdf_tx_type_owed_to_them)
                        TransactionType.PAYMENT_TO_THEM -> context.getString(R.string.pdf_tx_type_payment_to_them)
                        else -> context.getString(R.string.pdf_tx_type_new)
                    }

                    val cleanDetails = CurrencyConfig.getCleanDetails(tx.description)
                    var descText = typeName
                    if (cleanDetails.isNotBlank()) {
                        descText += " - $cleanDetails"
                    }

                    if (tx.isRateCalculated) {
                        val origCurrency = CurrencyConfig.getBySymbol(tx.currencyCode)?.symbol ?: tx.currencyCode
                        val origAmount = if (tx.foreignAmount.compareTo(BigDecimal.ZERO) > 0) tx.foreignAmount else tx.amount
                        val formattedAmount = HabayebMathHelper.formatSmart(origAmount)
                        val formattedRate = HabayebMathHelper.formatRate(tx.exchangeRate)
                        descText += context.getString(R.string.excel_tx_rate_note, formattedAmount, origCurrency, formattedRate)
                    } else if (isTxForeign) {
                        val origCurrency = CurrencyConfig.getBySymbol(tx.currencyCode)?.symbol ?: tx.currencyCode
                        val origAmount = if (tx.foreignAmount.compareTo(BigDecimal.ZERO) > 0) tx.foreignAmount else tx.amount
                        val formattedAmount = HabayebMathHelper.formatSmart(origAmount)
                        descText += context.getString(R.string.excel_tx_foreign_note, formattedAmount, origCurrency)
                    }

                    val rRow = XlsxOpenXmlBuilder.Row(rIdx, ht = 24)
                    rRow.cell(0, index + 1, 6)
                    rRow.cell(1, fullDateStr, 6)
                    rRow.cell(2, descText, 0)

                    if (col4Amount.compareTo(BigDecimal.ZERO) > 0) {
                        rRow.cell(3, col4Amount, 2)
                    } else {
                        rRow.cell(3, "-", 6)
                    }

                    if (col5Amount.compareTo(BigDecimal.ZERO) > 0) {
                        rRow.cell(4, col5Amount, 3)
                    } else {
                        rRow.cell(4, "-", 6)
                    }

                    if (hasBaseAmount) {
                        rRow.cell(5, runningBal, 4)
                    } else {
                        rRow.cell(5, "-", 6)
                    }

                    rowsList.add(rRow)
                    rIdx++
                }

                // Independent Totals
                val rTotals = XlsxOpenXmlBuilder.Row(rIdx, ht = 28)
                rTotals.cell(0, context.getString(R.string.excel_totals_icon, context.getString(R.string.pdf_summary_independent_totals)), 11)
                rTotals.cell(3, summary.totalDebts, 12)
                rTotals.cell(4, summary.totalPayments, 13)
                rTotals.cell(5, currencySymbol, 14)
                rowsList.add(rTotals)
                mergesList.add(XlsxOpenXmlBuilder.MergeRange("A$rIdx:C$rIdx"))
                rIdx++
            }

            // Spacer
            rowsList.add(XlsxOpenXmlBuilder.Row(rIdx, ht = 12))
            rIdx++

            // Net balance Banner
            val rawPositive = summary.calculatedNetDebt.compareTo(BigDecimal.ZERO) > 0
            val rawNegative = summary.calculatedNetDebt.compareTo(BigDecimal.ZERO) < 0
            val isOwedToThemStatus = if (isOwedToThemAccount) rawPositive else rawNegative
            val isOwedByThemStatus = if (isOwedToThemAccount) rawNegative else rawPositive

            val (bannerStyle, statusTitle) = when {
                isOwedByThemStatus -> 8 to context.getString(R.string.pdf_net_banner_owed_by)
                isOwedToThemStatus -> 9 to context.getString(R.string.pdf_net_banner_owed_to)
                else -> 10 to context.getString(R.string.pdf_net_banner_balanced)
            }

            val formattedNetBalance = "${HabayebMathHelper.formatSmart(summary.calculatedNetDebt.abs())} $currencySymbol"

            val rBanner = XlsxOpenXmlBuilder.Row(rIdx, ht = 28)
            rBanner.cell(0, context.getString(R.string.excel_net_banner_format, statusTitle, formattedNetBalance), bannerStyle)
            rowsList.add(rBanner)
            mergesList.add(XlsxOpenXmlBuilder.MergeRange("A$rIdx:F$rIdx"))
            rIdx++

            // Unconverted Foreign summary
            if (summary.uncalculatedForeignSums.isNotEmpty()) {
                rowsList.add(XlsxOpenXmlBuilder.Row(rIdx, ht = 12))
                rIdx++

                val rForeignHeader = XlsxOpenXmlBuilder.Row(rIdx, ht = 24)
                rForeignHeader.cell(0, context.getString(R.string.excel_foreign_icon, context.getString(R.string.pdf_independent_totals_uncalculated)), 19)
                rowsList.add(rForeignHeader)
                mergesList.add(XlsxOpenXmlBuilder.MergeRange("A$rIdx:F$rIdx"))
                rIdx++

                for ((curr, amount) in summary.uncalculatedForeignSums) {
                    val isPositive = amount.compareTo(BigDecimal.ZERO) > 0
                    val isNegative = amount.compareTo(BigDecimal.ZERO) < 0
                    val statusText = if (isPositive) context.getString(R.string.pdf_status_owed_word) else if (isNegative) context.getString(R.string.pdf_status_to_him_word) else context.getString(R.string.pdf_status_balanced_word)
                    val foreignTag = context.getString(R.string.pdf_foreign_currency_tag)
                    val lineStr = context.getString(R.string.excel_foreign_line_format, context.getString(R.string.pdf_total_currency_prefix, curr), HabayebMathHelper.formatSmart(amount.abs()), curr, statusText, foreignTag)

                    val rForeignLine = XlsxOpenXmlBuilder.Row(rIdx, ht = 22)
                    rForeignLine.cell(0, lineStr, 7)
                    rowsList.add(rForeignLine)
                    mergesList.add(XlsxOpenXmlBuilder.MergeRange("A$rIdx:F$rIdx"))
                    rIdx++
                }
            }

            // Certified Signature
            rowsList.add(XlsxOpenXmlBuilder.Row(rIdx, ht = 16))
            rIdx++

            val rFooter = XlsxOpenXmlBuilder.Row(rIdx, ht = 24)
            rFooter.cell(0, context.getString(R.string.excel_footer_certified_icon, context.getString(R.string.pdf_footer_certified)), 17)
            rFooter.cell(3, context.getString(R.string.excel_footer_signature), 16)
            rowsList.add(rFooter)
            mergesList.add(XlsxOpenXmlBuilder.MergeRange("A$rIdx:C$rIdx"))
            mergesList.add(XlsxOpenXmlBuilder.MergeRange("D$rIdx:F$rIdx"))

            XlsxOpenXmlBuilder.buildXlsxFile(
                sheetName = context.getString(R.string.excel_sheet_single),
                columns = columns,
                rows = rowsList,
                merges = mergesList,
                file = file
            )
            return file
        } catch (e: Exception) {
            Log.e(TAG, "Error writing XLSX statement file", e)
            return null
        }
    }
}
