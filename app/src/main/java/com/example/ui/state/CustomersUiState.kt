package com.example.ui.state

import androidx.compose.runtime.Immutable
import com.example.data.local.entities.HabayebCustomer
import java.math.BigDecimal

@Immutable
data class CustomerUiState(
    val id: String,
    val name: String,
    val phone: String = "",
    val notes: String = "",
    val createdAt: Long = 0L,
    val totalTransactions: Int = 0,
    val netDebt: Double = 0.0,
    val displayNetDebt: Double = 0.0,
    val displayCurrencySymbol: String = "",
    val lastTransactionTimestamp: Long = 0L,
    val isStable: Boolean = true,
    val originalCustomer: HabayebCustomer,
    val foreignDebts: Map<String, java.math.BigDecimal> = emptyMap(),
    val defaultCurrencyTotal: java.math.BigDecimal = java.math.BigDecimal.ZERO,
    val normalizedName: String = "",
    val defaultCurrencyTotalAbs: java.math.BigDecimal = java.math.BigDecimal.ZERO
) {
    val isClosed: Boolean get() = defaultCurrencyTotal.setScale(4, java.math.RoundingMode.HALF_EVEN).compareTo(java.math.BigDecimal.ZERO) == 0 && foreignDebts.isEmpty()
}

@Immutable
data class CustomersUiState(
    val customers: List<CustomerUiState> = emptyList(),
    val totalOwedByThem: BigDecimal = BigDecimal.ZERO,
    val totalOwedToThem: BigDecimal = BigDecimal.ZERO,
    val isLoading: Boolean = false
)
