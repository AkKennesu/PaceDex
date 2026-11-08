package com.poketch.wear.ui.battle

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.poketch.wear.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/**
 * Authentic GBA FireRed/LeafGreen Battle Home Screen on a 192x192 Virtual Pixel Canvas.
 * Supports direct touch quadrant taps, Wear OS Rotary Crown cursor navigation,
 * real-time Calorie Deficit HP tracks, Hydration EXP tracks, and all 4 GBA branches.
 */
@Composable
fun GbaWatchBattleHomeScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GbaWatchBattleViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRound = LocalConfiguration.current.isScreenRound
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val menuState by viewModel.commandMenuState.collectAsStateWithLifecycle()
    val selectedRootOption by viewModel.selectedRootOption.collectAsStateWithLifecycle()
    val selectedFightOption by viewModel.selectedFightOption.collectAsStateWithLifecycle()
    var isBuddyBouncing by remember { mutableStateOf(false) }

    // Alert haptic trigger when entering OverTargetWarning
    val isOverCalorie = state.consumedKcal > state.targetCalorieCeiling
    LaunchedEffect(isOverCalorie) {
        if (isOverCalorie) {
            viewModel.playOverTargetHaptic()
        }
    }

    // Intercept Back button when in Fight menu to return to Root command menu (GBA B button behavior)
    androidx.activity.compose.BackHandler(enabled = menuState == GbaCommandMenuState.FIGHT_MENU) {
        viewModel.setCommandMenuState(GbaCommandMenuState.ROOT_MENU)
    }

    // Buddy bounce animation on tap
    val buddyScale by animateFloatAsState(
        targetValue = if (isBuddyBouncing) 1.18f else 1.0f,
        animationSpec = tween(180),
        finishedListener = { isBuddyBouncing = false },
        label = "buddy_bounce"
    )

    // Target virtual coordinates for arrow cursor based on active menu and option
    val targetCursorX = when (menuState) {
        GbaCommandMenuState.ROOT_MENU -> when (selectedRootOption) {
            GbaRootOption.FIGHT, GbaRootOption.POKEMON -> 44f
            GbaRootOption.BAG, GbaRootOption.RUN -> 104f
        }
        GbaCommandMenuState.FIGHT_MENU -> when (selectedFightOption) {
            GbaFightOption.LOG_CAL, GbaFightOption.HABITS -> 44f
            GbaFightOption.WATER, GbaFightOption.SYNC -> 104f
        }
    }

    val targetCursorY = when (menuState) {
        GbaCommandMenuState.ROOT_MENU -> when (selectedRootOption) {
            GbaRootOption.FIGHT, GbaRootOption.BAG -> 133f
            GbaRootOption.POKEMON, GbaRootOption.RUN -> 149f
        }
        GbaCommandMenuState.FIGHT_MENU -> when (selectedFightOption) {
            GbaFightOption.LOG_CAL, GbaFightOption.WATER -> 133f
            GbaFightOption.HABITS, GbaFightOption.SYNC -> 149f
        }
    }

    // Smooth cursor glide animation to destination
    val animatedCursorX by animateFloatAsState(
        targetValue = targetCursorX,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cursor_x"
    )

    val animatedCursorY by animateFloatAsState(
        targetValue = targetCursorY,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cursor_y"
    )

    // Subtle horizontal idle pulsation of the GBA cursor arrow
    val cursorIdleTransition = rememberInfiniteTransition(label = "cursor_idle")
    val cursorIdleOffset by cursorIdleTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_idle_offset"
    )

    GbaWatchCanvas(
        modifier = modifier
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onRotaryScrollEvent { event ->
                val delta = event.verticalScrollPixels
                if (delta > 0) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (menuState == GbaCommandMenuState.ROOT_MENU) {
                        viewModel.setSelectedRootOption(when (selectedRootOption) {
                            GbaRootOption.FIGHT -> GbaRootOption.BAG
                            GbaRootOption.BAG -> GbaRootOption.RUN
                            GbaRootOption.RUN -> GbaRootOption.POKEMON
                            GbaRootOption.POKEMON -> GbaRootOption.FIGHT
                        })
                    } else {
                        viewModel.setSelectedFightOption(when (selectedFightOption) {
                            GbaFightOption.LOG_CAL -> GbaFightOption.WATER
                            GbaFightOption.WATER -> GbaFightOption.SYNC
                            GbaFightOption.SYNC -> GbaFightOption.HABITS
                            GbaFightOption.HABITS -> GbaFightOption.LOG_CAL
                        })
                    }
                    true
                } else if (delta < 0) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (menuState == GbaCommandMenuState.ROOT_MENU) {
                        viewModel.setSelectedRootOption(when (selectedRootOption) {
                            GbaRootOption.FIGHT -> GbaRootOption.POKEMON
                            GbaRootOption.POKEMON -> GbaRootOption.RUN
                            GbaRootOption.RUN -> GbaRootOption.BAG
                            GbaRootOption.BAG -> GbaRootOption.FIGHT
                        })
                    } else {
                        viewModel.setSelectedFightOption(when (selectedFightOption) {
                            GbaFightOption.LOG_CAL -> GbaFightOption.HABITS
                            GbaFightOption.HABITS -> GbaFightOption.SYNC
                            GbaFightOption.SYNC -> GbaFightOption.WATER
                            GbaFightOption.WATER -> GbaFightOption.LOG_CAL
                        })
                    }
                    true
                } else false
            }
    ) { scale ->
        val ceiling = state.targetCalorieCeiling.coerceAtLeast(1)
        val consumed = state.consumedKcal

        // 1. ARENA BACKGROUND LAYER (192x192 at (0, 0))
        val arenaRes = if (isRound) {
            R.drawable.gba_battle_arena_watch_round
        } else {
            R.drawable.gba_battle_arena_watch_square
        }
        GbaPixelImage(
            resId = arenaRes,
            contentDescription = "GBA Battle Arena",
            modifier = Modifier.gbaVirtualBounds(0, 0, 192, 192, scale)
        )

        // 2. WILD TARGET POKÉMON SPRITE
        // Uses PokemonSpriteMetricsTable for pixel-perfect platform anchoring and sizing
        val target = state.wildTarget
        val targetPokedexNum = target?.pokedexNumber ?: 77
        val targetUri = com.poketch.core.domain.model.Gen3PokemonSpriteResolver.getFrontSpriteUri(targetPokedexNum)
        val wildMetrics = com.poketch.core.domain.model.PokemonSpriteMetricsTable.get(targetPokedexNum)
        val wildScale = wildMetrics.wildScaleFactor
        val wildFrameSize = (64f * wildScale).roundToInt()
        val wildVisibleBottomOffset = (64f - wildMetrics.frontBottomPad.toFloat()) * wildScale
        val wildBaselineY = if (wildMetrics.isFlying) 65f else 70f
        val wildTopY = (wildBaselineY - wildVisibleBottomOffset).roundToInt()
        val wildLeftX = (146f - wildFrameSize / 2f).roundToInt()

        Box(
            modifier = Modifier
                .gbaVirtualBounds(wildLeftX, wildTopY, wildFrameSize, wildFrameSize, scale)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onNavigate("pokedex_missions")
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(targetUri)
                    .crossfade(false)
                    .error(R.drawable.gba_ball_empty)
                    .fallback(R.drawable.gba_ball_empty)
                    .build(),
                contentDescription = target?.name ?: "Wild Target",
                filterQuality = FilterQuality.None,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3. ACTIVE BUDDY POKÉMON SPRITE
        // Uses PokemonSpriteMetricsTable for pixel-perfect baseline anchoring on top of Box 3 (menu)
        val buddy = state.activeBuddy
        val buddyPokedexNum = buddy?.pokedexNumber ?: 1
        val buddyUri = com.poketch.core.domain.model.Gen3PokemonSpriteResolver.getBackSpriteUri(buddyPokedexNum)
        val buddyMetrics = com.poketch.core.domain.model.PokemonSpriteMetricsTable.get(buddyPokedexNum)
        val buddySpriteScale = buddyMetrics.buddyScaleFactor
        val buddyFrameSize = (64f * buddySpriteScale).roundToInt()
        val buddyVisibleBottomOffset = (64f - buddyMetrics.backBottomPad.toFloat()) * buddySpriteScale
        val buddyBaselineY = if (buddyMetrics.isFlying) 122f else 125f
        val buddyTopY = (buddyBaselineY - buddyVisibleBottomOffset).roundToInt()
        val buddyLeftX = 18

        Box(
            modifier = Modifier
                .gbaVirtualBounds(buddyLeftX, buddyTopY, buddyFrameSize, buddyFrameSize, scale)
                .graphicsLayer {
                    scaleX = buddyScale
                    scaleY = buddyScale
                    transformOrigin = TransformOrigin(0.5f, 1.0f)
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isBuddyBouncing = true
                    if (isOverCalorie) {
                        onNavigate("over_calorie_red_alert")
                    } else {
                        val buddyName = (buddy?.name ?: "BUDDY").uppercase()
                        val remaining = (ceiling - consumed).coerceAtLeast(0)
                        viewModel.showTemporaryDialog("$buddyName is ready! $remaining kcal to goal!", durationMillis = 3000L)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(buddyUri)
                    .crossfade(false)
                    .error(R.drawable.gba_ball_healthy)
                    .fallback(R.drawable.gba_ball_healthy)
                    .build(),
                contentDescription = buddy?.name ?: "Active Buddy",
                filterQuality = FilterQuality.None,
                contentScale = ContentScale.Fit,
                colorFilter = if (isOverCalorie) {
                    ColorFilter.tint(Color(0xFFFF8A80), BlendMode.Modulate)
                } else null,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 4. BOX 1 — TOP-LEFT TIME & SLIM HP UNLOCK PROGRESS BOX (86x34 at (x = 29, y = 34))
        GbaPixelImage(
            resId = R.drawable.gba_box1_time_hp,
            contentDescription = "Box 1 Time HP",
            modifier = Modifier
                .gbaVirtualBounds(29, 34, 86, 34, scale)
                .clickable {
                    viewModel.triggerDemoUnlock(targetPokedexNum)
                }
        )

        // Row 1 — Live Time (Left) & Offline Weather (Right) aligned with HP bar unit: (x = 37, y = 36, w = 74, h = 14)
        val liveTime by produceState(
            initialValue = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)).uppercase()
        ) {
            while (true) {
                value = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)).uppercase()
                delay(1000.milliseconds)
            }
        }
        val weatherTemp = "${state.currentWeather.tempCelsius}°C"
        Row(
            modifier = Modifier.gbaVirtualBounds(37, 36, 74, 14, scale),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GbaText(
                text = liveTime,
                scale = scale,
                color = Color(0xFF202820),
                fontSizeVirtualPx = 12.8f,
                lineHeightVirtualPx = 13.0f,
                letterSpacingVirtualPx = 0.0f,
                scaleX = 0.96f,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                includeFontPadding = false,
            )
            GbaText(
                text = weatherTemp,
                scale = scale,
                color = Color(0xFF202820),
                fontSizeVirtualPx = 12.8f,
                lineHeightVirtualPx = 13.0f,
                letterSpacingVirtualPx = 0.0f,
                scaleX = 0.96f,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                includeFontPadding = false,
            )
        }

        // Row 2 — Slim HP Progress Bar Fill: (x = 53, y = 57, maxWidth = 58, height = 3)
        // Starts FULL (100% HP) and slowly DECREASES towards 0 as mission progress increases towards unlock
        val missionProgressRatio = when {
            target == null -> 0f
            target.isUnlocked -> 1f
            else -> target.progressRatio.toFloat()
        }
        val enemyHpRemainingRatio = (1.0f - missionProgressRatio).coerceIn(0f, 1f)
        val animatedHpRatio by animateFloatAsState(
            targetValue = enemyHpRemainingRatio,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            label = "EnemyHpRemainingRatio"
        )
        val enemyHpBarWidth = (58f * animatedHpRatio).roundToInt().coerceIn(0, 58)
        if (enemyHpBarWidth > 0) {
            val enemyHpBarRes = when {
                animatedHpRatio > 0.50f -> R.drawable.gba_hp_green
                animatedHpRatio > 0.20f -> R.drawable.gba_hp_yellow
                else -> R.drawable.gba_hp_red
            }
            GbaPixelImage(
                resId = enemyHpBarRes,
                contentDescription = "Mission Progress Bar",
                modifier = Modifier.gbaVirtualBounds(53, 57, enemyHpBarWidth, 3, scale)
            )
        }

        // 5. BOX 2 — MIDDLE-RIGHT DATE/WEATHER + STP BAR + STEP COUNT + WTR BAR (86x36 at (x = 84, y = 86))
        GbaPixelImage(
            resId = R.drawable.gba_box2_date_stp_wtr,
            contentDescription = "Box 2 Date STP WTR",
            modifier = Modifier.gbaVirtualBounds(84, 86, 86, 36, scale)
        )

        // Row 1 — Date with Year: (x = 90, y = 88, w = 74, h = 11)
        val currentDateWithYear = remember {
            LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)).uppercase()
        }
        GbaText(
            text = currentDateWithYear,
            scale = scale,
            color = Color(0xFF202820),
            fontSizeVirtualPx = 9.5f,
            lineHeightVirtualPx = 9.5f,
            letterSpacingVirtualPx = 0.2f,
            scaleX = 1.05f,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            includeFontPadding = false,
            modifier = Modifier.gbaVirtualBounds(90, 88, 74, 11, scale)
        )

        // Row 2 — Full-Length Slim STP (Step) Bar Fill: (x = 108, y = 100, maxWidth = 54, height = 3)
        // Starts FULL (100%) and goes LOWER as steps are taken towards daily step goal
        val stepGoal = state.dailyStepGoal.coerceAtLeast(1)
        val stepRemainingRatio = ((stepGoal - state.todaySteps).toFloat() / stepGoal.toFloat()).coerceIn(0f, 1f)
        val stepBarWidth = (54f * stepRemainingRatio).roundToInt().coerceIn(0, 54)
        if (stepBarWidth > 0) {
            val stepBarRes = when {
                stepRemainingRatio > 0.50f -> R.drawable.gba_hp_green
                stepRemainingRatio > 0.20f -> R.drawable.gba_hp_yellow
                else -> R.drawable.gba_hp_red
            }
            GbaPixelImage(
                resId = stepBarRes,
                contentDescription = "Step Bar",
                modifier = Modifier.gbaVirtualBounds(108, 100, stepBarWidth, 3, scale)
            )
        }

        // Row 3 — Bottom-Right Step Count Readout: (x = 104, y = 106, w = 60, h = 9), right-aligned
        GbaText(
            text = "${state.todaySteps} / 10k",
            scale = scale,
            color = Color(0xFF283028),
            fontSizeVirtualPx = 6.3f,
            lineHeightVirtualPx = 6.3f,
            letterSpacingVirtualPx = 0.2f,
            scaleX = 1.05f,
            textAlign = TextAlign.End,
            maxLines = 1,
            includeFontPadding = false,
            modifier = Modifier.gbaVirtualBounds(104, 106, 60, 9, scale)
        )

        // Bottom Slate Lip — Slim WTR (Water) Bar Fill: (x = 110, y = 117, maxWidth = 54, height = 2)
        val waterGoal = state.dailyWaterGoalMl.coerceAtLeast(1)
        val waterRatio = (state.todayWaterMl.toFloat() / waterGoal.toFloat()).coerceIn(0f, 1f)
        val wtrWidth = (54f * waterRatio).roundToInt().coerceIn(0, 54)
        if (wtrWidth > 0) {
            GbaPixelImage(
                resId = R.drawable.gba_exp_cyan,
                contentDescription = "Water Bar",
                modifier = Modifier.gbaVirtualBounds(110, 117, wtrWidth, 2, scale)
            )
        }

        // 6. BOX 3 — BOTTOM BLANK MENU / CHOICES BOX (122x43 at (x = 35, y = 124))
        val dialogMsg = state.dialogMessage
        if (dialogMsg != null) {
            // Teal Message Dialog Overlay
            GbaPixelImage(
                resId = R.drawable.gba_box3_dialog_teal_blank,
                contentDescription = "GBA Dialog Box",
                modifier = Modifier
                    .gbaVirtualBounds(35, 124, 122, 43, scale)
                    .clickable { viewModel.clearDialog() }
            )
            GbaText(
                text = dialogMsg,
                scale = scale,
                color = Color(0xFFF8F8F8),
                fontSizeVirtualPx = 7.5f,
                lineHeightVirtualPx = 8.5f,
                letterSpacingVirtualPx = 0.3f,
                scaleX = 1.05f,
                maxLines = 2,
                softWrap = true,
                includeFontPadding = false,
                modifier = Modifier.gbaVirtualBounds(42, 129, 108, 33, scale)
            )
        } else if (menuState == GbaCommandMenuState.ROOT_MENU) {
            // ROOT COMMAND MENU (FIGHT | BAG / POKéMON | RUN)
            GbaPixelImage(
                resId = R.drawable.gba_box3_menu_compact_blank,
                contentDescription = "GBA Command Menu",
                modifier = Modifier.gbaVirtualBounds(35, 124, 122, 43, scale)
            )

            // 4 Action Labels for Root Menu
            // Top-Left: (x = 53, y = 131) -> FIGHT
            GbaText(
                text = "FIGHT",
                scale = scale,
                color = Color(0xFF283038),
                fontSizeVirtualPx = 13.5f,
                lineHeightVirtualPx = 13.5f,
                letterSpacingVirtualPx = 0.2f,
                scaleX = 1.12f,
                includeFontPadding = false,
                modifier = Modifier.gbaVirtualBounds(53, 131, 46, 15, scale)
            )
            // Top-Right: (x = 113, y = 131) -> BAG
            GbaText(
                text = "BAG",
                scale = scale,
                color = Color(0xFF283038),
                fontSizeVirtualPx = 13.5f,
                lineHeightVirtualPx = 13.5f,
                letterSpacingVirtualPx = 0.2f,
                scaleX = 1.12f,
                includeFontPadding = false,
                modifier = Modifier.gbaVirtualBounds(113, 131, 44, 15, scale)
            )
            // Bottom-Left: (x = 53, y = 147) -> POKéMON
            GbaText(
                text = "POKéMON",
                scale = scale,
                color = Color(0xFF283038),
                fontSizeVirtualPx = 13.5f,
                lineHeightVirtualPx = 13.5f,
                letterSpacingVirtualPx = 0.2f,
                scaleX = 1.12f,
                includeFontPadding = false,
                modifier = Modifier.gbaVirtualBounds(53, 147, 46, 15, scale)
            )
            // Bottom-Right: (x = 113, y = 147) -> RUN
            GbaText(
                text = "RUN",
                scale = scale,
                color = Color(0xFF283038),
                fontSizeVirtualPx = 13.5f,
                lineHeightVirtualPx = 13.5f,
                letterSpacingVirtualPx = 0.2f,
                scaleX = 1.12f,
                includeFontPadding = false,
                modifier = Modifier.gbaVirtualBounds(113, 147, 44, 15, scale)
            )

            // Animated Cursor for Root Menu
            GbaPixelImage(
                resId = R.drawable.gba_cursor_black,
                contentDescription = "Cursor",
                modifier = Modifier
                    .offset(
                        x = ((animatedCursorX + cursorIdleOffset) * scale).dp,
                        y = (animatedCursorY * scale).dp
                    )
                    .size(width = (6 * scale).dp, height = (10 * scale).dp)
            )

            // Interactive Quadrants: Box 3 is (35, 124, 122, 43)
            // Top-Left: FIGHT (35, 124, 61, 21)
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(35, 124, 61, 21, scale)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val prev = selectedRootOption
                        viewModel.setSelectedRootOption(GbaRootOption.FIGHT)
                        coroutineScope.launch {
                            if (prev != GbaRootOption.FIGHT) {
                                delay(150.milliseconds)
                            }
                            viewModel.setCommandMenuState(GbaCommandMenuState.FIGHT_MENU)
                        }
                    }
            )
            // Top-Right: BAG (96, 124, 61, 21)
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(96, 124, 61, 21, scale)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val prev = selectedRootOption
                        viewModel.setSelectedRootOption(GbaRootOption.BAG)
                        coroutineScope.launch {
                            if (prev != GbaRootOption.BAG) {
                                delay(150.milliseconds)
                            }
                            onNavigate("bag_settings")
                        }
                    }
            )
            // Bottom-Left: POKéMON (35, 145, 61, 22)
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(35, 145, 61, 22, scale)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val prev = selectedRootOption
                        viewModel.setSelectedRootOption(GbaRootOption.POKEMON)
                        coroutineScope.launch {
                            if (prev != GbaRootOption.POKEMON) {
                                delay(150.milliseconds)
                            }
                            onNavigate("pokedex_missions")
                        }
                    }
            )
            // Bottom-Right: RUN (96, 145, 61, 22)
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(96, 145, 61, 22, scale)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val prev = selectedRootOption
                        viewModel.setSelectedRootOption(GbaRootOption.RUN)
                        coroutineScope.launch {
                            if (prev != GbaRootOption.RUN) {
                                delay(150.milliseconds)
                            }
                            viewModel.showTemporaryDialog("Got away safely! Battery Saver ON", durationMillis = 700L)
                            delay(700.milliseconds)
                            onNavigate("battery_saver_mode")
                        }
                    }
            )
        } else {
            // FIGHT SUB-MENU (LOG CAL | WATER / HABITS | SYNC)
            GbaPixelImage(
                resId = R.drawable.gba_box3_menu_compact_blank,
                contentDescription = "Fight Moves Blank Menu",
                modifier = Modifier.gbaVirtualBounds(35, 124, 122, 43, scale)
            )

            // 4 Action Labels for Fight Menu
            // Top-Left: (x = 53, y = 131) -> LOG CAL
            GbaText(
                text = "LOG CAL",
                scale = scale,
                color = Color(0xFF283038),
                fontSizeVirtualPx = 13.5f,
                lineHeightVirtualPx = 13.5f,
                letterSpacingVirtualPx = 0.2f,
                scaleX = 1.12f,
                includeFontPadding = false,
                modifier = Modifier.gbaVirtualBounds(53, 131, 46, 15, scale)
            )
            // Top-Right: (x = 113, y = 131) -> WATER
            GbaText(
                text = "WATER",
                scale = scale,
                color = Color(0xFF283038),
                fontSizeVirtualPx = 13.5f,
                lineHeightVirtualPx = 13.5f,
                letterSpacingVirtualPx = 0.2f,
                scaleX = 1.12f,
                includeFontPadding = false,
                modifier = Modifier.gbaVirtualBounds(113, 131, 44, 15, scale)
            )
            // Bottom-Left: (x = 53, y = 147) -> HABITS
            GbaText(
                text = "HABITS",
                scale = scale,
                color = Color(0xFF283038),
                fontSizeVirtualPx = 13.5f,
                lineHeightVirtualPx = 13.5f,
                letterSpacingVirtualPx = 0.2f,
                scaleX = 1.12f,
                includeFontPadding = false,
                modifier = Modifier.gbaVirtualBounds(53, 147, 46, 15, scale)
            )
            // Bottom-Right: (x = 113, y = 147) -> SYNC
            GbaText(
                text = "SYNC",
                scale = scale,
                color = Color(0xFF283038),
                fontSizeVirtualPx = 13.5f,
                lineHeightVirtualPx = 13.5f,
                letterSpacingVirtualPx = 0.2f,
                scaleX = 1.12f,
                includeFontPadding = false,
                modifier = Modifier.gbaVirtualBounds(113, 147, 44, 15, scale)
            )

            // Animated Cursor for Fight Menu
            GbaPixelImage(
                resId = R.drawable.gba_cursor_black,
                contentDescription = "Fight Cursor",
                modifier = Modifier
                    .offset(
                        x = ((animatedCursorX + cursorIdleOffset) * scale).dp,
                        y = (animatedCursorY * scale).dp
                    )
                    .size(width = (6 * scale).dp, height = (10 * scale).dp)
            )

            // Interactive Quadrants: Box 3 is (35, 124, 122, 43)
            // 1. LOG CAL (35, 124, 61, 21)
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(35, 124, 61, 21, scale)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val prev = selectedFightOption
                        viewModel.setSelectedFightOption(GbaFightOption.LOG_CAL)
                        coroutineScope.launch {
                            if (prev != GbaFightOption.LOG_CAL) {
                                delay(150.milliseconds)
                            }
                            if (isOverCalorie) {
                                onNavigate("over_calorie_red_alert")
                            } else {
                                onNavigate("rotary_calorie_logger")
                            }
                        }
                    }
            )
            // 2. WATER (+250ml) (96, 124, 61, 21)
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(96, 124, 61, 21, scale)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setSelectedFightOption(GbaFightOption.WATER)
                        viewModel.logWaterQuick250()
                    }
            )
            // 3. HABITS (1-Mile Run & Daily Habits) (35, 145, 61, 22)
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(35, 145, 61, 22, scale)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val prev = selectedFightOption
                        viewModel.setSelectedFightOption(GbaFightOption.HABITS)
                        coroutineScope.launch {
                            if (prev != GbaFightOption.HABITS) {
                                delay(150.milliseconds)
                            }
                            onNavigate("quick_habit_actions")
                        }
                    }
            )
            // 4. SYNC (Data Layer Flush) (96, 145, 61, 22)
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(96, 145, 61, 22, scale)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setSelectedFightOption(GbaFightOption.SYNC)
                        viewModel.triggerSync()
                    }
            )
            // Animated Cursor for Fight Menu
            GbaPixelImage(
                resId = R.drawable.gba_cursor_black,
                contentDescription = "Fight Cursor",
                modifier = Modifier
                    .offset(
                        x = ((animatedCursorX + cursorIdleOffset) * scale).dp,
                        y = (animatedCursorY * scale).dp
                    )
                    .size(width = (6 * scale).dp, height = (10 * scale).dp)
            )

            // Interactive Quadrants: Box 3 is (35, 124, 122, 43)
            // 1. LOG CAL (35, 124, 61, 21)
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(35, 124, 61, 21, scale)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val prev = selectedFightOption
                        viewModel.setSelectedFightOption(GbaFightOption.LOG_CAL)
                        coroutineScope.launch {
                            if (prev != GbaFightOption.LOG_CAL) {
                                delay(150.milliseconds)
                            }
                            if (isOverCalorie) {
                                onNavigate("over_calorie_red_alert")
                            } else {
                                onNavigate("rotary_calorie_logger")
                            }
                        }
                    }
            )
            // 2. WATER (+250ml) (96, 124, 61, 21)
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(96, 124, 61, 21, scale)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setSelectedFightOption(GbaFightOption.WATER)
                        viewModel.logWaterQuick250()
                    }
            )
            // 3. HABITS (1-Mile Run & Daily Habits) (35, 145, 61, 22)
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(35, 145, 61, 22, scale)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val prev = selectedFightOption
                        viewModel.setSelectedFightOption(GbaFightOption.HABITS)
                        coroutineScope.launch {
                            if (prev != GbaFightOption.HABITS) {
                                delay(150.milliseconds)
                            }
                            onNavigate("quick_habit_actions")
                        }
                    }
            )
            // 4. SYNC (Data Layer Flush) (96, 145, 61, 22)
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(96, 145, 61, 22, scale)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setSelectedFightOption(GbaFightOption.SYNC)
                        viewModel.triggerSync()
                    }
            )
        }

        // 7. POKÉMON UNLOCKED CELEBRATION POP-UP OVERLAY
        val unlockEvent = state.newlyUnlockedPokemon
        if (unlockEvent != null) {
            val unlockedPoke = unlockEvent.pokemon
            val frontSpriteUri = com.poketch.core.domain.model.Gen3PokemonSpriteResolver.getFrontSpriteUri(unlockedPoke.pokedexNumber)

            // Dimmed scrim covering the whole 192x192 canvas
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(0, 0, 192, 192, scale)
                    .background(Color(0xD8000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        viewModel.dismissUnlockCelebration()
                    }
            )

            // GBA Dialogue / Specimen Card at (x = 24, y = 22, w = 144, h = 148)
            // Inscribed perfectly inside circular smartwatch screen boundaries
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(24, 22, 144, 148, scale)
                    .background(Color(0xFFF8F8F0), shape = RoundedCornerShape((18 * scale).dp))
                    .border((2 * scale).dp, Color(0xFF204840), shape = RoundedCornerShape((18 * scale).dp))
                    .padding(horizontal = (10 * scale).dp, vertical = (7 * scale).dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header Banner
                    GbaText(
                        text = "★ POKéMON UNLOCKED! ★",
                        scale = scale,
                        color = Color(0xFFD04800),
                        fontSizeVirtualPx = 9.5f,
                        lineHeightVirtualPx = 10f,
                        fontWeight = FontWeight.Black,
                        scaleX = 1.0f,
                        letterSpacingVirtualPx = 0.0f,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Specimen Monitor Box
                    Box(
                        modifier = Modifier
                            .size((46 * scale).dp)
                            .background(Color(0xFFE8F0E8), shape = RoundedCornerShape((8 * scale).dp))
                            .border((1.5f * scale).dp, Color(0xFF507870), shape = RoundedCornerShape((8 * scale).dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(frontSpriteUri)
                                .crossfade(false)
                                .error(R.drawable.gba_ball_empty)
                                .fallback(R.drawable.gba_ball_empty)
                                .build(),
                            contentDescription = unlockedPoke.name,
                            filterQuality = FilterQuality.None,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size((40 * scale).dp)
                        )
                    }

                    // Pokémon Name & Number
                    GbaText(
                        text = "#%03d %s".format(unlockedPoke.pokedexNumber, unlockedPoke.name.uppercase()),
                        scale = scale,
                        color = Color(0xFF182820),
                        fontSizeVirtualPx = 11f,
                        lineHeightVirtualPx = 12f,
                        fontWeight = FontWeight.Bold,
                        scaleX = 1.0f,
                        letterSpacingVirtualPx = 0.0f,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Trigger Reason / Mission Description
                    val reason = unlockEvent.triggerReason.ifBlank { unlockedPoke.unlockRule.description }
                    GbaText(
                        text = reason,
                        scale = scale,
                        color = Color(0xFF405848),
                        fontSizeVirtualPx = 7.5f,
                        lineHeightVirtualPx = 8.5f,
                        scaleX = 1.0f,
                        letterSpacingVirtualPx = 0.0f,
                        maxLines = 2,
                        softWrap = true,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = (4 * scale).dp)
                    )

                    // Action Buttons (Row: SET BUDDY | GOTCHA!)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = (4 * scale).dp),
                        horizontalArrangement = Arrangement.spacedBy((6 * scale).dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // SET BUDDY Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height((20 * scale).dp)
                                .background(Color(0xFFC83838), shape = RoundedCornerShape((5 * scale).dp))
                                .border((1 * scale).dp, Color(0xFF701818), shape = RoundedCornerShape((5 * scale).dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.setUnlockedAsActiveBuddy(unlockedPoke.pokedexNumber)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            GbaText(
                                text = "SET BUDDY",
                                scale = scale,
                                color = Color.White,
                                fontSizeVirtualPx = 8f,
                                fontWeight = FontWeight.Bold,
                                scaleX = 1.0f,
                                letterSpacingVirtualPx = 0.0f,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = (-1f * scale).dp)
                            )
                        }

                        // GOTCHA! Button (Yellow)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height((20 * scale).dp)
                                .background(Color(0xFFFFC820), shape = RoundedCornerShape((5 * scale).dp))
                                .border((1 * scale).dp, Color(0xFF885800), shape = RoundedCornerShape((5 * scale).dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.dismissUnlockCelebration()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            GbaText(
                                text = "GOTCHA!",
                                scale = scale,
                                color = Color(0xFF281C04),
                                fontSizeVirtualPx = 8f,
                                fontWeight = FontWeight.Bold,
                                scaleX = 1.0f,
                                letterSpacingVirtualPx = 0.0f,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = (-1f * scale).dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
