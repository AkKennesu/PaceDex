package com.poketch.wear.ui.battle

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Legacy wrapper pointing to the authentic Generation III GBA Item Bag & Settings Screen
 * rendered on the 192x192 virtual canvas.
 */
@Composable
fun GbaBagSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GbaWatchBattleViewModel = viewModel(),
) {
    GbaWatchBagSettingsScreen(
        onNavigateBack = onNavigateBack,
        modifier = modifier,
        viewModel = viewModel
    )
}
