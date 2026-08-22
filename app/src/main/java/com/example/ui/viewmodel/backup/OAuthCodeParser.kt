package com.example.ui.viewmodel.backup

import android.net.Uri

object OAuthCodeParser {
    fun extractCodeFromInput(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.contains("code=")) {
            var extracted = ""
            try {
                val parsedUri = Uri.parse(trimmed)
                extracted = parsedUri.getQueryParameter("code") ?: ""
            } catch (e: Exception) {}
            if (extracted.isEmpty()) {
                val idx = trimmed.indexOf("code=")
                if (idx != -1) {
                    val start = idx + 5
                    val end = trimmed.indexOf("&", start).let { if (it == -1) trimmed.length else it }
                    extracted = trimmed.substring(start, end)
                }
            }
            return extracted.takeIf { it.isNotEmpty() } ?: trimmed
        }
        return trimmed
    }
}
