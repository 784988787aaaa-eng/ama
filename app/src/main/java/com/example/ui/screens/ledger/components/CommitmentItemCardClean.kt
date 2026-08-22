package com.example.ui.screens.ledger.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entities.FixedCommitment
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SoftGreen
import com.example.ui.theme.SoftRed
import java.math.BigDecimal

private fun String.toWesternDigits(): String {
    var result = this
    val eastern = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    val western = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    for (i in 0..9) {
        result = result.replace(eastern[i], western[i])
    }
    return result
}

@Composable
fun CommitmentItemCardClean(
    index: Int,
    fc: FixedCommitment,
    allocated: BigDecimal,
    remaining: BigDecimal,
    totalCash: BigDecimal,
    currencySymbol: String,
    formatCurrency: (BigDecimal, String) -> String,
    totalCommitmentsCount: Int,
    onCheckedChange: (FixedCommitment, Boolean) -> Unit,
    onSetReorderTarget: (FixedCommitment) -> Unit,
    onReorderCommitment: (FixedCommitment, Int) -> Unit,
    onEditCommitmentClick: (FixedCommitment) -> Unit,
    onDeleteClick: (FixedCommitment) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isCovered = remaining.compareTo(BigDecimal.ZERO) <= 0
    val progressFraction = if (fc.targetAmount.compareTo(BigDecimal.ZERO) > 0) {
        (allocated.toDouble() / fc.targetAmount.toDouble()).coerceIn(0.0, 1.0).toFloat()
    } else 0f
    val progressPercent = (progressFraction * 100).toInt()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val itemGradient = remember {
        Brush.horizontalGradient(
            colors = listOf(
                NeonGreen,
                NeonCyan
            )
        )
    }

    Card(
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCovered) SoftGreen.copy(alpha = if (isDark) 0.10f else 0.04f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isCovered) SoftGreen.copy(alpha = 0.28f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Layer 1: Title & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox + Goal Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (isCovered) SoftGreen else Color.Transparent)
                            .border(
                                1.5.dp,
                                if (isCovered) SoftGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                CircleShape
                            )
                            .clickable {
                                onCheckedChange(fc, !isCovered)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCovered) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Text(
                        text = fc.name.toWesternDigits(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = if (isCovered) SoftGreen else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }

                // Remaining Badge + Progress %
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isCovered) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SoftGreen.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "مكتمل",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = SoftGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SoftRed.copy(alpha = 0.10f)
                        ) {
                            Text(
                                text = "متبقي: ${formatCurrency(remaining, currencySymbol)}".toWesternDigits(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = SoftRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "$progressPercent%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isCovered) SoftGreen else EmeraldPrimary
                    )
                }
            }

            // Layer 2: Sleek Goal Gradient Progress Bar (5dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))
                )
                if (progressFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.5.dp))
                            .background(if (isCovered) Brush.horizontalGradient(listOf(SoftGreen, SoftGreen)) else itemGradient)
                    )
                }
            }

            // Layer 3: Target Amount + Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Target Amount
                Text(
                    text = "المستهدف: ${formatCurrency(fc.targetAmount, currencySymbol)}".toWesternDigits(),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                // Micro Action Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Edit Button ✏️
                    IconButton(
                        onClick = { onEditCommitmentClick(fc) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(id = R.string.ledger_edit_commitment_title),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    // Delete Button 🗑️
                    IconButton(
                        onClick = {
                            onDeleteClick(fc)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.ledger_commitment_delete),
                            tint = SoftRed.copy(alpha = 0.65f),
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    // Reorder Handle ☰
                    var dragOffset by remember { mutableFloatStateOf(0f) }
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .clickable {
                                onSetReorderTarget(fc)
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { _ -> dragOffset = 0f },
                                    onDrag = { _, dragAmount ->
                                        dragOffset += dragAmount.y
                                        if (dragOffset > 60f) {
                                            dragOffset = 0f
                                            val pos = index + 2
                                            if (pos <= totalCommitmentsCount) {
                                                onReorderCommitment(fc, pos)
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        } else if (dragOffset < -60f) {
                                            dragOffset = 0f
                                            val pos = index
                                            if (pos >= 1) {
                                                onReorderCommitment(fc, pos)
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        }
                                    },
                                    onDragEnd = { dragOffset = 0f }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(id = R.string.ledger_reorder_apply),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}
