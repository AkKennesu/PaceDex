package com.poketch.wear

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.poketch.wear.ui.alert.OverTargetRedAlertScreen
import com.poketch.wear.ui.alert.OverTargetRedAlertViewModel
import com.poketch.wear.ui.calorie.RotaryCalorieLoggerScreen
import com.poketch.wear.ui.calorie.RotaryCalorieLoggerViewModel
import com.poketch.wear.ui.battle.GbaBagSettingsScreen
import com.poketch.wear.ui.battle.GbaBatterySaverScreen
import com.poketch.wear.ui.battle.GbaPokedexMissionsScreen
import com.poketch.wear.ui.battle.GbaWatchBattleHomeScreen
import com.poketch.wear.ui.battle.GbaWatchBattleViewModel
import com.poketch.wear.ui.dashboard.WatchDashboardScreen
import com.poketch.wear.ui.dashboard.WatchDashboardViewModel
import com.poketch.wear.ui.diagnostics.SyncDiagnosticsScreen
import com.poketch.wear.ui.diagnostics.SyncDiagnosticsViewModel
import com.poketch.wear.ui.habits.QuickHabitActionsScreen
import com.poketch.wear.ui.habits.QuickHabitActionsViewModel
import com.poketch.wear.ui.run.RunTrackerScreen
import com.poketch.wear.ui.run.RunTrackerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class WearMainActivity : ComponentActivity() {

    private val battleViewModel: GbaWatchBattleViewModel by viewModels()
    private val dashboardViewModel: WatchDashboardViewModel by viewModels()
    private val calorieLoggerViewModel: RotaryCalorieLoggerViewModel by viewModels()
    private val alertViewModel: OverTargetRedAlertViewModel by viewModels()
    private val habitsViewModel: QuickHabitActionsViewModel by viewModels()
    private val runTrackerViewModel: RunTrackerViewModel by viewModels()
    private val diagnosticsViewModel: SyncDiagnosticsViewModel by viewModels()

    private val pendingRoute = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialRoute = intent?.getStringExtra("destination_route") ?: "watch_dashboard"

        setContent {
            MaterialTheme {
                val navController = rememberSwipeDismissableNavController()

                LaunchedEffect(Unit) {
                    pendingRoute.collect { route ->
                        if (route != null) {
                            navController.navigate(route)
                            pendingRoute.value = null
                        }
                    }
                }

                SwipeDismissableNavHost(
                    navController = navController,
                    startDestination = initialRoute,
                ) {
                    composable("watch_dashboard") {
                        GbaWatchBattleHomeScreen(
                            onNavigate = { route -> navController.navigate(route) },
                            viewModel = battleViewModel
                        )
                    }

                    composable("watch_battle") {
                        GbaWatchBattleHomeScreen(
                            onNavigate = { route -> navController.navigate(route) },
                            viewModel = battleViewModel
                        )
                    }

                    composable("bag_settings") {
                        GbaBagSettingsScreen(
                            onNavigateBack = { navController.popBackStack() },
                            viewModel = battleViewModel
                        )
                    }

                    composable("pokedex_missions") {
                        com.poketch.wear.ui.battle.GbaWatchPokedexScreen(
                            onNavigateBack = { navController.popBackStack() },
                            viewModel = battleViewModel
                        )
                    }

                    composable("battery_saver_mode") {
                        GbaBatterySaverScreen(
                            onResumeBattle = { navController.popBackStack() },
                            viewModel = battleViewModel
                        )
                    }

                    composable("rotary_calorie_logger") {
                        RotaryCalorieLoggerScreen(
                            onNavigateToAlert = { navController.navigate("over_calorie_red_alert") },
                            onNavigateBack = { navController.popBackStack() },
                            viewModel = calorieLoggerViewModel
                        )
                    }

                    composable("over_calorie_red_alert") {
                        OverTargetRedAlertScreen(
                            onStartRun = { navController.navigate("run_tracker") },
                            onAcknowledge = { navController.popBackStack() },
                            viewModel = alertViewModel
                        )
                    }

                    composable("quick_habit_actions") {
                        QuickHabitActionsScreen(
                            onStartRun = { navController.navigate("run_tracker") },
                            viewModel = habitsViewModel
                        )
                    }

                    composable("run_tracker") {
                        RunTrackerScreen(
                            onNavigateBack = { navController.popBackStack() },
                            viewModel = runTrackerViewModel
                        )
                    }

                    composable("sync_diagnostics") {
                        SyncDiagnosticsScreen(
                            onNavigateBack = { navController.popBackStack() },
                            viewModel = diagnosticsViewModel
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("destination_route")?.let {
            pendingRoute.value = it
        }
    }
}
