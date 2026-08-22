package com.example.ui.screens.habayeb.utils

import com.example.domain.formatters.AppDateTimeFormatter
import java.util.Date

/**
 * Legacy delegate to unified AppDateTimeFormatter for Mizan and Habayeb screens.
 */
object MizanDateFormatter {
    fun formatShortDate(date: Date): String = AppDateTimeFormatter.formatShortDate(date)
    fun formatShortDate(timestampSeconds: Long): String = AppDateTimeFormatter.formatShortDate(timestampSeconds)

    fun formatDateArabic(date: Date): String = AppDateTimeFormatter.formatDateArabic(date)
    fun formatDateArabic(timestampSeconds: Long): String = AppDateTimeFormatter.formatDateArabic(timestampSeconds)

    fun formatTime12h(date: Date): String = AppDateTimeFormatter.formatTime12h(date)
    fun formatTime12h(timestampSeconds: Long): String = AppDateTimeFormatter.formatTime12h(timestampSeconds)

    fun formatFullDateTime(date: Date): String = AppDateTimeFormatter.formatFullDateTime(date)
    fun formatFullDateTime(timestampMillis: Long): String = AppDateTimeFormatter.formatFullDateTime(timestampMillis)
}
