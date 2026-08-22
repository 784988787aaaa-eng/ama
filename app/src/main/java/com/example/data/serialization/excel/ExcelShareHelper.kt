package com.example.data.serialization.excel

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.local.entities.HabayebCustomer
import com.example.data.serialization.CsvReportGenerator
import com.example.ui.helper.LocalFileSaver
import com.example.ui.screens.habayeb.utils.CustomerShareHelper
import java.io.File

/**
 * Helper to handle Excel file sharing, local disk export, and direct messaging integrations.
 */
object ExcelShareHelper {
    private const val TAG = "ExcelShareHelper"
    const val MIME_TYPE_EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

    fun handleReportAction(
        context: Context,
        file: File,
        action: CsvReportGenerator.CsvAction,
        customer: HabayebCustomer? = null,
        shareSubject: String? = null
    ) {
        when (action) {
            CsvReportGenerator.CsvAction.SAVE_LOCAL -> {
                LocalFileSaver.saveAndShowToast(
                    context = context,
                    cachedFile = file,
                    mimeType = MIME_TYPE_EXCEL,
                    displayName = file.name
                )
            }
            CsvReportGenerator.CsvAction.WHATSAPP_DIRECT -> {
                if (customer != null) {
                    CustomerShareHelper.triggerWhatsAppDirectFile(
                        context = context,
                        customer = customer,
                        file = file,
                        mimeType = MIME_TYPE_EXCEL
                    )
                } else {
                    triggerShareIntent(context, file, shareSubject ?: file.name)
                }
            }
            CsvReportGenerator.CsvAction.SHARE -> {
                triggerShareIntent(context, file, shareSubject ?: (customer?.name ?: file.name))
            }
        }
    }

    fun triggerShareIntent(context: Context, file: File, titleOrSubject: String) {
        try {
            val authority = "${context.packageName}$FILE_PROVIDER_SUFFIX"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = MIME_TYPE_EXCEL
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.csv_share_subject, titleOrSubject))
                putExtra(Intent.EXTRA_TEXT, context.getString(R.string.csv_share_text, titleOrSubject))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.csv_share_chooser_title)))
            Toast.makeText(context, context.getString(R.string.habayeb_export_csv_success), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share Excel statement", e)
            Toast.makeText(context, context.getString(R.string.habayeb_export_csv_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
        }
    }
}
