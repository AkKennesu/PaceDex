package com.poketch.wear.ui.habits

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.poketch.core.domain.engine.UnlockEvent
import com.poketch.wear.ui.layout.rememberResponsiveColumnState

@Composable
fun QuickHabitActionsScreen(
    onStartRun: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuickHabitActionsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val columnState = rememberResponsiveColumnState()
    val haptic = LocalHapticFeedback.current

    var celebrationEvent by remember { mutableStateOf<UnlockEvent.NewPokemonUnlocked?>(null) }

    // Listen for new Pokemon unlock celebrations on wrist
    LaunchedEffect(Unit) {
        viewModel.unlockEvents.collect { event ->
            celebrationEvent = event
            viewModel.playUnlockFanfare()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = columnState.listState) }
    ) {
        ScalingLazyColumn(
            state = columnState.listState,
            contentPadding = columnState.contentPadding,
            autoCentering = columnState.autoCentering,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                Text(
                    text = "Quick Habits",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onSurface
                )
            }

            // Hydration progress readout
            item {
                Text(
                    text = "💧 ${state.todayWaterMl} / ${state.dailyWaterGoalMl} ml",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF00E5FF)
                )
            }

            // Action 1: +250 ml Water (Glass)
            item {
                CompactChip(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.logWater(250.0)
                    },
                    label = {
                        Text(
                            text = "+250 ml Water (Glass)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF006064),
                        contentColor = Color(0xFFE0F7FA)
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .height(34.dp)
                )
            }

            // Action 2: +500 ml Water (Bottle)
            item {
                CompactChip(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.logWater(500.0)
                    },
                    label = {
                        Text(
                            text = "+500 ml Water (Bottle)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF00838F),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .height(34.dp)
                )
            }

            // Action 3: Start 1-Mile Run
            item {
                CompactChip(
                    onClick = onStartRun,
                    label = {
                        Text(
                            text = "Start 1-Mile Run",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = ChipDefaults.primaryChipColors(
                        backgroundColor = Color(0xFF2E7D32),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .height(34.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }

    // Celebratory wrist unlock / evolution overlay
    celebrationEvent?.let { event ->
        if (event.isEvolution && event.preEvolution != null) {
            WearEvolutionOverlay(
                event = event,
                onSetAsActiveBuddy = { buddyId ->
                    viewModel.setActiveBuddy(buddyId)
                    celebrationEvent = null
                },
                onDismiss = { celebrationEvent = null }
            )
        } else {
            WearCelebrationOverlay(
                event = event,
                onDismiss = { celebrationEvent = null }
            )
        }
    }
}

@Composable
private fun WearEvolutionOverlay(
    event: UnlockEvent.NewPokemonUnlocked,
    onSetAsActiveBuddy: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preEvolution = event.preEvolution
    val evolved = event.pokemon
    val context = LocalContext.current

    var stage by remember { mutableStateOf(1) }
    LaunchedEffect(Unit) {
        delay(1600)
        stage = 2
    }

    val infiniteTransition = rememberInfiniteTransition(label = "WearEvolutionSparkle")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xF0101828))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = if (stage == 1) "✨ EVOLVING... ✨" else "🎉 EVOLVED! 🎉",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = if (stage == 1) Color(0xFFFFB300) else Color(0xFF4CAF50),
                textAlign = TextAlign.Center
            )

            val currentPokemon = if (stage == 1 && preEvolution != null) preEvolution else evolved
            val spriteUri = "file:///android_asset/${currentPokemon.spriteAssetPath}"

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(spriteUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = currentPokemon.name,
                    filterQuality = FilterQuality.None,
                    modifier = Modifier
                        .size(44.dp)
                        .scale(if (stage == 1) pulseScale else 1f)
                )
            }

            Text(
                text = if (stage == 1) {
                    "${preEvolution?.name} is evolving!"
                } else {
                    "${evolved.name} (#%03d)".format(evolved.pokedexNumber)
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            if (stage == 2) {
                CompactChip(
                    onClick = { onSetAsActiveBuddy(evolved.pokedexNumber) },
                    label = {
                        Text(
                            text = "Set as Active Buddy",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = ChipDefaults.primaryChipColors(
                        backgroundColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(28.dp)
                )

                CompactChip(
                    onClick = onDismiss,
                    label = {
                        Text(
                            text = "Keep Current",
                            fontSize = 9.sp
                        )
                    },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(24.dp)
                )
            }
        }
    }
}

@Composable
private fun WearCelebrationOverlay(
    event: UnlockEvent.NewPokemonUnlocked,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pokemon = event.pokemon
    val context = LocalContext.current
    val imageUri = "file:///android_asset/${pokemon.spriteAssetPath}"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xE61B5E20))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "🎉 POKÉMON UNLOCKED!",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFFD54F),
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = pokemon.name,
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.size(42.dp)
                )
            }

            Text(
                text = "#%03d ${pokemon.name.uppercase()}".format(pokemon.pokedexNumber),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            CompactChip(
                onClick = onDismiss,
                label = {
                    Text(
                        text = "Awesome!",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = ChipDefaults.primaryChipColors(
                    backgroundColor = Color(0xFFD32F2F),
                    contentColor = Color.White
                ),
                modifier = Modifier.height(28.dp)
            )
        }
    }
}
