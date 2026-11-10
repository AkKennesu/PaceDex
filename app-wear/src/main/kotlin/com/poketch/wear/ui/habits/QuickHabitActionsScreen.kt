package com.poketch.wear.ui.habits

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.poketch.core.domain.engine.UnlockEvent
import com.poketch.wear.ui.battle.PokemonDsFontFamily
import com.poketch.wear.ui.layout.rememberResponsiveColumnState
import kotlinx.coroutines.delay
import java.util.Locale

// Option B: Borderless Modern GBA Light Theme Color Palette
private val ScreenBackground = Color(0xFFE2F0D8) // Soft Sage-Mint (pure, no outer outline)
private val ShadowSage = Color(0xFFCCDCC2)       // Subtle sage elevation shadow
private val HeaderNavy = Color(0xFF1E3A5F)       // Borderless Deep Navy
private val HeaderText = Color(0xFFFFFFFF)       // Crisp white
private val CardCream = Color(0xFFFCF8E3)        // Borderless Cream
private val WarmSand = Color(0xFFEAE4CA)         // Borderless Warm Sand
private val TextDark = Color(0xFF1E221E)         // Deep readable dark charcoal
private val WaterBlueHero = Color(0xFF0288D1)    // Vibrant Water Blue
private val TextMuted = Color(0xFF4A504A)        // Muted slate charcoal
private val TrackBg = Color(0xFFDCE0D8)          // Track background #DCE0D8
private val ActionGreen = Color(0xFF248C3E)      // Emerald Green #248C3E
private val ActionGreenShadow = Color(0xFF165C28)

/**
 * Produces crisp, bold pixel typography with simulated bold stroke expansion
 * so the retro GBA font remains thick, punchy, and instantly readable on watch displays.
 */
private fun boldGbaStyle(
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.Bold,
    fontFamily: FontFamily = PokemonDsFontFamily,
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
 * Redesigned Quick Habits screen using Option B: Borderless Modern GBA Light Theme.
 * Features a soft Sage-Mint background, borderless deep navy header pill,
 * a main cream card with subtle sage bottom elevation shadow, hydration hero readout,
 * borderless WTR progress bar, quick log steppers, emerald green run button,
 * and bottom mileage pill.
 */
@Composable
fun QuickHabitActionsScreen(
    onStartRun: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuickHabitActionsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val columnState = rememberResponsiveColumnState(
        initialItemIndex = 1,
        verticalPaddingRatio = 0.08f
    )
    val haptic = LocalHapticFeedback.current

    var celebrationEvent by remember { mutableStateOf<UnlockEvent.NewPokemonUnlocked?>(null) }

    // Listen for new Pokemon unlock celebrations on wrist
    LaunchedEffect(Unit) {
        viewModel.unlockEvents.collect { event ->
            celebrationEvent = event
            viewModel.playUnlockFanfare()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground),
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
            // 1. Top Header Pill: Borderless Deep Navy #1E3A5F with "QUICK HABITS" in crisp white
            item {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(HeaderNavy)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "QUICK HABITS",
                        style = boldGbaStyle(
                            color = HeaderText,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp,
                            shadowOffset = 0.8f
                        )
                    )
                }
            }

            // 2. Main Cream Card: Borderless Cream #FCF8E3 with subtle 2.dp bottom elevation shadow in #CCDCC2
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(0.92f),
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
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Hydration Hero Readout
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = "💧 ${state.todayWaterMl} ML",
                                style = boldGbaStyle(
                                    color = WaterBlueHero,
                                    fontSize = 28.sp,
                                    shadowOffset = 1.0f
                                )
                            )
                            Text(
                                text = "HYDRATION TODAY",
                                style = boldGbaStyle(
                                    color = TextMuted,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.8.sp,
                                    shadowOffset = 0.6f
                                )
                            )
                        }

                        // Quick Log Stepper Buttons: [+250 ML] & [+500 ML]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Warm Sand [+250 ML GLASS] Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(WarmSand)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.logWater(250.0)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "+250 ML",
                                        style = boldGbaStyle(
                                            color = TextDark,
                                            fontSize = 12.sp,
                                            shadowOffset = 0.8f
                                        )
                                    )
                                    Text(
                                        text = "GLASS",
                                        style = boldGbaStyle(
                                            color = TextMuted,
                                            fontSize = 8.sp,
                                            shadowOffset = 0.5f
                                        )
                                    )
                                }
                            }

                            // Deep Navy [+500 ML BOTTLE] Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(HeaderNavy)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.logWater(500.0)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "+500 ML",
                                        style = boldGbaStyle(
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            shadowOffset = 0.8f
                                        )
                                    )
                                    Text(
                                        text = "BOTTLE",
                                        style = boldGbaStyle(
                                            color = Color(0xFFBACADB),
                                            fontSize = 8.sp,
                                            shadowOffset = 0.5f
                                        )
                                    )
                                }
                            }
                        }

                        // Daily Total & Borderless WTR Bar
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            val goalReached = state.todayWaterMl >= state.dailyWaterGoalMl
                            Text(
                                text = if (goalReached) {
                                    "★ GOAL REACHED (${state.todayWaterMl}/${state.dailyWaterGoalMl} ML) ★"
                                } else {
                                    "GOAL: ${state.todayWaterMl} / ${state.dailyWaterGoalMl} ML"
                                },
                                style = boldGbaStyle(
                                    color = if (goalReached) ActionGreen else TextDark,
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
                                    text = "WTR",
                                    style = boldGbaStyle(
                                        color = TextMuted,
                                        fontSize = 9.sp,
                                        shadowOffset = 0.7f
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

                                    val goal = state.dailyWaterGoalMl.coerceAtLeast(1).toFloat()
                                    val progressFraction = (state.todayWaterMl.toFloat() / goal).coerceIn(0f, 1f)
                                    val progressWidth = progressFraction * width

                                    if (progressWidth > 0f) {
                                        drawRect(
                                            color = WaterBlueHero,
                                            topLeft = Offset.Zero,
                                            size = Size(progressWidth, height)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Primary Action Button: Borderless Emerald Green #248C3E button [▶ START 1-MILE RUN]
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
                                onStartRun()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "▶ START 1-MILE RUN",
                            style = boldGbaStyle(
                                color = Color.White,
                                fontSize = 13.5.sp,
                                shadowOffset = 0.9f
                            )
                        )
                    }
                }
            }

            // 4. Bottom Distance Pill: Borderless Cream #FCF8E3 pill
            item {
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardCream)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🏃 RUN TODAY: ${String.format(Locale.US, "%.1f", state.todayRunMiles)} MI",
                        style = boldGbaStyle(
                            color = TextDark,
                            fontSize = 10.5.sp,
                            shadowOffset = 0.8f
                        )
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    // Celebratory wrist unlock / evolution overlay with matching GBA styling
    celebrationEvent?.let { event ->
        if (event.isEvolution && event.preEvolution != null) {
            WearEvolutionOverlay(
                event = event,
                onSetAsActiveBuddy = { buddyId ->
                    viewModel.setActiveBuddy(buddyId)
                    celebrationEvent = null
                },
                onDismiss = { celebrationEvent = null }
            )
        } else {
            WearCelebrationOverlay(
                event = event,
                onDismiss = { celebrationEvent = null }
            )
        }
    }
}

@Composable
private fun WearEvolutionOverlay(
    event: UnlockEvent.NewPokemonUnlocked,
    onSetAsActiveBuddy: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preEvolution = event.preEvolution
    val evolved = event.pokemon
    val context = LocalContext.current

    var stage by remember { mutableIntStateOf(1) }
    LaunchedEffect(Unit) {
        delay(1600)
        stage = 2
    }

    val infiniteTransition = rememberInfiniteTransition(label = "WearEvolutionSparkle")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xF0101828))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (stage == 1) "✨ EVOLVING... ✨" else "🎉 EVOLVED! 🎉",
                style = boldGbaStyle(
                    color = if (stage == 1) Color(0xFFFFB300) else Color(0xFF4CAF50),
                    fontSize = 12.sp,
                    shadowOffset = 0.8f
                ),
                textAlign = TextAlign.Center
            )

            val currentPokemon = if (stage == 1 && preEvolution != null) preEvolution else evolved
            val spriteUri = "file:///android_asset/${currentPokemon.spriteAssetPath}"

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(spriteUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = currentPokemon.name,
                    filterQuality = FilterQuality.None,
                    modifier = Modifier
                        .size(44.dp)
                        .scale(if (stage == 1) pulseScale else 1f)
                )
            }

            Text(
                text = if (stage == 1) {
                    "${preEvolution?.name} is evolving!"
                } else {
                    "${evolved.name} (#%03d)".format(evolved.pokedexNumber)
                },
                style = boldGbaStyle(
                    color = Color.White,
                    fontSize = 11.sp,
                    shadowOffset = 0.7f
                ),
                textAlign = TextAlign.Center
            )

            if (stage == 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF248C3E))
                        .clickable { onSetAsActiveBuddy(evolved.pokedexNumber) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SET AS ACTIVE BUDDY",
                        style = boldGbaStyle(
                            color = Color.White,
                            fontSize = 10.sp,
                            shadowOffset = 0.7f
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFCF8E3))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "KEEP CURRENT",
                        style = boldGbaStyle(
                            color = Color(0xFF1E221E),
                            fontSize = 9.sp,
                            shadowOffset = 0.6f
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun WearCelebrationOverlay(
    event: UnlockEvent.NewPokemonUnlocked,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pokemon = event.pokemon
    val context = LocalContext.current
    val imageUri = "file:///android_asset/${pokemon.spriteAssetPath}"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xF0101828))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = "🎉 POKÉMON UNLOCKED!",
                style = boldGbaStyle(
                    color = Color(0xFFFFD54F),
                    fontSize = 11.sp,
                    shadowOffset = 0.8f
                ),
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = pokemon.name,
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.size(42.dp)
                )
            }

            Text(
                text = "#%03d ${pokemon.name.uppercase()}".format(pokemon.pokedexNumber),
                style = boldGbaStyle(
                    color = Color.White,
                    fontSize = 11.sp,
                    shadowOffset = 0.7f
                )
            )

            Box(
                modifier = Modifier
                    .height(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF248C3E))
                    .padding(horizontal = 16.dp)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AWESOME!",
                    style = boldGbaStyle(
                        color = Color.White,
                        fontSize = 10.sp,
                        shadowOffset = 0.7f
                    )
                )
            }
        }
    }
}
