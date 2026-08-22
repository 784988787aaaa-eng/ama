package com.example.ui.screens.security.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary

/**
 * Standard PIN Keypad Digit / Action Button with custom styling and haptics support.
 */
@Composable
fun KeypadButton(
    text: String,
    isFunctional: Boolean,
    onClick: () -> Unit
) {
    val bg = remember(isFunctional) { if (isFunctional) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.12f) }
    val textCol = remember(isFunctional) { if (isFunctional) Color.White.copy(alpha = 0.8f) else Color.White }
    val textSize = remember(isFunctional) { if (isFunctional) 13.sp else 24.sp }
    val fontWeight = remember(isFunctional) { if (isFunctional) FontWeight.Medium else FontWeight.ExtraBold }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(bg)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = CircleShape
            )
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = interactionSource
            )
            .testTag("keypad_btn_$text"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textCol,
            fontSize = textSize,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Biometric / Action Icon Button on Keypad.
 */
@Composable
fun KeypadIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(EmeraldPrimary.copy(alpha = 0.15f))
            .border(
                width = 1.dp,
                color = EmeraldPrimary.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = interactionSource
            )
            .testTag("keypad_biometric_btn"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = EmeraldPrimary,
            modifier = Modifier.size(30.dp)
        )
    }
}

/**
 * Single Row of 3 Keypad Digit Buttons.
 */
@Composable
fun KeypadRow(
    row: List<String>,
    onKeyClick: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        row.forEach { digit ->
            KeypadButton(text = digit, isFunctional = false, onClick = { onKeyClick(digit) })
        }
    }
}
