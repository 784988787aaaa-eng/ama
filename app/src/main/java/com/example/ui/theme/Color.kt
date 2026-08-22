package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Mizan Al-Dar Color Palette - Styled with the premium Violet & Neon Cyan "الدفتر الذكي" Palette
val EmeraldPrimary = Color(0xFF4B36A2)      // Premium Glowing Violet/Purple (#4B36A2)
val EmeraldLight = Color(0xFF8C7CFF)        // Lighter Purple/Lavender for beautiful gradients (#8C7CFF)
val CoralAccent = Color(0xFF00B2FE)         // Neon Cyan / Electric Blue Accent (#00B2FE)

val EmeraldDark = Color(0xFF4B36A2)         // Rich Glowing Violet/Purple directly in dark mode
val CoralDark = Color(0xFF00B2FE)           // Rich Glowing Neon Cyan directly in dark mode
val IvoryBackground = Color(0xFFF8F9FA)     // Solid high-contrast background (#F8F9FA)

// Financial semantic colors - High Contrast Display Tokens
val SoftRed = Color(0xFFD32F2F)            // Debt (لنا / مدين) - Vivid Solid Red Light (#D32F2F)
val SoftGreen = Color(0xFF2E7D32)          // Credit (علينا / دائن) - Deep Emerald Green Light (#2E7D32)

val CreditGreen = Color(0xFF2E7D32)        // Credit Green Light (#2E7D32)
val DebtRed = Color(0xFFD32F2F)            // Debt Red Light (#D32F2F)
val CreditGreenDark = Color(0xFF51CF66)    // Credit Green Dark (Soft neon green #51CF66)
val DebtRedDark = Color(0xFFFF6B6B)        // Debt Red Dark (Soft high-contrast red #FF6B6B)

// Financial Card / Container backgrounds (Solid Safe Colors for replacement & low-contrast screens)
val CreditContainerLight = Color(0xFFF0FDF4) // خلفية بطاقة "علينا" الخضراء في الوضع النهاري (#F0FDF4)
val CreditContainerDark = Color(0xFF16281E)  // خلفية بطاقة "علينا" الخضراء في الوضع الليلي (#16281E)
val CreditBorderLight = Color(0xFFA7F3D0)
val CreditBorderDark = Color(0xFF1B4D2E)

val DebtContainerLight = Color(0xFFFDF2F2)   // خلفية بطاقة "لنا" الحمراء في الوضع النهاري (#FDF2F2)
val DebtContainerDark = Color(0xFF2C1A1D)    // خلفية بطاقة "لنا" الحمراء في الوضع الليلي (#2C1A1D)
val DebtBorderLight = Color(0xFFFECDD3)
val DebtBorderDark = Color(0xFF531A21)

// Selection colors
val SelectionGreen = Color(0xFF10B981)
val SelectionGreenContainerLight = Color(0xFFE6F4EA)
val SelectionGreenContainerDark = Color(0xFF152D1F)

val DarkBackground = Color(0xFF121212)     // Pure Standard Deep Dark (#121212)
val DarkSurface = Color(0xFF1E1E1E)        // High-Contrast Solid Dark Surface (#1E1E1E)
val LightSurface = Color(0xFFFFFFFF)       // Pure Clean White Surface (#FFFFFF)

val TextPrimaryDark = Color(0xFFF5F5F5)     // Bright Crisp White for dark mode contrast
val TextSecondaryDark = Color(0xFFAAAAAA)   // High-contrast muted secondary text dark
val TextPrimaryLight = Color(0xFF1A1A1A)    // Deep dark crisp primary text light
val TextSecondaryLight = Color(0xFF555555)  // Clear legible secondary text light

val BorderDark = Color(0xFF333333)          // High contrast dark border
val BorderLight = Color(0xFFE0E0E0)         // Clean light border

// Modern Pre-allocated Static Gradients (Zero-allocation during recomposition)
val PrimaryGradient = Brush.linearGradient(
    colors = listOf(EmeraldPrimary, EmeraldLight)
)

val CoralGradient = Brush.linearGradient(
    colors = listOf(CoralAccent, Color(0xFF0284C7))
)

val IncomeGradientLight = Brush.linearGradient(
    colors = listOf(Color(0xFFF0FDF4), LightSurface)
)

val IncomeGradientDark = Brush.linearGradient(
    colors = listOf(CreditContainerDark, DarkSurface)
)

val ExpenseGradientLight = Brush.linearGradient(
    colors = listOf(Color(0xFFFDF2F2), LightSurface)
)

val ExpenseGradientDark = Brush.linearGradient(
    colors = listOf(DebtContainerDark, DarkSurface)
)

val SelectedItemGradientLight = Brush.linearGradient(
    colors = listOf(SelectionGreenContainerLight, Color(0xFFD1FAE5))
)

val SelectedItemGradientDark = Brush.linearGradient(
    colors = listOf(SelectionGreenContainerDark, Color(0xFF121F17))
)

val NeonGreenCyanGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF00E676), Color(0xFF00B0FF))
)

val VioletHeroGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF4B36A2), Color(0xFF7C3AED), Color(0xFF8C7CFF))
)

val HeaderCardGradientDark = Brush.linearGradient(
    colors = listOf(Color(0xFF1E1E1E), Color(0xFF262626))
)

val HeaderCardGradientLight = Brush.linearGradient(
    colors = listOf(Color(0xFFFFFFFF), Color(0xFFF8F9FA))
)

val GoldLicenseGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFFB45309))
)

val WarningGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFF59E0B), Color(0xFFEF4444))
)

val SplashSweepGradient = Brush.sweepGradient(
    colorStops = arrayOf(
        0.0f to Color(0xFF2563EB),
        0.5f to Color(0xFF10B981),
        1.0f to Color(0xFF2563EB)
    )
)

val SplashRadialGlow = Brush.radialGradient(
    colors = listOf(
        Color(0xFF2563EB).copy(alpha = 0.22f),
        Color(0xFF10B981).copy(alpha = 0.15f),
        Color.Transparent
    )
)

// Specific Functional Palette Colors
val WhatsAppGreen = Color(0xFF128C7E)
val WhatsAppLightGreen = Color(0xFF25D366)
val WarningAmber = Color(0xFFF59E0B)
val WarningAmberBg = Color(0xFFFFF8E1)
val WarningAmberBorder = Color(0xFFFFB300)
val WarningDarkRedText = Color(0xFFB71C1C)
val WarningOrangeButton = Color(0xFFE65100)
val LicenseGreenBg = Color(0xFFE8F5E9)
val LicenseGreenText = Color(0xFF2E7D32)
val LicenseBadgeGreenText = Color(0xFF1B5E20)
val InfoBlue = Color(0xFF3B82F6)
val InfoBlueBgLight = Color(0xFFEFF6FF)
val InfoBlueBgDark = Color(0xFF1E293B)
val InfoBlueTextLight = Color(0xFF1D4ED8)
val InfoBlueTextDark = Color(0xFF60A5FA)

// Slate & UI Accents
val Slate50 = Color(0xFFF8FAFC)
val Slate100 = Color(0xFFF1F5F9)
val Slate200 = Color(0xFFE2E8F0)
val Slate300 = Color(0xFFCBD5E1)
val Slate400 = Color(0xFF94A3B8)
val Slate500 = Color(0xFF64748B)
val Slate600 = Color(0xFF475569)
val Slate700 = Color(0xFF334155)
val Slate800 = Color(0xFF1E293B)
val Slate900 = Color(0xFF0F172A)

// Status & Action tokens
val NeonGreen = Color(0xFF00E676)
val NeonCyan = Color(0xFF00B0FF)
val IndigoAccent = Color(0xFF6366F1)
val PurpleAccent = Color(0xFF8B5CF6)
val SoftLavender = Color(0xFFC4B5FD)
val WarningRed = Color(0xFFE53935)
val WarningRedBorder = Color(0xFFB91C1C)
val WarningRedBorderLight = Color(0xFFD93025)
val LightRedTint = Color(0xFFFF8A80)

// Alert Gold (Yellow/Amber badge tokens)
val AlertGoldBgDark = Color(0xFF451A03)
val AlertGoldBorderDark = Color(0xFF92400E)
val AlertGoldTextDark = Color(0xFFFBBF24)
val AlertGoldTextLight = Color(0xFFB45309)

// Success Green badge tokens
val SuccessGreenBgDark = Color(0xFF064E3B)
val SuccessGreenBgLight = Color(0xFFE6F4EA)
val SuccessGreenBorderDark = Color(0xFF10B981)
val SuccessGreenBorderLight = Color(0xFF137333)
val MutedTextDark = Color(0xFF9AA0A6)
val MutedTextLight = Color(0xFF5F6368)
val DarkNeutralTrack = Color(0xFF2D2D2D)
val LightNeutralTrack = Color(0xFFEEEEEE)

// Chip Filter Color Tokens (Solid High-Contrast Safe Values)
val ChipRedBgDarkSelected = Color(0xFF3B2025)
val ChipRedBgDarkUnselected = Color(0xFF2C1A1D)
val ChipRedBgLightSelected = Color(0xFFFCE8E8)
val ChipRedBgLightUnselected = Color(0xFFFDF2F2)

val ChipRedBorderDarkSelected = Color(0xFFFF6B6B)
val ChipRedBorderDarkUnselected = Color(0xFF531A21)
val ChipRedBorderLightSelected = Color(0xFFD32F2F)
val ChipRedBorderLightUnselected = Color(0xFFFECDD3)

val ChipRedTextDark = Color(0xFFFF6B6B)
val ChipRedTextLight = Color(0xFFD32F2F)
val ChipRedHeaderDark = Color(0xFFFFA3A3)
val ChipRedHeaderLight = Color(0xFFB71C1C)

val ChipGreenBgDarkSelected = Color(0xFF1D3528)
val ChipGreenBgDarkUnselected = Color(0xFF16281E)
val ChipGreenBgLightSelected = Color(0xFFE6F9ED)
val ChipGreenBgLightUnselected = Color(0xFFF0FDF4)

val ChipGreenBorderDarkSelected = Color(0xFF51CF66)
val ChipGreenBorderDarkUnselected = Color(0xFF1B4D2E)
val ChipGreenBorderLightSelected = Color(0xFF2E7D32)
val ChipGreenBorderLightUnselected = Color(0xFFA7F3D0)

val ChipGreenTextDark = Color(0xFF51CF66)
val ChipGreenTextLight = Color(0xFF2E7D32)
val ChipGreenHeaderDark = Color(0xFF86EFAC)
val ChipGreenHeaderLight = Color(0xFF1B5E20)

// Financial Semantic Color Resolvers
fun financialCreditColor(isDark: Boolean): Color = if (isDark) CreditGreenDark else CreditGreen
fun financialDebtColor(isDark: Boolean): Color = if (isDark) DebtRedDark else DebtRed
fun financialCreditBg(isDark: Boolean): Color = if (isDark) CreditContainerDark else CreditContainerLight
fun financialDebtBg(isDark: Boolean): Color = if (isDark) DebtContainerDark else DebtContainerLight
fun financialCreditBorder(isDark: Boolean): Color = if (isDark) CreditBorderDark else CreditBorderLight
fun financialDebtBorder(isDark: Boolean): Color = if (isDark) DebtBorderDark else DebtBorderLight

// Category Palette Tokens for Domain Utils
object CategoryPalette {
    val AMBER_DARK = Color(0xFF451A03)
    val AMBER_LIGHT = Color(0xFFFEF3C7)
    val PINK_DARK = Color(0xFF4D1222)
    val PINK_LIGHT = Color(0xFFFCE7F3)
    val GRAY_LIGHT_DARK = Color(0xFF262626)
    val GRAY_LIGHT_LIGHT = Color(0xFFEFEFEF)
    val RED_SOFT_DARK = Color(0xFF3E1F1F)
    val RED_SOFT_LIGHT = Color(0xFFFEE2E2)
    val YELLOW_DARK = Color(0xFF3F3701)
    val YELLOW_LIGHT = Color(0xFFFEF9C3)
    val BLUE_SOFT_DARK = Color(0xFF172554)
    val BLUE_SOFT_LIGHT = Color(0xFFDBEAFE)
    val SKY_DARK = Color(0xFF0C4A6E)
    val SKY_LIGHT = Color(0xFFE0F2FE)
    val PURPLE_DARK = Color(0xFF3B0764)
    val PURPLE_LIGHT = Color(0xFFF3E8FF)
    val EMERALD_SOFT_DARK = Color(0xFF064E3B)
    val EMERALD_SOFT_LIGHT = Color(0xFFD1FAE5)
    val GREEN_FIFTY_DARK = Color(0xFF022C22)
    val GREEN_FIFTY_LIGHT = Color(0xFFECFDF5)
    val SLATE_DEFAULT_DARK = Slate900
    val SLATE_DEFAULT_LIGHT = Slate100
}

// Avatar Pastel Palette
val AvatarPastelPalette = listOf(
    Color(0xFFFCA5A5), Color(0xFFFDBA74), Color(0xFFFDE047),
    Color(0xFF86EFAC), Color(0xFF93C5FD), Color(0xFFC4B5FD),
    Color(0xFFF472B6), Color(0xFF2DD4BF)
)



