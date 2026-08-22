package com.example.data.serialization.pdf

enum class PdfAction {
    SHARE,
    VIEW,
    SAVE_LOCAL,
    WHATSAPP_DIRECT;

    companion object {
        fun from(action: String): PdfAction {
            return values().find { it.name.equals(action, ignoreCase = true) } ?: SHARE
        }
    }
}
