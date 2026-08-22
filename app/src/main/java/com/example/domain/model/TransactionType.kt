package com.example.domain.model

enum class TransactionType(val value: String) {
    OWED_BY_THEM("OWED_BY_THEM"),
    OWED_TO_THEM("OWED_TO_THEM"),
    PAYMENT_BY_THEM("PAYMENT_BY_THEM"),
    PAYMENT_TO_THEM("PAYMENT_TO_THEM"),
    INCOME("INCOME"),
    EXPENSE("EXPENSE");

    companion object {
        private val VALUE_MAP = entries.associateBy { it.value }

        fun fromValue(value: String?): TransactionType {
            if (value == null) return OWED_BY_THEM
            return VALUE_MAP[value] ?: OWED_BY_THEM
        }
    }
}

