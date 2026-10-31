package com.poketch.wear.ui.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState

class ResponsiveColumnState(
    val listState: ScalingLazyListState,
    val contentPadding: PaddingValues,
    val autoCentering: AutoCenteringParams?,
    val isRound: Boolean
)

/**
 * Enforces round and rectangular screen adaptability across Wear OS screens.
 * Automatically computes proportional bezel insets and auto-centering so
 * top and bottom list items never clip against circular or rounded bezels.
 */
@Composable
fun rememberResponsiveColumnState(
    firstItemIsFullWidth: Boolean = false,
    initialItemIndex: Int = 0,
    initialItemOffset: Int = 0,
    verticalPaddingRatio: Float = 0.18f,
    horizontalPaddingRatio: Float = 0.052f,
    extraTopPadding: Dp = 0.dp,
    extraBottomPadding: Dp = 0.dp
): ResponsiveColumnState {
    val configuration = LocalConfiguration.current
    val isRound = configuration.isScreenRound
    val screenHeightDp = configuration.screenHeightDp.dp
    val screenWidthDp = configuration.screenWidthDp.dp

    val listState = rememberScalingLazyListState(
        initialCenterItemIndex = initialItemIndex,
        initialCenterItemScrollOffset = initialItemOffset
    )

    return remember(isRound, screenHeightDp, screenWidthDp) {
        val horizontalPadding = if (isRound) {
            screenWidthDp * horizontalPaddingRatio
        } else {
            8.dp
        }

        // On round screens, top and bottom need extra inset so items don't clip under curved bezels
        val topPadding = if (isRound) {
            (screenHeightDp * verticalPaddingRatio) + extraTopPadding
        } else {
            12.dp + extraTopPadding
        }

        val bottomPadding = if (isRound) {
            (screenHeightDp * verticalPaddingRatio) + extraBottomPadding
        } else {
            12.dp + extraBottomPadding
        }

        val paddingValues = PaddingValues(
            start = horizontalPadding,
            end = horizontalPadding,
            top = topPadding,
            bottom = bottomPadding
        )

        val autoCentering = if (isRound && !firstItemIsFullWidth) {
            AutoCenteringParams(itemIndex = 0)
        } else {
            null
        }

        ResponsiveColumnState(
            listState = listState,
            contentPadding = paddingValues,
            autoCentering = autoCentering,
            isRound = isRound
        )
    }
}
