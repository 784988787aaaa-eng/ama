package com.example.ui.screens.ledger.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.local.entities.FixedCommitment
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SoftRed
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.MonthLedger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun DeleteDaysConfirmDialog(
    showDeleteDaysDialog: Boolean,
    onDismiss: () -> Unit,
    monthlyLedger: List<MonthLedger>,
    selectedDayKeys: MutableList<String>,
    viewModel: FinanceViewModel,
    scope: CoroutineScope,
    context: Context,
    onSuccess: () -> Unit
) {
    if (showDeleteDaysDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(id = R.string.ledger_bulk_delete_days_title),
                    fontWeight = FontWeight.Bold,
                    color = SoftRed,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.ledger_bulk_delete_days_msg),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDismiss()
                        scope.launch {
                            val txsToDelete = mutableListOf<String>()
                            monthlyLedger.forEach { ml ->
                                ml.days.forEach { day ->
                                    val dayKey = "${ml.monthKey}_${day.dayNumber}"
                                    if (selectedDayKeys.contains(dayKey)) {
                                        day.transactions.forEach { tx ->
                                            txsToDelete.add(tx.id)
                                        }
                                    }
                                }
                            }
                            viewModel.deleteTransactionsBulk(txsToDelete, context.getString(R.string.ledger_bulk_delete_days_desc))
                            onSuccess()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftRed)
                ) {
                    Text(stringResource(id = R.string.ledger_bulk_delete_days_confirm_btn), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(id = R.string.common_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        )
    }
}

@Composable
fun ReorderCommitmentDialog(
    reorderCommitmentTarget: FixedCommitment?,
    commitmentsSize: Int,
    onDismiss: () -> Unit,
    onApplyReorder: (FixedCommitment, Int) -> Unit,
    context: Context
) {
    if (reorderCommitmentTarget != null) {
        var targetPositionStr by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }
        val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

        val view = androidx.compose.ui.platform.LocalView.current
        DisposableEffect(view) {
            val window = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            onDispose {}
        }

        LaunchedEffect(Unit) {
            try {
                kotlinx.coroutines.delay(120)
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val applyAction = {
            val pos = targetPositionStr.toIntOrNull()
            if (pos == null || pos < 1 || pos > commitmentsSize) {
                errorMsg = "أدخل رقماً بين 1 و $commitmentsSize"
            } else {
                onApplyReorder(reorderCommitmentTarget, pos)
            }
        }

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = true
            )
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .fillMaxWidth(0.78f)
                        .clip(RoundedCornerShape(20.dp)),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Clean Title
                        Text(
                            text = "تغيير موضع الالتزام",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )

                        // Instruction label
                        Text(
                            text = "نقل «${reorderCommitmentTarget.name}» إلى الترتيب رقم:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        // Compact centered input field (70dp width x 44dp height)
                        OutlinedTextField(
                            value = targetPositionStr,
                            onValueChange = {
                                targetPositionStr = it
                                errorMsg = ""
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { applyAction() }),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .width(70.dp)
                                .height(44.dp)
                                .focusRequester(focusRequester),
                            textStyle = TextStyle(
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            isError = errorMsg.isNotEmpty()
                        )

                        // Range reminder or error text
                        if (errorMsg.isNotEmpty()) {
                            Text(
                                text = errorMsg,
                                color = SoftRed,
                                fontSize = 10.5.sp,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "(من 1 إلى $commitmentsSize)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Compact action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                contentPadding = PaddingValues(vertical = 0.dp)
                            ) {
                                Text(
                                    text = "إلغاء",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = { applyAction() },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(36.dp),
                                contentPadding = PaddingValues(vertical = 0.dp)
                            ) {
                                Text(
                                    text = "تطبيق",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
