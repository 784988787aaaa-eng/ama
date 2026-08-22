package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.NavigationPreferences
import com.example.data.local.entities.AppSettings
import com.example.data.local.entities.CustomCategory
import com.example.data.local.entities.DeletedItemEntity
import com.example.data.local.entities.FixedCommitment
import com.example.data.local.entities.TransactionDb
import com.example.data.repository.FinanceRepository
import com.example.domain.DateUtils
import com.example.domain.StringUtils
import com.example.domain.model.TransactionType
import com.example.ui.state.MainLedgerUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

import com.example.ui.viewmodel.ledger.DayLedger
import com.example.ui.viewmodel.ledger.MonthLedger
import com.example.ui.viewmodel.ledger.TrashRestoreHandler

typealias MonthLedger = com.example.ui.viewmodel.ledger.MonthLedger
typealias DayLedger = com.example.ui.viewmodel.ledger.DayLedger

/**
 * نموذج العرض المالي الرئيسي (FinanceViewModel)
 * مسؤول عن تنسيق حالة الواجهة (UI Orchestration) وربط تفاعلات المستخدم مع المستودع المالي
 * دون تخزين منطق محاسبي أو التعامل المباشر مع طبقة التخزين.
 */
class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val PREFS_TRASH = "trash_prefs"
        private const val KEY_TRASH_AUTO_CLEANUP_PERIOD = "trash_auto_cleanup_period"
        private const val CLEANUP_PERIOD_NEVER = "never"
        private const val TRANSACTION_TYPE_EXPENSE = "EXPENSE"
        private const val PREFIX_HABAYEB = "habayeb_"
    }

    private val repository: FinanceRepository

    private val _autoCleanupPeriod = MutableStateFlow(CLEANUP_PERIOD_NEVER)
    val autoCleanupPeriod: StateFlow<String> = _autoCleanupPeriod.asStateFlow()

    private val _uiEventChannel = Channel<UiEvent>(Channel.BUFFERED)
    val uiEventFlow = _uiEventChannel.receiveAsFlow()

    private fun sendUiEvent(event: UiEvent) {
        _uiEventChannel.trySend(event)
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FinanceRepository(database, application)
        val trashPrefs = application.getSharedPreferences(PREFS_TRASH, Context.MODE_PRIVATE)
        _autoCleanupPeriod.value = trashPrefs.getString(KEY_TRASH_AUTO_CLEANUP_PERIOD, CLEANUP_PERIOD_NEVER) ?: CLEANUP_PERIOD_NEVER
    }

    private val navigationPrefs = NavigationPreferences(application)

    val tabOrderState: StateFlow<String> = navigationPrefs.tabOrderFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NavigationPreferences.DEFAULT_ORDER)

    val defaultStartDestinationState: StateFlow<String> = navigationPrefs.defaultStartFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NavigationPreferences.DEFAULT_START)

    fun saveTabOrder(order: String) {
        viewModelScope.launch {
            navigationPrefs.saveTabOrder(order)
        }
    }

    fun saveDefaultStart(start: String) {
        viewModelScope.launch {
            navigationPrefs.saveDefaultStart(start)
        }
    }

    val isSettingsLoaded = MutableStateFlow(false)

    val settingsState: StateFlow<AppSettings> = repository.settingsFlow
        .onEach { isSettingsLoaded.value = true }
        .map { it ?: AppSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val commitmentsState: StateFlow<List<FixedCommitment>> = repository.commitmentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactionsState: StateFlow<List<TransactionDb>> = repository.transactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customCategoriesState: StateFlow<List<CustomCategory>> = repository.customCategoriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedItemsFlow: Flow<List<DeletedItemEntity>> = repository.deletedItemsFlow

    val totalTransactionsCount: StateFlow<Int> = combine(
        repository.getTransactionsCountFlow(),
        repository.getHabayebTransactionsCountFlow()
    ) { mainCount, habayebCount ->
        mainCount + habayebCount
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val sharedPrefs = application.getSharedPreferences(FinanceConstants.PREFS_NAME, Context.MODE_PRIVATE)

    fun hasShownOnboarding(): Boolean {
        return sharedPrefs.getBoolean(FinanceConstants.KEY_ONBOARDING_SHOWN, false)
    }

    fun markOnboardingShown() {
        viewModelScope.launch(Dispatchers.IO) {
            sharedPrefs.edit().putBoolean(FinanceConstants.KEY_ONBOARDING_SHOWN, true).apply()
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val searchResultsState: StateFlow<List<TransactionDb>> = combine(transactionsState, _searchQuery) { transactions, query ->
        if (query.isBlank()) emptyList()
        else {
            val normalizedQuery = StringUtils.normalizeArabic(query, getApplication<Application>())
            transactions.filter { tx ->
                StringUtils.normalizeArabic(tx.description, getApplication<Application>()).contains(normalizedQuery, ignoreCase = true)
            }.sortedByDescending { it.timestamp }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun calculateSumByType(transactions: List<TransactionDb>, type: String): BigDecimal {
        val targetType = TransactionType.fromValue(type)
        var sum = BigDecimal.ZERO
        for (tx in transactions) {
            val txType = TransactionType.fromValue(tx.type)
            if (txType == targetType || tx.type.equals(type, ignoreCase = true)) {
                sum = sum.add(tx.amount)
            }
        }
        return sum.setScale(2, RoundingMode.HALF_EVEN)
    }

    val totalCashState: StateFlow<BigDecimal> = repository.getTotalCashFlow()
        .map { it.setScale(2, RoundingMode.HALF_EVEN) }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BigDecimal.ZERO)

    val dailyExpenseComparisonState: StateFlow<Pair<BigDecimal, BigDecimal>> = transactionsState
        .map { txList ->
            val nowSec = System.currentTimeMillis() / 1000
            val todayKey = DateUtils.formatDateFull(nowSec)
            val yesterdayKey = DateUtils.formatDateFull(nowSec - 86400L)

            var todayExpenses = BigDecimal.ZERO
            var yesterdayExpenses = BigDecimal.ZERO

            for (tx in txList) {
                val txType = TransactionType.fromValue(tx.type)
                if (txType == TransactionType.EXPENSE || tx.type.equals(TRANSACTION_TYPE_EXPENSE, ignoreCase = true)) {
                    val txDate = DateUtils.formatDateFull(tx.timestamp)
                    if (txDate == todayKey) {
                        todayExpenses = todayExpenses.add(tx.amount)
                    } else if (txDate == yesterdayKey) {
                        yesterdayExpenses = yesterdayExpenses.add(tx.amount)
                    }
                }
            }
            Pair(todayExpenses, yesterdayExpenses)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(BigDecimal.ZERO, BigDecimal.ZERO))

    val ledgerUiState: StateFlow<MainLedgerUiState> = combine(
        searchResultsState,
        totalCashState,
        _searchQuery
    ) { txList, totalCash, query ->
        MainLedgerUiState(
            transactions = txList,
            totalCash = totalCash.toDouble(),
            isSearching = query.isNotBlank(),
            isLoading = false
        )
    }.flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainLedgerUiState()
    )

    val monthlyLedgerState: StateFlow<List<MonthLedger>> = transactionsState
        .map { txList -> LedgerCalculator.computeMonthlyLedger(txList) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveSettings(settings: AppSettings) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSettings(settings)
        }
    }

    fun addTransaction(type: String, category: String, amount: Double, description: String, timestamp: Long = System.currentTimeMillis() / 1000, presetId: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            if (repository.isTrialExpiredDirect()) {
                sendUiEvent(UiEvent.ShowToast(R.string.licensing_dialog_desc, true))
                sendUiEvent(UiEvent.ShowActivationDialog)
                return@launch
            }
            val id = presetId ?: "tx_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
            val tx = TransactionDb(
                id = id,
                timestamp = timestamp,
                type = type,
                category = category,
                amount = BigDecimal(amount.toString()),
                description = description
            )
            repository.saveTransaction(tx)
        }
    }

    fun permanentlyDeleteDeletedItem(item: DeletedItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.removeDeletedItem(item)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun permanentlyDeleteMultipleItems(items: List<DeletedItemEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                items.forEach { repository.removeDeletedItem(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun restoreMultipleItems(items: List<DeletedItemEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                items.forEach { repository.restoreDeletedItem(it) }
                items.forEach { TrashRestoreHandler.restorePrefsForDeletedItem(context, it) }
                sendUiEvent(UiEvent.ShowToast(R.string.toast_restore_success))
            } catch (e: Exception) {
                e.printStackTrace()
                sendUiEvent(UiEvent.ShowToast(R.string.toast_operation_failed))
            }
        }
    }

    fun restoreDeletedItem(item: DeletedItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                repository.restoreDeletedItem(item)
                TrashRestoreHandler.restorePrefsForDeletedItem(context, item)
                sendUiEvent(UiEvent.ShowToast(R.string.toast_restore_success))
            } catch (e: Exception) {
                e.printStackTrace()
                sendUiEvent(UiEvent.ShowToast(R.string.toast_operation_failed))
            }
        }
    }

    fun restoreSingleTransactionFromBundle(itemId: String, txId: String, item: DeletedItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                repository.restoreSingleTransactionFromBundle(itemId, txId)
                TrashRestoreHandler.restorePrefsForDeletedItem(context, item)
                sendUiEvent(UiEvent.ShowToast(R.string.toast_restore_success))
            } catch (e: Exception) {
                e.printStackTrace()
                sendUiEvent(UiEvent.ShowToast(R.string.toast_operation_failed))
            }
        }
    }

    fun updateAutoCleanupPeriod(period: String) {
        _autoCleanupPeriod.value = period
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val trashPrefs = context.getSharedPreferences(PREFS_TRASH, Context.MODE_PRIVATE)
            trashPrefs.edit().putString(KEY_TRASH_AUTO_CLEANUP_PERIOD, period).apply()

            // Schedule background worker
            com.example.TrashCleanupWorker.schedulePeriodicCleanup(context, period)

            // Immediate execution of cleanup if not "never"
            if (period != CLEANUP_PERIOD_NEVER) {
                try {
                    val ageInMillis = com.example.TrashCleanupWorker.getPeriodDurationMillis(period)
                    if (ageInMillis > 0L) {
                        val thresholdTime = System.currentTimeMillis() - ageInMillis
                        val items = repository.getAllDeletedItemsDirect()
                        val expiredItems = items.filter { it.deletedAt < thresholdTime }
                        expiredItems.forEach { item ->
                            repository.removeDeletedItem(item)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun cleanLedgerTrashItems() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val systemHabayeb = getApplication<Application>().getString(R.string.source_system_habayeb)
                val allItems = repository.getAllDeletedItemsDirect()
                val nonHabayebItems = allItems.filter {
                    it.sourceSystem != systemHabayeb && !it.originalTableName.startsWith(PREFIX_HABAYEB)
                }
                nonHabayebItems.forEach {
                    repository.removeDeletedItem(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val systemHabayeb = getApplication<Application>().getString(R.string.source_system_habayeb)
                val allItems = repository.getAllDeletedItemsDirect()
                val habayebItems = allItems.filter {
                    it.sourceSystem == systemHabayeb || it.originalTableName.startsWith(PREFIX_HABAYEB)
                }
                habayebItems.forEach {
                    repository.removeDeletedItem(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteTransaction(tx: TransactionDb) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.softDeleteTransactionToTrash(tx)
                repository.deleteTransaction(tx)
            } catch (e: Exception) {
                e.printStackTrace()
                sendUiEvent(UiEvent.ShowToast(R.string.toast_delete_failed))
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
                repository.deleteTransactionById(id)
            } catch (e: Exception) {
                e.printStackTrace()
                sendUiEvent(UiEvent.ShowToast(R.string.toast_delete_failed))
            }
        }
    }

    fun deleteTransactionsBulk(ids: List<String>, bundleTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allTxs = transactionsState.value
                val idSet = ids.toSet()
                val toDelete = allTxs.filter { idSet.contains(it.id) }
                if (toDelete.isNotEmpty()) {
                    repository.softDeleteTransactionBundleToTrash(toDelete, bundleTitle)
                    toDelete.forEach { repository.deleteTransactionById(it.id) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                sendUiEvent(UiEvent.ShowToast(R.string.toast_delete_failed))
            }
        }
    }

    fun updateTransaction(tx: TransactionDb) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.saveTransaction(tx)
            } catch (e: Exception) {
                e.printStackTrace()
                sendUiEvent(UiEvent.ShowToast(R.string.toast_operation_failed))
            }
        }
    }

    fun saveCommitment(name: String, targetAmount: BigDecimal, currentProgress: BigDecimal) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val count = commitmentsState.value.size
                val fc = FixedCommitment(name, targetAmount, currentProgress, count)
                repository.saveCommitment(fc)
            } catch (e: Exception) {
                e.printStackTrace()
                sendUiEvent(UiEvent.ShowToast(R.string.toast_save_failed))
            }
        }
    }

    fun saveCommitment(name: String, targetAmount: Double, currentProgress: Double) {
        saveCommitment(name, BigDecimal(targetAmount.toString()), BigDecimal(currentProgress.toString()))
    }

    fun updateCommitmentDirectly(commitment: FixedCommitment) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.saveCommitment(commitment)
            } catch (e: Exception) {
                e.printStackTrace()
                sendUiEvent(UiEvent.ShowToast(R.string.toast_operation_failed))
            }
        }
    }

    fun reorderCommitment(commitment: FixedCommitment, toPosition: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentList = commitmentsState.value.toMutableList()
                currentList.sortBy { it.orderIndex }

                val targetIndex = (toPosition - 1).coerceIn(0, currentList.size - 1)
                val currentIndex = currentList.indexOfFirst { it.name == commitment.name }

                if (currentIndex != -1 && currentIndex != targetIndex) {
                    val item = currentList.removeAt(currentIndex)
                    currentList.add(targetIndex, item)

                    val updatedList = currentList.mapIndexed { index, fc ->
                        fc.copy(orderIndex = index)
                    }

                    repository.updateCommitments(updatedList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteCommitment(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val oldFc = commitmentsState.value.find { it.name == name }
                if (oldFc != null) {
                    repository.softDeleteCommitmentToTrash(oldFc)
                }
                repository.deleteCommitment(name)
            } catch (e: Exception) {
                e.printStackTrace()
                sendUiEvent(UiEvent.ShowToast(R.string.toast_delete_failed))
            }
        }
    }

    fun saveCustomCategory(name: String, tabType: String, emoji: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.saveCustomCategory(CustomCategory(name = name, tabType = tabType, iconEmoji = emoji))
            } catch (e: Exception) {
                e.printStackTrace()
                sendUiEvent(UiEvent.ShowToast(R.string.toast_save_failed))
            }
        }
    }

    fun deleteCustomCategory(customCategory: CustomCategory) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteCustomCategory(customCategory)
            } catch (e: Exception) {
                e.printStackTrace()
                sendUiEvent(UiEvent.ShowToast(R.string.toast_delete_failed))
            }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteAllData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
