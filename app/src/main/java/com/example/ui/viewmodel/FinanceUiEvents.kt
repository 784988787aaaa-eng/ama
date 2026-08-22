package com.example.ui.viewmodel

sealed class UiEvent {
    data class ShowToast(val messageRes: Int, val isLong: Boolean = false) : UiEvent()
    object ShowActivationDialog : UiEvent()
}
