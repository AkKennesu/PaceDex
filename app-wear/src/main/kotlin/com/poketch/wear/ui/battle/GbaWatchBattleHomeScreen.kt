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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.poketch.wear.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    // Flashing red HP bar animation for OverTargetWarning
    val infiniteTransition = rememberInfiniteTransition(label = "hp_red_flash")
    val redHpAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "red_alpha"
    )

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
            GbaRootOption.FIGHT, GbaRootOption.POKEMON -> 42f
            GbaRootOption.BAG, GbaRootOption.RUN -> 98f
        }
        GbaCommandMenuState.FIGHT_MENU -> when (selectedFightOption) {
            GbaFightOption.LOG_CAL, GbaFightOption.HABITS -> 40f
            GbaFightOption.WATER, GbaFightOption.SYNC -> 96f
        }
    }

    val targetCursorY = when (menuState) {
        GbaCommandMenuState.ROOT_MENU -> when (selectedRootOption) {
            GbaRootOption.FIGHT, GbaRootOption.BAG -> 131f
            GbaRootOption.POKEMON, GbaRootOption.RUN -> 147f
        }
        GbaCommandMenuState.FIGHT_MENU -> when (selectedFightOption) {
            GbaFightOption.LOG_CAL, GbaFightOption.WATER -> 130f
            GbaFightOption.HABITS, GbaFightOption.SYNC -> 146f
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

        // 2. WILD TARGET POKÉMON SPRITE (64x64 at (x = 102, y = 14))
        // Wild encounter is viewed from the FRONT
        // 2. WILD TARGET POKÉMON SPRITE (64x64 at (x = 102, y = 14))
        // Wild encounter is viewed from the FRONT (full art on Battle Home Screen)
        val target = state.wildTarget
        val targetPokedexNum = target?.pokedexNumber ?: 77
        val targetUri = com.poketch.core.domain.model.Gen3PokemonSpriteResolver.getFrontSpriteUri(targetPokedexNum)
        val targetFallbackUrl = com.poketch.core.domain.model.Gen3PokemonSpriteResolver.getPokeApiFrontUrl(targetPokedexNum)

        Box(
            modifier = Modifier
                .gbaVirtualBounds(102, 14, 64, 64, scale)
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

        // 3. ACTIVE BUDDY POKÉMON SPRITE (64x64 at (x = 18, y = 62))
        // Player's active Buddy is viewed from the BACK (authentic GBA battle perspective)
        val buddy = state.activeBuddy
        val buddyPokedexNum = buddy?.pokedexNumber ?: 1
        val buddyUri = com.poketch.core.domain.model.Gen3PokemonSpriteResolver.getBackSpriteUri(buddyPokedexNum)

        Box(
            modifier = Modifier
                .gbaVirtualBounds(18, 62, 64, 64, scale)
                .scale(buddyScale)
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
                        viewModel.showTemporaryDialog("$buddyName is ready! $remaining kcal to goal!", durationMillis = 1500L)
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

        // 4. WILD TARGET STATUS BOX (100x29 at (x = 28, y = 18))
        GbaPixelImage(
            resId = R.drawable.gba_enemy_status_box_empty_track,
            contentDescription = "Enemy Status Box",
            modifier = Modifier.gbaVirtualBounds(28, 18, 100, 29, scale)
        )

        // Wild Target Name at (x = 34, y = 21)
        val targetDisplayName = (target?.name ?: "PONYTA").uppercase()
        GbaText(
            text = targetDisplayName,
            scale = scale,
            color = Color(0xFF383838),
            fontSizeVirtualPx = 9.5f,
            letterSpacingVirtualPx = 0f,
            modifier = Modifier.gbaVirtualBounds(34, 19, 66, 14, scale)
        )

        // Wild Target Level at (x = 107, y = 19)
        GbaText(
            text = "1",
            scale = scale,
            color = Color(0xFF383838),
            fontSizeVirtualPx = 9.5f,
            letterSpacingVirtualPx = 0f,
            modifier = Modifier.gbaVirtualBounds(107, 19, 16, 14, scale)
        )

        // Mission Progress Bar at (x = 67, y = 35), max width = 48, height = 3
        val missionProgressRatio = target?.progressRatio?.toFloat() ?: 0f
        val missionBarWidth = (48f * missionProgressRatio).roundToInt().coerceIn(0, 48)
        if (missionBarWidth > 0) {
            GbaPixelImage(
                resId = R.drawable.gba_hp_yellow,
                contentDescription = "Mission Progress Bar",
                modifier = Modifier.gbaVirtualBounds(67, 35, missionBarWidth, 3, scale)
            )
        }

        // 5. PLAYER ACTIVE BUDDY STATUS BOX (104x37 at (x = 76, y = 82))
        GbaPixelImage(
            resId = R.drawable.gba_player_status_box_empty_track,
            contentDescription = "Player Status Box",
            modifier = Modifier.gbaVirtualBounds(76, 82, 104, 37, scale)
        )

        // Buddy Name at (x = 84, y = 83)
        val buddyDisplayName = (buddy?.name ?: "BULBASAUR").uppercase()
        GbaText(
            text = buddyDisplayName,
            scale = scale,
            color = Color(0xFF383838),
            fontSizeVirtualPx = 9.5f,
            letterSpacingVirtualPx = 0f,
            modifier = Modifier.gbaVirtualBounds(84, 83, 68, 14, scale)
        )

        // Streak Level at (x = 164, y = 83)
        GbaText(
            text = "1",
            scale = scale,
            color = Color(0xFF383838),
            fontSizeVirtualPx = 9.5f,
            letterSpacingVirtualPx = 0f,
            modifier = Modifier.gbaVirtualBounds(164, 83, 14, 14, scale)
        )

        // Live Calorie Deficit HP Bar at (x = 124, y = 99), max width = 48, height = 3
        val remainingRatio = ((ceiling - consumed).toFloat() / ceiling.toFloat()).coerceIn(0f, 1f)

        if (isOverCalorie) {
            // Flashing full red bar
            GbaPixelImage(
                resId = R.drawable.gba_hp_red,
                contentDescription = "Red Alert HP Track",
                alpha = redHpAlpha,
                modifier = Modifier.gbaVirtualBounds(124, 99, 48, 3, scale)
            )
        } else {
            val hpBarRes = if (consumed < 0.90f * ceiling) {
                R.drawable.gba_hp_green
            } else {
                R.drawable.gba_hp_yellow
            }
            val hpWidth = (48f * remainingRatio).roundToInt().coerceIn(1, 48)
            GbaPixelImage(
                resId = hpBarRes,
                contentDescription = "Calorie Deficit HP Track",
                modifier = Modifier.gbaVirtualBounds(124, 99, hpWidth, 3, scale)
            )
        }

        // Live Deficit Status Numbers at (x = 112, y = 103)
        if (isOverCalorie) {
            val surplus = consumed - ceiling
            GbaText(
                text = "+$surplus OVER",
                scale = scale,
                color = Color(0xFFD32F2F),
                fontSizeVirtualPx = 8.5f,
                letterSpacingVirtualPx = 0f,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.gbaVirtualBounds(112, 103, 64, 12, scale)
            )
        } else {
            val remainingKcal = ceiling - consumed
            GbaText(
                text = "$remainingKcal/$ceiling",
                scale = scale,
                color = Color(0xFF383838),
                fontSizeVirtualPx = 8.5f,
                letterSpacingVirtualPx = 0f,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.gbaVirtualBounds(112, 103, 64, 12, scale)
            )
        }

        // Live Hydration & Habit EXP Bar at (x = 108, y = 115), max width = 64, height = 2
        val waterGoal = state.dailyWaterGoalMl.coerceAtLeast(1)
        val dailyHabitExpRatio = (state.todayWaterMl.toFloat() / waterGoal.toFloat()).coerceIn(0f, 1f)
        val expBarWidth = (64f * dailyHabitExpRatio).roundToInt().coerceIn(0, 64)
        if (expBarWidth > 0) {
            GbaPixelImage(
                resId = R.drawable.gba_exp_cyan,
                contentDescription = "Hydration EXP Bar",
                modifier = Modifier.gbaVirtualBounds(108, 115, expBarWidth, 2, scale)
            )
        }

        // 6. BOTTOM COMMAND MENU (120x48 at (x = 36, y = 121))
        val dialogMsg = state.dialogMessage
        if (dialogMsg != null) {
            // Teal Message Dialog Overlay
            GbaPixelImage(
                resId = R.drawable.gba_watch_dialog_teal_120x48,
                contentDescription = "GBA Dialog Box",
                modifier = Modifier
                    .gbaVirtualBounds(36, 121, 120, 48, scale)
                    .clickable { viewModel.clearDialog() }
            )
            GbaText(
                text = dialogMsg,
                scale = scale,
                color = Color(0xFFF8F8F8),
                fontSizeVirtualPx = 9f,
                letterSpacingVirtualPx = 0f,
                maxLines = 2,
                softWrap = true,
                modifier = Modifier.gbaVirtualBounds(42, 128, 108, 34, scale)
            )
        } else if (menuState == GbaCommandMenuState.ROOT_MENU) {
            // ROOT COMMAND MENU (FIGHT | BAG / POKéMON | RUN)
            GbaPixelImage(
                resId = R.drawable.gba_command_menu_box,
                contentDescription = "GBA Command Menu",
                modifier = Modifier.gbaVirtualBounds(36, 121, 120, 48, scale)
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

            // Interactive Quadrants:
            // Top-Left: FIGHT
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(36, 121, 60, 24, scale)
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
            // Top-Right: BAG
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(96, 121, 60, 24, scale)
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
            // Bottom-Left: POKéMON
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(36, 145, 60, 24, scale)
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
            // Bottom-Right: RUN
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(96, 145, 60, 24, scale)
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
                resId = R.drawable.gba_command_menu_blank,
                contentDescription = "Fight Moves Blank Menu",
                modifier = Modifier.gbaVirtualBounds(36, 121, 120, 48, scale)
            )

            // 4 Action Labels
            GbaText(
                text = "LOG CAL",
                scale = scale,
                fontSizeVirtualPx = 9.5f,
                letterSpacingVirtualPx = 0f,
                modifier = Modifier.gbaVirtualBounds(48, 127, 48, 15, scale)
            )
            GbaText(
                text = "WATER",
                scale = scale,
                fontSizeVirtualPx = 9.5f,
                letterSpacingVirtualPx = 0f,
                modifier = Modifier.gbaVirtualBounds(104, 127, 48, 15, scale)
            )
            GbaText(
                text = "HABITS",
                scale = scale,
                fontSizeVirtualPx = 9.5f,
                letterSpacingVirtualPx = 0f,
                modifier = Modifier.gbaVirtualBounds(48, 143, 48, 15, scale)
            )
            GbaText(
                text = "SYNC",
                scale = scale,
                fontSizeVirtualPx = 9.5f,
                letterSpacingVirtualPx = 0f,
                modifier = Modifier.gbaVirtualBounds(104, 143, 48, 15, scale)
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

            // Interactive Quadrants:
            // 1. LOG CAL
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(36, 121, 60, 24, scale)
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
            // 2. WATER (+250ml)
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(96, 121, 60, 24, scale)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setSelectedFightOption(GbaFightOption.WATER)
                        viewModel.logWaterQuick250()
                    }
            )
            // 3. HABITS (1-Mile Run & Daily Habits)
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(36, 145, 60, 24, scale)
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
            // 4. SYNC (Data Layer Flush)
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(96, 145, 60, 24, scale)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setSelectedFightOption(GbaFightOption.SYNC)
                        viewModel.triggerSync()
                    }
            )
        }
    }
}
