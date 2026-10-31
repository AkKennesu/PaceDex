package com.poketch.wear.ui.run

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
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
import com.poketch.core.domain.model.Pokemon
import com.poketch.wear.BuildConfig
import com.poketch.wear.health.RunStatus
import com.poketch.wear.ui.layout.rememberResponsiveColumnState

@Composable
fun RunTrackerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RunTrackerViewModel = viewModel(),
) {
    val state by viewModel.runState.collectAsStateWithLifecycle()
    val activeBuddy by viewModel.activeBuddy.collectAsStateWithLifecycle()
    val columnState = rememberResponsiveColumnState()

    val progressFraction = (state.distanceMiles / 1.0).toFloat().coerceIn(0f, 1f)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = columnState.listState) }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Live 1.0-Mile Circular Progress Ring around Bezel
            CircularProgressIndicator(
                progress = progressFraction,
                modifier = Modifier.fillMaxSize(),
                startAngle = 270f,
                indicatorColor = if (state.hasCompletedOneMile) Color(0xFFFFD54F) else Color(0xFF4CAF50),
                trackColor = MaterialTheme.colors.onBackground.copy(alpha = 0.12f),
                strokeWidth = 5.dp
            )

            ScalingLazyColumn(
                state = columnState.listState,
                contentPadding = columnState.contentPadding,
                autoCentering = columnState.autoCentering,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Live Animated Buddy Pokémon with pace/HR cadence
                item {
                    LiveBuddyCoachingSection(
                        buddy = activeBuddy,
                        runStatus = state.status,
                        heartRateBpm = state.heartRateBpm
                    )
                }

                // Distance Readout
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "%.2f / 1.00 mi".format(state.distanceMiles),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = if (state.hasCompletedOneMile) Color(0xFFFFD54F) else Color.White
                        )

                        // Timer and Heart Rate
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val minutes = state.elapsedSeconds / 60
                            val seconds = state.elapsedSeconds % 60
                            Text(
                                text = "%02d:%02d".format(minutes, seconds),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colors.onSurface
                            )

                            if (state.heartRateBpm > 0) {
                                Text(
                                    text = "❤️ ${state.heartRateBpm}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFF5252)
                                )
                            }
                        }

                        // Current Pace
                        Text(
                            text = "Pace: ${state.currentPaceMinPerMile}",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Dynamic Milestone Banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (state.hasCompletedOneMile) Color(0x33FFD54F) else Color(0x334CAF50))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.hasCompletedOneMile) {
                                "🎉 1.0-Mile Complete!\n#077 Ponyta Unlocked!"
                            } else {
                                "%.2f mi left to unlock #077 Ponyta!".format(state.remainingMilesToPonyta)
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.hasCompletedOneMile) Color(0xFFFFD54F) else Color(0xFF81C784),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Primary Run Controls
                item {
                    when (state.status) {
                        RunStatus.PREPARING -> {
                            CompactChip(
                                onClick = { viewModel.startRun() },
                                label = { Text("Start 1-Mi Run", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = ChipDefaults.primaryChipColors(backgroundColor = Color(0xFF2E7D32)),
                                modifier = Modifier
                                    .fillMaxWidth(0.82f)
                                    .height(32.dp)
                            )
                        }
                        RunStatus.ACTIVE -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth(0.82f)
                            ) {
                                CompactChip(
                                    onClick = { viewModel.pauseRun() },
                                    label = { Text("Pause", fontSize = 10.sp) },
                                    colors = ChipDefaults.secondaryChipColors(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                )
                                CompactChip(
                                    onClick = { viewModel.finishRun() },
                                    label = { Text("Finish", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    colors = ChipDefaults.primaryChipColors(backgroundColor = Color(0xFFD32F2F)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                )
                            }
                        }
                        RunStatus.PAUSED -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth(0.82f)
                            ) {
                                CompactChip(
                                    onClick = { viewModel.resumeRun() },
                                    label = { Text("Resume", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    colors = ChipDefaults.primaryChipColors(backgroundColor = Color(0xFF2E7D32)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                )
                                CompactChip(
                                    onClick = { viewModel.finishRun() },
                                    label = { Text("Finish", fontSize = 10.sp) },
                                    colors = ChipDefaults.primaryChipColors(backgroundColor = Color(0xFFD32F2F)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                )
                            }
                        }
                        RunStatus.COMPLETED -> {
                            CompactChip(
                                onClick = onNavigateBack,
                                label = { Text("Back to Dashboard", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                colors = ChipDefaults.primaryChipColors(backgroundColor = Color(0xFF388E3C)),
                                modifier = Modifier
                                    .fillMaxWidth(0.82f)
                                    .height(32.dp)
                            )
                        }
                    }
                }

                // Debug Simulator Button (BuildConfig.DEBUG only)
                if (BuildConfig.DEBUG && (state.status != RunStatus.COMPLETED)) {
                    item {
                        CompactChip(
                            onClick = { viewModel.simulateProgress(0.25) },
                            label = {
                                Text(
                                    text = "⚡ Simulate +0.25 mi",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            colors = ChipDefaults.secondaryChipColors(
                                backgroundColor = Color(0xFF263238),
                                contentColor = Color(0xFF80D8FF)
                            ),
                            modifier = Modifier
                                .fillMaxWidth(0.80f)
                                .height(26.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveBuddyCoachingSection(
    buddy: Pokemon?,
    runStatus: RunStatus,
    heartRateBpm: Int,
    modifier: Modifier = Modifier
) {
    // Dynamic cadence duration (animation frame rate) based on runner's pace and HR zone
    val cadenceDurationMs = when {
        runStatus != RunStatus.ACTIVE -> 1000 // Resting idle bobbing
        heartRateBpm > 160 -> 240 // Fast sprint cadence
        heartRateBpm > 140 -> 340 // High tempo cardio cadence
        heartRateBpm > 115 -> 460 // Steady aerobic jog cadence
        else -> 600 // Warmup cadence
    }

    val hrZone = when {
        heartRateBpm <= 0 -> null
        heartRateBpm < 115 -> "Warmup Zone" to Color(0xFF42A5F5)
        heartRateBpm in 115..140 -> "Aerobic Zone" to Color(0xFF4CAF50)
        heartRateBpm in 141..160 -> "Cardio Zone" to Color(0xFFFFB300)
        else -> "Peak Zone" to Color(0xFFFF5252)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "BuddyRunCadence")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (runStatus == RunStatus.ACTIVE) -8f else -2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = cadenceDurationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Bounce"
    )
    val scaleFactor by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (runStatus == RunStatus.ACTIVE) 1.06f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = cadenceDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val spritePath = buddy?.spriteAssetPath ?: "sprites/pokemon_077.png"
        val context = LocalContext.current

        Box(
            modifier = Modifier
                .size(46.dp)
                .offset(y = bounceOffset.dp)
                .scale(scaleFactor),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data("file:///android_asset/$spritePath")
                    .crossfade(true)
                    .build(),
                contentDescription = buddy?.name ?: "Running Buddy",
                modifier = Modifier.size(44.dp)
            )
        }

        if (hrZone != null && runStatus == RunStatus.ACTIVE) {
            Text(
                text = hrZone.first,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = hrZone.second
            )
        }
    }
}
