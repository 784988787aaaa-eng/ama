package com.example.ui.screens.habayeb.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.state.CustomerUiState

@Composable
fun HabayebListSection(
    listState: LazyListState,
    filteredCustomers: List<CustomerUiState>,
    selectedFilterTab: Int,
    selectedCategory: String? = null,
    selectedCustomerIds: List<String>,
    isPrivacyMode: Boolean,
    pinnedCustomerIds: Set<String>,
    isMultiSelectActive: Boolean,
    activeThemeColor: Color,
    activeSubColor: Color,
    currencySymbol: String,
    haptic: HapticFeedback,
    onCustomerClick: (CustomerUiState) -> Unit,
    onCustomerLongClick: (String) -> Unit,
    onQuickAdd: (CustomerUiState) -> Unit,
    getCustomerCategory: (String) -> String?,
    onRemoveFromCategory: (String) -> Unit,
    highlightedCustomerId: String? = null,
    modifier: Modifier = Modifier
) {
    val scrollbarMetrics = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            val totalItems = layoutInfo.totalItemsCount
            if (visibleItems.isEmpty() || totalItems == 0) {
                Pair(0f, 0f)
            } else {
                val sumHeight = visibleItems.sumOf { it.size }
                val avgItemHeight = sumHeight.toFloat() / visibleItems.size
                val estimatedTotalHeight = avgItemHeight * totalItems
                val estimatedScrollOffset = (listState.firstVisibleItemIndex * avgItemHeight) + listState.firstVisibleItemScrollOffset
                val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                val sizeFraction = if (estimatedTotalHeight > 0f) (viewportHeight.toFloat() / estimatedTotalHeight).coerceIn(0.1f, 1f) else 1f
                val maxScrollOffset = estimatedTotalHeight - viewportHeight
                val offsetFraction = if (maxScrollOffset > 0f) (estimatedScrollOffset / maxScrollOffset).coerceIn(0f, 1f) else 0f
                Pair(offsetFraction, sizeFraction)
            }
        }
    }

    val catKey = selectedCategory ?: "all"

    LazyColumn(
        state = listState,
        modifier = modifier
            .drawWithContent {
                drawContent()
                val metrics = scrollbarMetrics.value
                val offsetFraction = metrics.first
                val sizeFraction = metrics.second
                if (sizeFraction < 1.0f) {
                    val thicknessPx = 2.5.dp.toPx()
                    val marginPx = 6.dp.toPx()
                    val thumbLeft = marginPx
                    val thumbHeight = (size.height * sizeFraction).coerceIn(40.dp.toPx(), size.height)
                    val maxScrollableHeight = size.height - thumbHeight
                    val thumbTop = maxScrollableHeight * offsetFraction

                    drawRoundRect(
                        color = activeThemeColor.copy(alpha = 0.85f),
                        topLeft = Offset(x = thumbLeft, y = thumbTop),
                        size = Size(width = thicknessPx, height = thumbHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                    )
                }
            },
        contentPadding = PaddingValues(top = 2.dp, bottom = 72.dp)
    ) {
        if (filteredCustomers.isEmpty()) {
            item(key = "empty_state_${catKey}_$selectedFilterTab") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillParentMaxHeight(0.6f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🤝", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = when (selectedFilterTab) {
                                1 -> stringResource(id = R.string.habayeb_no_debtors)
                                2 -> stringResource(id = R.string.habayeb_no_creditors)
                                else -> stringResource(id = R.string.habayeb_empty_list)
                            },
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        } else {
            items(
                items = filteredCustomers,
                key = { it.id },
                contentType = { "customer_row" }
            ) { customer ->
                val isPinned = pinnedCustomerIds.contains(customer.id)
                val isHighlighted = customer.id == highlightedCustomerId
                val isSelected = selectedCustomerIds.contains(customer.id)
                RenderCustomerRowItem(
                    customer = customer,
                    isPinned = isPinned,
                    isHighlighted = isHighlighted,
                    isSelected = isSelected,
                    activeThemeColor = activeThemeColor,
                    activeSubColor = activeSubColor,
                    haptic = haptic,
                    onCustomerClick = onCustomerClick,
                    onCustomerLongClick = onCustomerLongClick,
                    onQuickAdd = onQuickAdd,
                    getCustomerCategory = getCustomerCategory,
                    onRemoveFromCategory = onRemoveFromCategory
                )
            }
        }
    }
}

@Composable
private fun RenderCustomerRowItem(
    customer: CustomerUiState,
    isPinned: Boolean,
    isHighlighted: Boolean,
    isSelected: Boolean,
    activeThemeColor: Color,
    activeSubColor: Color,
    haptic: HapticFeedback,
    onCustomerClick: (CustomerUiState) -> Unit,
    onCustomerLongClick: (String) -> Unit,
    onQuickAdd: (CustomerUiState) -> Unit,
    getCustomerCategory: (String) -> String?,
    onRemoveFromCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val onRemoveFromCategoryClick = remember(customer.id, onRemoveFromCategory) {
        { onRemoveFromCategory(customer.id) }
    }

    Box(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 1.5.dp)
    ) {
        CustomerItemRow(
            isPinned = isPinned,
            isHighlighted = isHighlighted,
            customer = customer,
            isSelected = isSelected,
            activeThemeColor = activeThemeColor,
            activeSubColor = activeSubColor,
            haptic = haptic,
            onCustomerClick = onCustomerClick,
            onCustomerLongClick = onCustomerLongClick,
            onQuickAdd = onQuickAdd,
            currentActiveCategory = getCustomerCategory(customer.id),
            onRemoveFromCategory = onRemoveFromCategoryClick
        )
    }
}
