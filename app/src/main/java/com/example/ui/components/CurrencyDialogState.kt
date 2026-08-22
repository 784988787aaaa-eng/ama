package com.example.ui.components

sealed interface CurrencyDialogState {
    object None : CurrencyDialogState
    data class RevalueConfirm(val targetCurrency: String, val newRate: Double) : CurrencyDialogState
}
