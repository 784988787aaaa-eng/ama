package com.example.domain.formatters

import android.content.Context
import com.example.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Unified, enterprise-grade, thread-safe DateTime Formatter Engine.
 * Serves as the single source of truth for date, time, and relative duration formatting across the entire app.
 */
object AppDateTimeFormatter {
    private val arabicLocale = Locale("ar")

    private val dateArabicFormatter = ThreadLocal.withInitial { SimpleDateFormat("yyyy/MM/dd", arabicLocale) }
    private val dateDefaultFormatter = ThreadLocal.withInitial { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
    private val dateShortFormatter = ThreadLocal.withInitial { SimpleDateFormat("yy/MM/dd", Locale.ENGLISH) }
    private val dateIsoFormatter = ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }
    private val time12hFormatter = ThreadLocal.withInitial { SimpleDateFormat("hh:mm a", arabicLocale) }
    private val time12hDefaultFormatter = ThreadLocal.withInitial { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    private val fullDateTimeArabicFormatter = ThreadLocal.withInitial { SimpleDateFormat("yyyy/MM/dd hh:mm a", arabicLocale) }
    private val dayOfWeekFormatter = ThreadLocal.withInitial { SimpleDateFormat("EEEE", arabicLocale) }
    private val monthNameFormatter = ThreadLocal.withInitial { SimpleDateFormat("MMMM", arabicLocale) }
    private val yearOnlyFormatter = ThreadLocal.withInitial { SimpleDateFormat("yyyy", Locale.ENGLISH) }
    private val yearMonthFormatter = ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM", Locale.ENGLISH) }
    private val calendarLocal = ThreadLocal.withInitial { Calendar.getInstance() }

    fun formatDateArabic(date: Date): String {
        return dateArabicFormatter.get().format(date)
    }

    fun formatDateArabic(timestampSeconds: Long): String {
        return formatDateArabic(Date(normalizeToMillis(timestampSeconds)))
    }

    fun formatDateDefault(date: Date): String {
        return dateDefaultFormatter.get().format(date)
    }

    fun formatDateDefault(timestampSeconds: Long): String {
        return formatDateDefault(Date(normalizeToMillis(timestampSeconds)))
    }

    fun formatShortDate(date: Date): String {
        return dateShortFormatter.get().format(date)
    }

    fun formatShortDate(timestampSeconds: Long): String {
        return formatShortDate(Date(normalizeToMillis(timestampSeconds)))
    }

    fun formatDateIso(date: Date): String {
        return dateIsoFormatter.get().format(date)
    }

    fun formatDateIso(timestampSeconds: Long): String {
        return formatDateIso(Date(normalizeToMillis(timestampSeconds)))
    }

    fun formatTime12h(date: Date): String {
        return time12hDefaultFormatter.get().format(date)
    }

    fun formatTime12h(timestampSeconds: Long): String {
        return formatTime12h(Date(normalizeToMillis(timestampSeconds)))
    }

    fun formatTime12hArabic(date: Date): String {
        return time12hFormatter.get().format(date)
    }

    fun formatFullDateTime(date: Date): String {
        return fullDateTimeArabicFormatter.get().format(date)
    }

    fun formatFullDateTime(timestampMillisOrSeconds: Long): String {
        if (timestampMillisOrSeconds <= 0L) return ""
        val ms = normalizeToMillis(timestampMillisOrSeconds)
        return try {
            formatFullDateTime(Date(ms))
        } catch (e: Exception) {
            ""
        }
    }

    fun getDayOfWeekArabic(timestampSeconds: Long): String =
        dayOfWeekFormatter.get().format(Date(normalizeToMillis(timestampSeconds)))

    fun getDayOfWeekResId(timestampSeconds: Long): Int {
        val cal = calendarLocal.get().apply {
            timeInMillis = normalizeToMillis(timestampSeconds)
        }
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> R.string.day_saturday
            Calendar.SUNDAY -> R.string.day_sunday
            Calendar.MONDAY -> R.string.day_monday
            Calendar.TUESDAY -> R.string.day_tuesday
            Calendar.WEDNESDAY -> R.string.day_wednesday
            Calendar.THURSDAY -> R.string.day_thursday
            Calendar.FRIDAY -> R.string.day_friday
            else -> R.string.day_sunday
        }
    }

    fun getDayOfMonth(timestampSeconds: Long): Int =
        calendarLocal.get().apply {
            timeInMillis = normalizeToMillis(timestampSeconds)
        }.get(Calendar.DAY_OF_MONTH)

    fun getMonthNameArabic(timestampSeconds: Long): String {
        val date = Date(normalizeToMillis(timestampSeconds))
        return "${monthNameFormatter.get().format(date)} ${yearOnlyFormatter.get().format(date)}"
    }

    fun getYearMonthKey(timestampSeconds: Long): String =
        yearMonthFormatter.get().format(Date(normalizeToMillis(timestampSeconds)))

    fun formatDurationBetween(newerSec: Long, olderSec: Long, context: Context? = null): String {
        val diffSec = (newerSec - olderSec).coerceAtLeast(0)
        val days = diffSec / 86400
        val hours = (diffSec % 86400) / 3600

        return when {
            days > 30 -> context?.getString(R.string.date_diff_over_month).orEmpty()
            days > 1 -> context?.getString(R.string.date_diff_days_pattern, days).orEmpty()
            days == 1L -> context?.getString(R.string.date_diff_one_day).orEmpty()
            hours > 1 -> context?.getString(R.string.date_diff_hours_pattern, hours).orEmpty()
            else -> context?.getString(R.string.date_diff_very_close).orEmpty()
        }
    }

    private fun normalizeToMillis(timestamp: Long): Long {
        return if (timestamp in 1..999999999999L) timestamp * 1000L else timestamp
    }
}
