package com.example.data.serialization.excel

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.serialization.BusinessProfileLoader
import com.example.ui.helper.HabayebMathHelper
import com.example.ui.state.CustomerUiState
import java.io.File
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Specialized engine responsible for generating all accounts / comprehensive summary Excel (.xlsx) files.
 */
object AllCustomersExcelEngine {
    private const val TAG = "AllCustomersExcel"
    private const val LOCALE_AR = "ar"
    private const val LOCALE_EN = "en"
    private const val FILE_PREFIX_ALL = "all_accounts_"

    private val DATE_FORMATTER_EN = ThreadLocal.withInitial { SimpleDateFormat("yyyy/MM/dd", Locale(LOCALE_EN)) }
    private val TIME_FORMATTER_AR = ThreadLocal.withInitial { SimpleDateFormat("hh:mm a", Locale(LOCALE_AR)) }
    private val DAY_FORMATTER_AR = ThreadLocal.withInitial { SimpleDateFormat("EEEE", Locale(LOCALE_AR)) }

    fun generate(
        context: Context,
        customers: List<CustomerUiState>,
        currencySymbol: String
    ): File? {
        val fileName = "${FILE_PREFIX_ALL}${System.currentTimeMillis() % 100000}.xlsx"
        val file = File(context.cacheDir, fileName)

        try {
            val bizHeader = BusinessProfileLoader.load(context)
            val now = Date()
            val dayName = try { DAY_FORMATTER_AR.get().format(now) } catch (e: Exception) { "" }
            val dateFormatted = try { DATE_FORMATTER_EN.get().format(now) } catch (e: Exception) { "" }
            val timeFormatted = try { TIME_FORMATTER_AR.get().format(now) } catch (e: Exception) { "" }
            val docDateText = "$dayName $dateFormatted"

            var totalOwedByThem = BigDecimal.ZERO
            var totalOwedToThem = BigDecimal.ZERO
            val foreignSumsMap = mutableMapOf<String, BigDecimal>()

            customers.forEach { c ->
                val bdVal = c.defaultCurrencyTotal
                if (bdVal.compareTo(BigDecimal.ZERO) > 0) {
                    totalOwedByThem = totalOwedByThem.add(bdVal)
                } else if (bdVal.compareTo(BigDecimal.ZERO) < 0) {
                    totalOwedToThem = totalOwedToThem.add(bdVal.abs())
                }
                c.foreignDebts.forEach { (curr, valBd) ->
                    if (valBd.compareTo(BigDecimal.ZERO) != 0) {
                        foreignSumsMap[curr] = (foreignSumsMap[curr] ?: BigDecimal.ZERO).add(valBd)
                    }
                }
            }
            val grandNetBalance = totalOwedByThem.subtract(totalOwedToThem)

            val columns = listOf(
                XlsxOpenXmlBuilder.SheetColumn(1, 1, 6.0),   // م
                XlsxOpenXmlBuilder.SheetColumn(2, 2, 38.0),  // الحساب / الهاتف
                XlsxOpenXmlBuilder.SheetColumn(3, 3, 22.0),  // الرصيد الأساسي
                XlsxOpenXmlBuilder.SheetColumn(4, 4, 24.0),  // العملات الأخرى
                XlsxOpenXmlBuilder.SheetColumn(5, 5, 18.0)   // الحالة
            )

            val rowsList = mutableListOf<XlsxOpenXmlBuilder.Row>()
            val mergesList = mutableListOf<XlsxOpenXmlBuilder.MergeRange>()

            // 1. Title Row
            val rTitle = XlsxOpenXmlBuilder.Row(1, ht = 32)
            rTitle.cell(0, context.getString(R.string.excel_all_title), 15)
            rowsList.add(rTitle)
            mergesList.add(XlsxOpenXmlBuilder.MergeRange("A1:E1"))

            // 2. Biz info
            val rBiz = XlsxOpenXmlBuilder.Row(2, ht = 22)
            rBiz.cell(0, bizHeader.displayedName + " - " + bizHeader.displayedDesc, 16)
            rBiz.cell(3, context.getString(R.string.excel_date_format, docDateText), 17)
            rowsList.add(rBiz)
            mergesList.add(XlsxOpenXmlBuilder.MergeRange("A2:C2"))
            mergesList.add(XlsxOpenXmlBuilder.MergeRange("D2:E2"))

            // 3. Phone / Certified Badge
            val rBizSub = XlsxOpenXmlBuilder.Row(3, ht = 22)
            rBizSub.cell(0, context.getString(R.string.excel_phone_format, bizHeader.phonesStr), 16)
            rBizSub.cell(3, "", 17)
            rowsList.add(rBizSub)
            mergesList.add(XlsxOpenXmlBuilder.MergeRange("A3:C3"))
            mergesList.add(XlsxOpenXmlBuilder.MergeRange("D3:E3"))

            // Spacer
            rowsList.add(XlsxOpenXmlBuilder.Row(4, ht = 12))

            // 4. Summary card overview
            val rStats = XlsxOpenXmlBuilder.Row(5, ht = 28)
            val statsText = context.getString(
                R.string.excel_all_stats_format,
                HabayebMathHelper.formatSmart(totalOwedByThem),
                currencySymbol,
                HabayebMathHelper.formatSmart(totalOwedToThem),
                HabayebMathHelper.formatSmart(grandNetBalance),
                customers.size
            )
            rStats.cell(0, statsText, 7)
            rowsList.add(rStats)
            mergesList.add(XlsxOpenXmlBuilder.MergeRange("A5:E5"))

            // Spacer
            rowsList.add(XlsxOpenXmlBuilder.Row(6, ht = 12))

            // 5. Table Header Row
            val rHeader = XlsxOpenXmlBuilder.Row(7, ht = 28)
            rHeader.cell(0, context.getString(R.string.excel_col_seq), 1)
            rHeader.cell(1, context.getString(R.string.pdf_col_account_name), 1)
            rHeader.cell(2, context.getString(R.string.pdf_col_primary_balance) + " ($currencySymbol)", 1)
            rHeader.cell(3, context.getString(R.string.pdf_col_other_currencies), 1)
            rHeader.cell(4, context.getString(R.string.pdf_col_status), 1)
            rowsList.add(rHeader)

            // 6. Customers Loop
            var rIdx = 8
            customers.forEachIndexed { index, c ->
                val bdVal = c.defaultCurrencyTotal
                val isPositive = bdVal.compareTo(BigDecimal.ZERO) > 0
                val isNegative = bdVal.compareTo(BigDecimal.ZERO) < 0

                val balanceStyle = when {
                    isPositive -> 2  // Red
                    isNegative -> 3  // Green
                    else -> 4        // Normal Gray/Center
                }

                val statusText = when {
                    isPositive -> context.getString(R.string.pdf_status_owed_word)
                    isNegative -> context.getString(R.string.pdf_status_to_him_word)
                    else -> context.getString(R.string.pdf_status_balanced_word)
                }

                val foreignList = c.foreignDebts.filter { it.value.compareTo(BigDecimal.ZERO) != 0 }
                val foreignStr = if (foreignList.isEmpty()) "-" else foreignList.entries.joinToString("  |  ") { (curr, bd) ->
                    val formatted = HabayebMathHelper.formatSmart(bd.abs())
                    val prefix = if (bd.compareTo(BigDecimal.ZERO) > 0) "+" else "-"
                    "$prefix$formatted $curr"
                }

                val phoneVal = c.phone.ifEmpty { "-" }
                val fullAccountText = context.getString(R.string.excel_account_phone_format, c.name, phoneVal)

                val rRow = XlsxOpenXmlBuilder.Row(rIdx, ht = 24)
                rRow.cell(0, index + 1, 6)
                rRow.cell(1, fullAccountText, 5) // bold text aligned right
                rRow.cell(2, bdVal.abs(), balanceStyle)
                rRow.cell(3, foreignStr, 6)
                rRow.cell(4, statusText, balanceStyle)
                rowsList.add(rRow)
                rIdx++
            }

            // Totals Row
            val rTotals = XlsxOpenXmlBuilder.Row(rIdx, ht = 28)
            rTotals.cell(0, context.getString(R.string.excel_totals_icon, context.getString(R.string.pdf_summary_independent_totals)), 11)
            rTotals.cell(2, grandNetBalance.abs(), 14)
            rTotals.cell(3, "-", 14)
            rTotals.cell(4, "-", 14)
            rowsList.add(rTotals)
            mergesList.add(XlsxOpenXmlBuilder.MergeRange("A$rIdx:B$rIdx"))
            rIdx++

            // Spacer
            rowsList.add(XlsxOpenXmlBuilder.Row(rIdx, ht = 16))
            rIdx++

            // Certified Signature Footer
            val rFooter = XlsxOpenXmlBuilder.Row(rIdx, ht = 24)
            rFooter.cell(0, context.getString(R.string.excel_footer_certified_icon, context.getString(R.string.pdf_footer_certified)), 17)
            rFooter.cell(3, context.getString(R.string.excel_footer_signature), 16)
            rowsList.add(rFooter)
            mergesList.add(XlsxOpenXmlBuilder.MergeRange("A$rIdx:C$rIdx"))
            mergesList.add(XlsxOpenXmlBuilder.MergeRange("D$rIdx:E$rIdx"))

            XlsxOpenXmlBuilder.buildXlsxFile(
                sheetName = context.getString(R.string.excel_sheet_all),
                columns = columns,
                rows = rowsList,
                merges = mergesList,
                file = file
            )
            return file
        } catch (e: Exception) {
            Log.e(TAG, "Error writing XLSX All Customers file", e)
            return null
        }
    }
}
