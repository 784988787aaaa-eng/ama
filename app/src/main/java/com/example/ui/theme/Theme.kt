package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val MizanLightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADBFF),
    onPrimaryContainer = Color(0xFF24005A),
    secondary = CoralAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E4FF),
    onSecondaryContainer = Color(0xFF001C38),
    tertiary = CreditGreen,
    onTertiary = Color.White,
    tertiaryContainer = CreditContainerLight,
    onTertiaryContainer = Color(0xFF14532D),
    error = DebtRed,
    onError = Color.White,
    errorContainer = DebtContainerLight,
    onErrorContainer = Color(0xFF7F1D1D),
    background = IvoryBackground,
    surface = LightSurface,
    surfaceVariant = Color(0xFFF1F3F5),
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    outlineVariant = Color(0xFFEAEAEA),
    surfaceContainer = LightSurface,
    surfaceContainerHigh = Color(0xFFF8F9FA),
    surfaceContainerLow = LightSurface
)

private val MizanDarkColorScheme = darkColorScheme(
    primary = EmeraldDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF352478),
    onPrimaryContainer = Color(0xFFEADBFF),
    secondary = CoralDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF004881),
    onSecondaryContainer = Color(0xFFD3E4FF),
    tertiary = CreditGreenDark,
    onTertiary = Color.Black,
    tertiaryContainer = CreditContainerDark,
    onTertiaryContainer = Color(0xFFA7F3D0),
    error = DebtRedDark,
    onError = Color.Black,
    errorContainer = DebtContainerDark,
    onErrorContainer = Color(0xFFFECDD3),
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = Color(0xFF262626),
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
    outlineVariant = Color(0xFF2A2A2A),
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = Color(0xFF262626),
    surfaceContainerLow = Color(0xFF181818)
)

@Composable
fun ColorScheme.animated(
    animationSpec: AnimationSpec<Color> = tween(durationMillis = 280, easing = FastOutSlowInEasing)
): ColorScheme {
    return copy(
        primary = animateColorAsState(primary, animationSpec, label = "th_primary").value,
        onPrimary = animateColorAsState(onPrimary, animationSpec, label = "th_onPrimary").value,
        primaryContainer = animateColorAsState(primaryContainer, animationSpec, label = "th_primaryContainer").value,
        onPrimaryContainer = animateColorAsState(onPrimaryContainer, animationSpec, label = "th_onPrimaryContainer").value,
        inversePrimary = animateColorAsState(inversePrimary, animationSpec, label = "th_inversePrimary").value,
        secondary = animateColorAsState(secondary, animationSpec, label = "th_secondary").value,
        onSecondary = animateColorAsState(onSecondary, animationSpec, label = "th_onSecondary").value,
        secondaryContainer = animateColorAsState(secondaryContainer, animationSpec, label = "th_secondaryContainer").value,
        onSecondaryContainer = animateColorAsState(onSecondaryContainer, animationSpec, label = "th_onSecondaryContainer").value,
        tertiary = animateColorAsState(tertiary, animationSpec, label = "th_tertiary").value,
        onTertiary = animateColorAsState(onTertiary, animationSpec, label = "th_onTertiary").value,
        tertiaryContainer = animateColorAsState(tertiaryContainer, animationSpec, label = "th_tertiaryContainer").value,
        onTertiaryContainer = animateColorAsState(onTertiaryContainer, animationSpec, label = "th_onTertiaryContainer").value,
        background = animateColorAsState(background, animationSpec, label = "th_background").value,
        onBackground = animateColorAsState(onBackground, animationSpec, label = "th_onBackground").value,
        surface = animateColorAsState(surface, animationSpec, label = "th_surface").value,
        onSurface = animateColorAsState(onSurface, animationSpec, label = "th_onSurface").value,
        surfaceVariant = animateColorAsState(surfaceVariant, animationSpec, label = "th_surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(onSurfaceVariant, animationSpec, label = "th_onSurfaceVariant").value,
        surfaceTint = animateColorAsState(surfaceTint, animationSpec, label = "th_surfaceTint").value,
        inverseSurface = animateColorAsState(inverseSurface, animationSpec, label = "th_inverseSurface").value,
        inverseOnSurface = animateColorAsState(inverseOnSurface, animationSpec, label = "th_inverseOnSurface").value,
        error = animateColorAsState(error, animationSpec, label = "th_error").value,
        onError = animateColorAsState(onError, animationSpec, label = "th_onError").value,
        errorContainer = animateColorAsState(errorContainer, animationSpec, label = "th_errorContainer").value,
        onErrorContainer = animateColorAsState(onErrorContainer, animationSpec, label = "th_onErrorContainer").value,
        outline = animateColorAsState(outline, animationSpec, label = "th_outline").value,
        outlineVariant = animateColorAsState(outlineVariant, animationSpec, label = "th_outlineVariant").value,
        scrim = animateColorAsState(scrim, animationSpec, label = "th_scrim").value,
        surfaceBright = animateColorAsState(surfaceBright, animationSpec, label = "th_surfaceBright").value,
        surfaceDim = animateColorAsState(surfaceDim, animationSpec, label = "th_surfaceDim").value,
        surfaceContainer = animateColorAsState(surfaceContainer, animationSpec, label = "th_surfaceContainer").value,
        surfaceContainerHigh = animateColorAsState(surfaceContainerHigh, animationSpec, label = "th_surfaceContainerHigh").value,
        surfaceContainerHighest = animateColorAsState(surfaceContainerHighest, animationSpec, label = "th_surfaceContainerHighest").value,
        surfaceContainerLow = animateColorAsState(surfaceContainerLow, animationSpec, label = "th_surfaceContainerLow").value,
        surfaceContainerLowest = animateColorAsState(surfaceContainerLowest, animationSpec, label = "th_surfaceContainerLowest").value,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MizanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val targetColorScheme = if (darkTheme) MizanDarkColorScheme else MizanLightColorScheme
    val animatedColorScheme = targetColorScheme.animated()

    val customRippleConfiguration = RippleConfiguration(
        color = if (darkTheme) Color.White.copy(alpha = 0.15f) else Color(0xFF6B21A8).copy(alpha = 0.12f)
    )

    CompositionLocalProvider(
        LocalRippleConfiguration provides customRippleConfiguration
    ) {
        MaterialTheme(
            colorScheme = animatedColorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MizanTheme(darkTheme = darkTheme, content = content)
}
