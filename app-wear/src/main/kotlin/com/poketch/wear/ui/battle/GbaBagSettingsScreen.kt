package com.poketch.wear.ui.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.poketch.core.domain.model.DeficitTier
import com.poketch.wear.ui.layout.rememberResponsiveColumnState

/**
 * State C: GBA BAG Settings & TDEE Calculator Screen.
 * Contains 3 GBA Pockets:
 * - Pocket 1 (KEY ITEMS): Interactive Mifflin-St Jeor BMR/TDEE & Calorie Deficit Calculator.
 * - Pocket 2 (POKé BALLS): Hydration (08:00-21:00) and 1-Mile Run (17:00) reminder toggles.
 * - Pocket 3 (LINK CABLE): Paired device status, immediate Data Layer sync, and haptics.
 */
@Composable
fun GbaBagSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GbaWatchBattleViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val columnState = rememberResponsiveColumnState()
    val haptic = LocalHapticFeedback.current

    var selectedPocketIndex by remember { mutableIntStateOf(0) }
    val pocketNames = listOf("KEY ITEMS", "POKé BALLS", "LINK CABLE")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = columnState.listState) }
    ) {
        ScalingLazyColumn(
            state = columnState.listState,
            contentPadding = columnState.contentPadding,
            autoCentering = columnState.autoCentering,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1410)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header: GBA BAG Title
            item {
                Text(
                    text = "BAG",
                    color = Color(0xFFFFD54F),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PokemonDsFontFamily,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            // Pocket Selector Tabs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E2820))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pocketNames.forEachIndexed { index, name ->
                        val isSelected = selectedPocketIndex == index
                        CompactChip(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedPocketIndex = index
                            },
                            label = {
                                Text(
                                    text = if (index == 0) "KEYS" else if (index == 1) "BALLS" else "LINK",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                    fontFamily = PokemonDsFontFamily
                                )
                            },
                            colors = ChipDefaults.chipColors(
                                backgroundColor = if (isSelected) Color(0xFF2E7D32) else Color.Transparent,
                                contentColor = if (isSelected) Color.White else Color(0xFFB0BEC5)
                            ),
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }
            }

            // Pocket Subtitle
            item {
                Text(
                    text = "— ${pocketNames[selectedPocketIndex]} —",
                    color = Color(0xFF81C784),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PokemonDsFontFamily,
                    textAlign = TextAlign.Center
                )
            }

            // Pocket 1: KEY ITEMS (Mifflin-St Jeor BMR/TDEE & Deficit Calculator)
            if (selectedPocketIndex == 0) {
                val profile = state.userProfile
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A241C))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "BMR: ${profile?.bmrKcal ?: 1650} kcal",
                            color = Color(0xFFE0E0E0),
                            fontSize = 14.sp,
                            fontFamily = PokemonDsFontFamily
                        )
                        Text(
                            text = "TDEE: ${profile?.tdeeKcal ?: 2300} kcal",
                            color = Color(0xFFE0E0E0),
                            fontSize = 14.sp,
                            fontFamily = PokemonDsFontFamily
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "CEILING: ${state.targetCalorieCeiling} kcal",
                            color = Color(0xFFFFD54F),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PokemonDsFontFamily
                        )
                        Text(
                            text = "TIER: ${profile?.deficitTier?.name ?: "MODERATE_500"}",
                            color = Color(0xFF80CBC4),
                            fontSize = 13.sp,
                            fontFamily = PokemonDsFontFamily
                        )
                    }
                }

                item {
                    Text(
                        text = "CHANGE DEFICIT TIER:",
                        color = Color(0xFFB0BEC5),
                        fontSize = 13.sp,
                        fontFamily = PokemonDsFontFamily,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                item {
                    Chip(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val p = state.userProfile ?: return@Chip
                            val nextTier = when (p.deficitTier) {
                                DeficitTier.MILD -> DeficitTier.STANDARD
                                DeficitTier.STANDARD -> DeficitTier.AGGRESSIVE
                                DeficitTier.AGGRESSIVE -> DeficitTier.MILD
                            }
                            viewModel.updateProfileBiometrics(
                                age = p.ageYears,
                                sex = p.sex,
                                heightCm = p.heightCm,
                                weightKg = p.weightKg,
                                activityLevel = p.activityLevel,
                                tier = nextTier
                            )
                        },
                        label = {
                            Text(
                                text = "CYCLE: ${profile?.deficitTier?.name ?: "STANDARD"}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PokemonDsFontFamily
                            )
                        },
                        colors = ChipDefaults.primaryChipColors(
                            backgroundColor = Color(0xFF2E7D32),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(0.92f)
                    )
                }
            }

            // Pocket 2: POKé BALLS (Reminder Toggles)
            if (selectedPocketIndex == 1) {
                item {
                    Chip(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.toggleHydrationReminders()
                        },
                        label = {
                            Text(
                                text = "HYDRATION: ${if (state.hydrationRemindersEnabled) "ON (08-21)" else "OFF"}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PokemonDsFontFamily
                            )
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = if (state.hydrationRemindersEnabled) Color(0xFF006064) else Color(0xFF263238),
                            contentColor = if (state.hydrationRemindersEnabled) Color(0xFF00E5FF) else Color(0xFF90A4AE)
                        ),
                        modifier = Modifier.fillMaxWidth(0.92f)
                    )
                }

                item {
                    Chip(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.toggleRunReminders()
                        },
                        label = {
                            Text(
                                text = "1-MI RUN: ${if (state.runRemindersEnabled) "ON (17:00)" else "OFF"}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PokemonDsFontFamily
                            )
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = if (state.runRemindersEnabled) Color(0xFFBF360C) else Color(0xFF263238),
                            contentColor = if (state.runRemindersEnabled) Color(0xFFFFCCBC) else Color(0xFF90A4AE)
                        ),
                        modifier = Modifier.fillMaxWidth(0.92f)
                    )
                }
            }

            // Pocket 3: LINK CABLE (Data Sync & Diagnostics)
            if (selectedPocketIndex == 2) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A241C))
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "STATUS: ONLINE",
                            color = Color(0xFF69F0AE),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PokemonDsFontFamily
                        )
                        Text(
                            text = "WEARABLE DATA LAYER",
                            color = Color(0xFFB0BEC5),
                            fontSize = 12.sp,
                            fontFamily = PokemonDsFontFamily
                        )
                    }
                }

                item {
                    Chip(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.triggerSync()
                        },
                        label = {
                            Text(
                                text = if (state.isSyncing) "SYNCING..." else "FLUSH SYNC NOW",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PokemonDsFontFamily
                            )
                        },
                        colors = ChipDefaults.primaryChipColors(
                            backgroundColor = Color(0xFF1565C0),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(0.92f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Back button
            item {
                CompactChip(
                    onClick = onNavigateBack,
                    label = {
                        Text(
                            text = "◀ BATTLE ARENA",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PokemonDsFontFamily
                        )
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF37474F),
                        contentColor = Color(0xFFECEFF1)
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(36.dp)
                )
            }
        }
    }
}
