package com.poketch.mobile.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.launch

/**
 * Tactile spring press scale + subtle haptic tick for modern buttons & cards.
 * Uses graphicsLayer so it runs purely in the draw phase (zero recomposition lag).
 */
fun Modifier.bouncyPress(
    pressedScale: Float = 0.94f,
    enableHaptics: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bouncyPressScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null
        ) {
            if (enableHaptics) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            onClick()
        }
}

/**
 * Smoothly rolls integer metrics (calories, steps, water ml) with tabular stability.
 */
@Composable
fun rememberAnimatedCounter(targetValue: Int, durationMs: Int = 550): State<Int> {
    return animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
        label = "animatedCounter"
    )
}

/**
 * Text composable with tabular figures that rolls up/down smoothly using animatedCounter.
 */
@Composable
fun AnimatedStatNumber(
    targetValue: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    prefix: String = "",
    suffix: String = "",
    durationMs: Int = 550
) {
    val animatedValue by rememberAnimatedCounter(targetValue, durationMs)
    val tabularStyle = remember(style) {
        style.copy(fontFeatureSettings = "tnum")
    }
    Text(
        text = "$prefix$animatedValue$suffix",
        modifier = modifier,
        style = tabularStyle,
        color = color
    )
}

/**
 * Smooth spring progress value for calorie rings, macro gauges, and mission bars.
 */
@Composable
fun rememberAnimatedProgress(targetFraction: Float): State<Float> {
    return animateFloatAsState(
        targetValue = targetFraction.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessLow
        ),
        label = "animatedProgress"
    )
}

@Composable
fun AnimatedProgressValue(targetFraction: Float): Float {
    return rememberAnimatedProgress(targetFraction).value
}

/**
 * Gentle idle breathing float + tap-to-hop physics for the Active Buddy sprite.
 */
@Composable
fun rememberBuddyMotion(): Pair<Float, () -> Unit> {
    val infinite = rememberInfiniteTransition(label = "buddyIdle")
    val idleBob by infinite.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "buddyIdleBob"
    )

    val jumpOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val triggerHop: () -> Unit = {
        scope.launch {
            jumpOffset.animateTo(-36f, tween(130, easing = FastOutSlowInEasing))
            jumpOffset.animateTo(
                0f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
            )
        }
    }

    return (idleBob + jumpOffset.value) to triggerHop
}
