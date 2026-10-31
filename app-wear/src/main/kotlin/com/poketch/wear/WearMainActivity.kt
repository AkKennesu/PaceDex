package com.poketch.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.poketch.wear.ui.calorie.RotaryCalorieLoggerScreen
import com.poketch.wear.ui.dashboard.WatchDashboardScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WearMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberSwipeDismissableNavController()

                SwipeDismissableNavHost(
                    navController = navController,
                    startDestination = "watch_dashboard",
                ) {
                    composable("watch_dashboard") {
                        WatchDashboardScreen(
                            onNavigate = { route -> navController.navigate(route) }
                        )
                    }

                    composable("rotary_calorie_logger") {
                        RotaryCalorieLoggerScreen(
                            onNavigateToAlert = { navController.navigate("over_calorie_red_alert") },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("over_calorie_red_alert") {
                        OverCalorieRedAlertDestination(
                            onBack = { navController.popBackStack() },
                            onGoToRun = { navController.navigate("run_tracker") }
                        )
                    }

                    composable("quick_habit_actions") {
                        QuickHabitActionsDestination(
                            onBack = { navController.popBackStack() },
                            onStartRun = { navController.navigate("run_tracker") }
                        )
                    }

                    composable("run_tracker") {
                        RunTrackerDestination(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverCalorieRedAlertDestination(
    onBack: () -> Unit,
    onGoToRun: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScalingLazyListState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF330000)),
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = scrollState) }
    ) {
        ScalingLazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                Text(
                    text = "⚠️ OVER TARGET ALERT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF5252),
                    textAlign = TextAlign.Center
                )
            }
            item {
                Text(
                    text = "Surplus detected! Offset with a brisk walk or 1-mile jog to keep your Pokémon energized.",
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
            }
            item {
                CompactChip(
                    onClick = onGoToRun,
                    label = { Text("Start 1-Mi Run", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = ChipDefaults.primaryChipColors(backgroundColor = Color(0xFFD32F2F)),
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
            }
            item {
                CompactChip(
                    onClick = onBack,
                    label = { Text("Dismiss", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
            }
        }
    }
}

@Composable
private fun QuickHabitActionsDestination(
    onBack: () -> Unit,
    onStartRun: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScalingLazyListState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = scrollState) }
    ) {
        ScalingLazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                Text(
                    text = "Quick Habit Actions",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                CompactChip(
                    onClick = onStartRun,
                    label = { Text("Log 1-Mile Run", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = ChipDefaults.primaryChipColors(backgroundColor = Color(0xFF2E7D32)),
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }
            item {
                CompactChip(
                    onClick = onBack,
                    label = { Text("Back", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }
        }
    }
}

@Composable
private fun RunTrackerDestination(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScalingLazyListState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = scrollState) }
    ) {
        ScalingLazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                Text(
                    text = "🏃 1-Mile Run Tracker",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
            item {
                Text(
                    text = "GPS & Step pace tracking active. Complete 1.0 mile to unlock Ponyta & Rapidash!",
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
            }
            item {
                CompactChip(
                    onClick = onBack,
                    label = { Text("Complete & Save", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = ChipDefaults.primaryChipColors(backgroundColor = Color(0xFF2E7D32)),
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }
        }
    }
}
