package com.example.data.serialization.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import java.io.File

object PdfIntentLauncher {
    private const val TAG = "PdfIntentLauncher"
    private const val MIME_TYPE_PDF = "application/pdf"
    private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

    fun triggerShareOrViewIntent(context: Context, file: File?, action: PdfAction) {
        if (file == null) {
            Toast.makeText(
                context,
                context.getString(R.string.habayeb_toast_pdf_export_failed, context.getString(R.string.csv_error_creating_file)),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        try {
            val authority = "${context.packageName}$FILE_PROVIDER_SUFFIX"
            val uri = FileProvider.getUriForFile(context, authority, file)

            when (action) {
                PdfAction.SHARE -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = MIME_TYPE_PDF
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.pdf_chooser_title)))
                }
                PdfAction.VIEW -> {
                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, MIME_TYPE_PDF)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(viewIntent)
                }
                else -> {}
            }
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.habayeb_toast_pdf_export_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
        }
    }

    fun recycleBitmapsSafely(rawBitmap: Bitmap?, scaledLogo: Bitmap?) {
        try {
            if (rawBitmap != null && !rawBitmap.isRecycled) {
                if (scaledLogo != null && scaledLogo != rawBitmap && !scaledLogo.isRecycled) {
                    scaledLogo.recycle()
                }
                rawBitmap.recycle()
            } else if (scaledLogo != null && !scaledLogo.isRecycled) {
                scaledLogo.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error recycling bitmaps", e)
        }
    }
}
