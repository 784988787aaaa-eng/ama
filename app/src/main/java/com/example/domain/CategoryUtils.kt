package com.example.domain

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.example.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import com.example.ui.theme.CategoryPalette

private val EMOJI_REGEX = "[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+".toRegex()
private val ARABIC_LOCALE = Locale("ar")

private val DAY_NAME_FORMATTER = ThreadLocal.withInitial { SimpleDateFormat("EEEE", ARABIC_LOCALE) }
private val DATE_NUMBERS_FORMATTER = ThreadLocal.withInitial { SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH) }
private val DATE_TIME_FORMATTER = ThreadLocal.withInitial { SimpleDateFormat("dd-MM-yyyy | hh:mm", Locale.ENGLISH) }
private val AM_PM_FORMATTER = ThreadLocal.withInitial { SimpleDateFormat("a", ARABIC_LOCALE) }

fun extractEmoji(category: String, defaultEmoji: String): String {
    if (category.isEmpty()) return defaultEmoji
    val cat = category.lowercase()

    return when {
        cat.contains("دقيق") || cat.contains("🌾") -> "🌾"
        cat.contains("غاز") || cat.contains("🔥") -> "🔥"
        cat.contains("كهرباء") || cat.contains("⚡") -> "⚡"
        cat.contains("ماء") || cat.contains("💧") -> "💧"
        cat.contains("حليب") || cat.contains("🍼") -> "🍼"
        cat.contains("حفاظ") || cat.contains("👶") -> "👶"
        cat.contains("سكر") || cat.contains("🍬") -> "🍬"
        cat.contains("شاي") || cat.contains("☕") -> "☕"
        cat.contains("نت") || cat.contains("رصيد") || cat.contains("🌐") || cat.contains("إنترنت") -> "🌐"
        cat.contains("مدرس") || cat.contains("🎒") -> "🎒"
        cat.contains("ادخار") || cat.contains("🏦") -> "🏦"
        else -> EMOJI_REGEX.find(category)?.value ?: defaultEmoji
    }
}

private inline fun selectColor(isDark: Boolean, dark: Color, light: Color) = if (isDark) dark else light

fun getEmojiBgColor(emoji: String, isDark: Boolean = false): Color {
    return when (emoji) {
        "🌾" -> selectColor(isDark, CategoryPalette.AMBER_DARK, CategoryPalette.AMBER_LIGHT)
        "🍬", "🍭" -> selectColor(isDark, CategoryPalette.PINK_DARK, CategoryPalette.PINK_LIGHT)
        "☕" -> selectColor(isDark, CategoryPalette.GRAY_LIGHT_DARK, CategoryPalette.GRAY_LIGHT_LIGHT)
        "🔥" -> selectColor(isDark, CategoryPalette.RED_SOFT_DARK, CategoryPalette.RED_SOFT_LIGHT)
        "⚡" -> selectColor(isDark, CategoryPalette.YELLOW_DARK, CategoryPalette.YELLOW_LIGHT)
        "💧" -> selectColor(isDark, CategoryPalette.BLUE_SOFT_DARK, CategoryPalette.BLUE_SOFT_LIGHT)
        "🚀", "🌐" -> selectColor(isDark, CategoryPalette.SKY_DARK, CategoryPalette.SKY_LIGHT)
        "🍼", "👶" -> selectColor(isDark, CategoryPalette.PURPLE_DARK, CategoryPalette.PURPLE_LIGHT)
        "🎒" -> selectColor(isDark, CategoryPalette.SKY_DARK, CategoryPalette.SKY_LIGHT)
        "🏦" -> selectColor(isDark, CategoryPalette.EMERALD_SOFT_DARK, CategoryPalette.EMERALD_SOFT_LIGHT)
        "💰" -> selectColor(isDark, CategoryPalette.GREEN_FIFTY_DARK, CategoryPalette.GREEN_FIFTY_LIGHT)
        else -> selectColor(isDark, CategoryPalette.SLATE_DEFAULT_DARK, CategoryPalette.SLATE_DEFAULT_LIGHT)
    }
}

private val CALENDAR_THREAD_LOCAL = ThreadLocal.withInitial { Calendar.getInstance() }

fun getAuditLogGroupDate(timestampMs: Long, context: Context? = null): String {
    val cal = CALENDAR_THREAD_LOCAL.get()
    val now = System.currentTimeMillis()

    cal.timeInMillis = now
    val currentYear = cal.get(Calendar.YEAR)
    val currentDayOfYear = cal.get(Calendar.DAY_OF_YEAR)

    cal.timeInMillis = timestampMs
    val logYear = cal.get(Calendar.YEAR)
    val logDayOfYear = cal.get(Calendar.DAY_OF_YEAR)

    val isSameYear = logYear == currentYear
    val dayDiff = if (isSameYear) currentDayOfYear - logDayOfYear else -1

    return when {
        isSameYear && dayDiff == 0 -> context?.getString(R.string.ledger_day_today).orEmpty()
        isSameYear && dayDiff == 1 -> context?.getString(R.string.ledger_day_yesterday).orEmpty()
        isSameYear && dayDiff == 2 -> context?.getString(R.string.ledger_day_before_yesterday).orEmpty()
        else -> {
            val date = Date(timestampMs)
            val dayName = DAY_NAME_FORMATTER.get()?.format(date).orEmpty()
            val dateNumbers = DATE_NUMBERS_FORMATTER.get()?.format(date).orEmpty()
            "$dayName، $dateNumbers"
        }
    }
}

fun formatAuditLogTime(timestampMs: Long): String {
    val date = Date(timestampMs)
    val datePart = DATE_TIME_FORMATTER.get()?.format(date).orEmpty()
    val amPmPart = AM_PM_FORMATTER.get()?.format(date).orEmpty()
    return "$datePart $amPmPart"
}
