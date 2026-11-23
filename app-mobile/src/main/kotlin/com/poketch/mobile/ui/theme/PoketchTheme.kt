package com.poketch.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =========================================================================
// Google Stitch Pokétch Design Tokens & Tactical Mint OLED System
// =========================================================================

@Immutable
data class StitchColorPalette(
    val canvasBackground: Color,
    val surfaceCard: Color,
    val surfaceCardElevated: Color,
    val surfaceContainerLow: Color,
    val borderSubtle: Color,
    val borderCard: Color,
    val darkSlate: Color,
    val darkSlateCard: Color,
    val primary: Color,
    val primaryTint: Color,
    val primaryContainer: Color,
    val partnerMint: Color,
    val partnerBorder: Color,
    val hydration: Color,
    val hydrationTint: Color,
    val stepsAmber: Color,
    val stepsTint: Color,
    val alertCoral: Color,
    val alertTint: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val isOledDark: Boolean
)

val StitchLightColors = StitchColorPalette(
    canvasBackground = Color(0xFFF7FAF8),
    surfaceCard = Color(0xFFFFFFFF),
    surfaceCardElevated = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F5F2),
    borderSubtle = Color(0xFFE4EBE7),
    borderCard = Color(0xFFE2E8F0),
    darkSlate = Color(0xFF0F172A),
    darkSlateCard = Color(0xFF1E293B),
    primary = Color(0xFF16A34A),
    primaryTint = Color(0xFFDCFCE7),
    primaryContainer = Color(0xFF00873A),
    partnerMint = Color(0xFFEEF9F1),
    partnerBorder = Color(0xFFC7EBD2),
    hydration = Color(0xFF0EA5E9),
    hydrationTint = Color(0xFFE0F2FE),
    stepsAmber = Color(0xFFF59E0B),
    stepsTint = Color(0xFFFEF3C7),
    alertCoral = Color(0xFFEF4444),
    alertTint = Color(0xFFFEE2E2),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF94A3B8),
    isOledDark = false
)

// Tactical Mint OLED Dark specification per Google Stitch Export Pack
val StitchOledDarkColors = StitchColorPalette(
    canvasBackground = Color(0xFF000000),      // True pitch-black OLED background
    surfaceCard = Color(0xFF0D1310),           // Tactical deep slate / forest mint card
    surfaceCardElevated = Color(0xFF141C18),   // Slightly elevated card
    surfaceContainerLow = Color(0xFF090F0D),   // Recessed containers / inputs
    borderSubtle = Color(0xFF1F2E26),          // Tactical hairline emerald border
    borderCard = Color(0xFF1F2E26),
    darkSlate = Color(0xFFF8FAFC),             // Maps headlines to crisp white in dark mode
    darkSlateCard = Color(0xFF131E19),
    primary = Color(0xFF34D399),               // Tactical Mint green (#34D399)
    primaryTint = Color(0xFF052E16),           // Deep dark emerald container
    primaryContainer = Color(0xFF059669),
    partnerMint = Color(0xFF0D1F17),           // Dark botanical partner surface
    partnerBorder = Color(0xFF1E3A2B),
    hydration = Color(0xFF38BDF8),             // Hydration Cyan
    hydrationTint = Color(0xFF082F49),
    stepsAmber = Color(0xFFFBBF24),            // Telemetry Amber
    stepsTint = Color(0xFF451A03),
    alertCoral = Color(0xFFF87171),            // Tactical Coral
    alertTint = Color(0xFF450A0A),
    textPrimary = Color(0xFFF8FAFC),           // Off-white primary text
    textSecondary = Color(0xFF94A3B8),         // Slate 400
    textMuted = Color(0xFF64748B),             // Slate 500
    isOledDark = true
)

val LocalStitchColors = staticCompositionLocalOf { StitchLightColors }
val LocalThemeIsOledDark = compositionLocalOf { mutableStateOf(false) }

// Dynamic Composable Color Token Accessors
val StitchCanvasBackground: Color @Composable get() = LocalStitchColors.current.canvasBackground
val StitchSurfaceCard: Color @Composable get() = LocalStitchColors.current.surfaceCard
val StitchSurfaceCardElevated: Color @Composable get() = LocalStitchColors.current.surfaceCardElevated
val StitchBorderSubtle: Color @Composable get() = LocalStitchColors.current.borderSubtle
val StitchBorderCard: Color @Composable get() = LocalStitchColors.current.borderCard
val StitchDarkSlate: Color @Composable get() = LocalStitchColors.current.darkSlate
val StitchDarkSlateCard: Color @Composable get() = LocalStitchColors.current.darkSlateCard
val StitchPrimary: Color @Composable get() = LocalStitchColors.current.primary
val StitchPrimaryTint: Color @Composable get() = LocalStitchColors.current.primaryTint
val StitchPrimaryContainer: Color @Composable get() = LocalStitchColors.current.primaryContainer
val StitchPartnerMint: Color @Composable get() = LocalStitchColors.current.partnerMint
val StitchPartnerBorder: Color @Composable get() = LocalStitchColors.current.partnerBorder
val StitchHydration: Color @Composable get() = LocalStitchColors.current.hydration
val StitchHydrationTint: Color @Composable get() = LocalStitchColors.current.hydrationTint
val StitchStepsAmber: Color @Composable get() = LocalStitchColors.current.stepsAmber
val StitchStepsTint: Color @Composable get() = LocalStitchColors.current.stepsTint
val StitchAlertCoral: Color @Composable get() = LocalStitchColors.current.alertCoral
val StitchAlertTint: Color @Composable get() = LocalStitchColors.current.alertTint
val StitchTextPrimary: Color @Composable get() = LocalStitchColors.current.textPrimary
val StitchTextSecondary: Color @Composable get() = LocalStitchColors.current.textSecondary
val StitchTextMuted: Color @Composable get() = LocalStitchColors.current.textMuted

// Backward Compatibility Aliases
val PoketchBackground: Color @Composable get() = StitchCanvasBackground
val PoketchSurface: Color @Composable get() = StitchSurfaceCard
val PoketchSurfaceElevated: Color @Composable get() = StitchSurfaceCardElevated
val PoketchSurfaceVariant: Color @Composable get() = LocalStitchColors.current.surfaceContainerLow
val PoketchBorder: Color @Composable get() = StitchBorderSubtle
val PoketchBorderSubtle: Color @Composable get() = StitchBorderSubtle
val PoketchCrimson: Color @Composable get() = StitchAlertCoral
val PoketchCrimsonDark: Color @Composable get() = LocalStitchColors.current.alertTint
val PoketchCrimsonContainer: Color @Composable get() = StitchAlertTint
val PoketchCyan: Color @Composable get() = StitchHydration
val PoketchCyanContainer: Color @Composable get() = StitchHydrationTint
val PoketchEmerald: Color @Composable get() = StitchPrimary
val PoketchEmeraldContainer: Color @Composable get() = StitchPrimaryTint
val PoketchAmber: Color @Composable get() = StitchStepsAmber
val PoketchAmberContainer: Color @Composable get() = StitchStepsTint
val PoketchIndigo: Color = Color(0xFF6366F1)
val PoketchTextPrimary: Color @Composable get() = StitchTextPrimary
val PoketchTextSecondary: Color @Composable get() = StitchTextSecondary
val PoketchTextMuted: Color @Composable get() = StitchTextMuted

// Stitch Light Color Scheme (Default)
private val StitchLightColorScheme = lightColorScheme(
    primary = Color(0xFF16A34A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = Color(0xFF003816),
    secondary = Color(0xFF0EA5E9),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF00354F),
    tertiary = Color(0xFFF59E0B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF452B00),
    background = Color(0xFFF7FAF8),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F3),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFE4EBE7),
    outlineVariant = Color(0xFFEDF2EF),
    error = Color(0xFFEF4444),
    onError = Color.White
)

// Stitch Tactical Mint OLED Dark Color Scheme
private val StitchOledDarkColorScheme = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF022C22),
    primaryContainer = Color(0xFF059669),
    onPrimaryContainer = Color(0xFFDCFCE7),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF00354A),
    secondaryContainer = Color(0xFF004C69),
    onSecondaryContainer = Color(0xFFC4E7FF),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5C4300),
    onTertiaryContainer = Color(0xFFFFDF9F),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF0D1310),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF131E19),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF1F2E26),
    outlineVariant = Color(0xFF141C18),
    error = Color(0xFFF87171),
    onError = Color(0xFF690005)
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
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.1).sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp
    )
)

@Composable
fun PoketchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val oledDarkState = rememberSaveable { mutableStateOf(darkTheme) }
    androidx.compose.runtime.LaunchedEffect(darkTheme) {
        oledDarkState.value = darkTheme
    }

    val isDark = oledDarkState.value
    val colors = if (isDark) StitchOledDarkColors else StitchLightColors
    val colorScheme = if (isDark) StitchOledDarkColorScheme else StitchLightColorScheme

    CompositionLocalProvider(
        LocalStitchColors provides colors,
        LocalThemeIsOledDark provides oledDarkState
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PoketchTypography,
            shapes = PoketchShapes,
            content = content
        )
    }
}
