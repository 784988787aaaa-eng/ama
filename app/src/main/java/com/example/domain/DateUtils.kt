package com.example.domain

import android.content.Context
import com.example.domain.formatters.AppDateTimeFormatter

/**
 * Legacy DateUtils facade delegating to unified AppDateTimeFormatter.
 */
object DateUtils {
    fun getDayOfWeekArabic(timestampSec: Long): String =
        AppDateTimeFormatter.getDayOfWeekArabic(timestampSec)

    fun formatTime24Or12(timestampSec: Long): String =
        AppDateTimeFormatter.formatTime12hArabic(java.util.Date(if (timestampSec in 1..999999999999L) timestampSec * 1000L else timestampSec))

    fun formatDateFull(timestampSec: Long): String =
        AppDateTimeFormatter.formatDateIso(timestampSec)

    fun getYearMonthKey(timestampSec: Long): String =
        AppDateTimeFormatter.getYearMonthKey(timestampSec)

    fun getDayOfMonth(timestampSec: Long): Int =
        AppDateTimeFormatter.getDayOfMonth(timestampSec)

    fun getMonthNameArabic(timestampSec: Long): String =
        AppDateTimeFormatter.getMonthNameArabic(timestampSec)

    fun formatDurationBetween(newerSec: Long, olderSec: Long, context: Context? = null): String =
        AppDateTimeFormatter.formatDurationBetween(newerSec, olderSec, context)
}