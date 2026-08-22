package com.example.domain.usecase.habayeb

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import com.example.data.local.entities.AppSettings
import com.example.data.local.entities.DatabaseDefaults
import com.example.data.local.entities.HabayebCustomer
import com.example.data.local.entities.HabayebTransaction
import com.example.data.repository.FinanceRepository
import com.example.ui.helper.VibrationHelper
import com.example.ui.screens.habayeb.utils.CurrencyConfig
import com.example.ui.viewmodel.FinanceConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.UUID

import androidx.room.withTransaction
import com.example.data.local.AppDatabase

data class HistoricalRevalueResult(
    val updatedCount: Int,
    val skippedCount: Int,
    val failed: Boolean = false,
    val errorMessage: String? = null
)

class HabayebTransactionUseCase(
    private val application: Application,
    private val repository: FinanceRepository,
    private val sharedPrefs: SharedPreferences
) {
    companion object {
        private const val TAG = "HabayebTxUseCase"
        private const val FALLBACK_NONE = "NONE"
        private fun generateTxId(): String = "dtx_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
    }

    suspend fun saveHabayebCustomer(
        customer: HabayebCustomer,
        transaction: HabayebTransaction?,
        selectedCategoryFilter: String?,
        onActivationRequired: () -> Unit,
        onCategoryUpdated: () -> Unit
    ) = withContext(Dispatchers.IO) {
        if (transaction != null && transaction.amount > BigDecimal.ZERO && repository.isTrialExpiredDirect()) {
            onActivationRequired()
            return@withContext
        }
        try {
            repository.insertCustomerWithOpeningTransaction(customer, transaction)

            if (selectedCategoryFilter != null && selectedCategoryFilter != FinanceConstants.CATEGORY_CLOSED) {
                sharedPrefs.edit().putString("${HabayebCategoryManager.PREFIX_CAT_LINK}${customer.id}", selectedCategoryFilter).apply()
                onCategoryUpdated()
            }

            VibrationHelper.triggerSuccessVibration(application)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Error saving customer", e)
        }
    }

    suspend fun saveHabayebCustomer(
        customer: HabayebCustomer,
        initialAmount: BigDecimal,
        initialType: String,
        customTimestamp: Long = System.currentTimeMillis() / 1000,
        initialDetails: String = "",
        isForeign: Boolean = false,
        currencyCode: String = FinanceConstants.DEFAULT_CURRENCY_CODE,
        foreignAmount: BigDecimal = BigDecimal.ZERO,
        exchangeRate: BigDecimal = BigDecimal.ONE,
        isRateCalculated: Boolean = false,
        equivalentAmount: BigDecimal = BigDecimal.ZERO,
        selectedCategoryFilter: String?,
        settings: AppSettings,
        onActivationRequired: () -> Unit,
        onCategoryUpdated: () -> Unit
    ) {
        val transaction = if (initialAmount > BigDecimal.ZERO) {
            HabayebTransaction(
                id = generateTxId(),
                customerId = customer.id,
                type = initialType,
                amount = initialAmount,
                timestamp = customTimestamp,
                description = initialDetails.ifEmpty { customer.notes },
                isForeign = isForeign,
                currencyCode = currencyCode,
                foreignAmount = foreignAmount,
                exchangeRate = exchangeRate,
                isRateCalculated = isRateCalculated,
                equivalentAmount = equivalentAmount,
                baseCurrencyCode = settings.currencySymbol
            )
        } else null

        saveHabayebCustomer(
            customer = customer,
            transaction = transaction,
            selectedCategoryFilter = selectedCategoryFilter,
            onActivationRequired = onActivationRequired,
            onCategoryUpdated = onCategoryUpdated
        )
    }

    suspend fun addHabayebTransaction(
        transaction: HabayebTransaction,
        onActivationRequired: () -> Unit
    ) = withContext(Dispatchers.IO) {
        if (repository.isTrialExpiredDirect()) {
            onActivationRequired()
            return@withContext
        }
        try {
            repository.insertHabayebTransaction(transaction)
            VibrationHelper.triggerSuccessVibration(application)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Error adding transaction", e)
        }
    }

    suspend fun addHabayebTransaction(
        customerId: String,
        type: String,
        amount: BigDecimal,
        desc: String,
        timestamp: Long = System.currentTimeMillis() / 1000,
        editingTxId: String? = null,
        linkedMainTxId: String? = null,
        isForeign: Boolean = false,
        currencyCode: String = FinanceConstants.DEFAULT_CURRENCY_CODE,
        foreignAmount: BigDecimal = BigDecimal.ZERO,
        exchangeRate: BigDecimal = BigDecimal.ONE,
        isRateCalculated: Boolean = false,
        equivalentAmount: BigDecimal = BigDecimal.ZERO,
        baseCurrencySymbol: String,
        onActivationRequired: () -> Unit
    ) {
        val txId = editingTxId ?: generateTxId()
        val candidateLinkedId = if (linkedMainTxId != null) {
            linkedMainTxId
        } else if (editingTxId != null) {
            repository.getHabayebTransactionById(editingTxId)?.linkedMainTxId
        } else {
            null
        }
        val cleanLinkedMainTxId = candidateLinkedId?.trim()?.takeIf { 
            it.isNotBlank() && !it.equals("null", ignoreCase = true) && it != "0" && it != txId 
        }

        val transaction = HabayebTransaction(
            id = txId,
            customerId = customerId,
            type = type,
            amount = amount,
            timestamp = timestamp,
            description = desc,
            linkedMainTxId = cleanLinkedMainTxId,
            isForeign = isForeign,
            currencyCode = currencyCode,
            foreignAmount = foreignAmount,
            exchangeRate = exchangeRate,
            isRateCalculated = isRateCalculated,
            equivalentAmount = equivalentAmount,
            baseCurrencyCode = baseCurrencySymbol
        )
        addHabayebTransaction(transaction, onActivationRequired)
    }

    suspend fun updateTransactionExchangeRate(
        txId: String,
        newRate: BigDecimal,
        calculateRate: Boolean,
        defaultCurrency: String
    ) = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(application)
            db.withTransaction {
                val tx = repository.getHabayebTransactionById(txId)
                if (tx != null) {
                    val parsed = CurrencyConfig.parseTransactionCurrency(tx.description, FALLBACK_NONE)
                    val txCurrency = if (tx.currencyCode != FinanceConstants.DEFAULT_CURRENCY_CODE && tx.currencyCode.isNotBlank()) {
                        tx.currencyCode
                    } else if (parsed.first != FALLBACK_NONE) {
                        parsed.first
                    } else if (tx.baseCurrencyCode != FinanceConstants.DEFAULT_CURRENCY_CODE && tx.baseCurrencyCode.isNotBlank()) {
                        tx.baseCurrencyCode
                    } else defaultCurrency

                    val pair = com.example.domain.model.CurrencyPair(
                        baseCurrency = defaultCurrency,
                        targetCurrency = txCurrency,
                        rate = newRate
                    )

                    val isSelfConversion = pair.isSelfPair
                    if (!isSelfConversion && !pair.isValid) {
                        throw IllegalArgumentException("Exchange rate must be greater than zero")
                    }
                    val finalRate = if (isSelfConversion) BigDecimal.ONE else pair.safeRate
                    val finalCalculateRate = if (isSelfConversion) false else calculateRate

                    val sourceAmount = if (tx.foreignAmount.compareTo(BigDecimal.ZERO) > 0) tx.foreignAmount else tx.amount
                    val finalEquivalent = CurrencyConfig.convertAmountBigDecimal(
                        sourceAmount,
                        pair.baseCurrency,
                        pair.targetCurrency,
                        finalRate
                    )

                    if (tx.linkedMainTxId != null) {
                        val mainTx = repository.getTransactionById(tx.linkedMainTxId)
                        if (mainTx != null) {
                            val updatedMainTx = mainTx.copy(amount = if (finalCalculateRate) finalEquivalent else BigDecimal.ZERO)
                            repository.saveTransaction(updatedMainTx)
                        }
                    }

                    val updatedTx = tx.copy(
                        currencyCode = txCurrency,
                        baseCurrencyCode = defaultCurrency,
                        isForeign = !pair.isSelfPair,
                        exchangeRate = finalRate,
                        isRateCalculated = finalCalculateRate,
                        equivalentAmount = if (finalCalculateRate) finalEquivalent else BigDecimal.ZERO,
                        amount = if (finalCalculateRate) finalEquivalent else sourceAmount,
                        foreignAmount = sourceAmount
                    )
                    repository.insertHabayebTransaction(updatedTx)
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Error updating transaction exchange rate", e)
        }
    }

    suspend fun revalueHistoricalTransactions(
        baseCurrencyCode: String,
        targetCurrencyCode: String,
        newRate: BigDecimal
    ): HistoricalRevalueResult = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(application)
            db.withTransaction {
                val transactions = repository.getAllTransactionsDirect()
                require(newRate > BigDecimal.ZERO) { "Exchange rate must be greater than zero" }
                val finalRate = newRate.setScale(8, java.math.RoundingMode.HALF_EVEN)
                var updatedCount = 0
                var skippedCount = 0

                val normTargetCurrency = CurrencyConfig.getBySymbol(targetCurrencyCode)?.symbol ?: targetCurrencyCode
                val normBaseCurrency = CurrencyConfig.getBySymbol(baseCurrencyCode)?.symbol ?: baseCurrencyCode

                transactions.forEach { tx ->
                    val txCurrencyRaw = if (tx.currencyCode != FinanceConstants.DEFAULT_CURRENCY_CODE && tx.currencyCode.isNotBlank()) tx.currencyCode else baseCurrencyCode
                    val txBaseCurrencyRaw = if (tx.baseCurrencyCode != FinanceConstants.DEFAULT_CURRENCY_CODE && tx.baseCurrencyCode.isNotBlank()) tx.baseCurrencyCode else DatabaseDefaults.DEFAULT_CURRENCY_SYMBOL

                    val normTxCurrency = CurrencyConfig.getBySymbol(txCurrencyRaw)?.symbol ?: txCurrencyRaw
                    val normTxBaseCurrency = CurrencyConfig.getBySymbol(txBaseCurrencyRaw)?.symbol ?: txBaseCurrencyRaw

                    if (normTxCurrency == normTargetCurrency && normTxBaseCurrency == normBaseCurrency && tx.isRateCalculated) {
                        val sourceForeign = if (tx.foreignAmount.compareTo(BigDecimal.ZERO) > 0) tx.foreignAmount else tx.amount
                        val finalEquivalent = CurrencyConfig.convertAmountBigDecimal(
                            sourceForeign,
                            baseCurrencyCode,
                            normTxCurrency,
                            finalRate
                        )

                        if (tx.linkedMainTxId != null) {
                            val mainTx = repository.getTransactionById(tx.linkedMainTxId)
                            if (mainTx != null) {
                                val updatedMainTx = mainTx.copy(amount = finalEquivalent)
                                repository.saveTransaction(updatedMainTx)
                            }
                        }

                        val updatedTx = tx.copy(
                            foreignAmount = sourceForeign,
                            exchangeRate = finalRate,
                            equivalentAmount = finalEquivalent,
                            amount = finalEquivalent,
                            baseCurrencyCode = baseCurrencyCode
                        )
                        repository.insertHabayebTransaction(updatedTx)
                        updatedCount++
                    } else {
                        skippedCount++
                    }
                }
                HistoricalRevalueResult(updatedCount = updatedCount, skippedCount = skippedCount)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Error revaluing historical transactions", e)
            HistoricalRevalueResult(
                updatedCount = 0,
                skippedCount = 0,
                failed = true,
                errorMessage = e.message
            )
        }
    }

    suspend fun updateCustomerName(customerId: String, newName: String) = withContext(Dispatchers.IO) {
        try {
            repository.updateCustomerName(customerId, newName)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Error updating customer name", e)
        }
    }

    suspend fun updateCustomer(customer: HabayebCustomer) = withContext(Dispatchers.IO) {
        try {
            repository.updateCustomer(customer)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Error updating customer", e)
        }
    }

    suspend fun deleteCustomer(customerId: String) = withContext(Dispatchers.IO) {
        try {
            val customer = repository.getCustomerByIdDirect(customerId)
            val customerTxs = repository.getTransactionsForCustomerDirect(customerId)

            if (customer != null) {
                repository.softDeleteHabayebBundleToTrash(customer, customerTxs)
            }
            repository.deleteCustomerAndTransactions(customerId)
            VibrationHelper.triggerDeleteVibration(application)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Error deleting customer", e)
        }
    }

    suspend fun deleteMultipleCustomers(customerIds: List<String>) = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(application)
            db.withTransaction {
                val allCustomers = repository.getAllCustomersDirect()
                val customerMap = allCustomers.associateBy { it.id }
                for (id in customerIds) {
                    val customer = customerMap[id]
                    val customerTxs = repository.getTransactionsForCustomerDirect(id)
                    if (customer != null) {
                        repository.softDeleteHabayebBundleToTrash(customer, customerTxs)
                    }
                    repository.deleteCustomerAndTransactions(id)
                }
            }
            VibrationHelper.triggerDeleteVibration(application)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Error deleting multiple customers", e)
        }
    }

    suspend fun deleteTransaction(txId: String, isEdit: Boolean = false) = withContext(Dispatchers.IO) {
        try {
            val tx = repository.getHabayebTransactionById(txId)
            if (tx != null && !isEdit) {
                repository.softDeleteHabayebTransactionToTrash(tx)
            }
            if (tx?.linkedMainTxId != null) {
                repository.deleteTransactionById(tx.linkedMainTxId)
            }
            repository.deleteHabayebTransactionById(txId)
            if (!isEdit) {
                VibrationHelper.triggerDeleteVibration(application)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Error deleting transaction", e)
        }
    }

    suspend fun deleteMultipleTransactions(txIds: List<String>) = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(application)
            db.withTransaction {
                txIds.forEach { txId ->
                    val tx = repository.getHabayebTransactionById(txId)
                    if (tx != null) {
                        repository.softDeleteHabayebTransactionToTrash(tx)
                        if (tx.linkedMainTxId != null) {
                            repository.deleteTransactionById(tx.linkedMainTxId)
                        }
                    }
                    repository.deleteHabayebTransactionById(txId)
                }
            }
            VibrationHelper.triggerDeleteVibration(application)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Error deleting multiple transactions", e)
        }
    }
}
