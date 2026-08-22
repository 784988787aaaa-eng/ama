package com.example.ui.screens.ledger.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.FixedCommitment
import com.example.ui.theme.EmeraldPrimary
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * Unified Facade for Fixed Commitments & Goals Management Dialog.
 * Beautifully modularized into independent, reusable subcomponents.
 */
@Composable
fun CommitmentsListDialog(
    showCommitmentsListSheet: Boolean,
    commitments: List<FixedCommitment>,
    computedCommitments: List<Triple<FixedCommitment, BigDecimal, BigDecimal>>,
    totalCash: BigDecimal,
    currencySymbol: String,
    formatCurrency: (BigDecimal, String) -> String,
    onDismissRequest: () -> Unit,
    onAddCommitmentClick: () -> Unit,
    onEditCommitmentClick: (FixedCommitment) -> Unit,
    onDeleteCommitment: (String) -> Unit,
    onReorderCommitment: (FixedCommitment, Int) -> Unit,
    onCheckedChange: (FixedCommitment, Boolean) -> Unit,
    onSetReorderTarget: (FixedCommitment) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!showCommitmentsListSheet) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val commitmentsScaleFraction = remember { Animatable(0f) }
    var commitmentToDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showCommitmentsListSheet) {
        commitmentsScaleFraction.animateTo(
            targetValue = 1f,
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        )
    }

    val closeAction = {
        scope.launch {
            commitmentsScaleFraction.animateTo(
                targetValue = 0f,
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            )
            onDismissRequest()
        }
    }

    Dialog(onDismissRequest = { closeAction() }) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .graphicsLayer(
                        scaleX = commitmentsScaleFraction.value,
                        scaleY = commitmentsScaleFraction.value,
                        alpha = commitmentsScaleFraction.value,
                        transformOrigin = TransformOrigin(0.5f, 0.8f)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Sleek Header: [ ✕ Close ] - [ الأهداف والالتزامات ] - [ 🔗 Share ]
                    CommitmentHeaderClean(
                        onCloseClick = { closeAction() },
                        onShareClick = {
                            CommitmentShareHelper.shareCommitments(
                                context = context,
                                commitments = commitments,
                                computedCommitments = computedCommitments,
                                totalCash = totalCash,
                                currencySymbol = currencySymbol,
                                formatCurrency = formatCurrency
                            )
                        }
                    )

                    if (commitments.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "لا توجد أهداف أو التزامات مدونة حالياً",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "اضغط على الزر أدناه لإضافة هدفك المالي الأول",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        val (totalTargetSum, totalAllocatedSum, coveredCount) = remember(commitments, computedCommitments) {
                            val target = commitments.fold(BigDecimal.ZERO) { acc, fc -> acc.add(fc.targetAmount) }
                            val allocated = computedCommitments.fold(BigDecimal.ZERO) { acc, triple -> acc.add(triple.second) }
                            val covered = computedCommitments.count { it.third.compareTo(BigDecimal.ZERO) <= 0 }
                            Triple(target, allocated, covered)
                        }

                        // 2. Matching Gradient Overview Card
                        CommitmentSummaryGradientCard(
                            totalTargetSum = totalTargetSum,
                            totalAllocatedSum = totalAllocatedSum,
                            coveredCount = coveredCount,
                            totalCount = commitments.size,
                            currencySymbol = currencySymbol,
                            formatCurrency = formatCurrency
                        )

                        // 3. Goal Items (3-layer sleek cards)
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth()
                        ) {
                            itemsIndexed(
                                items = computedCommitments,
                                key = { _, item -> item.first.name }
                            ) { index, (fc, allocated, remaining) ->
                                CommitmentItemCardClean(
                                    index = index,
                                    fc = fc,
                                    allocated = allocated,
                                    remaining = remaining,
                                    totalCash = totalCash,
                                    currencySymbol = currencySymbol,
                                    formatCurrency = formatCurrency,
                                    totalCommitmentsCount = commitments.size,
                                    onCheckedChange = onCheckedChange,
                                    onSetReorderTarget = onSetReorderTarget,
                                    onReorderCommitment = onReorderCommitment,
                                    onEditCommitmentClick = onEditCommitmentClick,
                                    onDeleteClick = { fcToDelete ->
                                        commitmentToDelete = fcToDelete.name
                                    }
                                )
                            }
                        }
                    }

                    // 4. Primary Bottom Action Button: [ + إضافة التزام / هدف جديد ]
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = EmeraldPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onAddCommitmentClick()
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "+ إضافة التزام / هدف جديد",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Delete Confirmation Dialog
                CommitmentDeleteConfirmationDialog(
                    commitmentName = commitmentToDelete,
                    onConfirmDelete = { name ->
                        onDeleteCommitment(name)
                    },
                    onDismiss = {
                        commitmentToDelete = null
                    }
                )
            }
        }
    }
}
