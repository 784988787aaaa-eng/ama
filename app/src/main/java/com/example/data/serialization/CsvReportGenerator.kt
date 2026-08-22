package com.example.data.serialization

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.R
import com.example.data.local.entities.HabayebCustomer
import com.example.data.local.entities.HabayebTransaction
import com.example.data.serialization.excel.AllCustomersExcelEngine
import com.example.data.serialization.excel.ExcelShareHelper
import com.example.data.serialization.excel.SingleCustomerExcelEngine
import com.example.ui.state.CustomerUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Enterprise-grade OpenXML (.xlsx) report generator Facade for financial statements.
 * Highly optimized for mobile devices (Microsoft Excel Mobile, Google Sheets, WPS Office, Apple Numbers).
 * Features native RTL sheet alignment, custom cell formats (fonts, fills, borders),
 * color-coded debit/credit cells, actual numeric cell types (for direct spreadsheet summing),
 * and dynamic column sizing, delivering a polished look identical to high-end PDFs.
 */
object CsvReportGenerator {
    private const val TAG = "CsvReportGenerator"
    private const val DEFAULT_EXCHANGE_RATES_JSON = "{}"
    const val MIME_TYPE_EXCEL = ExcelShareHelper.MIME_TYPE_EXCEL

    enum class CsvAction {
        SHARE,
        SAVE_LOCAL,
        WHATSAPP_DIRECT;

        companion object {
            fun from(action: String): CsvAction {
                return values().find { it.name.equals(action, ignoreCase = true) } ?: SHARE
            }
        }
    }

    fun generateAndShareCsvReport(
        context: Context,
        scope: CoroutineScope,
        customer: HabayebCustomer,
        transactions: List<HabayebTransaction>,
        currencySymbol: String,
        exchangeRatesJson: String = DEFAULT_EXCHANGE_RATES_JSON,
        onFinished: () -> Unit = {}
    ) {
        generateAndHandleCsvReportAsync(
            context = context,
            scope = scope,
            customer = customer,
            transactions = transactions,
            currencySymbol = currencySymbol,
            exchangeRatesJson = exchangeRatesJson,
            action = CsvAction.SHARE,
            onFinished = onFinished
        )
    }

    fun generateAndHandleCsvReportAsync(
        context: Context,
        scope: CoroutineScope,
        customer: HabayebCustomer,
        transactions: List<HabayebTransaction>,
        currencySymbol: String,
        exchangeRatesJson: String = DEFAULT_EXCHANGE_RATES_JSON,
        action: CsvAction,
        onFinished: () -> Unit = {}
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val file = SingleCustomerExcelEngine.generate(
                    context = context,
                    customer = customer,
                    transactions = transactions,
                    currencySymbol = currencySymbol,
                    exchangeRatesJson = exchangeRatesJson
                )
                withContext(Dispatchers.Main) {
                    if (file != null) {
                        ExcelShareHelper.handleReportAction(
                            context = context,
                            file = file,
                            action = action,
                            customer = customer
                        )
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.habayeb_export_csv_failed, context.getString(R.string.csv_error_creating_file)),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating Excel statement", e)
            } finally {
                withContext(Dispatchers.Main) {
                    onFinished()
                }
            }
        }
    }

    fun generateAndHandleAllCustomersExcelReportAsync(
        context: Context,
        scope: CoroutineScope,
        customers: List<CustomerUiState>,
        currencySymbol: String,
        action: CsvAction = CsvAction.SHARE,
        onFinished: () -> Unit = {}
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val file = AllCustomersExcelEngine.generate(
                    context = context,
                    customers = customers,
                    currencySymbol = currencySymbol
                )
                withContext(Dispatchers.Main) {
                    if (file != null) {
                        ExcelShareHelper.handleReportAction(
                            context = context,
                            file = file,
                            action = action,
                            shareSubject = context.getString(R.string.pdf_comprehensive_report_title)
                        )
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.habayeb_export_csv_failed, context.getString(R.string.csv_error_creating_file)),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating All Customers Excel", e)
            } finally {
                withContext(Dispatchers.Main) {
                    onFinished()
                }
            }
        }
    }
}
