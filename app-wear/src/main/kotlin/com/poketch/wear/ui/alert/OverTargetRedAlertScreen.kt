package com.poketch.wear.ui.alert

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.poketch.wear.ui.layout.rememberResponsiveColumnState

@Composable
fun OverTargetRedAlertScreen(
    onStartRun: () -> Unit,
    onAcknowledge: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OverTargetRedAlertViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Trigger urgent double-pulse haptics on entrance
    LaunchedEffect(Unit) {
        viewModel.triggerAlertHaptic()
    }

    OverTargetRedAlertContent(
        state = state,
        onStartRun = onStartRun,
        onAcknowledge = onAcknowledge,
        modifier = modifier
    )
}

@Composable
fun OverTargetRedAlertContent(
    state: OverTargetRedAlertUiState,
    onStartRun: () -> Unit,
    onAcknowledge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val columnState = rememberResponsiveColumnState()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFB71C1C)),
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = columnState.listState) }
    ) {
        ScalingLazyColumn(
            state = columnState.listState,
            contentPadding = columnState.contentPadding,
            autoCentering = columnState.autoCentering,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Warning Icon & Badge
            item {
                Text(
                    text = "🚨 RED ALERT 🚨",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD54F),
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Distressed Buddy Sprite
            item {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF7F0000)),
                    contentAlignment = Alignment.Center
                ) {
                    val spritePath = state.activeBuddy?.spriteAssetPath ?: "sprites/pokemon/other/official-artwork/1.png"
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data("file:///android_asset/$spritePath")
                            .crossfade(true)
                            .build(),
                        contentDescription = "Distressed Buddy",
                        filterQuality = FilterQuality.None,
                        colorFilter = ColorFilter.tint(Color(0xFFFF8A80), BlendMode.Modulate),
                        modifier = Modifier.size(46.dp)
                    )

                    // Sweat drop indicator
                    Text(
                        text = "💦",
                        fontSize = 14.sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 2.dp)
                    )
                }
            }

            // Header: OVER TARGET BY +X KCAL
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "OVER TARGET BY +${state.surplusKcal} KCAL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${state.consumedKcal} / ${state.targetCalorieCeiling} kcal",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFE082),
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Action 1: Start 1-Mile Burn Run
            item {
                CompactChip(
                    onClick = onStartRun,
                    label = {
                        Text(
                            text = "Start 1-Mile Burn Run",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = ChipDefaults.primaryChipColors(
                        backgroundColor = Color(0xFFFFD54F),
                        contentColor = Color(0xFF212121)
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(32.dp)
                )
            }

            // Action 2: Acknowledge
            item {
                CompactChip(
                    onClick = onAcknowledge,
                    label = {
                        Text(
                            text = "Acknowledge",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = ChipDefaults.secondaryChipColors(
                        backgroundColor = Color(0x66000000),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(30.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
