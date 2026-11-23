package com.poketch.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =========================================================================
// Google Stitch Pokétch Design Tokens (DESIGN.md)
// =========================================================================

// Botanical Canvas Backgrounds & Card Surfaces
val StitchCanvasBackground = Color(0xFFF7FAF8)
val StitchSurfaceCard = Color(0xFFFFFFFF)
val StitchBorderSubtle = Color(0xFFE4EBE7)
val StitchBorderCard = Color(0xFFE2E8F0)

// Deep Slate-Navy (Hero TDEE Banner, Active Navigation Pill, Dark Buttons)
val StitchDarkSlate = Color(0xFF0F172A)
val StitchDarkSlateCard = Color(0xFF1E293B)

// Primary Accent (Emerald Green & Tints)
val StitchPrimary = Color(0xFF16A34A)
val StitchPrimaryTint = Color(0xFFDCFCE7)
val StitchPrimaryContainer = Color(0xFF00873A)
val StitchPartnerMint = Color(0xFFEEF9F1)
val StitchPartnerBorder = Color(0xFFC7EBD2)

// Hydration Accent (Sky Cyan)
val StitchHydration = Color(0xFF0EA5E9)
val StitchHydrationTint = Color(0xFFE0F2FE)

// Steps & Evolution Gauge Accent (Warm Amber)
val StitchStepsAmber = Color(0xFFF59E0B)
val StitchStepsTint = Color(0xFFFEF3C7)

// Alert & Destructive
val StitchAlertCoral = Color(0xFFEF4444)
val StitchAlertTint = Color(0xFFFEE2E2)

// Typography Text Hierarchy
val StitchTextPrimary = Color(0xFF0F172A)
val StitchTextSecondary = Color(0xFF475569)
val StitchTextMuted = Color(0xFF94A3B8)

// Backward Compatibility Aliases for existing screen imports
val PoketchBackground = StitchCanvasBackground
val PoketchSurface = StitchSurfaceCard
val PoketchSurfaceElevated = Color(0xFFFFFFFF)
val PoketchSurfaceVariant = Color(0xFFF1F5F3)
val PoketchBorder = StitchBorderSubtle
val PoketchBorderSubtle = Color(0xFFEDF2EF)

val PoketchCrimson = StitchAlertCoral
val PoketchCrimsonDark = Color(0xFFB91C1C)
val PoketchCrimsonContainer = StitchAlertTint

val PoketchCyan = StitchHydration
val PoketchCyanContainer = StitchHydrationTint

val PoketchEmerald = StitchPrimary
val PoketchEmeraldContainer = StitchPrimaryTint

val PoketchAmber = StitchStepsAmber
val PoketchAmberContainer = StitchStepsTint

val PoketchIndigo = Color(0xFF6366F1)
val PoketchTextPrimary = StitchTextPrimary
val PoketchTextSecondary = StitchTextSecondary
val PoketchTextMuted = StitchTextMuted

// Stitch Light Color Scheme (Default per Google Stitch DESIGN.md)
private val StitchLightColorScheme = lightColorScheme(
    primary = StitchPrimary,
    onPrimary = Color.White,
    primaryContainer = StitchPrimaryTint,
    onPrimaryContainer = Color(0xFF003816),

    secondary = StitchHydration,
    onSecondary = Color.White,
    secondaryContainer = StitchHydrationTint,
    onSecondaryContainer = Color(0xFF00354F),

    tertiary = StitchStepsAmber,
    onTertiary = Color.White,
    tertiaryContainer = StitchStepsTint,
    onTertiaryContainer = Color(0xFF452B00),

    background = StitchCanvasBackground,
    onBackground = StitchTextPrimary,

    surface = StitchSurfaceCard,
    onSurface = StitchTextPrimary,
    surfaceVariant = Color(0xFFF1F5F3),
    onSurfaceVariant = StitchTextSecondary,

    outline = StitchBorderSubtle,
    outlineVariant = Color(0xFFEDF2EF),

    error = StitchAlertCoral,
    onError = Color.White
)

val PoketchShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

val PoketchTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
        color = StitchTextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
        color = StitchTextPrimary
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
        color = StitchTextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = StitchTextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.1).sp,
        color = StitchTextPrimary
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = StitchTextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = StitchTextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = StitchTextSecondary
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = StitchTextMuted
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
        color = StitchTextPrimary
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        color = StitchTextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp,
        color = StitchTextMuted
    )
)

@Composable
fun PoketchTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StitchLightColorScheme,
        typography = PoketchTypography,
        shapes = PoketchShapes,
        content = content
    )
}
