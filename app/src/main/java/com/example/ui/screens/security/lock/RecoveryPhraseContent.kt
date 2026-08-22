package com.example.ui.screens.security.lock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.StringUtils.toEnglishDigits
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.WarningAmber

/**
 * Visual content for the Recovery Phrase screen when the user forgets the PIN.
 */
@Composable
fun RecoveryPhraseContent(
    recoveryPhraseInput: String,
    onRecoveryPhraseChange: (String) -> Unit,
    recoveryHint: String?,
    showHintText: Boolean,
    onToggleHint: () -> Unit,
    onVerifyClick: () -> Unit,
    onReturnToKeypadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(CoralAccent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = stringResource(id = R.string.lock_recover_account),
                tint = CoralAccent,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.lock_recovery_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(id = R.string.lock_recovery_desc),
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = recoveryPhraseInput,
            onValueChange = { onRecoveryPhraseChange(it.toEnglishDigits()) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("recovery_phrase_input_lock"),
            label = { Text(stringResource(id = R.string.lock_recovery_phrase_hint), color = Color.White.copy(alpha = 0.6f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CoralAccent,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedLabelColor = CoralAccent,
                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (recoveryPhraseInput.isNotBlank()) {
                        onVerifyClick()
                    }
                }
            )
        )

        if (!recoveryHint.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .clickable { onToggleHint() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (showHintText) stringResource(id = R.string.lock_hide_hint) else stringResource(id = R.string.lock_show_hint),
                    color = WarningAmber,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        AnimatedVisibility(visible = showHintText && !recoveryHint.isNullOrBlank()) {
            Text(
                text = stringResource(id = R.string.lock_hint_prefix, recoveryHint ?: ""),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onVerifyClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("verify_recovery_unlock_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = CoralAccent),
            shape = RoundedCornerShape(16.dp),
            enabled = recoveryPhraseInput.isNotBlank()
        ) {
            Text(
                text = stringResource(id = R.string.lock_verify_and_unlock),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onReturnToKeypadClick
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = stringResource(id = R.string.lock_back_to_keypad_desc),
                    tint = Color.LightGray
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(id = R.string.lock_return_to_keypad),
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
