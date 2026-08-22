package com.example.ui.screens.security.lock

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldPrimary

/**
 * Visual content for the PIN Passcode Keypad, including animated lock icon header,
 * progressive dot indicators, digit keypad, biometric action, and forgot PIN button.
 */
@Composable
fun PasscodeKeypadContent(
    enteredPasscode: String,
    isCheckingPasscode: Boolean,
    shakeOffsetPx: Float,
    isBiometricSupported: Boolean,
    onKeyPress: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onForgotClick: () -> Unit,
    onBiometricClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val row1 = remember { listOf("1", "2", "3") }
    val row2 = remember { listOf("4", "5", "6") }
    val row3 = remember { listOf("7", "8", "9") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header Area with Micro-animation
        val lockHeaderScale by animateFloatAsState(
            targetValue = if (isCheckingPasscode) 1.15f else if (enteredPasscode.isNotEmpty()) 1.05f else 1.0f,
            animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "lockHeaderScale"
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .scale(lockHeaderScale)
                    .clip(CircleShape)
                    .background(EmeraldPrimary.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(id = R.string.lock_app_locked_desc),
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.lock_ledger_locked),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.lock_enter_pin_prompt),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.62f)
            )
        }

        // 4 Round Indicators
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PasscodeDotIndicators(
                enteredLength = enteredPasscode.length,
                shakeOffsetPx = shakeOffsetPx
            )
        }

        // Keypad Area
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                KeypadRow(row = row1, onKeyClick = onKeyPress)
                KeypadRow(row = row2, onKeyClick = onKeyPress)
                KeypadRow(row = row3, onKeyClick = onKeyPress)

                // Last row with Biometric Icon / "0" / Delete
                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isBiometricSupported) {
                        KeypadIconButton(
                            icon = Icons.Default.Fingerprint,
                            contentDescription = stringResource(id = R.string.sec_biometric_title),
                            onClick = onBiometricClick
                        )
                    } else {
                        Box(modifier = Modifier.size(72.dp))
                    }

                    KeypadButton(text = "0", isFunctional = false) {
                        onKeyPress("0")
                    }

                    KeypadButton(text = stringResource(id = R.string.lock_delete_btn), isFunctional = true) {
                        onDeleteClick()
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(id = R.string.lock_forgot_pin),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoralAccent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onForgotClick() }
                        .padding(8.dp)
                        .testTag("lock_forgot_pin_btn"),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
