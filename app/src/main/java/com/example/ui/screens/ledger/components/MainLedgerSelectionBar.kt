package com.example.ui.screens.ledger.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.EmeraldPrimary

@Composable
fun MainLedgerSelectionBar(
    isSelectionActive: Boolean,
    isDaySelectionMode: Boolean,
    isSelectAllChecked: Boolean,
    selectedDayKeysCountText: String,
    selectedTxCount: Int,
    allKeys: List<String>,
    selectedDayKeys: MutableList<String>,
    haptic: HapticFeedback,
    onClearSelection: () -> Unit,
    onDeleteClick: () -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isSelectionActive,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier.padding(bottom = bottomPadding + 16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .shadow(16.dp, RoundedCornerShape(30.dp), spotColor = Color.Black.copy(alpha = 0.1f))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(30.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(30.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Cancel Button
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClearSelection()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(id = R.string.ledger_cancel_all),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Selection Info & Select All (for days)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isDaySelectionMode) {
                            if (selectedDayKeys.size == allKeys.size) {
                                selectedDayKeys.clear()
                            } else {
                                selectedDayKeys.clear()
                                selectedDayKeys.addAll(allKeys)
                            }
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (isDaySelectionMode) {
                    Icon(
                        imageVector = if (isSelectAllChecked) Icons.Default.Check else Icons.Default.List,
                        contentDescription = stringResource(id = R.string.ledger_select_all),
                        tint = if (isSelectAllChecked) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Text(
                    text = if (isDaySelectionMode) {
                        selectedDayKeysCountText
                    } else {
                        stringResource(id = R.string.text_selected_count, selectedTxCount)
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Delete Button
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDeleteClick()
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(id = R.string.ledger_bulk_delete_days_desc),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
