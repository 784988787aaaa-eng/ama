package com.example.data.repository

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * مدير مسارات ومجلدات النسخ الاحتياطي المحلي (Local Backup Directory Manager)
 *
 * المسؤوليات المعمارية:
 * 1. توفير مسارات تخزين متوافقة وآمنة مع متطلبات التخزين المحدود (Scoped Storage).
 * 2. تقسيم النسخ الاحتياطية تلقائياً في مجلدات شهرية (yyyy-MM) لتنظيم الملفات وسهولة تصفحها.
 * 3. استرجاع والبحث عن ملفات النسخ الاحتياطي المشفرة ذات الامتداد (.mzd) بأمان تام.
 */
class BackupDirectoryManager(private val context: Context) {

    companion object {
        private const val TAG = "BackupDirManager"
        private const val MZD_EXTENSION = ".mzd"
        private const val MONTH_DATE_PATTERN = "yyyy-MM"
    }

    fun getBaseBackupDirectory(): File {
        val folderName = context.getString(R.string.backup_folder_name)
        val safeDocsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        val mainDir = File(safeDocsDir, folderName)
        try {
            if (!mainDir.exists()) {
                mainDir.mkdirs()
            }
            if (mainDir.exists()) {
                return mainDir
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating backup directory", e)
        }
        return safeDocsDir
    }

    fun getBackupDirectory(): File {
        val baseDir = getBaseBackupDirectory()
        val sdf = SimpleDateFormat(MONTH_DATE_PATTERN, Locale.US)
        val monthStr = sdf.format(Date())
        val targetDir = File(baseDir, monthStr)
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        return targetDir
    }

    fun getAllMzdFilesRecursively(rootDir: File): List<File> {
        if (!rootDir.exists()) return emptyList()
        return rootDir.walkTopDown().filter { it.isFile && it.name.endsWith(MZD_EXTENSION) }.toList()
    }
}
