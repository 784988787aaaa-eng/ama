package com.example.ui.screens.habayeb.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.data.local.entities.CustomCategory
import com.example.ui.viewmodel.FinanceConstants
import kotlin.math.abs

/**
 * Pinned Action Filter Ribbon (High-Density & Clean Layout):
 * 1. Pinned Right (Start RTL): Fixed "All" category chip ("الكل 66")
 * 2. Scrollable Center (Weight 1f): Custom categories with Drag-to-Reorder & Option triggers
 * 3. Pinned Left (End RTL): Fixed Action Buttons:
 *     - Dedicated Quick Add Category Button (+) 32dp Circle
 *     - Sort Menu Trigger (⇅) with Dropdown Filter options
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HabayebFilterToolbar(
    selectedCategory: String?,
    customCategories: List<CustomCategory>,
    orderedCategories: List<String>,
    categoryCounts: Map<String, Int>,
    activeCustomersCount: Int,
    financialSortMode: Int,
    onFinancialSortModeChanged: (Int) -> Unit,
    historicalSortMode: Int,
    onHistoricalSortModeChanged: (Int) -> Unit,
    activeThemeColor: Color,
    activeSubColor: Color,
    haptic: HapticFeedback,
    onScrollToTop: () -> Unit,
    onCategorySelected: (String?) -> Unit,
    onAddCategoryClick: () -> Unit,
    onRenameCategory: (CustomCategory, String) -> Unit,
    onDeleteCategory: (CustomCategory, Boolean) -> Unit,
    onMoveCategoryLeft: (String) -> Unit,
    onMoveCategoryRight: (String) -> Unit,
    onReorderCategories: (List<String>) -> Unit,
    closedCategoryName: String,
    onRenameClosedCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    var activeCategoryOptions by remember { mutableStateOf<String?>(null) }
    var categoryToDelete by remember { mutableStateOf<CustomCategory?>(null) }

    // Intercept hardware/system back button when options panel is visible
    BackHandler(enabled = activeCategoryOptions != null) {
        activeCategoryOptions = null
    }

    // Drag-to-reorder state management
    var draggedCategoryKey by remember { mutableStateOf<String?>(null) }
    var dragOffsetX by remember { mutableStateOf(0f) }
    val itemPositions = remember { mutableStateMapOf<String, Float>() }
    val itemWidths = remember { mutableStateMapOf<String, Float>() }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surfaceContainer = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
    val neutralWhite = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val backgroundLight = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        // High-Density Toolbar Row: Pinned All -> Scrollable Custom Categories -> Pinned Actions (+, Sort)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(surfaceContainer)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 1. PINNED RIGHT (RTL Start): Fixed "All" category chip ("الكل")
            val isAllSelected = selectedCategory == null
            val allCount = activeCustomersCount
            CustomCategoryChip(
                selected = isAllSelected,
                label = stringResource(id = R.string.habayeb_filter_all) + " " + allCount,
                activeThemeColor = activeThemeColor,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCategorySelected(null)
                    activeCategoryOptions = null
                    onScrollToTop()
                },
                onLongClick = {}
            )

            // 2. SCROLLABLE CENTER: Custom Categories (Drag-to-Reorder supported)
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val fullCategoryList = remember(orderedCategories, customCategories) {
                    val customNames = customCategories.map { it.name }
                    val list = mutableListOf<String>()
                    list.addAll(orderedCategories.filter { it in customNames })
                    customNames.forEach { if (it !in list) list.add(it) }
                    list.add(FinanceConstants.CATEGORY_CLOSED)
                    list
                }

                fullCategoryList.forEach { categoryKey ->
                    key(categoryKey) {
                        val isClosed = categoryKey == FinanceConstants.CATEGORY_CLOSED
                        val displayName = if (isClosed) closedCategoryName else categoryKey
                        val isSelected = selectedCategory == categoryKey
                        val isBeingDragged = draggedCategoryKey == categoryKey
                        val count = categoryCounts[categoryKey] ?: 0

                        val isCustomCat = !isClosed
                        val customCat = if (isCustomCat) customCategories.find { it.name == categoryKey } else null

                        Box(
                            modifier = Modifier
                                .onGloballyPositioned { coordinates ->
                                    itemPositions[categoryKey] = coordinates.positionInParent().x
                                    itemWidths[categoryKey] = coordinates.size.width.toFloat()
                                }
                                .zIndex(if (isBeingDragged) 10f else 1f)
                                .graphicsLayer {
                                    if (isBeingDragged) {
                                        translationX = dragOffsetX
                                        scaleX = 1.08f
                                        scaleY = 1.08f
                                        shadowElevation = 8f
                                    }
                                }
                                .then(
                                    if (isCustomCat && customCat != null) {
                                        Modifier.pointerInput(categoryKey, fullCategoryList) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    draggedCategoryKey = categoryKey
                                                    dragOffsetX = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffsetX += dragAmount.x

                                                    val currentPos = (itemPositions[categoryKey] ?: 0f) + dragOffsetX
                                                    val currentIdx = fullCategoryList.indexOf(categoryKey)

                                                    fullCategoryList.forEachIndexed { targetIdx, targetKey ->
                                                        if (targetKey != categoryKey && targetKey != FinanceConstants.CATEGORY_CLOSED) {
                                                            val targetPos = itemPositions[targetKey] ?: 0f
                                                            val targetWidth = itemWidths[targetKey] ?: 0f
                                                            val targetCenter = targetPos + targetWidth / 2

                                                            if (abs(currentPos - targetCenter) < targetWidth / 2) {
                                                                if (currentIdx != targetIdx && targetIdx >= 0 && targetIdx < fullCategoryList.size - 1) {
                                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                    val mutable = fullCategoryList.toMutableList()
                                                                    mutable.removeAt(currentIdx)
                                                                    mutable.add(targetIdx, categoryKey)
                                                                    val customOnly = mutable.filter { it != FinanceConstants.CATEGORY_CLOSED }
                                                                    onReorderCategories(customOnly)
                                                                }
                                                            }
                                                        }
                                                    }
                                                },
                                                onDragEnd = {
                                                    draggedCategoryKey = null
                                                    dragOffsetX = 0f
                                                },
                                                onDragCancel = {
                                                    draggedCategoryKey = null
                                                    dragOffsetX = 0f
                                                }
                                            )
                                        }
                                    } else Modifier
                                )
                        ) {
                            if (isCustomCat) {
                                CustomCategoryChip(
                                    selected = isSelected,
                                    label = "$displayName $count",
                                    activeThemeColor = activeThemeColor,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (isSelected) {
                                            activeCategoryOptions = if (activeCategoryOptions == categoryKey) null else categoryKey
                                        } else {
                                            onCategorySelected(categoryKey)
                                            activeCategoryOptions = null
                                            onScrollToTop()
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        activeCategoryOptions = categoryKey
                                    }
                                )
                            } else {
                                CustomCategoryChip(
                                    selected = isSelected,
                                    label = "$displayName $count",
                                    activeThemeColor = activeThemeColor,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (isSelected) {
                                            activeCategoryOptions = if (activeCategoryOptions == categoryKey) null else categoryKey
                                        } else {
                                            onCategorySelected(categoryKey)
                                            activeCategoryOptions = null
                                            onScrollToTop()
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        activeCategoryOptions = categoryKey
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 3. PINNED LEFT (RTL End): Fixed Action Buttons (+ Quick Add, ⇅ Sort Menu)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // (+) Dedicated Quick Add Category Button
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(activeThemeColor.copy(alpha = 0.12f))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAddCategoryClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(id = R.string.habayeb_category_add_desc),
                            tint = activeThemeColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Sort Dropdown Button (⇅)
                Box {
                    val isSortActive = financialSortMode != 0 || historicalSortMode != 1
                    val sortBtnBg = if (isSortActive) activeThemeColor else MaterialTheme.colorScheme.surfaceVariant
                    val sortIconTint = if (isSortActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isSortMenuExpanded = true
                            },
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(sortBtnBg)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = stringResource(id = R.string.filter_sort_default),
                                tint = sortIconTint,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    HabayebSortDropdownMenu(
                        expanded = isSortMenuExpanded,
                        onDismissRequest = { isSortMenuExpanded = false },
                        financialSortMode = financialSortMode,
                        historicalSortMode = historicalSortMode,
                        onFinancialSortModeChanged = onFinancialSortModeChanged,
                        onHistoricalSortModeChanged = onHistoricalSortModeChanged,
                        onScrollToTop = onScrollToTop,
                        haptic = haptic,
                        neutralWhite = neutralWhite,
                        textPrimary = textPrimary,
                        backgroundLight = backgroundLight
                    )
                }
            }
        }

        // Category Options Sub-panel (When user long-presses or taps a category for options)
        activeCategoryOptions?.let { categoryKey ->
            Spacer(modifier = Modifier.height(4.dp))
            CategoryOptionsPanel(
                categoryKey = categoryKey,
                closedCategoryName = closedCategoryName,
                customCategories = customCategories,
                activeThemeColor = activeThemeColor,
                onDismiss = { activeCategoryOptions = null },
                onRename = onRenameCategory,
                onRenameClosed = onRenameClosedCategory,
                onDelete = { cat -> categoryToDelete = cat },
                onMoveLeft = onMoveCategoryLeft,
                onMoveRight = onMoveCategoryRight
            )
        }
    }

    // Category Delete Confirmation Dialog
    categoryToDelete?.let { cat ->
        CategoryDeleteConfirmationDialog(
            categoryName = cat.name,
            activeThemeColor = activeThemeColor,
            onDismiss = { categoryToDelete = null },
            onConfirmDelete = { deleteLinkedAccounts ->
                onDeleteCategory(cat, deleteLinkedAccounts)
                categoryToDelete = null
                activeCategoryOptions = null
            }
        )
    }
}
