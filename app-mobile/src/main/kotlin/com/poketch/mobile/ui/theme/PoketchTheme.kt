package com.poketch.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Pokétch Cyber-Fitness Palette
val PoketchBackground = Color(0xFF0B0F19)
val PoketchSurface = Color(0xFF131B2A)
val PoketchSurfaceElevated = Color(0xFF1A233A)
val PoketchSurfaceVariant = Color(0xFF172033)
val PoketchBorder = Color(0xFF24324D)
val PoketchBorderSubtle = Color(0xFF1D283E)

val PoketchCrimson = Color(0xFFEF4444)
val PoketchCrimsonDark = Color(0xFFB91C1C)
val PoketchCrimsonContainer = Color(0xFF3F1318)

val PoketchCyan = Color(0xFF06B6D4)
val PoketchCyanContainer = Color(0xFF0E3240)

val PoketchEmerald = Color(0xFF10B981)
val PoketchEmeraldContainer = Color(0xFF0E382A)

val PoketchAmber = Color(0xFFF59E0B)
val PoketchAmberContainer = Color(0xFF3D2908)

val PoketchIndigo = Color(0xFF6366F1)
val PoketchTextPrimary = Color(0xFFF8FAFC)
val PoketchTextSecondary = Color(0xFF94A3B8)
val PoketchTextMuted = Color(0xFF64748B)

private val PoketchDarkColorScheme = darkColorScheme(
    primary = PoketchCrimson,
    onPrimary = Color.White,
    primaryContainer = PoketchCrimsonContainer,
    onPrimaryContainer = Color(0xFFFFD9DD),

    secondary = PoketchCyan,
    onSecondary = Color(0xFF00363F),
    secondaryContainer = PoketchCyanContainer,
    onSecondaryContainer = Color(0xFFA5F3FC),

    tertiary = PoketchEmerald,
    onTertiary = Color(0xFF003822),
    tertiaryContainer = PoketchEmeraldContainer,
    onTertiaryContainer = Color(0xFF6EE7B7),

    background = PoketchBackground,
    onBackground = PoketchTextPrimary,

    surface = PoketchSurface,
    onSurface = PoketchTextPrimary,
    surfaceVariant = PoketchSurfaceVariant,
    onSurfaceVariant = PoketchTextSecondary,

    outline = PoketchBorder,
    outlineVariant = PoketchBorderSubtle,

    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

val PoketchShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val PoketchTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp,
        color = PoketchTextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp,
        color = PoketchTextPrimary
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = PoketchTextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = PoketchTextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp,
        color = PoketchTextPrimary
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        color = PoketchTextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp,
        color = PoketchTextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
        color = PoketchTextSecondary
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        color = PoketchTextMuted
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp,
        color = PoketchTextPrimary
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.8.sp,
        color = PoketchTextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.6.sp,
        color = PoketchTextMuted
    )
)

@Composable
fun PoketchTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PoketchDarkColorScheme,
        typography = PoketchTypography,
        shapes = PoketchShapes,
        content = content
    )
}
