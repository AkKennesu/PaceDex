package com.poketch.wear.ui.calorie

import androidx.compose.foundation.focusable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText

@Composable
fun RotaryCalorieLoggerScreen(
    onNavigateToAlert: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RotaryCalorieLoggerViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScalingLazyListState()
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    var accumulatedDelta by remember { mutableFloatStateOf(0f) }
    val deltaThreshold = 35f

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onRotaryScrollEvent { event ->
                accumulatedDelta += event.verticalScrollPixels
                if (accumulatedDelta >= deltaThreshold) {
                    val steps = (accumulatedDelta / deltaThreshold).toInt()
                    accumulatedDelta %= deltaThreshold
                    val changed = viewModel.adjustCalories(steps * 50)
                    if (changed) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    true
                } else if (accumulatedDelta <= -deltaThreshold) {
                    val steps = (accumulatedDelta / deltaThreshold).toInt()
                    accumulatedDelta %= deltaThreshold
                    val changed = viewModel.adjustCalories(steps * 50)
                    if (changed) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    true
                } else {
                    true
                }
            },
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = scrollState) }
    ) {
        ScalingLazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Header title
            item {
                Text(
                    text = "Quick Calorie Dial",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onSurface
                )
            }

            // Crown Stepper & On-Screen -50 / +50 Buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val changed = viewModel.adjustCalories(-50)
                            if (changed) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        colors = ButtonDefaults.secondaryButtonColors()
                    ) {
                        Text(
                            text = "-50",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "+${state.selectedCalories}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (state.willExceedCeiling) Color(0xFFFF5252) else Color(0xFFFFD54F)
                        )
                        Text(
                            text = "kcal",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }

                    Button(
                        onClick = {
                            val changed = viewModel.adjustCalories(50)
                            if (changed) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        colors = ButtonDefaults.secondaryButtonColors()
                    ) {
                        Text(
                            text = "+50",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Live Budget Preview
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .clip(RoundedCornerShape(8.dp))
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Total: ${state.newTotalCalories} / ${state.targetCalorieCeiling} kcal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (state.willExceedCeiling) Color(0xFFFF5252) else Color(0xFF81C784),
                            textAlign = TextAlign.Center
                        )
                        if (state.willExceedCeiling) {
                            Text(
                                text = "⚠️ +${state.surplusKcal} kcal Surplus!",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFF5252),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Confirm Action Chip
            item {
                Chip(
                    onClick = {
                        viewModel.confirmCalorieLog(
                            onOverTarget = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateToAlert()
                            },
                            onSuccess = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateBack()
                            }
                        )
                    },
                    label = {
                        Text(
                            text = "Log +${state.selectedCalories} kcal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    },
                    colors = ChipDefaults.primaryChipColors(
                        backgroundColor = if (state.willExceedCeiling) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .height(36.dp)
                )
            }

            item {
                Text(
                    text = "Presets",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 3 Preset Quick Chips
            item {
                CompactChip(
                    onClick = {
                        viewModel.setCalories(150)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    label = {
                        Text(
                            text = "+150 kcal Snack",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .height(28.dp)
                )
            }

            item {
                CompactChip(
                    onClick = {
                        viewModel.setCalories(500)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    label = {
                        Text(
                            text = "+500 kcal Meal",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .height(28.dp)
                )
            }

            item {
                CompactChip(
                    onClick = {
                        viewModel.setCalories(120)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    label = {
                        Text(
                            text = "+120 kcal Drink",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .height(28.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
