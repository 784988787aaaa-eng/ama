package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entities.AppSettings
import com.example.data.local.entities.FixedCommitment
import com.example.data.local.entities.TransactionDb
import com.example.domain.FormatUtils
import com.example.ui.components.circularReveal
import com.example.ui.screens.ledger.components.LedgerBottomDock
import com.example.ui.screens.ledger.components.MainLedgerDialogsManager
import com.example.ui.screens.ledger.components.MainLedgerListSection
import com.example.ui.screens.ledger.components.MainLedgerSelectionBar
import com.example.ui.screens.ledger.components.PinnedMainLedgerHeader
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.HabayebFinanceViewModel
import com.example.ui.viewmodel.SecurityAndLicenseViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal

sealed interface MainLedgerDialogState {
    object None : MainLedgerDialogState
    data class AddTransaction(val type: String = "EXPENSE", val editingTx: TransactionDb? = null) : MainLedgerDialogState
    object Search : MainLedgerDialogState
    object CommitmentsList : MainLedgerDialogState
    data class AddCommitment(val editingCommitment: FixedCommitment? = null) : MainLedgerDialogState
    data class ReorderCommitment(val target: FixedCommitment) : MainLedgerDialogState
    object DeleteDaysConfirm : MainLedgerDialogState
    object DeviceActivation : MainLedgerDialogState
}

@Composable
fun MainLedgerView(
    viewModel: FinanceViewModel,
    habayebViewModel: HabayebFinanceViewModel,
    securityViewModel: SecurityAndLicenseViewModel,
    settings: AppSettings,
    onBackIntercept: (Boolean) -> Unit,
    onMenuClick: () -> Unit = {},
    isDrawerOpen: Boolean = false,
    isFloatingSearchActive: Boolean = false,
    onFloatingSearchActiveChanged: (Boolean) -> Unit = {},
    isSearchActive: Boolean = false,
    onSearchActiveChanged: (Boolean) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues()
) {
    val bottomPadding = contentPadding.calculateBottomPadding()
    val totalCash by viewModel.totalCashState.collectAsStateWithLifecycle()
    val commitments by viewModel.commitmentsState.collectAsStateWithLifecycle()
    val monthlyLedger by viewModel.monthlyLedgerState.collectAsStateWithLifecycle()
    val appSettingsState by viewModel.settingsState.collectAsStateWithLifecycle()

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    val systemDark = isSystemInDarkTheme()
    val isDark = remember(appSettingsState.themeMode, systemDark) {
        when (appSettingsState.themeMode) { 1 -> false; 2 -> true; else -> systemDark }
    }
    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    val lazyListState = rememberLazyListState()
    val collapseFractionProvider = remember {
        { if (lazyListState.firstVisibleItemIndex > 0) 1f else (lazyListState.firstVisibleItemScrollOffset.toFloat() / 180f).coerceIn(0f, 1f) }
    }
    val isPinnedVisible = remember {
        derivedStateOf { collapseFractionProvider() > 0f }
    }

    var activeDialogState by remember { mutableStateOf<MainLedgerDialogState>(MainLedgerDialogState.None) }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive && activeDialogState !is MainLedgerDialogState.Search) {
            activeDialogState = MainLedgerDialogState.Search
        } else if (!isSearchActive && activeDialogState is MainLedgerDialogState.Search) {
            activeDialogState = MainLedgerDialogState.None
        }
    }

    LaunchedEffect(activeDialogState) {
        val isSearch = activeDialogState is MainLedgerDialogState.Search
        if (isSearch != isSearchActive) {
            onSearchActiveChanged(isSearch)
        }
    }

    val deviceId by securityViewModel.deviceIdState.collectAsStateWithLifecycle()
    val showActivationRequired by securityViewModel.showActivationRequired.collectAsStateWithLifecycle()

    LaunchedEffect(showActivationRequired) {
        if (showActivationRequired) {
            activeDialogState = MainLedgerDialogState.DeviceActivation
            securityViewModel.showActivationRequired.value = false
        }
    }

    var expandedDayKeys by remember { mutableStateOf(setOf<String>()) }
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResultsState.collectAsStateWithLifecycle()

    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedTxIds = remember { mutableStateListOf<String>() }
    var collapsedMonths by remember { mutableStateOf(setOf<String>()) }
    var isHabayebActive by rememberSaveable { mutableStateOf(false) }

    var isDaySelectionMode by remember { mutableStateOf(false) }
    val selectedDayKeys = remember { mutableStateListOf<String>() }

    BackHandler(enabled = !isDrawerOpen && (isHabayebActive || activeDialogState !is MainLedgerDialogState.None || isSelectionMode || isDaySelectionMode || expandedDayKeys.isNotEmpty())) {
        if (isHabayebActive) {
            isHabayebActive = false
        } else if (activeDialogState !is MainLedgerDialogState.None) {
            activeDialogState = MainLedgerDialogState.None
        } else if (isSelectionMode || isDaySelectionMode) {
            selectedTxIds.clear()
            selectedDayKeys.clear()
            isSelectionMode = false
            isDaySelectionMode = false
        } else if (expandedDayKeys.isNotEmpty()) {
            expandedDayKeys = emptySet()
        }
    }

    val clearSelection = remember {
        {
            selectedTxIds.clear()
            selectedDayKeys.clear()
            isSelectionMode = false
            isDaySelectionMode = false
        }
    }

    val linkHabayebDebts by habayebViewModel.linkHabayebDebtsState.collectAsStateWithLifecycle()
    val habayebOwedByThemTotal by habayebViewModel.habayebOwedByThemTotalState.collectAsStateWithLifecycle()

    val computedCommitments = remember(commitments, totalCash, linkHabayebDebts, habayebOwedByThemTotal) {
        var remainingCash = if (linkHabayebDebts) totalCash.add(habayebOwedByThemTotal) else totalCash
        commitments.map { fc ->
            val target = fc.targetAmount
            val alreadyPaid = fc.currentProgress
            val needed = (target.subtract(alreadyPaid)).max(BigDecimal.ZERO)
            val allocatedFromCash = if (remainingCash >= needed) {
                remainingCash = remainingCash.subtract(needed)
                needed
            } else if (remainingCash > BigDecimal.ZERO) {
                val temp = remainingCash
                remainingCash = BigDecimal.ZERO
                temp
            } else BigDecimal.ZERO
            Triple(fc, alreadyPaid.add(allocatedFromCash), needed.subtract(allocatedFromCash))
        }
    }

    val isPrivacyMode by securityViewModel.isPrivacyModeEnabled.collectAsStateWithLifecycle()
    val allKeys = remember(monthlyLedger) { monthlyLedger.flatMap { ml -> ml.days.map { "${ml.monthKey}_${it.dayNumber}" } } }
    val selectedDayKeysCountText = when (selectedDayKeys.size) {
        1 -> stringResource(R.string.ledger_selected_days_count_1)
        2 -> stringResource(R.string.ledger_selected_days_count_2)
        else -> stringResource(R.string.ledger_selected_days_count_more, selectedDayKeys.size)
    }
    val isSelectAllChecked = selectedDayKeys.size == allKeys.size && allKeys.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            PinnedMainLedgerHeader(
                isDaySelectionMode = isDaySelectionMode,
                selectedDayKeys = selectedDayKeys,
                onCancelDaySelection = { isDaySelectionMode = false; selectedDayKeys.clear() },
                onSelectAllDays = { if (selectedDayKeys.size == allKeys.size) selectedDayKeys.clear() else { selectedDayKeys.clear(); selectedDayKeys.addAll(allKeys) } },
                onDeleteSelectedDays = { if (selectedDayKeys.isNotEmpty()) activeDialogState = MainLedgerDialogState.DeleteDaysConfirm },
                isSelectAllChecked = isSelectAllChecked,
                selectedDayKeysCountText = selectedDayKeysCountText,
                onMenuClick = onMenuClick,
                onSearchClick = { activeDialogState = MainLedgerDialogState.Search },
                isFloatingSearchActive = isFloatingSearchActive,
                onFloatingSearchActiveChanged = onFloatingSearchActiveChanged,
                totalCash = totalCash,
                isPrivacyMode = isPrivacyMode,
                onTogglePrivacyMode = { securityViewModel.togglePrivacyMode() },
                currencySymbol = settings.currencySymbol,
                formatCurrency = { v, s -> FormatUtils.formatCurrency(v, s, context) },
                commitments = commitments,
                computedCommitments = computedCommitments,
                linkHabayebDebts = linkHabayebDebts,
                onLinkHabayebDebtsChange = { habayebViewModel.toggleLinkHabayebDebts(it) }
            )

            MainLedgerListSection(
                lazyListState = lazyListState,
                bottomPadding = bottomPadding,
                isDaySelectionMode = isDaySelectionMode,
                selectedDayKeys = selectedDayKeys,
                currencySymbol = settings.currencySymbol,
                formatCurrency = { v, s -> FormatUtils.formatCurrency(v, s, context) },
                formatDoubleCurrency = { v, s -> FormatUtils.formatDoubleCurrency(v, s, context) },
                monthlyLedger = monthlyLedger,
                isScreenReady = true,
                collapsedMonths = collapsedMonths,
                onToggleMonthCollapsed = { mKey -> collapsedMonths = if (collapsedMonths.contains(mKey)) collapsedMonths - mKey else collapsedMonths + mKey },
                expandedDayKeys = expandedDayKeys,
                haptic = haptic,
                context = context,
                viewModel = viewModel,
                onEditTransaction = { tx -> activeDialogState = MainLedgerDialogState.AddTransaction(type = tx.type, editingTx = tx) },
                onDayClick = { key ->
                    if (isDaySelectionMode) {
                        if (selectedDayKeys.contains(key)) {
                            selectedDayKeys.remove(key)
                            if (selectedDayKeys.isEmpty()) isDaySelectionMode = false
                        } else selectedDayKeys.add(key)
                    } else expandedDayKeys = if (expandedDayKeys.contains(key)) expandedDayKeys - key else expandedDayKeys + key
                },
                onDayLongClick = { key ->
                    if (!isDaySelectionMode && !isSelectionMode) { isDaySelectionMode = true; selectedDayKeys.add(key) }
                    else if (isDaySelectionMode) {
                        if (selectedDayKeys.contains(key)) {
                            selectedDayKeys.remove(key)
                            if (selectedDayKeys.isEmpty()) isDaySelectionMode = false
                        } else selectedDayKeys.add(key)
                    }
                },
                isSelectionMode = isSelectionMode,
                selectedTxIds = selectedTxIds,
                onTransactionSelectToggle = { txId ->
                    if (selectedTxIds.contains(txId)) {
                        selectedTxIds.remove(txId)
                        if (selectedTxIds.isEmpty()) isSelectionMode = false
                    } else {
                        if (!isSelectionMode) isSelectionMode = true
                        selectedTxIds.add(txId)
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }

        LedgerBottomDock(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = bottomPadding + 12.dp),
            isSelectionMode = isSelectionMode || isDaySelectionMode,
            selectedTxIdsCount = selectedTxIds.size,
            onDeleteSelectedClick = {
                viewModel.deleteTransactionsBulk(selectedTxIds.toList(), context.getString(R.string.ledger_delete_selected_warning, selectedTxIds.size))
                clearSelection()
            },
            onShowCommitmentsClick = { activeDialogState = MainLedgerDialogState.CommitmentsList },
            onAddIncomeClick = { activeDialogState = MainLedgerDialogState.AddTransaction(type = "INCOME", editingTx = null) },
            onAddExpenseClick = { activeDialogState = MainLedgerDialogState.AddTransaction(type = "EXPENSE", editingTx = null) }
        )

        MainLedgerSelectionBar(
            isSelectionActive = (isSelectionMode && selectedTxIds.isNotEmpty()) || (isDaySelectionMode && selectedDayKeys.isNotEmpty()),
            isDaySelectionMode = isDaySelectionMode,
            isSelectAllChecked = isSelectAllChecked,
            selectedDayKeysCountText = selectedDayKeysCountText,
            selectedTxCount = selectedTxIds.size,
            allKeys = allKeys,
            selectedDayKeys = selectedDayKeys,
            haptic = haptic,
            onClearSelection = { clearSelection() },
            onDeleteClick = {
                if (isDaySelectionMode) {
                    if (selectedDayKeys.isNotEmpty()) activeDialogState = MainLedgerDialogState.DeleteDaysConfirm
                } else if (selectedTxIds.isNotEmpty()) {
                    viewModel.deleteTransactionsBulk(selectedTxIds.toList(), context.getString(R.string.ledger_delete_selected_warning, selectedTxIds.size))
                    clearSelection()
                }
            },
            bottomPadding = bottomPadding,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    MainLedgerDialogsManager(
        showTxDialog = activeDialogState is MainLedgerDialogState.AddTransaction,
        txDialogType = (activeDialogState as? MainLedgerDialogState.AddTransaction)?.type ?: "EXPENSE",
        editingTransaction = (activeDialogState as? MainLedgerDialogState.AddTransaction)?.editingTx,
        currencySymbol = settings.currencySymbol,
        onDismissTxDialog = { activeDialogState = MainLedgerDialogState.None },
        onSaveTransaction = { id, type, cat, amt, desc ->
            val editingTx = (activeDialogState as? MainLedgerDialogState.AddTransaction)?.editingTx
            if (editingTx != null) {
                viewModel.updateTransaction(editingTx.copy(amount = BigDecimal(amt.toString()), description = desc, category = cat))
            } else {
                viewModel.addTransaction(type = type, category = cat, amount = amt, description = desc)
            }
            activeDialogState = MainLedgerDialogState.None
            scope.launch {
                lazyListState.scrollToItem(0)
            }
        },
        showSearch = activeDialogState is MainLedgerDialogState.Search,
        searchQuery = searchQuery,
        searchResults = searchResults,
        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
        onDismissSearch = { activeDialogState = MainLedgerDialogState.None },
        showCommitmentsListSheet = activeDialogState is MainLedgerDialogState.CommitmentsList || activeDialogState is MainLedgerDialogState.AddCommitment || activeDialogState is MainLedgerDialogState.ReorderCommitment,
        commitments = commitments,
        computedCommitments = computedCommitments,
        totalCash = totalCash,
        formatCurrency = { v, s -> FormatUtils.formatCurrency(v, s, context) },
        formatDoubleCurrency = { v, s -> FormatUtils.formatDoubleCurrency(v, s, context) },
        onDismissCommitmentsList = { activeDialogState = MainLedgerDialogState.None },
        onAddCommitmentClick = { activeDialogState = MainLedgerDialogState.AddCommitment(editingCommitment = null) },
        onEditCommitmentClick = { fc -> activeDialogState = MainLedgerDialogState.AddCommitment(editingCommitment = fc) },
        onDeleteCommitment = { name -> viewModel.deleteCommitment(name) },
        onReorderCommitment = { fc, pos -> viewModel.reorderCommitment(fc, pos) },
        onCommitmentCheckedChange = { fc, checked -> viewModel.saveCommitment(fc.name, fc.targetAmount, if (checked) fc.targetAmount else BigDecimal.ZERO) },
        onSetReorderTarget = { fc -> activeDialogState = MainLedgerDialogState.ReorderCommitment(fc) },
        showCommitmentDialog = activeDialogState is MainLedgerDialogState.AddCommitment,
        editingCommitment = (activeDialogState as? MainLedgerDialogState.AddCommitment)?.editingCommitment,
        onDismissCommitmentDialog = { activeDialogState = MainLedgerDialogState.CommitmentsList },
        onSaveCommitment = { name, targetAmt, progress ->
            viewModel.saveCommitment(name, targetAmt, progress)
            activeDialogState = MainLedgerDialogState.CommitmentsList
        },
        reorderCommitmentTarget = (activeDialogState as? MainLedgerDialogState.ReorderCommitment)?.target,
        onDismissReorderTarget = { activeDialogState = MainLedgerDialogState.CommitmentsList },
        onApplyReorderTarget = { target, pos ->
            viewModel.reorderCommitment(target, pos)
            activeDialogState = MainLedgerDialogState.CommitmentsList
        },
        showActivationDialog = activeDialogState is MainLedgerDialogState.DeviceActivation,
        deviceId = deviceId,
        securityViewModel = securityViewModel,
        onDismissActivationDialog = { activeDialogState = MainLedgerDialogState.None },
        showDeleteDaysDialog = activeDialogState is MainLedgerDialogState.DeleteDaysConfirm,
        onDismissDeleteDaysDialog = { activeDialogState = MainLedgerDialogState.None },
        monthlyLedger = monthlyLedger,
        selectedDayKeys = selectedDayKeys,
        viewModel = viewModel,
        scope = scope,
        context = context,
        onSuccessDeleteDays = {
            clearSelection()
            activeDialogState = MainLedgerDialogState.None
        }
    )

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(isHabayebActive) {
        if (isHabayebActive) animProgress.animateTo(1f, animationSpec = tween(450, easing = FastOutSlowInEasing))
        else animProgress.animateTo(0f, animationSpec = tween(400, easing = FastOutSlowInEasing))
    }

    if (animProgress.value > 0f) {
        val revealCenter = Offset(250f, 400f)

        Box(
            modifier = Modifier.fillMaxSize().graphicsLayer {
                alpha = animProgress.value
                scaleX = animProgress.value
                scaleY = animProgress.value
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            }.circularReveal(animProgress.value, revealCenter, isRelative = true)
        ) {
            HabayebScreen(
                viewModel = habayebViewModel,
                securityViewModel = securityViewModel,
                onMenuClick = onMenuClick,
                onClose = { scope.launch { isHabayebActive = false } }
            )
        }
    }
}
