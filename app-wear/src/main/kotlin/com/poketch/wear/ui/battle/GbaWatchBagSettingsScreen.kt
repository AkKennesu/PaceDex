package com.poketch.wear.ui.battle

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poketch.wear.R
import kotlin.math.abs

enum class BagPocket(val tabLabel: String, val bannerTitle: String) {
    KEYS("KEYS", "< KEY ITEMS >"),
    BALLS("BALLS", "< POKé BALLS >"),
    LINK("LINK", "< LINK CABLE >")
}

/**
 * Authentic Generation III GBA Item Bag & Settings Screen rendered on a fixed
 * 192x192 virtual coordinate canvas with nearest-neighbor FilterQuality.None pixel scaling.
 */
@Composable
fun GbaWatchBagSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GbaWatchBattleViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRound = LocalConfiguration.current.isScreenRound
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }

    var selectedPocket by remember { mutableStateOf(BagPocket.KEYS) }
    var rotaryAccumulator by remember { mutableFloatStateOf(0f) }
    val stepThreshold = 38f

    // Intercept physical Back button / gesture
    BackHandler {
        onNavigateBack()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val profile = state.userProfile
    val bmr = profile?.bmrKcal ?: 1650
    val tdee = profile?.tdeeKcal ?: 2300
    val ceiling = state.targetCalorieCeiling
    val currentDeficit = profile?.dailyDeficitGoalKcal ?: profile?.deficitTier?.dailyDeficitKcal ?: 500
    val deficitTierName = when (currentDeficit) {
        250 -> "MILD"
        500 -> "STANDARD"
        750 -> "AGGRESSIVE"
        else -> "CUSTOM"
    }

    GbaWatchCanvas(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onRotaryScrollEvent {
                val delta = it.verticalScrollPixels
                if (selectedPocket == BagPocket.KEYS) {
                    rotaryAccumulator += delta
                    if (abs(rotaryAccumulator) >= stepThreshold) {
                        val steps = (rotaryAccumulator / stepThreshold).toInt()
                        rotaryAccumulator %= stepThreshold
                        val newDeficit = (currentDeficit + steps * 50).coerceIn(100, 1000)
                        if (newDeficit != currentDeficit) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.updateProfileDeficit(newDeficit)
                        }
                    }
                    true
                } else {
                    rotaryAccumulator += delta
                    if (abs(rotaryAccumulator) >= stepThreshold * 1.5f) {
                        val steps = (rotaryAccumulator / (stepThreshold * 1.5f)).toInt()
                        rotaryAccumulator %= (stepThreshold * 1.5f)
                        val pockets = BagPocket.entries
                        val nextIndex = (selectedPocket.ordinal + steps).mod(pockets.size)
                        selectedPocket = pockets[nextIndex]
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    true
                }
            }
    ) { scale ->

        // 1. BASE BACKGROUND (192x192 at (0, 0))
        val shellRes = if (isRound) {
            R.drawable.gba_bag_watch_round_base
        } else {
            R.drawable.gba_bag_watch_square_base
        }
        GbaPixelImage(
            resId = shellRes,
            contentDescription = "GBA Bag Shell",
            modifier = Modifier.gbaVirtualBounds(0, 0, 192, 192, scale)
        )

        // 2. TOP POCKET SELECTOR BAR (x = 43..149, y = 25..43)
        GbaPixelImage(
            resId = R.drawable.gba_bag_pocket_tab_bar,
            contentDescription = "Tab Bar",
            modifier = Modifier.gbaVirtualBounds(43, 25, 106, 18, scale)
        )

        // Tab 1: KEYS (x = 46, y = 28, w = 32, h = 12)
        Box(
            modifier = Modifier
                .gbaVirtualBounds(46, 28, 32, 12, scale)
                .clickable {
                    if (selectedPocket != BagPocket.KEYS) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedPocket = BagPocket.KEYS
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val tabRes = if (selectedPocket == BagPocket.KEYS) {
                R.drawable.gba_bag_tab_active
            } else {
                R.drawable.gba_bag_tab_inactive
            }
            GbaPixelImage(
                resId = tabRes,
                contentDescription = "Keys Tab",
                modifier = Modifier.fillMaxSize()
            )
            GbaText(
                text = "KEYS",
                scale = scale,
                color = if (selectedPocket == BagPocket.KEYS) Color.White else Color(0xFFB4CDDC),
                fontSizeVirtualPx = 8.5f,
                lineHeightVirtualPx = 9.0f,
                letterSpacingVirtualPx = 0.0f,
                scaleX = 1.0f,
                fontWeight = if (selectedPocket == BagPocket.KEYS) FontWeight.ExtraBold else FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        // Tab 2: BALLS (x = 80, y = 28, w = 32, h = 12)
        Box(
            modifier = Modifier
                .gbaVirtualBounds(80, 28, 32, 12, scale)
                .clickable {
                    if (selectedPocket != BagPocket.BALLS) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedPocket = BagPocket.BALLS
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val tabRes = if (selectedPocket == BagPocket.BALLS) {
                R.drawable.gba_bag_tab_active
            } else {
                R.drawable.gba_bag_tab_inactive
            }
            GbaPixelImage(
                resId = tabRes,
                contentDescription = "Balls Tab",
                modifier = Modifier.fillMaxSize()
            )
            GbaText(
                text = "BALLS",
                scale = scale,
                color = if (selectedPocket == BagPocket.BALLS) Color.White else Color(0xFFB4CDDC),
                fontSizeVirtualPx = 8.5f,
                lineHeightVirtualPx = 9.0f,
                letterSpacingVirtualPx = 0.0f,
                scaleX = 1.0f,
                fontWeight = if (selectedPocket == BagPocket.BALLS) FontWeight.ExtraBold else FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        // Tab 3: LINK (x = 114, y = 28, w = 32, h = 12)
        Box(
            modifier = Modifier
                .gbaVirtualBounds(114, 28, 32, 12, scale)
                .clickable {
                    if (selectedPocket != BagPocket.LINK) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedPocket = BagPocket.LINK
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val tabRes = if (selectedPocket == BagPocket.LINK) {
                R.drawable.gba_bag_tab_active
            } else {
                R.drawable.gba_bag_tab_inactive
            }
            GbaPixelImage(
                resId = tabRes,
                contentDescription = "Link Tab",
                modifier = Modifier.fillMaxSize()
            )
            GbaText(
                text = "LINK",
                scale = scale,
                color = if (selectedPocket == BagPocket.LINK) Color.White else Color(0xFFB4CDDC),
                fontSizeVirtualPx = 8.5f,
                lineHeightVirtualPx = 9.0f,
                letterSpacingVirtualPx = 0.0f,
                scaleX = 1.0f,
                fontWeight = if (selectedPocket == BagPocket.LINK) FontWeight.ExtraBold else FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        // 3. POCKET RIBBON SUB-HEADER BANNER (x = 44, y = 45, w = 104, h = 13)
        Box(
            modifier = Modifier
                .gbaVirtualBounds(44, 45, 104, 13, scale)
                .clickable {
                    // Tap banner to cycle pocket
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val pockets = BagPocket.entries
                    val nextIndex = (selectedPocket.ordinal + 1).mod(pockets.size)
                    selectedPocket = pockets[nextIndex]
                },
            contentAlignment = Alignment.Center
        ) {
            GbaPixelImage(
                resId = R.drawable.gba_bag_pocket_banner,
                contentDescription = "Pocket Banner",
                modifier = Modifier.fillMaxSize()
            )
            GbaText(
                text = selectedPocket.bannerTitle,
                scale = scale,
                color = Color(0xFF283030),
                fontSizeVirtualPx = 9.5f,
                lineHeightVirtualPx = 10.0f,
                letterSpacingVirtualPx = 0.2f,
                scaleX = 1.0f,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }

        // 4. MAIN PARCHMENT WINDOW (x = 26, y = 59, w = 140, h = 72)
        GbaPixelImage(
            resId = R.drawable.gba_bag_parchment_panel,
            contentDescription = "Parchment Panel",
            modifier = Modifier.gbaVirtualBounds(26, 59, 140, 72, scale)
        )

        // POCKET VIEW A: KEYS
        if (selectedPocket == BagPocket.KEYS) {
            // Gold-Teal GBA Stats Card (x = 32, y = 63, w = 128, h = 32)
            Box(
                modifier = Modifier.gbaVirtualBounds(32, 63, 128, 32, scale)
            ) {
                GbaPixelImage(
                    resId = R.drawable.gba_bag_keys_stat_card,
                    contentDescription = "Stats Card",
                    modifier = Modifier.fillMaxSize()
                )

                // Line 1: Left BMR (38, 65) & Right TDEE (96, 65)
                GbaText(
                    text = "BMR: $bmr",
                    scale = scale,
                    color = Color(0xFFE6F0FF),
                    fontSizeVirtualPx = 8.2f,
                    lineHeightVirtualPx = 8.5f,
                    letterSpacingVirtualPx = 0.0f,
                    scaleX = 1.0f,
                    modifier = Modifier.offset(x = (6 * scale).dp, y = (2 * scale).dp)
                )
                GbaText(
                    text = "TDEE: $tdee",
                    scale = scale,
                    color = Color(0xFFE6F0FF),
                    fontSizeVirtualPx = 8.2f,
                    lineHeightVirtualPx = 8.5f,
                    letterSpacingVirtualPx = 0.0f,
                    scaleX = 1.0f,
                    modifier = Modifier.offset(x = (64 * scale).dp, y = (2 * scale).dp)
                )

                // Line 2: CEILING (38, 73)
                GbaText(
                    text = "CEILING: $ceiling KCAL",
                    scale = scale,
                    color = Color(0xFFF8D840),
                    fontSizeVirtualPx = 9.5f,
                    lineHeightVirtualPx = 10.0f,
                    letterSpacingVirtualPx = 0.1f,
                    scaleX = 1.0f,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.offset(x = (6 * scale).dp, y = (11 * scale).dp)
                )

                // Line 3: DEFICIT (38, 83)
                GbaText(
                    text = "DEFICIT: $deficitTierName (-$currentDeficit)",
                    scale = scale,
                    color = Color(0xFF78F0C8),
                    fontSizeVirtualPx = 8.0f,
                    lineHeightVirtualPx = 8.5f,
                    letterSpacingVirtualPx = 0.0f,
                    scaleX = 1.0f,
                    modifier = Modifier.offset(x = (6 * scale).dp, y = (21 * scale).dp)
                )
            }

            // MANUAL DEFICIT ADJUST: Header Label (x = 32, y = 96, w = 128, h = 9)
            Box(
                modifier = Modifier.gbaVirtualBounds(32, 96, 128, 9, scale),
                contentAlignment = Alignment.Center
            ) {
                GbaText(
                    text = "MANUAL DEFICIT ADJUST:",
                    scale = scale,
                    color = Color(0xFF283030),
                    fontSizeVirtualPx = 8.2f,
                    lineHeightVirtualPx = 8.5f,
                    letterSpacingVirtualPx = 0.0f,
                    scaleX = 1.0f,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            // Interactive Manual Deficit Stepper: y = 106, h = 18
            // Left [-50] Stepper Button (x = 32, y = 106, w = 28, h = 18)
            val minusInteractionSource = remember { MutableInteractionSource() }
            val isMinusPressed by minusInteractionSource.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(32, 106, 28, 18, scale)
                    .graphicsLayer {
                        if (isMinusPressed) {
                            translationY = 1f * scale
                        }
                    }
                    .clickable(
                        interactionSource = minusInteractionSource,
                        indication = null
                    ) {
                        val newDeficit = (currentDeficit - 50).coerceIn(100, 1000)
                        if (newDeficit != currentDeficit) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.updateProfileDeficit(newDeficit)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                GbaPixelImage(
                    resId = R.drawable.gba_bag_btn_step_minus,
                    contentDescription = "Step Minus 50",
                    modifier = Modifier.fillMaxSize()
                )
                GbaText(
                    text = "-50",
                    scale = scale,
                    color = Color.White,
                    fontSizeVirtualPx = 10.5f,
                    lineHeightVirtualPx = 11.0f,
                    letterSpacingVirtualPx = 0.0f,
                    scaleX = 1.0f,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }

            // Center Digital Deficit Value Box (x = 64, y = 106, w = 64, h = 18)
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(64, 106, 64, 18, scale)
                    .clickable {
                        // Cycle presets 250 -> 500 -> 750 -> 250
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val nextPreset = when (currentDeficit) {
                            250 -> 500
                            500 -> 750
                            else -> 250
                        }
                        viewModel.updateProfileDeficit(nextPreset)
                    },
                contentAlignment = Alignment.Center
            ) {
                GbaPixelImage(
                    resId = R.drawable.gba_bag_deficit_value_box,
                    contentDescription = "Deficit Value Box",
                    modifier = Modifier.fillMaxSize()
                )
                GbaText(
                    text = "-$currentDeficit KCAL",
                    scale = scale,
                    color = Color(0xFFF8D840),
                    fontSizeVirtualPx = 10.5f,
                    lineHeightVirtualPx = 11.0f,
                    letterSpacingVirtualPx = 0.1f,
                    scaleX = 1.0f,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }

            // Right [+50] Stepper Button (x = 132, y = 106, w = 28, h = 18)
            val plusInteractionSource = remember { MutableInteractionSource() }
            val isPlusPressed by plusInteractionSource.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(132, 106, 28, 18, scale)
                    .graphicsLayer {
                        if (isPlusPressed) {
                            translationY = 1f * scale
                        }
                    }
                    .clickable(
                        interactionSource = plusInteractionSource,
                        indication = null
                    ) {
                        val newDeficit = (currentDeficit + 50).coerceIn(100, 1000)
                        if (newDeficit != currentDeficit) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.updateProfileDeficit(newDeficit)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                GbaPixelImage(
                    resId = R.drawable.gba_bag_btn_step_plus,
                    contentDescription = "Step Plus 50",
                    modifier = Modifier.fillMaxSize()
                )
                GbaText(
                    text = "+50",
                    scale = scale,
                    color = Color.White,
                    fontSizeVirtualPx = 10.5f,
                    lineHeightVirtualPx = 11.0f,
                    letterSpacingVirtualPx = 0.0f,
                    scaleX = 1.0f,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // POCKET VIEW B: BALLS (Hydration & 1-Mile Run Reminders)
        if (selectedPocket == BagPocket.BALLS) {
            // Row 1 — Hydration Reminder Slot Card (x = 32, y = 65, w = 128, h = 26)
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(32, 65, 128, 26, scale)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.toggleHydrationReminders()
                    }
            ) {
                GbaPixelImage(
                    resId = R.drawable.gba_bag_item_row_card,
                    contentDescription = "Hydration Card",
                    modifier = Modifier.fillMaxSize()
                )

                // Water Icon (x = 39 - 32 = 7, y = 72 - 65 = 7, w = 12, h = 12)
                GbaPixelImage(
                    resId = R.drawable.gba_bag_icon_water,
                    contentDescription = "Water Icon",
                    modifier = Modifier
                        .offset(x = (7 * scale).dp, y = (7 * scale).dp)
                        .size((12 * scale).dp)
                )

                // Title (x = 55 - 32 = 23, y = 68 - 65 = 3)
                GbaText(
                    text = "WATER (08-21)",
                    scale = scale,
                    color = Color(0xFF283030),
                    fontSizeVirtualPx = 8.5f,
                    lineHeightVirtualPx = 9.0f,
                    letterSpacingVirtualPx = 0.0f,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(x = (23 * scale).dp, y = (3.5f * scale).dp)
                )

                // Subtitle (x = 55 - 32 = 23, y = 78 - 65 = 13)
                GbaText(
                    text = "REMINDER NUDGE",
                    scale = scale,
                    color = Color(0xFF687060),
                    fontSizeVirtualPx = 7.5f,
                    lineHeightVirtualPx = 8.0f,
                    letterSpacingVirtualPx = 0.0f,
                    modifier = Modifier.offset(x = (23 * scale).dp, y = (13.5f * scale).dp)
                )

                // Toggle Switch Badge (x = 127 - 32 = 95, y = 71 - 65 = 6, w = 28, h = 14)
                val isHydrationOn = state.hydrationRemindersEnabled
                val toggleRes = if (isHydrationOn) {
                    R.drawable.gba_bag_toggle_on
                } else {
                    R.drawable.gba_bag_toggle_off
                }
                Box(
                    modifier = Modifier
                        .offset(x = (95 * scale).dp, y = (6 * scale).dp)
                        .size(width = (28 * scale).dp, height = (14 * scale).dp),
                    contentAlignment = Alignment.Center
                ) {
                    GbaPixelImage(
                        resId = toggleRes,
                        contentDescription = "Toggle Hydration",
                        modifier = Modifier.fillMaxSize()
                    )
                    GbaText(
                        text = if (isHydrationOn) "ON" else "OFF",
                        scale = scale,
                        color = Color.White,
                        fontSizeVirtualPx = 8.5f,
                        lineHeightVirtualPx = 9.0f,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Row 2 — 1-Mile Run Challenge Slot Card (x = 32, y = 96, w = 128, h = 26)
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(32, 96, 128, 26, scale)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.toggleRunReminders()
                    }
            ) {
                GbaPixelImage(
                    resId = R.drawable.gba_bag_item_row_card,
                    contentDescription = "Run Card",
                    modifier = Modifier.fillMaxSize()
                )

                // Pokéball Icon (x = 39 - 32 = 7, y = 103 - 96 = 7, w = 12, h = 12)
                GbaPixelImage(
                    resId = R.drawable.gba_bag_icon_pokeball,
                    contentDescription = "Pokeball Icon",
                    modifier = Modifier
                        .offset(x = (7 * scale).dp, y = (7 * scale).dp)
                        .size((12 * scale).dp)
                )

                // Title (x = 55 - 32 = 23, y = 99 - 96 = 3)
                GbaText(
                    text = "1-MI RUN (17:00)",
                    scale = scale,
                    color = Color(0xFF283030),
                    fontSizeVirtualPx = 8.0f,
                    lineHeightVirtualPx = 8.5f,
                    letterSpacingVirtualPx = 0.0f,
                    scaleX = 0.98f,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(x = (23 * scale).dp, y = (3.5f * scale).dp)
                )

                // Subtitle (x = 55 - 32 = 23, y = 109 - 96 = 13)
                GbaText(
                    text = "DAILY CHALLENGE",
                    scale = scale,
                    color = Color(0xFF687060),
                    fontSizeVirtualPx = 7.5f,
                    lineHeightVirtualPx = 8.0f,
                    letterSpacingVirtualPx = 0.0f,
                    modifier = Modifier.offset(x = (23 * scale).dp, y = (13.5f * scale).dp)
                )

                // Toggle Switch Badge (x = 127 - 32 = 95, y = 102 - 96 = 6, w = 28, h = 14)
                val isRunOn = state.runRemindersEnabled
                val toggleRes = if (isRunOn) {
                    R.drawable.gba_bag_toggle_on
                } else {
                    R.drawable.gba_bag_toggle_off
                }
                Box(
                    modifier = Modifier
                        .offset(x = (95 * scale).dp, y = (6 * scale).dp)
                        .size(width = (28 * scale).dp, height = (14 * scale).dp),
                    contentAlignment = Alignment.Center
                ) {
                    GbaPixelImage(
                        resId = toggleRes,
                        contentDescription = "Toggle Run",
                        modifier = Modifier.fillMaxSize()
                    )
                    GbaText(
                        text = if (isRunOn) "ON" else "OFF",
                        scale = scale,
                        color = Color.White,
                        fontSizeVirtualPx = 8.5f,
                        lineHeightVirtualPx = 9.0f,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // POCKET VIEW C: LINK (Wearable Data Layer Sync)
        if (selectedPocket == BagPocket.LINK) {
            // GBA Link Cable Comms Monitor (x = 32, y = 65, w = 128, h = 30)
            Box(
                modifier = Modifier.gbaVirtualBounds(32, 65, 128, 30, scale)
            ) {
                GbaPixelImage(
                    resId = R.drawable.gba_bag_link_monitor,
                    contentDescription = "Link Monitor",
                    modifier = Modifier.fillMaxSize()
                )

                // Cable Icon (x = 40 - 32 = 8, y = 74 - 65 = 9, w = 12, h = 12)
                GbaPixelImage(
                    resId = R.drawable.gba_bag_icon_link_cable,
                    contentDescription = "Link Icon",
                    modifier = Modifier
                        .offset(x = (8 * scale).dp, y = (9 * scale).dp)
                        .size((12 * scale).dp)
                )

                // Line 1: STATUS: ONLINE (x = 54 - 32 = 22, y = 69 - 65 = 4)
                GbaText(
                    text = "STATUS: ONLINE",
                    scale = scale,
                    color = Color(0xFF78F890),
                    fontSizeVirtualPx = 9.5f,
                    lineHeightVirtualPx = 10.0f,
                    letterSpacingVirtualPx = 0.1f,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(x = (22 * scale).dp, y = (4.5f * scale).dp)
                )

                // Line 2: WEAR DATA LAYER (x = 54 - 32 = 22, y = 80 - 65 = 15)
                GbaText(
                    text = "WEAR DATA LAYER",
                    scale = scale,
                    color = Color(0xFFDCEBFF),
                    fontSizeVirtualPx = 8.5f,
                    lineHeightVirtualPx = 9.0f,
                    letterSpacingVirtualPx = 0.0f,
                    modifier = Modifier.offset(x = (22 * scale).dp, y = (15.5f * scale).dp)
                )
            }

            // 3D GBA Sapphire Sync Button (x = 32, y = 100, w = 128, h = 24)
            val syncInteractionSource = remember { MutableInteractionSource() }
            val isSyncPressed by syncInteractionSource.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(32, 100, 128, 24, scale)
                    .graphicsLayer {
                        if (isSyncPressed) {
                            translationY = 1f * scale
                        }
                    }
                    .clickable(
                        interactionSource = syncInteractionSource,
                        indication = null
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.triggerSync()
                    },
                contentAlignment = Alignment.Center
            ) {
                GbaPixelImage(
                    resId = R.drawable.gba_bag_btn_sync_sapphire,
                    contentDescription = "Sync Button",
                    modifier = Modifier.fillMaxSize()
                )
                GbaText(
                    text = if (state.isSyncing) "SYNCING..." else "FLUSH SYNC NOW",
                    scale = scale,
                    color = Color.White,
                    fontSizeVirtualPx = 11.5f,
                    lineHeightVirtualPx = 12.0f,
                    letterSpacingVirtualPx = 0.2f,
                    scaleX = 1.0f,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 5. BOTTOM GBA COMMAND FOOTER (x = 44, y = 135, w = 104, h = 24)
        val backInteractionSource = remember { MutableInteractionSource() }
        val isBackPressed by backInteractionSource.collectIsPressedAsState()
        Box(
            modifier = Modifier
                .gbaVirtualBounds(44, 135, 104, 24, scale)
                .graphicsLayer {
                    if (isBackPressed) {
                        translationY = 1f * scale
                    }
                }
                .clickable(
                    interactionSource = backInteractionSource,
                    indication = null
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNavigateBack()
                },
            contentAlignment = Alignment.Center
        ) {
            GbaPixelImage(
                resId = R.drawable.gba_bag_btn_back_arena,
                contentDescription = "Back to Battle Arena",
                modifier = Modifier.fillMaxSize()
            )
            GbaText(
                text = "< BATTLE ARENA",
                scale = scale,
                color = Color(0xFF283038),
                fontSizeVirtualPx = 11.5f,
                lineHeightVirtualPx = 12.0f,
                letterSpacingVirtualPx = 0.1f,
                scaleX = 1.0f,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }
    }
}
