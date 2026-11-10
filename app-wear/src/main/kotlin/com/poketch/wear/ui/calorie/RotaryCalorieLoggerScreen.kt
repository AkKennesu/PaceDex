package com.poketch.wear.ui.calorie

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Legacy wrapper pointing to the Modern GBA Light Theme Quick Calorie Dial screen.
 */
@Composable
fun RotaryCalorieLoggerScreen(
    onNavigateToAlert: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RotaryCalorieLoggerViewModel = viewModel(),
) {
    QuickCalorieDialScreen(
        onNavigateToAlert = onNavigateToAlert,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
        viewModel = viewModel
    )
}
