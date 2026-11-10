package com.poketch.wear.ui.battle

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * GBA Battery Saver OLED Ambient Mode Screen.
 * Delegates directly to the Gen 3 Dark Centered Power-Saving HUD in [BatterySaverScreen].
 */
@Composable
fun GbaBatterySaverScreen(
    onResumeBattle: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GbaWatchBattleViewModel = viewModel(),
) {
    BatterySaverScreen(
        onResumeBattle = onResumeBattle,
        modifier = modifier,
        viewModel = viewModel
    )
}
