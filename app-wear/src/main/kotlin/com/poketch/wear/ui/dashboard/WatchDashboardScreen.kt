package com.poketch.wear.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.poketch.core.domain.calculator.CalorieBudgetState
import com.poketch.core.domain.model.Pokemon
import com.poketch.wear.R
import com.poketch.wear.ui.layout.rememberResponsiveColumnState
import kotlinx.coroutines.delay

@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        content()
    }
}

@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    scrollState: ScalingLazyListState? = null,
    timeText: @Composable () -> Unit = { TimeText() },
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        timeText = timeText,
        positionIndicator = {
            scrollState?.let { PositionIndicator(scalingLazyListState = it) }
        }
    ) {
        content()
    }
}

@Composable
fun WatchDashboardScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WatchDashboardViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val columnState = rememberResponsiveColumnState()
    val haptic = LocalHapticFeedback.current

    AppScaffold(modifier = modifier) {
        ScreenScaffold(scrollState = columnState.listState) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // 1. Dual Concentric Circular Progress Indicators on Watch Bezel
                DualConcentricGauges(
                    calorieProgress = state.calorieProgressRatio,
                    waterProgress = state.waterProgressRatio,
                    budgetState = state.budgetState
                )

                // 2. Center Content: Interactive Buddy & Compact Wrist Action Chips
                ScalingLazyColumn(
                    state = columnState.listState,
                    contentPadding = columnState.contentPadding,
                    autoCentering = columnState.autoCentering,
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Active Buddy Sprite with Tap Animation
                    item {
                        ActiveBuddySection(
                            buddy = state.activeBuddy,
                            isOverCalorie = state.budgetState is CalorieBudgetState.OverTargetWarning,
                            onAlertClick = { onNavigate("over_calorie_red_alert") }
                        )
                    }

                    // Key Metric Status Text
                    item {
                        BudgetMetricsText(
                            consumedKcal = state.consumedKcal,
                            targetCeiling = state.targetCalorieCeiling,
                            waterMl = state.todayWaterMl,
                            waterGoalMl = state.dailyWaterGoalMl,
                            budgetState = state.budgetState
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Chip 1: "+ Log kcal" -> rotary_calorie_logger
                    item {
                        CompactChip(
                            onClick = { onNavigate("rotary_calorie_logger") },
                            label = {
                                Text(
                                    text = "+ Log kcal",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = ChipDefaults.primaryChipColors(
                                backgroundColor = Color(0xFFD32F2F),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth(0.82f)
                                .height(32.dp)
                        )
                    }

                    // Chip 2: "+250ml Water" -> quick water log with haptic feedback
                    item {
                        CompactChip(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.logWaterQuick250()
                            },
                            label = {
                                Text(
                                    text = "+250ml Water",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = ChipDefaults.chipColors(
                                backgroundColor = Color(0xFF006064),
                                contentColor = Color(0xFF00E5FF)
                            ),
                            modifier = Modifier
                                .fillMaxWidth(0.82f)
                                .height(32.dp)
                        )
                    }

                    // Chip 3: "Habits & 1-Mi Run" -> quick_habit_actions
                    item {
                        CompactChip(
                            onClick = { onNavigate("quick_habit_actions") },
                            label = {
                                Text(
                                    text = "Habits & 1-Mi Run",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = ChipDefaults.secondaryChipColors(),
                            modifier = Modifier
                                .fillMaxWidth(0.82f)
                                .height(32.dp)
                        )
                    }

                    // Chip 4: "Sync & Diagnostics" -> sync_diagnostics
                    item {
                        CompactChip(
                            onClick = { onNavigate("sync_diagnostics") },
                            label = {
                                Text(
                                    text = "Sync & Diagnostics",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = ChipDefaults.chipColors(
                                backgroundColor = Color(0xFF1E293B),
                                contentColor = Color(0xFF90CAF9)
                            ),
                            modifier = Modifier
                                .fillMaxWidth(0.82f)
                                .height(32.dp)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DualConcentricGauges(
    calorieProgress: Float,
    waterProgress: Float,
    budgetState: CalorieBudgetState,
    modifier: Modifier = Modifier,
) {
    // Outer Ring: Calorie Budget Gauge (#4CAF50 Green <90%, #FFB300 Amber 90%-100%, #FF5252 Red >100%)
    val outerColor = when {
        budgetState is CalorieBudgetState.OverTargetWarning || calorieProgress > 1.0f -> Color(0xFFFF5252)
        calorieProgress >= 0.90f -> Color(0xFFFFB300)
        else -> Color(0xFF4CAF50)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Outer Ring: Calorie Gauge
        CircularProgressIndicator(
            progress = calorieProgress.coerceIn(0f, 1f),
            modifier = Modifier.fillMaxSize(),
            startAngle = 270f,
            indicatorColor = outerColor,
            trackColor = MaterialTheme.colors.onBackground.copy(alpha = 0.12f),
            strokeWidth = 6.dp
        )

        // Inner Ring: Hydration Goal Gauge (Cyan #00E5FF)
        CircularProgressIndicator(
            progress = waterProgress.coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            startAngle = 270f,
            indicatorColor = Color(0xFF00E5FF),
            trackColor = MaterialTheme.colors.onBackground.copy(alpha = 0.08f),
            strokeWidth = 5.dp
        )
    }
}

@Composable
private fun ActiveBuddySection(
    buddy: Pokemon?,
    isOverCalorie: Boolean,
    onAlertClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var isBouncing by remember { mutableStateOf(false) }
    var showHeart by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isBouncing) 1.22f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "buddy_bounce"
    )

    LaunchedEffect(isBouncing) {
        if (isBouncing) {
            delay(280)
            isBouncing = false
        }
    }

    LaunchedEffect(showHeart) {
        if (showHeart) {
            delay(1200)
            showHeart = false
        }
    }

    val spritePath = buddy?.spriteAssetPath ?: "sprites/pokemon/other/official-artwork/1.png"
    val imageUri = "file:///android_asset/$spritePath"

    Box(
        modifier = modifier
            .size(64.dp)
            .scale(scale)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isBouncing = true
                showHeart = true
                if (isOverCalorie) {
                    onAlertClick()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Buddy Background disc
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    if (isOverCalorie) Color(0xFF4A1414) else Color(0xFF1B3320)
                )
        )

        // 64x64 Sprite (with distressed tired effect if OverTargetWarning)
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUri)
                .crossfade(true)
                .error(R.drawable.splash_icon)
                .fallback(R.drawable.splash_icon)
                .placeholder(R.drawable.splash_icon)
                .build(),
            contentDescription = buddy?.name ?: "Buddy",
            filterQuality = FilterQuality.None,
            colorFilter = if (isOverCalorie) {
                ColorFilter.tint(Color(0xFFFF8A80), BlendMode.Modulate)
            } else null,
            modifier = Modifier.size(54.dp)
        )

        // Floating Heart on tap
        AnimatedVisibility(
            visible = showHeart,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Text(
                text = if (isOverCalorie) "💦" else "❤️",
                fontSize = 14.sp
            )
        }

        // Distressed / Warning Badge if over calorie target
        if (isOverCalorie) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color(0xFFFF5252))
                    .padding(2.dp)
            ) {
                Text(
                    text = "!",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun BudgetMetricsText(
    consumedKcal: Int,
    targetCeiling: Int,
    waterMl: Int,
    waterGoalMl: Int,
    budgetState: CalorieBudgetState,
    modifier: Modifier = Modifier,
) {
    val calorieColor = when (budgetState) {
        is CalorieBudgetState.OverTargetWarning -> Color(0xFFFF5252)
        is CalorieBudgetState.ApproachingCeiling -> Color(0xFFFFB300)
        is CalorieBudgetState.OnTrack -> Color(0xFF81C784)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$consumedKcal / $targetCeiling kcal",
            color = calorieColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$waterMl / $waterGoalMl ml",
            color = Color(0xFF00E5FF),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
