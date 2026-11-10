package com.poketch.wear.ui.calorie

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.TimeTextDefaults
import com.poketch.wear.ui.battle.PokemonDsFontFamily
import com.poketch.wear.ui.layout.rememberResponsiveColumnState

// Option B: Borderless Modern GBA Light Theme Color Palette
private val ScreenBackground = Color(0xFFE2F0D8) // Soft Sage-Mint (pure, no outer outline)
private val ShadowSage = Color(0xFFCCDCC2)       // Subtle sage elevation shadow
private val HeaderNavy = Color(0xFF1E3A5F)       // Borderless Deep Navy
private val HeaderText = Color(0xFFFFFFFF)       // Crisp white
private val CardCream = Color(0xFFFCF8E3)        // Borderless Cream
private val WarmSand = Color(0xFFEAE4CA)         // Borderless Warm Sand
private val TextDark = Color(0xFF1E221E)         // Deep readable dark charcoal
private val GoldHero = Color(0xFFD47C00)         // Rich, high-contrast readable Amber Gold
private val TextMuted = Color(0xFF4A504A)        // Punchy, readable slate charcoal
private val TrackBg = Color(0xFFDCE0D8)          // Track background #DCE0D8
private val GreenLogged = Color(0xFF38A04C)      // Green #38A04C
private val ActionGreen = Color(0xFF248C3E)      // Emerald Green #248C3E
private val ActionGreenShadow = Color(0xFF165C28)

val pokemonDsFontFamily = PokemonDsFontFamily

/**
 * Produces crisp, bold pixel typography with simulated bold stroke expansion
 * so the retro GBA font remains thick, punchy, and instantly readable on watch displays.
 */
private fun boldGbaStyle(
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.Bold,
    fontFamily: FontFamily = pokemonDsFontFamily,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    shadowOffset: Float = 0.8f,
): TextStyle {
    return TextStyle(
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        shadow = Shadow(
            color = color,
            offset = Offset(shadowOffset, 0.2f),
            blurRadius = 0f
        )
    )
}

/**
 * Option B: Borderless Modern GBA Light Theme Quick Calorie Dial.
 * Features a pure Sage-Mint background, borderless deep navy header pill,
 * a main cream card with subtle sage bottom elevation shadow, fine/coarse steppers,
 * borderless CAL preview bar, emerald green action button, and preset selector pill.
 */
@Composable
fun QuickCalorieDialScreen(
    onNavigateToAlert: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RotaryCalorieLoggerViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val columnState = rememberResponsiveColumnState(
        initialItemIndex = 1,
        verticalPaddingRatio = 0.08f
    )
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    var accumulatedDelta by remember { mutableFloatStateOf(0f) }
    val deltaThreshold = 35f

    val presets = remember {
        listOf(
            150 to "+150 KCAL SNACK",
            500 to "+500 KCAL MEAL",
            120 to "+120 KCAL DRINK",
            250 to "+250 KCAL QUICK",
            350 to "+350 KCAL LUNCH",
            80 to "+80 KCAL FRUIT"
        )
    }
    var currentPresetIdx by remember { mutableIntStateOf(0) }
    val currentPreset = presets[currentPresetIdx]

    val pendingKcal = state.selectedCalories
    val loggedKcal = state.currentConsumedKcal
    val dailyCeilingKcal = state.targetCalorieCeiling
    val totalKcal = loggedKcal + pendingKcal

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .focusRequester(focusRequester)
            .focusable()
            .onRotaryScrollEvent { event ->
                val isHighVelocity = kotlin.math.abs(event.verticalScrollPixels) > 80f
                val stepKcal = if (isHighVelocity) 50 else 1
                accumulatedDelta += event.verticalScrollPixels
                if (accumulatedDelta >= deltaThreshold) {
                    val steps = (accumulatedDelta / deltaThreshold).toInt()
                    accumulatedDelta %= deltaThreshold
                    val changed = viewModel.adjustCalories(steps * stepKcal)
                    if (changed) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    true
                } else if (accumulatedDelta <= -deltaThreshold) {
                    val steps = (accumulatedDelta / deltaThreshold).toInt()
                    accumulatedDelta %= deltaThreshold
                    val changed = viewModel.adjustCalories(steps * stepKcal)
                    if (changed) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    true
                } else {
                    true
                }
            },
        timeText = {
            TimeText(
                timeTextStyle = TimeTextDefaults.timeTextStyle(color = TextDark)
            )
        },
        positionIndicator = { PositionIndicator(scalingLazyListState = columnState.listState) }
    ) {
        ScalingLazyColumn(
            state = columnState.listState,
            contentPadding = columnState.contentPadding,
            autoCentering = columnState.autoCentering,
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBackground),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // 1. Top Header Pill: Borderless Deep Navy #1E3A5F with "QUICK CALORIE DIAL" in crisp #F8F8F8
            item {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(HeaderNavy)
                        .padding(horizontal = 11.dp, vertical = 3.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "QUICK CALORIE DIAL",
                        style = boldGbaStyle(
                            color = HeaderText,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp,
                            shadowOffset = 0.7f
                        )
                    )
                }
            }

            // 2. Main Cream Card: Borderless Cream #FCF8E3 with subtle 2.dp bottom elevation shadow in #CCDCC2
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Subtle 2.dp bottom elevation shadow
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(y = 2.dp)
                            .background(ShadowSage, RoundedCornerShape(12.dp))
                    )

                    // Card Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardCream, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // Left [-1] & Right [+1] Fine Steppers flanking Center Hero Calorie Readout
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left [-1] Fine Stepper: Borderless Warm Sand #EAE4CA squircle (38.dp × 34.dp, RoundedCornerShape(10.dp))
                            Box(
                                modifier = Modifier
                                    .size(width = 38.dp, height = 34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(WarmSand)
                                    .clickable {
                                        val changed = viewModel.adjustCalories(-1)
                                        if (changed) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "-1",
                                    style = boldGbaStyle(
                                        color = TextDark,
                                        fontSize = 16.sp,
                                        shadowOffset = 0.9f
                                    )
                                )
                            }

                            // Center Hero Calorie Readout: Bold "+$pendingKcal" in gold (28.sp) with "KCAL" (9.sp) below it
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "+$pendingKcal",
                                    style = boldGbaStyle(
                                        color = GoldHero,
                                        fontSize = 28.sp,
                                        shadowOffset = 1.0f
                                    )
                                )
                                Text(
                                    text = "KCAL",
                                    style = boldGbaStyle(
                                        color = TextMuted,
                                        fontSize = 9.sp,
                                        letterSpacing = 1.sp,
                                        shadowOffset = 0.6f
                                    )
                                )
                            }

                            // Right [+1] Fine Stepper: Borderless Warm Sand #EAE4CA squircle (38.dp × 34.dp, RoundedCornerShape(10.dp))
                            Box(
                                modifier = Modifier
                                    .size(width = 38.dp, height = 34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(WarmSand)
                                    .clickable {
                                        val changed = viewModel.adjustCalories(1)
                                        if (changed) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+1",
                                    style = boldGbaStyle(
                                        color = TextDark,
                                        fontSize = 16.sp,
                                        shadowOffset = 0.9f
                                    )
                                )
                            }
                        }

                        // Middle [-50] & [+50] Coarse Steppers: Borderless Deep Navy #1E3A5F pills (height = 22.dp, RoundedCornerShape(7.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(0.78f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(HeaderNavy)
                                    .clickable {
                                        val changed = viewModel.adjustCalories(-50)
                                        if (changed) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "-50",
                                    style = boldGbaStyle(
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        shadowOffset = 0.8f
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(HeaderNavy)
                                    .clickable {
                                        val changed = viewModel.adjustCalories(50)
                                        if (changed) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+50",
                                    style = boldGbaStyle(
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        shadowOffset = 0.8f
                                    )
                                )
                            }
                        }

                        // Daily Total & Borderless CAL Bar:
                        // "TOTAL: ${loggedKcal + pendingKcal} / $dailyCeilingKcal KCAL" in #1E221E
                        // above a borderless #DCE0D8 track (height = 10.dp, RoundedCornerShape(6.dp))
                        // filled with #38A04C (logged) and #D47C00 (pending preview)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "TOTAL: $totalKcal / $dailyCeilingKcal KCAL",
                                style = boldGbaStyle(
                                    color = TextDark,
                                    fontSize = 10.5.sp,
                                    shadowOffset = 0.8f
                                ),
                                textAlign = TextAlign.Center
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "CAL",
                                    style = boldGbaStyle(
                                        color = TextMuted,
                                        fontSize = 8.5.sp,
                                        shadowOffset = 0.6f
                                    )
                                )

                                Canvas(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                ) {
                                    val width = size.width
                                    val height = size.height
                                    val cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())

                                    // Background track #DCE0D8
                                    drawRoundRect(
                                        color = TrackBg,
                                        size = size,
                                        cornerRadius = cornerRadius
                                    )

                                    val totalCeiling = maxOf(dailyCeilingKcal, totalKcal).coerceAtLeast(1).toFloat()
                                    val loggedWidth = ((loggedKcal.toFloat() / totalCeiling) * width).coerceIn(0f, width)
                                    val pendingWidth = ((pendingKcal.toFloat() / totalCeiling) * width).coerceIn(0f, width - loggedWidth)

                                    // Logged progress
                                    if (loggedWidth > 0f) {
                                        drawRect(
                                            color = GreenLogged,
                                            topLeft = Offset.Zero,
                                            size = Size(loggedWidth, height)
                                        )
                                    }

                                    // Pending preview progress
                                    if (pendingWidth > 0f) {
                                        drawRect(
                                            color = GoldHero,
                                            topLeft = Offset(loggedWidth, 0f),
                                            size = Size(pendingWidth, height)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Primary Action Button: Borderless Emerald Green #248C3E button (height = 36.dp, RoundedCornerShape(12.dp)) with white text
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(0.88f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Subtle bottom elevation shadow
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(y = 2.dp)
                            .background(ActionGreenShadow, RoundedCornerShape(12.dp))
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ActionGreen)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.confirmCalorieLog(
                                    onOverTarget = onNavigateToAlert,
                                    onSuccess = onNavigateBack
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "▶ LOG +$pendingKcal KCAL",
                            style = boldGbaStyle(
                                color = Color.White,
                                fontSize = 13.sp,
                                shadowOffset = 0.8f
                            )
                        )
                    }
                }
            }

            // 4. Bottom Preset Selector Pill: Borderless Cream #FCF8E3 pill (height = 24.dp, RoundedCornerShape(8.dp)) with dark text
            item {
                Row(
                    modifier = Modifier
                        .height(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardCream)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "◀ ",
                        style = boldGbaStyle(
                            color = TextDark,
                            fontSize = 10.sp,
                            shadowOffset = 0.7f
                        ),
                        modifier = Modifier.clickable {
                            currentPresetIdx = (currentPresetIdx - 1 + presets.size) % presets.size
                            viewModel.setCalories(presets[currentPresetIdx].first)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )
                    Text(
                        text = currentPreset.second,
                        style = boldGbaStyle(
                            color = TextDark,
                            fontSize = 10.sp,
                            shadowOffset = 0.7f
                        ),
                        modifier = Modifier.clickable {
                            currentPresetIdx = (currentPresetIdx + 1) % presets.size
                            viewModel.setCalories(presets[currentPresetIdx].first)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )
                    Text(
                        text = " ▶",
                        style = boldGbaStyle(
                            color = TextDark,
                            fontSize = 10.sp,
                            shadowOffset = 0.7f
                        ),
                        modifier = Modifier.clickable {
                            currentPresetIdx = (currentPresetIdx + 1) % presets.size
                            viewModel.setCalories(presets[currentPresetIdx].first)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}
