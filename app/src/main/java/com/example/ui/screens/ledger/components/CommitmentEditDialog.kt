package com.example.ui.screens.ledger.components

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.local.entities.FixedCommitment
import com.example.ui.theme.EmeraldPrimary
import java.math.BigDecimal

@Composable
fun CommitmentEditDialog(
    showCommitmentDialog: Boolean,
    editingCommitment: FixedCommitment?,
    onDismissRequest: () -> Unit,
    onSaveCommitment: (name: String, targetAmount: BigDecimal, currentProgress: BigDecimal) -> Unit,
    onDeleteCommitment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!showCommitmentDialog) return

    val context = LocalContext.current
    val nameFocus = remember { FocusRequester() }
    val targetFocus = remember { FocusRequester() }
    val progressFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(view) {
        val window = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        onDispose {}
    }

    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    val initialName = editingCommitment?.name ?: ""
    val initialTarget = editingCommitment?.targetAmount?.let {
        if (it.compareTo(BigDecimal.ZERO) > 0) it.stripTrailingZeros().toPlainString() else ""
    } ?: ""
    val initialProgress = editingCommitment?.currentProgress?.let {
        if (it.compareTo(BigDecimal.ZERO) > 0) it.stripTrailingZeros().toPlainString() else ""
    } ?: ""

    var obligationNameTfv by remember(editingCommitment) {
        mutableStateOf(TextFieldValue(text = initialName, selection = TextRange(initialName.length)))
    }
    var targetAmtTfv by remember(editingCommitment) {
        mutableStateOf(TextFieldValue(text = initialTarget, selection = TextRange(initialTarget.length)))
    }
    var progressAmtTfv by remember(editingCommitment) {
        mutableStateOf(TextFieldValue(text = initialProgress, selection = TextRange(initialProgress.length)))
    }

    val obligationName = obligationNameTfv.text
    val targetAmtStr = targetAmtTfv.text
    val progressAmtStr = progressAmtTfv.text

    LaunchedEffect(Unit) {
        try {
            kotlinx.coroutines.delay(120)
            if (editingCommitment == null) {
                nameFocus.requestFocus()
            } else {
                targetFocus.requestFocus()
            }
            keyboardController?.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                modifier = modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth(0.88f)
                    .clip(RoundedCornerShape(22.dp)),
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
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Clean Title without emoji
                    Text(
                        text = if (editingCommitment != null) "تعديل الهدف المالي" else "إضافة التزام مالي",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // 2. Goal Name Input Field (Full Width, Sleek)
                    OutlinedTextField(
                        value = obligationNameTfv,
                        onValueChange = { if (editingCommitment == null) obligationNameTfv = it },
                        enabled = (editingCommitment == null),
                        placeholder = {
                            Text(
                                text = "اسم الالتزام (مثال: إيجار، قسط...)",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                            disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .focusRequester(nameFocus),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { targetFocus.requestFocus() }),
                        textStyle = TextStyle(
                            textAlign = TextAlign.Right,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    // 3. Amount Inputs: Target (Right) & Current (Left) Side-by-Side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Target Amount Field (Right in RTL)
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = targetAmtTfv,
                                onValueChange = { targetAmtTfv = it },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onNext = { progressFocus.requestFocus() }),
                                placeholder = {
                                    Text(
                                        text = "المبلغ المستهدف",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .focusRequester(targetFocus),
                                textStyle = TextStyle(
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        // Current Available Amount Field (Left in RTL, Optional)
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = progressAmtTfv,
                                onValueChange = { progressAmtTfv = it },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                placeholder = {
                                    Text(
                                        text = "المتوفر (اختياري)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .focusRequester(progressFocus),
                                textStyle = TextStyle(
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 4. Sleek Actions: Save & Cancel Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cancel Button (Soft & Minimalist)
                        OutlinedButton(
                            onClick = onDismissRequest,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            contentPadding = PaddingValues(vertical = 0.dp)
                        ) {
                            Text(
                                text = "إلغاء",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp
                            )
                        }

                        // Save Button (Luxurious Capsule)
                        Button(
                            onClick = {
                                val tar = targetAmtStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                val prg = progressAmtStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                if (obligationName.isNotBlank() && tar > BigDecimal.ZERO) {
                                    com.example.ui.helper.VibrationHelper.triggerSuccessVibration(context)
                                    onSaveCommitment(obligationName, tar, prg)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(40.dp),
                            contentPadding = PaddingValues(vertical = 0.dp)
                        ) {
                            Text(
                                text = if (editingCommitment != null) "حفظ التعديل" else "حفظ الهدف",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
