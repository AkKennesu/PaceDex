package com.poketch.mobile.ui.dashboard

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.poketch.core.domain.calculator.CalorieBudgetState
import com.poketch.core.domain.model.DeficitTier
import com.poketch.core.domain.model.Pokemon
import com.poketch.mobile.ui.theme.PoketchAmber
import com.poketch.mobile.ui.theme.PoketchBorder
import com.poketch.mobile.ui.theme.PoketchBorderSubtle
import com.poketch.mobile.ui.theme.PoketchCrimson
import com.poketch.mobile.ui.theme.PoketchCyan
import com.poketch.mobile.ui.theme.PoketchEmerald
import com.poketch.mobile.ui.theme.PoketchSurface
import com.poketch.mobile.ui.theme.PoketchSurfaceElevated

@Composable
fun CommandCenterScreen(
    viewModel: CommandCenterViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.syncMessage) {
        uiState.syncMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Professional App Bar Header
        DashboardHeader(
            isWatchConnected = uiState.isWatchConnected,
            connectedWatchName = uiState.connectedWatchName
        )

        // Trainer Field Guide (Collapsible onboarding tips)
        TrainerFieldGuideCard()

        // 1. Dynamic Red Over-Calorie Warning Card (Pinned to Top if active)
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

        // 2. High-Tech Daily Calorie Ring Gauge Card
        CalorieRingGaugeCard(
            consumedKcal = uiState.consumedKcal,
            targetCeilingKcal = uiState.targetCeilingKcal,
            tdeeKcal = uiState.tdeeKcal,
            budgetState = budgetState
        )

        // 3. Pokétch Watch Remote Companion & Quick Controls
        PoketchWatchCompanionCard(
            isWatchConnected = uiState.isWatchConnected,
            connectedWatchName = uiState.connectedWatchName,
            isSyncingWatch = uiState.isSyncingWatch,
            currentDeficitTier = uiState.currentDeficitTier,
            activeBuddyId = uiState.activeBuddy?.pokedexNumber,
            unlockedBuddies = uiState.unlockedBuddies,
            onQuickAddCalorie = { kcal, label -> viewModel.quickAddCalorie(kcal, label) },
            onQuickAddWater = { amount -> viewModel.quickAddWater(amount) },
            onSelectDeficitTier = { tier -> viewModel.updateDeficitTier(tier) },
            onSelectBuddy = { buddyId -> viewModel.setActiveBuddy(buddyId) },
            onSyncWatch = { viewModel.syncWithWatchNow() }
        )

        // 4. Snorlax Recovery Tracker Card
        SnorlaxRecoveryBadgeCard(
            recovery = uiState.snorlaxRecovery
        )

        // 5. Active Buddy Pokémon Card
        ActiveBuddyCard(
            buddy = uiState.activeBuddy,
            onQuickAddWater = { viewModel.quickAddWater(250.0) },
            onQuickLogRun = { viewModel.quickLogRun(1.0) }
        )

        // 6. Daily Habit Progress Section
        DailyHabitsCard(
            todayWaterMl = uiState.todayWaterMl,
            dailyWaterGoalMl = uiState.dailyWaterGoalMl,
            todaySteps = uiState.todaySteps,
            latestRunMiles = uiState.latestRunMiles
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DashboardHeader(
    isWatchConnected: Boolean,
    connectedWatchName: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(PoketchCrimson)
                )
                Text(
                    text = "POKÉTCH COMPANION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    color = PoketchCrimson
                )
            }
            Text(
                text = "Command Center",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
        }

        // Live watch connectivity status pill
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isWatchConnected) PoketchEmerald.copy(alpha = 0.15f) else Color(0xFF1E293B),
            border = BorderStroke(
                1.dp,
                if (isWatchConnected) PoketchEmerald.copy(alpha = 0.4f) else PoketchBorder
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isWatchConnected) PoketchEmerald else Color(0xFF94A3B8))
                )
                Text(
                    text = if (isWatchConnected) (connectedWatchName ?: "Watch Linked") else "Standalone",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isWatchConnected) PoketchEmerald else Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
private fun TrainerFieldGuideCard(
    modifier: Modifier = Modifier
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A)
        ),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PoketchCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎓", fontSize = 18.sp)
                    }
                    Column {
                        Text(
                            text = "Trainer Field Guide",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC)
                        )
                        Text(
                            text = if (isExpanded) "Tap to collapse briefing" else "New to Pokétch? Tap to learn mechanics",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1E293B)
                ) {
                    Text(
                        text = if (isExpanded) "▲" else "▼",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PoketchCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GuideTipRow(
                        emoji = "🎯",
                        title = "Target Ceiling (Daily Budget)",
                        desc = "Eat below this threshold to burn fat in deficit. Hitting your ceiling consistently unlocks Pokémon."
                    )
                    GuideTipRow(
                        emoji = "🔥",
                        title = "TDEE (Daily Calorie Burn)",
                        desc = "Your Total Daily Energy Expenditure: calories burned every 24 hours through metabolic rest and activity."
                    )
                    GuideTipRow(
                        emoji = "⭐",
                        title = "Unlocking Pokémon",
                        desc = "Stay in calorie deficit, hit your hydration targets, and log steps to discover and evolve new Pokémon!"
                    )
                    GuideTipRow(
                        emoji = "⌚",
                        title = "Watch Remote Hub",
                        desc = "Quick-add snacks, log water, or switch your active watch face buddy directly from your phone in 1 tap."
                    )
                    GuideTipRow(
                        emoji = "😴",
                        title = "Snorlax Recovery (#143)",
                        desc = "If you exceed your calorie budget, Snorlax falls asleep. Maintain 3 on-target days in a row to wake him up!"
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideTipRow(emoji: String, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.6f))
            .border(1.dp, Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = emoji, fontSize = 18.sp)
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )
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
        is CalorieBudgetState.OnTrack -> PoketchEmerald
        is CalorieBudgetState.ApproachingCeiling -> PoketchAmber
        is CalorieBudgetState.OverTargetWarning -> PoketchCrimson
    }

    val stateBadgeText = when (budgetState) {
        is CalorieBudgetState.OnTrack -> "IN DEFICIT"
        is CalorieBudgetState.ApproachingCeiling -> "NEAR CEILING"
        is CalorieBudgetState.OverTargetWarning -> "OVER BUDGET"
    }

    val remainingKcal = when (budgetState) {
        is CalorieBudgetState.OnTrack -> budgetState.remainingKcal
        is CalorieBudgetState.ApproachingCeiling -> budgetState.remainingKcal
        is CalorieBudgetState.OverTargetWarning -> -budgetState.surplusKcal
    }

    val progressRatio = (consumedKcal.toFloat() / targetCeilingKcal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val percentageInt = ((consumedKcal.toFloat() / targetCeilingKcal.coerceAtLeast(1)) * 100).toInt()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PoketchSurface),
        border = BorderStroke(1.dp, PoketchBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row with State Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DAILY CALORIE BUDGET",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Energy Balance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ringColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, ringColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = stateBadgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = ringColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Gauge + Numerical Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Circular Telemetry Gauge
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(130.dp)) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF1E293B),
                        strokeWidth = 12.dp,
                        trackColor = Color.Transparent
                    )
                    CircularProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier.fillMaxSize(),
                        color = ringColor,
                        strokeWidth = 12.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$consumedKcal",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "kcal eaten",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "$percentageInt%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ringColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Stats Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricRow(
                        label = "Target Ceiling",
                        value = "$targetCeilingKcal kcal",
                        valueColor = Color.White
                    )
                    MetricRow(
                        label = "Daily Burn (TDEE)",
                        value = "$tdeeKcal kcal",
                        valueColor = Color(0xFF94A3B8)
                    )
                    HorizontalDivider(color = PoketchBorderSubtle)
                    MetricRow(
                        label = if (remainingKcal >= 0) "Remaining Budget" else "Over Target By",
                        value = if (remainingKcal >= 0) "$remainingKcal kcal" else "${-remainingKcal} kcal",
                        valueColor = if (remainingKcal >= 0) PoketchEmerald else PoketchCrimson,
                        isBold = true
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    valueColor: Color,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8)
        )
        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun PoketchWatchCompanionCard(
    isWatchConnected: Boolean,
    connectedWatchName: String?,
    isSyncingWatch: Boolean,
    currentDeficitTier: DeficitTier,
    activeBuddyId: Int?,
    unlockedBuddies: List<Pokemon>,
    onQuickAddCalorie: (Int, String) -> Unit,
    onQuickAddWater: (Double) -> Unit,
    onSelectDeficitTier: (DeficitTier) -> Unit,
    onSelectBuddy: (Int) -> Unit,
    onSyncWatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PoketchSurface),
        border = BorderStroke(1.dp, PoketchBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Watch Connection & Sync Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PoketchCyan.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "⌚", fontSize = 18.sp)
                    }
                    Column {
                        Text(
                            text = "WATCH REMOTE HUB",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = PoketchCyan,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isWatchConnected) (connectedWatchName ?: "Linked to Watch") else "Standalone Mode (Offline)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Button(
                    onClick = onSyncWatch,
                    enabled = !isSyncingWatch,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    if (isSyncingWatch) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Syncing", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Quick Calorie Buttons
            Text(
                text = "QUICK CALORIE LOGGING (SYNC TO WATCH)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionChip(
                    text = "+100 kcal",
                    subtext = "Snack",
                    modifier = Modifier.weight(1f),
                    onClick = { onQuickAddCalorie(100, "Snack") }
                )
                QuickActionChip(
                    text = "+250 kcal",
                    subtext = "Light Meal",
                    modifier = Modifier.weight(1f),
                    onClick = { onQuickAddCalorie(250, "Light Meal") }
                )
                QuickActionChip(
                    text = "+500 kcal",
                    subtext = "Full Meal",
                    modifier = Modifier.weight(1f),
                    onClick = { onQuickAddCalorie(500, "Meal") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Quick Hydration Buttons
            Text(
                text = "QUICK HYDRATION LOGGING",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onQuickAddWater(250.0) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("💧 +250 ml", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onQuickAddWater(500.0) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("💧 +500 ml", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Watch Deficit Tier Selector
            Text(
                text = "DEFICIT PACE (DAILY TARGET REDUCTION)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, PoketchBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tiers = listOf(
                    DeficitTier.MILD to "Mild (-250)",
                    DeficitTier.STANDARD to "Std (-500)",
                    DeficitTier.AGGRESSIVE to "Aggr (-750)"
                )
                tiers.forEach { (tier, label) ->
                    val isSelected = currentDeficitTier == tier
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PoketchCrimson else Color.Transparent)
                            .clickable { onSelectDeficitTier(tier) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            if (unlockedBuddies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "SWITCH ACTIVE WATCHFACE BUDDY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val context = LocalContext.current
                    unlockedBuddies.forEach { poke ->
                        val isCurrent = poke.pokedexNumber == activeBuddyId
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCurrent) PoketchSurfaceElevated else Color(0xFF0F172A))
                                .border(
                                    1.dp,
                                    if (isCurrent) PoketchCrimson else PoketchBorderSubtle,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectBuddy(poke.pokedexNumber) }
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (isCurrent) PoketchCrimson.copy(alpha = 0.2f) else Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                val img = ImageRequest.Builder(context)
                                    .data("file:///android_asset/${poke.spriteAssetPath}")
                                    .crossfade(true)
                                    .build()
                                AsyncImage(
                                    model = img,
                                    contentDescription = poke.name,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = poke.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionChip(
    text: String,
    subtext: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, PoketchBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun SnorlaxRecoveryBadgeCard(
    recovery: SnorlaxRecoveryState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PoketchSurface),
        border = BorderStroke(
            1.dp,
            when {
                recovery.isUnlocked -> PoketchEmerald.copy(alpha = 0.5f)
                recovery.isCurrentlyInSurplus -> PoketchCrimson.copy(alpha = 0.5f)
                recovery.recoveryDays > 0 -> PoketchAmber.copy(alpha = 0.5f)
                else -> PoketchBorder
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("file:///android_asset/sprites/pokemon_143.png")
                        .crossfade(true)
                        .build(),
                    contentDescription = "Snorlax #143",
                    modifier = Modifier.size(50.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Snorlax Recovery (#143)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when {
                            recovery.isUnlocked -> PoketchEmerald.copy(alpha = 0.2f)
                            recovery.isCurrentlyInSurplus -> PoketchCrimson.copy(alpha = 0.2f)
                            recovery.recoveryDays > 0 -> PoketchAmber.copy(alpha = 0.2f)
                            else -> Color(0xFF1E293B)
                        }
                    ) {
                        Text(
                            text = when {
                                recovery.isUnlocked -> "✓ AWAKENED"
                                recovery.isCurrentlyInSurplus -> "SURPLUS"
                                recovery.recoveryDays > 0 -> "DAY ${recovery.recoveryDays}/3"
                                else -> "SLEEPING"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                recovery.isUnlocked -> PoketchEmerald
                                recovery.isCurrentlyInSurplus -> PoketchCrimson
                                recovery.recoveryDays > 0 -> PoketchAmber
                                else -> Color(0xFF94A3B8)
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = when {
                        recovery.isUnlocked -> "Conquered surplus days and awakened Snorlax in your Pokédex!"
                        recovery.isCurrentlyInSurplus -> "+${recovery.surplusAmountKcal} kcal over ceiling. Stay on track tomorrow to begin awakening!"
                        recovery.recoveryDays > 0 -> "Stay at or below ceiling for ${3 - recovery.recoveryDays} more day(s) to awaken Snorlax!"
                        else -> "Route 12 slumber. Exceeding calories puts Snorlax to sleep; 3 on-target days wake him up."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )

                if (!recovery.isUnlocked) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { recovery.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = when {
                            recovery.isCurrentlyInSurplus -> PoketchCrimson
                            recovery.recoveryDays > 0 -> PoketchAmber
                            else -> Color(0xFF475569)
                        },
                        trackColor = Color(0xFF1E293B),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
internal fun OverCalorieWarningCard(
    warning: CalorieBudgetState.OverTargetWarning,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF7F1D1D)
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFFCA5A5),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OVER CALORIE TARGET!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "+${warning.surplusKcal} kcal above target ceiling",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFEE2E2)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = Color.Black.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Suggested burn offsets:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFCA5A5)
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
private fun ActiveBuddyCard(
    buddy: Pokemon?,
    onQuickAddWater: () -> Unit,
    onQuickLogRun: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PoketchSurface),
        border = BorderStroke(1.dp, PoketchBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE WATCH BUDDY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = PoketchCyan
                )
                buddy?.primaryType?.let { type ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PoketchCyan.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = type.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PoketchCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

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
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF334155), Color(0xFF0F172A))
                            )
                        )
                        .border(2.dp, PoketchBorder, CircleShape),
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
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = buddy?.name ?: "Bulbasaur",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "#${buddy?.pokedexNumber?.toString()?.padStart(3, '0') ?: "001"} • Companion in Training",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "Cheering you on toward your calorie targets!",
                        style = MaterialTheme.typography.bodySmall,
                        color = PoketchEmerald
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onQuickAddWater,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("💧 +250 ml Water", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onQuickLogRun,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PoketchBorder)
                ) {
                    Text("👟 Log 1-Mi Run", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PoketchSurface),
        border = BorderStroke(1.dp, PoketchBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "TODAY'S HABIT PROGRESS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Hydration
            val waterProgress = (todayWaterMl.toFloat() / dailyWaterGoalMl.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💧 Hydration",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = "$todayWaterMl / $dailyWaterGoalMl ml (${(waterProgress * 100).toInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { waterProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF2563EB),
                trackColor = Color(0xFF1E293B)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Steps & Run Side-by-Side Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HabitMetricTile(
                    emoji = "👟",
                    title = "Daily Steps",
                    value = "$todaySteps steps",
                    modifier = Modifier.weight(1f)
                )
                HabitMetricTile(
                    emoji = "🏃",
                    title = "Latest Run",
                    value = "$latestRunMiles mi",
                    modifier = Modifier.weight(1f),
                    accentColor = if (latestRunMiles >= 1.0) PoketchEmerald else null
                )
            }
        }
    }
}

@Composable
private fun HabitMetricTile(
    emoji: String,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color? = null
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, PoketchBorderSubtle),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor ?: Color.White
            )
        }
    }
}
