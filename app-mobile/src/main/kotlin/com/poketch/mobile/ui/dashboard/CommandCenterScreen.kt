package com.poketch.mobile.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.poketch.core.domain.calculator.CalorieBudgetState
import com.poketch.core.domain.model.Pokemon

@Composable
fun CommandCenterScreen(
    viewModel: CommandCenterViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Command Center",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // 1. Dynamic Red Over-Calorie Warning Card (Pinned to Top)
        val budgetState = uiState.budgetState
        AnimatedVisibility(
            visible = budgetState is CalorieBudgetState.OverTargetWarning,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            if (budgetState is CalorieBudgetState.OverTargetWarning) {
                OverCalorieWarningCard(warning = budgetState)
            }
        }

        // 2. Daily Calorie Ring Gauge Card
        CalorieRingGaugeCard(
            consumedKcal = uiState.consumedKcal,
            targetCeilingKcal = uiState.targetCeilingKcal,
            tdeeKcal = uiState.tdeeKcal,
            budgetState = budgetState
        )

        // 3. Active Buddy Pokémon Card
        ActiveBuddyCard(
            buddy = uiState.activeBuddy,
            onQuickAddWater = { viewModel.quickAddWater(250.0) },
            onQuickLogRun = { viewModel.quickLogRun(1.0) }
        )

        // 4. Daily Habit Progress Section
        DailyHabitsCard(
            todayWaterMl = uiState.todayWaterMl,
            dailyWaterGoalMl = uiState.dailyWaterGoalMl,
            todaySteps = uiState.todaySteps,
            latestRunMiles = uiState.latestRunMiles
        )
    }
}

@Composable
private fun OverCalorieWarningCard(
    warning: CalorieBudgetState.OverTargetWarning,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFB71C1C), // High-contrast Crimson
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OVER CALORIE TARGET!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "+${warning.surplusKcal} kcal above target ceiling",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Suggested burn offsets:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Walk ${warning.suggestedOffsetSteps} steps (~${warning.suggestedOffsetSteps / 1000}k)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "• Or jog ${warning.suggestedOffsetRunMiles} miles",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CalorieRingGaugeCard(
    consumedKcal: Int,
    targetCeilingKcal: Int,
    tdeeKcal: Int,
    budgetState: CalorieBudgetState,
    modifier: Modifier = Modifier
) {
    val ringColor = when (budgetState) {
        is CalorieBudgetState.OnTrack -> Color(0xFF2E7D32) // Forest Green
        is CalorieBudgetState.ApproachingCeiling -> Color(0xFFF57F17) // Amber
        is CalorieBudgetState.OverTargetWarning -> Color(0xFFB71C1C) // Crimson Red
    }

    val stateText = when (budgetState) {
        is CalorieBudgetState.OnTrack -> "On Track (In Deficit)"
        is CalorieBudgetState.ApproachingCeiling -> "Approaching Target Ceiling"
        is CalorieBudgetState.OverTargetWarning -> "Over Budget Warning"
    }

    val remainingKcal = when (budgetState) {
        is CalorieBudgetState.OnTrack -> budgetState.remainingKcal
        is CalorieBudgetState.ApproachingCeiling -> budgetState.remainingKcal
        is CalorieBudgetState.OverTargetWarning -> -budgetState.surplusKcal
    }

    val progressRatio = (consumedKcal.toFloat() / targetCeilingKcal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Daily Calorie Budget",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Circular Gauge
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceDim,
                        strokeWidth = 10.dp,
                        trackColor = Color.Transparent
                    )
                    CircularProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier.fillMaxSize(),
                        color = ringColor,
                        strokeWidth = 10.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$consumedKcal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "kcal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Numerical breakdown
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stateText,
                        style = MaterialTheme.typography.labelLarge,
                        color = ringColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Target Ceiling: $targetCeilingKcal kcal",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "TDEE: $tdeeKcal kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (remainingKcal >= 0) "$remainingKcal kcal remaining" else "${-remainingKcal} kcal overage",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (remainingKcal >= 0) ringColor else Color(0xFFB71C1C)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveBuddyCard(
    buddy: Pokemon?,
    onQuickAddWater: () -> Unit,
    onQuickLogRun: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Active Watch Buddy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val context = LocalContext.current
                val imageModel = buddy?.let {
                    ImageRequest.Builder(context)
                        .data("file:///android_asset/${it.spriteAssetPath}")
                        .crossfade(true)
                        .build()
                }

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageModel != null) {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = buddy.name,
                            modifier = Modifier.size(64.dp)
                        )
                    } else {
                        Text(
                            text = "#${buddy?.pokedexNumber ?: 1}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = buddy?.name ?: "Bulbasaur",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "#${buddy?.pokedexNumber?.toString()?.padStart(3, '0') ?: "001"} • ${buddy?.primaryType ?: "Grass"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Cheering you on toward your goals!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick-Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onQuickAddWater,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+250 ml Water")
                }
                OutlinedButton(
                    onClick = onQuickLogRun,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Log 1-Mi Run")
                }
            }
        }
    }
}

@Composable
private fun DailyHabitsCard(
    todayWaterMl: Int,
    dailyWaterGoalMl: Int,
    todaySteps: Int,
    latestRunMiles: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Today's Habit Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Hydration
            val waterProgress = (todayWaterMl.toFloat() / dailyWaterGoalMl.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
            Text(
                text = "Hydration: $todayWaterMl / $dailyWaterGoalMl ml (${(waterProgress * 100).toInt()}%)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { waterProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF1976D2),
                trackColor = MaterialTheme.colorScheme.surfaceDim
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Steps & Run
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Daily Steps",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$todaySteps steps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Latest Run",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$latestRunMiles mi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (latestRunMiles >= 1.0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
