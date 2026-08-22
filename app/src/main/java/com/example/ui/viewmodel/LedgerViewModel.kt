package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.entities.AppSettings
import com.example.data.local.entities.CustomCategory
import com.example.data.local.entities.TransactionDb
import com.example.data.repository.FinanceRepository
import com.example.domain.StringUtils
import com.example.domain.model.TransactionType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

sealed interface LedgerUiEvent {
    object ScrollToTop : LedgerUiEvent
    data class ScrollToRecord(val recordId: String) : LedgerUiEvent
}

/**
 * LedgerViewModel handles all of the Daily Ledger and General Accounting logic,
 * cleanly isolated from the monolithic FinanceViewModel.
 *
 * It manages:
 * - Dynamic year/month/category filtered StateFlows of transactions.
 * - Reactive and leak-free balance calculations (Total Income, Total Expense, Net Balance).
 * - Full Arabic character search normalization via centralized StringUtils.
 * - Thread-safe insert, update, delete, and soft delete transactions via FinanceRepository.
 * - Custom category creation and removal.
 * - Dynamic mapping of error states to localized resource IDs with zero hardcoded strings.
 */
class LedgerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val transactionDao = database.transactionDao()
    private val customCategoryDao = database.customCategoryDao()
    private val settingsDao = database.settingsDao()
    private val repository = FinanceRepository(database, application)

    // --- UI Event Channel ---
    private val _uiEventChannel = Channel<LedgerUiEvent>(Channel.BUFFERED)
    val uiEventFlow = _uiEventChannel.receiveAsFlow()

    fun emitScrollToTop() {
        viewModelScope.launch { _uiEventChannel.send(LedgerUiEvent.ScrollToTop) }
    }

    // --- Core Database Flows ---
    val settingsState: StateFlow<AppSettings> = settingsDao.getSettingsFlow()
        .map { it ?: AppSettings() }
        .stateIn(viewModelScope, SharingStarted.Lazily, AppSettings())

    val transactionsState: StateFlow<List<TransactionDb>> = transactionDao.getAllTransactionsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val customCategoriesState: StateFlow<List<CustomCategory>> = customCategoryDao.getAllCustomCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- Search Query and Normalized Character Processing ---
    val searchQuery = MutableStateFlow("")

    val searchResultsState: StateFlow<List<TransactionDb>> = combine(
        transactionsState,
        searchQuery
    ) { transactions, query ->
        if (query.isBlank()) {
            emptyList()
        } else {
            val normalizedQuery = StringUtils.normalizeArabic(query, getApplication<Application>())
            transactions.filter { tx ->
                StringUtils.normalizeArabic(tx.description, getApplication<Application>()).contains(normalizedQuery, ignoreCase = true)
            }.sortedByDescending { it.timestamp }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- Interactive Filtering States ---
    val selectedYear = MutableStateFlow<Int?>(null)
    val selectedMonth = MutableStateFlow<Int?>(null)
    val selectedCategory = MutableStateFlow<String?>(null)

    // Combined filtered transactions stream
    val filteredTransactionsState: StateFlow<List<TransactionDb>> = combine(
        transactionsState,
        selectedYear,
        selectedMonth,
        selectedCategory
    ) { transactions, year, month, category ->
        val calendar = java.util.Calendar.getInstance()
        transactions.filter { tx ->
            calendar.timeInMillis = tx.timestamp * 1000
            val txYear = calendar.get(java.util.Calendar.YEAR)
            val txMonth = calendar.get(java.util.Calendar.MONTH) + 1

            val yearMatches = year == null || txYear == year
            val monthMatches = month == null || txMonth == month
            val categoryMatches = category == null || tx.category.trim() == category.trim()

            yearMatches && monthMatches && categoryMatches
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- General Balance and Dynamic Accounting Calculations ---
    val totalIncomeState: StateFlow<BigDecimal> = filteredTransactionsState
        .map { txList ->
            var sum = BigDecimal.ZERO
            for (i in txList.indices) {
                val tx = txList[i]
                val txType = TransactionType.fromValue(tx.type)
                if (txType == TransactionType.INCOME || tx.type.equals("INCOME", ignoreCase = true)) {
                    sum = sum.add(tx.amount)
                }
            }
            sum
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, BigDecimal.ZERO)

    val totalExpenseState: StateFlow<BigDecimal> = filteredTransactionsState
        .map { txList ->
            var sum = BigDecimal.ZERO
            for (i in txList.indices) {
                val tx = txList[i]
                val txType = TransactionType.fromValue(tx.type)
                if (txType == TransactionType.EXPENSE || tx.type.equals("EXPENSE", ignoreCase = true)) {
                    sum = sum.add(tx.amount)
                }
            }
            sum
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, BigDecimal.ZERO)

    val netBalanceState: StateFlow<BigDecimal> = combine(totalIncomeState, totalExpenseState) { income, expense ->
        income.subtract(expense)
    }.stateIn(viewModelScope, SharingStarted.Lazily, BigDecimal.ZERO)

    // --- Thread-Safe Core Ledger Mutations (IO-Bound) ---

    fun addTransaction(
        type: String,
        category: String,
        amount: Double,
        description: String,
        timestamp: Long = System.currentTimeMillis() / 1000,
        presetId: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (repository.isTrialExpiredDirect()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            getApplication(),
                            getApplication<Application>().getString(R.string.licensing_dialog_desc),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }
                val id = presetId ?: "tx_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6)}"
                val tx = TransactionDb(
                    id = id,
                    timestamp = timestamp,
                    type = type,
                    category = category,
                    amount = BigDecimal(amount.toString()),
                    description = description
                )
                repository.saveTransaction(tx)
                _uiEventChannel.send(LedgerUiEvent.ScrollToTop)
                com.example.ui.helper.VibrationHelper.triggerSuccessVibration(getApplication())
            } catch (e: Exception) {
                Log.e("LedgerViewModel", "Error in addTransaction: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        getApplication(),
                        getApplication<Application>().getString(R.string.toast_save_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun updateTransaction(tx: TransactionDb) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                transactionDao.insertTransaction(tx)
                _uiEventChannel.send(LedgerUiEvent.ScrollToTop)
                com.example.ui.helper.VibrationHelper.triggerSuccessVibration(getApplication())
            } catch (e: Exception) {
                Log.e("LedgerViewModel", "Error in updateTransaction: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        getApplication(),
                        getApplication<Application>().getString(R.string.toast_save_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun deleteTransaction(tx: TransactionDb) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.softDeleteTransactionToTrash(tx)
                transactionDao.deleteTransaction(tx)
                com.example.ui.helper.VibrationHelper.triggerDeleteVibration(getApplication())
            } catch (e: Exception) {
                Log.e("LedgerViewModel", "Error in deleteTransaction: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        getApplication(),
                        getApplication<Application>().getString(R.string.toast_delete_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun deleteTransactionById(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tx = transactionsState.value.find { it.id == id }
                if (tx != null) {
                    repository.softDeleteTransactionToTrash(tx)
                }
                transactionDao.deleteTransactionById(id)
                com.example.ui.helper.VibrationHelper.triggerDeleteVibration(getApplication())
            } catch (e: Exception) {
                Log.e("LedgerViewModel", "Error in deleteTransactionById: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        getApplication(),
                        getApplication<Application>().getString(R.string.toast_delete_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun deleteTransactionsBulk(ids: List<String>, bundleTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allTxs = transactionsState.value
                val toDelete = allTxs.filter { ids.contains(it.id) }
                if (toDelete.isNotEmpty()) {
                    repository.softDeleteTransactionBundleToTrash(toDelete, bundleTitle)
                    toDelete.forEach { transactionDao.deleteTransactionById(it.id) }
                    com.example.ui.helper.VibrationHelper.triggerDeleteVibration(getApplication())
                }
            } catch (e: Exception) {
                Log.e("LedgerViewModel", "Error in deleteTransactionsBulk: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        getApplication(),
                        getApplication<Application>().getString(R.string.toast_delete_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // --- Category Actions ---

    fun saveCustomCategory(name: String, tabType: String, emoji: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                customCategoryDao.insertCategory(CustomCategory(name = name, tabType = tabType, iconEmoji = emoji))
                com.example.ui.helper.VibrationHelper.triggerSuccessVibration(getApplication())
            } catch (e: Exception) {
                Log.e("LedgerViewModel", "Error in saveCustomCategory: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        getApplication(),
                        getApplication<Application>().getString(R.string.toast_save_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun deleteCustomCategory(customCategory: CustomCategory) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                customCategoryDao.deleteCategory(customCategory)
                com.example.ui.helper.VibrationHelper.triggerDeleteVibration(getApplication())
            } catch (e: Exception) {
                Log.e("LedgerViewModel", "Error in deleteCustomCategory: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        getApplication(),
                        getApplication<Application>().getString(R.string.toast_delete_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // --- Interactive Filtering Actions ---

    fun selectYear(year: Int?) {
        selectedYear.value = year
    }

    fun selectMonth(month: Int?) {
        selectedMonth.value = month
    }

    fun selectCategory(category: String?) {
        selectedCategory.value = category
    }

    fun clearFilters() {
        selectedYear.value = null
        selectedMonth.value = null
        selectedCategory.value = null
    }
}
