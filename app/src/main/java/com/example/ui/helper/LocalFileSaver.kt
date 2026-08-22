package com.example.ui.helper

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.R
import java.io.File

object LocalFileSaver {

    /**
     * Saves a cached file to the device's public Downloads directory.
     * Uses MediaStore API on Android 10+ (API 29+) to avoid requiring runtime storage permissions.
     * Uses standard file copy on Android 9 and below.
     *
     * @param context Android context
     * @param cachedFile The temporary file in cacheDir
     * @param mimeType MIME type of the file (e.g. "application/pdf", "text/csv")
     * @param displayName Desired file name (e.g. "statement_John_123.pdf")
     * @return Boolean indicating success
     */
    fun saveFileToPublicDownloads(
        context: Context,
        cachedFile: File,
        mimeType: String,
        displayName: String
    ): Boolean {
        if (!cachedFile.exists() || cachedFile.length() == 0L) {
            return false
        }
        return try {
            val resolver = context.contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                // Query existing file with the same name to overwrite or resolve uniqueness
                val collectionUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val uri = resolver.insert(collectionUri, contentValues)

                if (uri != null) {
                    resolver.openOutputStream(uri).use { outputStream ->
                        if (outputStream != null) {
                            cachedFile.inputStream().use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    true
                } else {
                    false
                }
            } else {
                // Fallback for Android 9 and below (requires WRITE_EXTERNAL_STORAGE permission, which is declared)
                val targetDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }
                val targetFile = File(targetDir, displayName)
                cachedFile.inputStream().use { inputStream ->
                    targetFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Helper to show toast messages upon saving.
     */
    fun saveAndShowToast(
        context: Context,
        cachedFile: File,
        mimeType: String,
        displayName: String
    ) {
        val success = saveFileToPublicDownloads(context, cachedFile, mimeType, displayName)
        if (success) {
            Toast.makeText(
                context,
                context.getString(R.string.autobackup_notification_title_local) + "\n" + displayName,
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.toast_save_failed),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
