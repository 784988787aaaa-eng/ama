package com.example.ui.screens.security.components

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.EmeraldPrimary

import com.example.domain.StringUtils.toEnglishDigits

@Composable
fun SecuritySetupForm(
    passcode: String,
    onPasscodeChange: (String) -> Unit,
    confirmPasscode: String,
    onConfirmPasscodeChange: (String) -> Unit,
    recoveryPhrase: String,
    onRecoveryPhraseChange: (String) -> Unit,
    recoveryHint: String,
    onRecoveryHintChange: (String) -> Unit,
    checkAcknowledged: Boolean,
    onCheckAcknowledgedChange: (Boolean) -> Unit,
    isSaving: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val isDark = MaterialTheme.colorScheme.background.run { red < 0.5f }

    var passcodeVisible by remember { mutableStateOf(false) }
    var confirmPasscodeVisible by remember { mutableStateOf(false) }

    val passcodeFocus = remember { FocusRequester() }
    val confirmPasscodeFocus = remember { FocusRequester() }
    val recoveryPhraseFocus = remember { FocusRequester() }
    val recoveryHintFocus = remember { FocusRequester() }

    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    // تأثير برامجي فوري لاستدعاء لوحة المفاتيح والتركيز التلقائي على الحقل الأول فور الفتح
    LaunchedEffect(Unit) {
        try {
            kotlinx.coroutines.android.awaitFrame()
            passcodeFocus.requestFocus()
            keyboardController?.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp) // تقارب الحقول بمسافات دقيقة واحترافية تمنع التشتت
        ) {
            Text(
                text = stringResource(id = R.string.sec_setup_title),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 4.dp)
            )

            // PASSCODE INPUT
            OutlinedTextField(
                value = passcode,
                onValueChange = { input ->
                    val clean = input.toEnglishDigits()
                    if (clean.length <= 4 && clean.all { c -> c.isDigit() }) {
                        onPasscodeChange(clean)
                        if (clean.length == 4) {
                            confirmPasscodeFocus.requestFocus()
                        }
                    }
                },
                label = { Text(stringResource(id = R.string.sec_label_code), fontSize = 12.sp) },
                placeholder = { Text(stringResource(id = R.string.sec_placeholder_code), fontSize = 12.sp) },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { passcodeVisible = !passcodeVisible }) {
                        Icon(
                            imageVector = if (passcodeVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Visibility",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                visualTransformation = if (passcodeVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { confirmPasscodeFocus.requestFocus() }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else EmeraldPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(passcodeFocus)
                    .testTag("pin_code_input")
            )

            // CONFIRM PASSCODE INPUT
            OutlinedTextField(
                value = confirmPasscode,
                onValueChange = { input ->
                    val clean = input.toEnglishDigits()
                    if (clean.length <= 4 && clean.all { c -> c.isDigit() }) {
                        onConfirmPasscodeChange(clean)
                        if (clean.length == 4) {
                            recoveryPhraseFocus.requestFocus()
                        }
                    }
                },
                label = { Text(stringResource(id = R.string.sec_label_confirm), fontSize = 12.sp) },
                placeholder = { Text(stringResource(id = R.string.sec_placeholder_confirm), fontSize = 12.sp) },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { confirmPasscodeVisible = !confirmPasscodeVisible }) {
                        Icon(
                            imageVector = if (confirmPasscodeVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Visibility",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                visualTransformation = if (confirmPasscodeVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { recoveryPhraseFocus.requestFocus() }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else EmeraldPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(confirmPasscodeFocus)
                    .testTag("pin_code_confirm_input")
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), 
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Text(
                text = stringResource(id = R.string.sec_recovery_title),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // RECOVERY PHRASE
            OutlinedTextField(
                value = recoveryPhrase,
                onValueChange = onRecoveryPhraseChange,
                label = { Text(stringResource(id = R.string.sec_label_recovery), fontSize = 12.sp) },
                placeholder = { Text(stringResource(id = R.string.sec_placeholder_recovery), fontSize = 12.sp) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Start,
                    fontSize = 14.sp
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { recoveryHintFocus.requestFocus() }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else EmeraldPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(recoveryPhraseFocus)
                    .testTag("recovery_phrase_input")
            )

            // RECOVERY HINT
            OutlinedTextField(
                value = recoveryHint,
                onValueChange = onRecoveryHintChange,
                label = { Text(stringResource(id = R.string.sec_label_hint), fontSize = 12.sp) },
                placeholder = { Text(stringResource(id = R.string.sec_placeholder_hint), fontSize = 12.sp) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Start,
                    fontSize = 14.sp
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else EmeraldPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(recoveryHintFocus)
                    .testTag("recovery_hint_input")
            )

            // ACK CHECKBOX
            val ackBg = if (isDark) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
            }
            val ackText = MaterialTheme.colorScheme.onSurface
            val checkboxBorderColor = MaterialTheme.colorScheme.outline

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ackBg)
                    .clickable { onCheckAcknowledgedChange(!checkAcknowledged) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.sec_checkbox_ack),
                    fontSize = 11.sp,
                    color = ackText,
                    textAlign = TextAlign.Start,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                Checkbox(
                    checked = checkAcknowledged,
                    onCheckedChange = onCheckAcknowledgedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = EmeraldPrimary,
                        uncheckedColor = checkboxBorderColor
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }

            val isValid = passcode.length == 4 &&
                    confirmPasscode == passcode &&
                    recoveryPhrase.isNotBlank() &&
                    checkAcknowledged &&
                    !isSaving

            // SAVE & ACTIVATE BUTTON
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                ),
                enabled = isValid,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(top = 4.dp) // تباعد دقيق ليعطي الزر متنفساً حركياً ملحوظاً
                    .testTag("security_save_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White, 
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.sec_btn_activate),
                        color = if (isValid) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
