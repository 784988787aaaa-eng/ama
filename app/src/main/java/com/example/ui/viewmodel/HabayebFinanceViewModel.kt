package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entities.*
import com.example.data.repository.FinanceRepository
import com.example.domain.LicenseManager
import com.example.domain.usecase.habayeb.*
import com.example.ui.state.CustomerUiState
import com.example.ui.state.CustomersUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

sealed interface HabayebUiEvent {
    data class ScrollToAccount(val accountId: String) : HabayebUiEvent
    object ResetScrollToTop : HabayebUiEvent
}

data class HabayebUiState(
    val customers: List<CustomerUiState> = emptyList(),
    val filteredCustomers: List<CustomerUiState> = emptyList(),
    val totalOwedByThem: BigDecimal = BigDecimal.ZERO,
    val totalOwedToThem: BigDecimal = BigDecimal.ZERO,
    val customCategories: List<CustomCategory> = emptyList(),
    val orderedCategories: List<String> = emptyList(),
    val categoryCounts: Map<String, Int> = emptyMap(),
    val closedCategoryName: String = "",
    val searchQuery: String = "",
    val selectedFilterTab: Int = 0,
    val financialSortMode: Int = 0,
    val historicalSortMode: Int = 0,
    val pinnedCustomerIds: Set<String> = emptySet(),
    val selectedCategory: String? = null,
    val selectedCustomerIds: List<String> = emptyList(),
    val activeCustomersCount: Int = 0
)

@OptIn(FlowPreview::class)
class HabayebFinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository = FinanceRepository(AppDatabase.getDatabase(application), application)
    private val sharedPrefs = application.getSharedPreferences("mizan_finance_prefs", Context.MODE_PRIVATE)

    private val categoryManager = HabayebCategoryManager(application, repository, sharedPrefs)
    private val transactionUseCase = HabayebTransactionUseCase(application, repository, sharedPrefs)

    private val _uiEventChannel = Channel<HabayebUiEvent>(Channel.BUFFERED)
    val uiEventFlow: Flow<HabayebUiEvent> = _uiEventChannel.receiveAsFlow()

    fun emitScrollToAccount(accountId: String) {
        _uiEventChannel.trySend(HabayebUiEvent.ScrollToAccount(accountId))
    }

    fun emitResetScrollToTop() {
        _uiEventChannel.trySend(HabayebUiEvent.ResetScrollToTop)
    }

    fun clearScrollTriggerEvent() {
        // Retained for backward compatibility
    }

    val pinnedCustomerIds: StateFlow<Set<String>> = categoryManager.pinnedCustomerIds
    private val _showActivationRequired = MutableStateFlow(false)
    val showActivationRequired = _showActivationRequired.asStateFlow()

    fun resetActivationRequired() { _showActivationRequired.value = false }
    fun triggerActivationRequired() { _showActivationRequired.value = true }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            categoryManager.loadPinnedForCategory("GLOBAL_ALL")
            categoryManager.ensureClosedCategoryExists()
        }
    }

    val settingsState: StateFlow<AppSettings> = repository.settingsFlow
        .map { it ?: AppSettings() }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val habayebCustomersState: StateFlow<List<HabayebCustomer>> = repository.habayebCustomersFlow
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habayebTransactionsState: StateFlow<List<HabayebTransaction>> = repository.habayebTransactionsFlow
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _linkHabayebDebtsState = MutableStateFlow(sharedPrefs.getBoolean("KEY_LINK_HABAYEB_DEBTS", false))
    val linkHabayebDebtsState = _linkHabayebDebtsState.asStateFlow()

    fun toggleLinkHabayebDebts(enabled: Boolean) {
        _linkHabayebDebtsState.value = enabled
        viewModelScope.launch(Dispatchers.IO) { sharedPrefs.edit().putBoolean("KEY_LINK_HABAYEB_DEBTS", enabled).apply() }
    }

    val totalTransactionsCount: StateFlow<Int> = combine(
        repository.getTransactionsCountFlow(), repository.getHabayebTransactionsCountFlow()
    ) { m, h -> m + h }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun isTrialExpired(): Boolean {
        if (repository.isAppActivated()) return false
        return totalTransactionsCount.value >= LicenseManager.SECURE_LIMIT_VAL
    }

    suspend fun isTrialExpiredDirect(): Boolean = repository.isTrialExpiredDirect()

    fun getTransactionsForCustomerFlow(customerId: String): Flow<List<HabayebTransaction>> =
        repository.getTransactionsForCustomerFlow(customerId)

    fun getInitialTransactionsForCustomer(customerId: String): List<HabayebTransaction> {
        val all = habayebTransactionsState.value
        if (all.isEmpty()) return emptyList()
        return all.filter { it.customerId == customerId }
    }

    fun getTransactionsForCustomerWithLimitFlow(customerId: String, limit: Int): Flow<List<HabayebTransaction>> =
        repository.getTransactionsForCustomerWithLimitFlow(customerId, limit)

    suspend fun getTransactionsForCustomerPaged(customerId: String, limit: Int, offset: Int): List<HabayebTransaction> =
        repository.getTransactionsForCustomerPaged(customerId, limit, offset)

    val searchQuery = MutableStateFlow("")
    val selectedFilterTab = MutableStateFlow(0)
    val financialSortMode = MutableStateFlow(0)
    val historicalSortMode = MutableStateFlow(1)
    val temporarilyHiddenCustomerIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCustomerIdsState = MutableStateFlow<List<String>>(emptyList())

    fun resetFiltersToDefault(resetCategory: Boolean = true) {
        searchQuery.value = ""
        selectedFilterTab.value = 0
        financialSortMode.value = 0
        historicalSortMode.value = 1
        if (resetCategory) {
            selectedCategoryFilter.value = null
        }
    }

    fun updateSelectedCustomerIds(ids: List<String>) { selectedCustomerIdsState.value = ids }
    fun updateSearchQuery(query: String) {
        searchQuery.value = query
        emitResetScrollToTop()
    }
    fun updateSelectedFilterTab(tab: Int) {
        selectedFilterTab.value = tab
        emitResetScrollToTop()
    }
    fun updateFinancialSortMode(mode: Int) {
        financialSortMode.value = mode
        emitResetScrollToTop()
    }
    fun updateHistoricalSortMode(mode: Int) {
        historicalSortMode.value = mode
        emitResetScrollToTop()
    }

    fun updateSelectedCategoryFilter(category: String?) {
        selectedCategoryFilter.value = category
        categoryManager.loadPinnedForCategory(category)
        emitResetScrollToTop()
    }

    fun togglePinCustomer(customerId: String) {
        viewModelScope.launch {
            val isCurrentlyPinned = categoryManager.pinnedCustomerIds.value.contains(customerId)
            val success = categoryManager.togglePinCustomer(customerId, selectedCategoryFilter.value)
            if (success && !isCurrentlyPinned) {
                emitScrollToAccount(customerId)
            }
        }
    }

    fun assignCategoryToCustomers(customerIds: List<String>, category: String?) {
        viewModelScope.launch { categoryManager.assignCategoryToCustomers(customerIds, category) }
    }

    fun getCustomerCategory(customerId: String): String? = categoryManager.getCustomerCategory(customerId)

    val customCategoriesState: StateFlow<List<CustomCategory>> = repository.customCategoriesFlow
        .map { list -> list.filter { !it.isSystemClosed } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orderedCategoriesState: StateFlow<List<String>> = repository.customCategoriesFlow
        .map { all -> all.sortedBy { it.displayOrder }.map { if (it.isSystemClosed) "CLOSED" else it.name } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("CLOSED"))

    val closedCategoryNameState: StateFlow<String> = repository.customCategoriesFlow
        .map { all -> 
            all.find { it.isSystemClosed }?.name 
                ?: getApplication<Application>().getString(com.example.R.string.category_system_closed)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 
            getApplication<Application>().getString(com.example.R.string.category_system_closed))

    fun renameClosedCategory(newName: String) { viewModelScope.launch { categoryManager.renameClosedCategory(newName) } }
    fun saveCustomCategory(name: String) { viewModelScope.launch { categoryManager.saveCustomCategory(name) } }
    fun renameCustomCategory(category: CustomCategory, newName: String) { viewModelScope.launch { categoryManager.renameCustomCategory(category, newName) } }
    fun deleteCustomCategoryWithChoice(category: CustomCategory, deleteLinked: Boolean) { viewModelScope.launch { categoryManager.deleteCustomCategoryWithChoice(category, deleteLinked) } }
    fun moveCategoryLeft(categoryName: String) { viewModelScope.launch { categoryManager.moveCategoryLeft(orderedCategoriesState.value, categoryName) } }
    fun moveCategoryRight(categoryName: String) { viewModelScope.launch { categoryManager.moveCategoryRight(orderedCategoriesState.value, categoryName) } }
    fun reorderCategories(newList: List<String>) { viewModelScope.launch { categoryManager.reorderCategories(newList) } }

    val customersUiState: StateFlow<CustomersUiState> = combine(
        repository.habayebCustomersFlow, repository.habayebTransactionsFlow, settingsState
    ) { customers, transactions, settings -> HabayebFinancialCalculator.calculateCustomersUiState(customers, transactions, settings) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CustomersUiState())

    private val filterGroup1Flow = combine(searchQuery, selectedFilterTab, financialSortMode, historicalSortMode) { q, t, f, h -> HabayebFilterGroup1(q, t, f, h) }
    private val filterGroup2Flow = combine(temporarilyHiddenCustomerIds, selectedCategoryFilter, pinnedCustomerIds) { hid, cat, pin -> HabayebFilterGroup2(hid, cat, pin) }
    private val filterParametersFlow = combine(filterGroup1Flow, filterGroup2Flow) { g1, g2 ->
        HabayebFilterParameters(g1.query, g1.tab, g1.finSort, g1.histSort, g2.hiddenIds, g2.selectedCat, g2.pinnedIds)
    }

    private val filteredResultFlow: Flow<FilteredResult> = combine(
        customersUiState,
        filterParametersFlow,
        categoryManager.categoryUpdateTrigger
    ) { ui, params, _ ->
        HabayebFinancialCalculator.calculateFilteredResult(ui, params, categoryManager.getCategoryMap())
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<HabayebUiState> = combine(
        filteredResultFlow,
        customCategoriesState,
        orderedCategoriesState,
        closedCategoryNameState,
        filterParametersFlow,
        selectedCustomerIdsState
    ) { flows ->
        val filteredRes = flows[0] as FilteredResult
        @Suppress("UNCHECKED_CAST")
        val customCats = flows[1] as List<CustomCategory>
        @Suppress("UNCHECKED_CAST")
        val orderedCats = flows[2] as List<String>
        val closedName = flows[3] as String
        val filterParams = flows[4] as HabayebFilterParameters
        @Suppress("UNCHECKED_CAST")
        val selectedIds = flows[5] as List<String>

        HabayebUiState(
            customers = customersUiState.value.customers,
            filteredCustomers = filteredRes.filteredCustomers,
            totalOwedByThem = filteredRes.totalOwedByThem,
            totalOwedToThem = filteredRes.totalOwedToThem,
            customCategories = customCats,
            orderedCategories = orderedCats,
            categoryCounts = filteredRes.categoryCounts,
            closedCategoryName = closedName,
            searchQuery = filterParams.query,
            selectedFilterTab = filterParams.tab,
            financialSortMode = filterParams.finSort,
            historicalSortMode = filterParams.histSort,
            pinnedCustomerIds = filterParams.pinnedIds,
            selectedCategory = filterParams.selectedCat,
            selectedCustomerIds = selectedIds,
            activeCustomersCount = filteredRes.activeCustomersCount
        )
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabayebUiState())

    val filteredCustomersState: StateFlow<List<CustomerUiState>> = uiState
        .map { it.filteredCustomers }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habayebOwedByThemTotalState: StateFlow<BigDecimal> = uiState
        .map { it.totalOwedByThem }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BigDecimal.ZERO)

    val habayebOwedToThemTotalState: StateFlow<BigDecimal> = uiState
        .map { it.totalOwedToThem }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BigDecimal.ZERO)

    val categoryCountsState: StateFlow<Map<String, Int>> = uiState
        .map { it.categoryCounts }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    suspend fun saveHabayebCustomer(
        customer: HabayebCustomer, initialAmount: BigDecimal, initialType: String,
        customTimestamp: Long = System.currentTimeMillis() / 1000, initialDetails: String = "",
        isForeign: Boolean = false, currencyCode: String = "DEFAULT", foreignAmount: BigDecimal = BigDecimal.ZERO,
        exchangeRate: BigDecimal = BigDecimal.ONE, isRateCalculated: Boolean = false, equivalentAmount: BigDecimal = BigDecimal.ZERO
    ) = withContext(Dispatchers.IO) {
        if (isTrialExpiredDirect()) {
            _showActivationRequired.value = true
            return@withContext
        }
        resetFiltersToDefault(resetCategory = true)

        transactionUseCase.saveHabayebCustomer(
            customer, initialAmount, initialType, customTimestamp, initialDetails, isForeign, currencyCode,
            foreignAmount, exchangeRate, isRateCalculated, equivalentAmount, null, settingsState.value,
            onActivationRequired = { _showActivationRequired.value = true }, onCategoryUpdated = { categoryManager.triggerUpdate() }
        )
        emitScrollToAccount(customer.id)
    }

    fun addHabayebTransaction(
        customerId: String, type: String, amount: BigDecimal, desc: String,
        timestamp: Long = System.currentTimeMillis() / 1000, editingTxId: String? = null, linkedMainTxId: String? = null,
        isForeign: Boolean = false, currencyCode: String = "DEFAULT", foreignAmount: BigDecimal = BigDecimal.ZERO,
        exchangeRate: BigDecimal = BigDecimal.ONE, isRateCalculated: Boolean = false, equivalentAmount: BigDecimal = BigDecimal.ZERO
    ) {
        viewModelScope.launch {
            if (isTrialExpiredDirect()) {
                _showActivationRequired.value = true
                return@launch
            }
            resetFiltersToDefault(resetCategory = true)

            transactionUseCase.addHabayebTransaction(
                customerId, type, amount, desc, timestamp, editingTxId, linkedMainTxId, isForeign, currencyCode,
                foreignAmount, exchangeRate, isRateCalculated, equivalentAmount, settingsState.value.currencySymbol,
                onActivationRequired = { _showActivationRequired.value = true }
            )
            emitScrollToAccount(customerId)
        }
    }

    fun updateTransactionExchangeRate(txId: String, newRate: BigDecimal, calculateRate: Boolean) {
        viewModelScope.launch { transactionUseCase.updateTransactionExchangeRate(txId, newRate, calculateRate, settingsState.value.currencySymbol) }
    }

    fun revalueHistoricalTransactions(baseCurrencyCode: String, targetCurrencyCode: String, newRate: BigDecimal) {
        viewModelScope.launch { transactionUseCase.revalueHistoricalTransactions(baseCurrencyCode, targetCurrencyCode, newRate) }
    }

    fun updateHabayebCustomerName(customerId: String, newName: String) { viewModelScope.launch { transactionUseCase.updateCustomerName(customerId, newName) } }
    fun updateHabayebCustomer(customer: HabayebCustomer) { viewModelScope.launch { transactionUseCase.updateCustomer(customer) } }
    fun deleteHabayebCustomer(customerId: String) { viewModelScope.launch { transactionUseCase.deleteCustomer(customerId) } }
    fun deleteMultipleHabayebCustomers(customerIds: List<String>) { viewModelScope.launch { transactionUseCase.deleteMultipleCustomers(customerIds) } }
    fun deleteHabayebTransaction(txId: String, isEdit: Boolean = false) { viewModelScope.launch { transactionUseCase.deleteTransaction(txId, isEdit) } }
    fun deleteMultipleHabayebTransactions(txIds: List<String>) { viewModelScope.launch { transactionUseCase.deleteMultipleTransactions(txIds) } }
    fun saveSettings(settings: AppSettings) { viewModelScope.launch(Dispatchers.IO) { repository.saveSettings(settings) } }
}
