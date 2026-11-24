package com.poketch.mobile.ui.dashboard

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.material3.OutlinedButton
import com.poketch.core.domain.calculator.CalorieBudgetState
import com.poketch.core.domain.model.DeficitTier
import com.poketch.core.domain.model.Pokemon
import com.poketch.core.domain.model.RecoveryQuest
import com.poketch.core.domain.model.WeatherBoost
import com.poketch.mobile.ui.theme.AnimatedStatNumber
import com.poketch.mobile.ui.theme.rememberAnimatedCounter
import com.poketch.mobile.ui.theme.StitchAlertCoral
import com.poketch.mobile.ui.theme.StitchAlertTint
import com.poketch.mobile.ui.theme.StitchBorderSubtle
import com.poketch.mobile.ui.theme.StitchCanvasBackground
import com.poketch.mobile.ui.theme.StitchDarkSlate
import com.poketch.mobile.ui.theme.StitchDarkSlateCard
import com.poketch.mobile.ui.theme.StitchHydration
import com.poketch.mobile.ui.theme.StitchHydrationTint
import com.poketch.mobile.ui.theme.StitchPartnerBorder
import com.poketch.mobile.ui.theme.StitchPartnerMint
import com.poketch.mobile.ui.theme.StitchPrimary
import com.poketch.mobile.ui.theme.StitchPrimaryTint
import com.poketch.mobile.ui.theme.StitchStepsAmber
import com.poketch.mobile.ui.theme.StitchStepsTint
import com.poketch.mobile.ui.theme.bouncyPress
import com.poketch.mobile.ui.theme.rememberAnimatedProgress
import com.poketch.mobile.ui.theme.rememberBuddyMotion
import androidx.compose.ui.graphics.graphicsLayer
import com.poketch.mobile.ui.theme.StitchSurfaceCard
import com.poketch.mobile.ui.theme.StitchTextMuted
import com.poketch.mobile.ui.theme.StitchTextPrimary
import com.poketch.mobile.ui.theme.StitchTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Google Stitch Bento "Today" Home Screen
 * Matches Stitch DESIGN.md: Airy botanical canvas, pure white rounded Bento cards,
 * spring-physics animations, floating partner mascot pedestal, and interactive habits.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandCenterScreen(
    viewModel: CommandCenterViewModel,
    modifier: Modifier = Modifier,
    onOpenProfile: () -> Unit = {},
    onOpenLogMeal: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showQuickAddDialog by rememberSaveable { mutableStateOf(false) }
    var showBuddyPickerSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.syncMessage) {
        uiState.syncMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.recoveryQuestClearedEvents.collect { message ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StitchCanvasBackground)
    ) {
        // Subtle emerald top ambient glow wash from Google Stitch
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            StitchPrimary.copy(alpha = 0.08f),
                            StitchPrimaryTint.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top App Bar & Live Connectivity Pill
            StitchTopBar(
                isWatchConnected = uiState.isWatchConnected,
                connectedWatchName = uiState.connectedWatchName,
                onProfileClick = onOpenProfile
            )

            // 2. Today Header Section
            StitchTodayHeader(
                isWatchConnected = uiState.isWatchConnected
            )

            // 3. Dynamic Red Over-Calorie Warning Card (Pinned when surplus and quest not completed)
            val budgetState = uiState.budgetState
            val isRecoveryCompleted = uiState.recoveryQuest?.isCompleted == true
            val isOverTargetActive = budgetState is CalorieBudgetState.OverTargetWarning && !isRecoveryCompleted
            AnimatedVisibility(
                visible = isOverTargetActive,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                if (budgetState is CalorieBudgetState.OverTargetWarning) {
                    OverCalorieWarningCard(
                        warning = budgetState,
                        recoveryQuest = uiState.recoveryQuest
                    )
                }
            }

            // 4. Hero Calorie Budget Bento Card with Animated Sweep Ring
            StitchCalorieBudgetCard(
                consumedKcal = uiState.consumedKcal,
                targetCeilingKcal = uiState.targetCeilingKcal,
                tdeeKcal = uiState.tdeeKcal,
                budgetState = budgetState,
                isRecoveryCompleted = isRecoveryCompleted
            )

            // 5. Active Partner Bento Card (Soft Mint with Floating 3D Mascot)
            StitchActivePartnerCard(
                partner = uiState.activeBuddy,
                weatherBoost = uiState.weatherBoost,
                onSelectBuddyClick = { showBuddyPickerSheet = true }
            )

            // 6. Two-Column Bento Habit Cards (Water & Steps)
            StitchTwoColumnHabitCards(
                todayWaterMl = uiState.todayWaterMl,
                dailyWaterGoalMl = uiState.dailyWaterGoalMl,
                todaySteps = uiState.todaySteps,
                onAddWater = { viewModel.quickAddWater(250.0) },
                onStartRun = { viewModel.quickLogRun(1.0) }
            )

            // 7. Full-Width Primary CTA: + Log Meal or Quick Calories
            StitchPrimaryActionCTA(
                onClick = { showQuickAddDialog = true }
            )

            // 8. Snorlax Recovery Bento Badge Card
            StitchSnorlaxRecoveryCard(
                recovery = uiState.snorlaxRecovery
            )

            // 9. Trainer Field Guide (Collapsible Tips)
            StitchTrainerFieldGuideCard()

            Spacer(modifier = Modifier.height(28.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    // Quick Add Calorie Dialog
    if (showQuickAddDialog) {
        QuickAddCalorieModal(
            onDismiss = { showQuickAddDialog = false },
            onQuickAdd = { kcal, label ->
                viewModel.quickAddCalorie(kcal, label)
                showQuickAddDialog = false
            },
            onGoToFullLog = {
                showQuickAddDialog = false
                onOpenLogMeal()
            }
        )
    }

    // Buddy Picker Modal Bottom Sheet
    if (showBuddyPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBuddyPickerSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = StitchSurfaceCard,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            BuddyPickerSheetContent(
                activeBuddy = uiState.activeBuddy,
                unlockedBuddies = uiState.unlockedBuddies,
                onSelectBuddy = { id ->
                    viewModel.setActiveBuddy(id)
                    showBuddyPickerSheet = false
                }
            )
        }
    }
}

/**
 * Top Navigation Bar with Pokétch Brand and Watch Connection Pill
 */
@Composable
private fun StitchTopBar(
    isWatchConnected: Boolean,
    connectedWatchName: String?,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(StitchPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "◓",
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
            Column {
                Text(
                    text = "Pokétch",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StitchDarkSlate,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "Companion Hub",
                    style = MaterialTheme.typography.labelSmall,
                    color = StitchTextMuted
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Profile Icon Button
            Surface(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable { onProfileClick() },
                shape = CircleShape,
                color = StitchSurfaceCard,
                border = BorderStroke(1.dp, StitchBorderSubtle),
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "👤", fontSize = 16.sp)
                }
            }
        }
    }
}

/**
 * Stitch "Today" Header Section with Current Date and Watch Status Pill
 */
@Composable
private fun StitchTodayHeader(
    isWatchConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val dateDisplay = remember {
        SimpleDateFormat("EEE, MMM d", Locale.US).format(Date())
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(
                text = "Today",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = StitchDarkSlate,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = dateDisplay,
                style = MaterialTheme.typography.bodyMedium,
                color = StitchTextMuted
            )
        }

        // Pill: "● Watch 92%"
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = StitchSurfaceCard,
            border = BorderStroke(1.dp, StitchBorderSubtle),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isWatchConnected) StitchPrimary else StitchStepsAmber)
                )
                Text(
                    text = if (isWatchConnected) "Watch 92%" else "Standalone",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchTextSecondary
                )
            }
        }
    }
}

/**
 * Hero Bento Card: Calorie Budget with Animated Circular Ring Gauge
 */
@Composable
private fun StitchCalorieBudgetCard(
    consumedKcal: Int,
    targetCeilingKcal: Int,
    tdeeKcal: Int,
    budgetState: CalorieBudgetState,
    modifier: Modifier = Modifier,
    isRecoveryCompleted: Boolean = false
) {
    val isOnTrack = budgetState is CalorieBudgetState.OnTrack || isRecoveryCompleted
    val isOver = budgetState is CalorieBudgetState.OverTargetWarning && !isRecoveryCompleted

    val statusBadgeText = when {
        isRecoveryCompleted -> "Deficit Restored"
        budgetState is CalorieBudgetState.OnTrack -> "On Track"
        budgetState is CalorieBudgetState.ApproachingCeiling -> "Near Ceiling"
        else -> "Over Target"
    }

    val targetBadgeColor = when {
        isOver -> StitchAlertCoral
        isOnTrack -> StitchPrimary
        else -> StitchStepsAmber
    }

    val targetBadgeBg = when {
        isOver -> StitchAlertTint
        isOnTrack -> StitchPrimaryTint
        else -> StitchStepsTint
    }

    val statusBadgeColor by animateColorAsState(
        targetValue = targetBadgeColor,
        animationSpec = tween(400),
        label = "status_badge_color"
    )

    val statusBadgeBg by animateColorAsState(
        targetValue = targetBadgeBg,
        animationSpec = tween(400),
        label = "status_badge_bg"
    )

    val remainingKcal = when (budgetState) {
        is CalorieBudgetState.OnTrack -> budgetState.remainingKcal
        is CalorieBudgetState.ApproachingCeiling -> budgetState.remainingKcal
        is CalorieBudgetState.OverTargetWarning -> -budgetState.surplusKcal
    }

    val targetRatio = (consumedKcal.toFloat() / targetCeilingKcal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val animatedProgress by rememberAnimatedProgress(targetRatio)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = StitchSurfaceCard,
        border = BorderStroke(1.dp, StitchBorderSubtle),
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Card Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(StitchPrimaryTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Calories",
                            tint = StitchPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Calorie Budget",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StitchDarkSlate
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusBadgeBg
                ) {
                    Text(
                        text = statusBadgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusBadgeColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Ring Gauge + Right Metrics Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Smooth Custom Circular Progress Ring
                Box(
                    modifier = Modifier.size(126.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val trackColor = StitchBorderSubtle
                    val ringColor = statusBadgeColor

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 10.dp.toPx()
                        drawCircle(
                            color = trackColor,
                            style = Stroke(width = strokeWidth)
                        )
                        drawArc(
                            color = ringColor,
                            startAngle = -90f,
                            sweepAngle = animatedProgress * 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedStatNumber(
                            targetValue = if (remainingKcal >= 0) remainingKcal else -remainingKcal,
                            prefix = if (remainingKcal < 0) "+" else "",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = StitchDarkSlate
                        )
                        Text(
                            text = if (remainingKcal >= 0) "kcal left" else "over target",
                            style = MaterialTheme.typography.labelSmall,
                            color = StitchTextMuted
                        )
                    }
                }

                // Stacked Metrics
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Consumed",
                            style = MaterialTheme.typography.bodySmall,
                            color = StitchTextSecondary
                        )
                        AnimatedStatNumber(
                            targetValue = consumedKcal,
                            suffix = " kcal",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = StitchDarkSlate
                        )
                    }

                    // Slim progress bar
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = statusBadgeColor,
                        trackColor = StitchBorderSubtle,
                        strokeCap = StrokeCap.Round
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Goal",
                            style = MaterialTheme.typography.bodySmall,
                            color = StitchTextSecondary
                        )
                        Text(
                            text = "$targetCeilingKcal kcal",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = StitchDarkSlate
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Target Deficit",
                            style = MaterialTheme.typography.bodySmall,
                            color = StitchTextSecondary
                        )
                        val deficit = (tdeeKcal - targetCeilingKcal).coerceAtLeast(0)
                        Text(
                            text = "-$deficit kcal",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = StitchPrimary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Active Partner Bento Card (Soft Mint #EEF9F1 with Floating 3D Mascot)
 */
@Composable
private fun StitchActivePartnerCard(
    partner: Pokemon?,
    weatherBoost: WeatherBoost? = null,
    onSelectBuddyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val partnerName = partner?.name ?: "Bulbasaur"
    val partnerAsset = partner?.spriteAssetPath ?: "sprites/pokemon_1.png"

    // Dynamic mascot breathing float & tap-to-hop physics via PoketchMotion (draw phase only)
    val (buddyOffsetProvider, triggerHop) = rememberBuddyMotion()
    val scope = rememberCoroutineScope()
    val heartAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    val heartY = remember { androidx.compose.animation.core.Animatable(0f) }

    val handleBuddyCardTap = {
        triggerHop()
        scope.launch {
            heartAlpha.snapTo(1f)
            heartY.snapTo(0f)
            launch {
                heartAlpha.animateTo(0f, tween(750, easing = FastOutSlowInEasing))
            }
            heartY.animateTo(-42f, tween(750, easing = FastOutSlowInEasing))
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { handleBuddyCardTap() },
        shape = RoundedCornerShape(26.dp),
        color = StitchPartnerMint,
        border = BorderStroke(1.dp, StitchPartnerBorder),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Eco,
                            contentDescription = "Partner",
                            tint = StitchPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "ACTIVE PARTNER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = StitchPrimary,
                            letterSpacing = 1.sp
                        )
                    }

                    // Tactile pill to switch buddy
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StitchPrimaryTint,
                        modifier = Modifier.bouncyPress { onSelectBuddyClick() }
                    ) {
                        Text(
                            text = "Switch",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = StitchPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = partnerName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = StitchDarkSlate
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = StitchStepsAmber,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "7-day streak • Next: Ponyta",
                        style = MaterialTheme.typography.bodySmall,
                        color = StitchTextSecondary
                    )
                }

                // Weather Boost Chip
                weatherBoost?.let { boost ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StitchPrimaryTint.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, StitchPrimary.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = boost.chipLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = StitchDarkSlate,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Evo Gauge Progress
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(0.85f),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Evo Gauge",
                            style = MaterialTheme.typography.labelSmall,
                            color = StitchTextMuted
                        )
                        Text(
                            text = "70%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = StitchStepsAmber
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { 0.70f },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = StitchStepsAmber,
                        trackColor = Color.White,
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            // 3D Companion Mascot Pedestal with gentle floating offset & tap reaction
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                // Soft pedestal glow
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(StitchPrimaryTint.copy(alpha = 0.7f))
                )

                val context = LocalContext.current
                val partnerImageRequest = remember(partnerAsset) {
                    ImageRequest.Builder(context)
                        .data("file:///android_asset/$partnerAsset")
                        .size(128)
                        .crossfade(120)
                        .build()
                }

                AsyncImage(
                    model = partnerImageRequest,
                    contentDescription = partnerName,
                    modifier = Modifier
                        .size(76.dp)
                        .graphicsLayer {
                            translationY = buddyOffsetProvider()
                        }
                )

                // Floating heart & sparkle pop (draw phase evaluation)
                Text(
                    text = "✨❤️✨",
                    fontSize = 14.sp,
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = heartAlpha.value
                            translationY = heartY.value
                        }
                )
            }
        }
    }
}

/**
 * Two-Column Bento Habit Cards (Water & Steps) with Tactile Spring Press Physics
 */
@Composable
private fun StitchTwoColumnHabitCards(
    todayWaterMl: Int,
    dailyWaterGoalMl: Int,
    todaySteps: Int,
    onAddWater: () -> Unit,
    onStartRun: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Water Hydration Bento Card
        val waterRatio = (todayWaterMl.toFloat() / dailyWaterGoalMl.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
        val animatedWaterProgress by rememberAnimatedProgress(waterRatio)
        val animatedWaterMl by rememberAnimatedCounter(todayWaterMl)
        val waterLiters = String.format(Locale.US, "%.1f", animatedWaterMl / 1000.0)
        val waterGoalLiters = String.format(Locale.US, "%.1f", dailyWaterGoalMl / 1000.0)

        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(22.dp),
            color = StitchSurfaceCard,
            border = BorderStroke(1.dp, StitchBorderSubtle),
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StitchHydrationTint
                    ) {
                        Text(
                            text = "Water",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = StitchHydration,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = "Water",
                        tint = StitchHydration,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = waterLiters,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StitchDarkSlate
                    )
                    Text(
                        text = " / ${waterGoalLiters} L",
                        style = MaterialTheme.typography.bodySmall,
                        color = StitchTextMuted,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                LinearProgressIndicator(
                    progress = { animatedWaterProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = StitchHydration,
                    trackColor = StitchBorderSubtle,
                    strokeCap = StrokeCap.Round
                )

                // Bouncy +250ml pill button with bouncyPress
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bouncyPress(onClick = onAddWater),
                    shape = RoundedCornerShape(20.dp),
                    color = StitchHydrationTint
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Water",
                            tint = StitchHydration,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "250 ml",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = StitchHydration
                        )
                    }
                }
            }
        }

        // Steps Bento Card
        val stepsRatio = (todaySteps.toFloat() / 10000f).coerceIn(0f, 1f)
        val animatedStepsProgress by rememberAnimatedProgress(stepsRatio)

        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(22.dp),
            color = StitchSurfaceCard,
            border = BorderStroke(1.dp, StitchBorderSubtle),
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StitchStepsTint
                    ) {
                        Text(
                            text = "Steps",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = StitchStepsAmber,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.DirectionsWalk,
                        contentDescription = "Steps",
                        tint = StitchStepsAmber,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    AnimatedStatNumber(
                        targetValue = todaySteps,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = StitchDarkSlate
                    )
                    Text(
                        text = " / 10k",
                        style = MaterialTheme.typography.bodySmall,
                        color = StitchTextMuted,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                LinearProgressIndicator(
                    progress = { animatedStepsProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = StitchStepsAmber,
                    trackColor = StitchBorderSubtle,
                    strokeCap = StrokeCap.Round
                )

                // Bouncy 1-Mi Run pill button with bouncyPress
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bouncyPress(onClick = onStartRun),
                    shape = RoundedCornerShape(20.dp),
                    color = StitchDarkSlate
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Run",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "1-Mi Run",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full-Width Emerald Primary CTA: + Log Meal or Quick Calories
 */
@Composable
private fun StitchPrimaryActionCTA(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = com.poketch.mobile.ui.theme.LocalStitchColors.current.isOledDark
    val ctaContentColor = if (isDark) Color(0xFF022C22) else Color.White

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .bouncyPress(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        color = StitchPrimary,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AddCircle,
                contentDescription = "Add",
                tint = ctaContentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Log Meal or Quick Calories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ctaContentColor
            )
        }
    }
}

/**
 * Snorlax Recovery Bento Badge Card
 */
@Composable
private fun StitchSnorlaxRecoveryCard(
    recovery: SnorlaxRecoveryState,
    modifier: Modifier = Modifier
) {
    val isDark = com.poketch.mobile.ui.theme.LocalStitchColors.current.isOledDark

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = StitchSurfaceCard,
        border = BorderStroke(1.dp, StitchBorderSubtle),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val context = LocalContext.current
            val snorlaxImageRequest = remember {
                ImageRequest.Builder(context)
                    .data("file:///android_asset/sprites/pokemon_143.png")
                    .size(128)
                    .crossfade(120)
                    .build()
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0xFF090F0D) else Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = snorlaxImageRequest,
                    contentDescription = "Snorlax #143",
                    modifier = Modifier.size(44.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Snorlax Slumber (#143)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = StitchDarkSlate
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            recovery.isUnlocked -> StitchPrimaryTint
                            recovery.isCurrentlyInSurplus -> StitchAlertTint
                            recovery.recoveryDays > 0 -> StitchStepsTint
                            else -> Color(0xFFF1F5F9)
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
                                recovery.isUnlocked -> StitchPrimary
                                recovery.isCurrentlyInSurplus -> StitchAlertCoral
                                recovery.recoveryDays > 0 -> StitchStepsAmber
                                else -> StitchTextMuted
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = when {
                        recovery.isUnlocked -> "Conquered surplus days and awakened Snorlax!"
                        recovery.isCurrentlyInSurplus -> "+${recovery.surplusAmountKcal} kcal over ceiling. Stay on track tomorrow."
                        recovery.recoveryDays > 0 -> "Stay at or below ceiling for ${3 - recovery.recoveryDays} more day(s) to awaken Snorlax!"
                        else -> "Route 12 slumber. 3 consecutive on-target days wake him up."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = StitchTextSecondary
                )
            }
        }
    }
}

/**
 * Collapsible Trainer Field Guide Card
 */
@Composable
private fun StitchTrainerFieldGuideCard(
    modifier: Modifier = Modifier
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(22.dp),
        color = StitchSurfaceCard,
        border = BorderStroke(1.dp, StitchBorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(StitchPrimaryTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎓", fontSize = 16.sp)
                    }
                    Column {
                        Text(
                            text = "Trainer Field Guide",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = StitchDarkSlate
                        )
                        Text(
                            text = if (isExpanded) "Tap to collapse briefing" else "Tap to view deficit rules & companion mechanics",
                            style = MaterialTheme.typography.bodySmall,
                            color = StitchTextMuted
                        )
                    }
                }
                Text(
                    text = if (isExpanded) "▲" else "▼",
                    color = StitchTextMuted,
                    fontSize = 14.sp
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FieldGuideRow(
                        emoji = "🎯",
                        title = "Target Ceiling (Budget)",
                        desc = "Eat below this threshold to burn fat in deficit and keep your companion streak active."
                    )
                    FieldGuideRow(
                        emoji = "🔥",
                        title = "TDEE (Daily Calorie Burn)",
                        desc = "Your Total Daily Energy Expenditure from metabolic basal rate and activity."
                    )
                    FieldGuideRow(
                        emoji = "⌚",
                        title = "Wear OS Watch Remote",
                        desc = "Quick-add snacks, log water, or switch your watch face companion in real time."
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldGuideRow(emoji: String, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8FAFC))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = emoji, fontSize = 16.sp)
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = StitchDarkSlate
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = StitchTextSecondary
            )
        }
    }
}

/**
 * Preserved OverCalorieWarningCard with exact test strings required by OverCalorieWarningBannerTest
 */
@Composable
internal fun OverCalorieWarningCard(
    warning: CalorieBudgetState.OverTargetWarning,
    recoveryQuest: RecoveryQuest? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFEF2F2)
        ),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = StitchAlertCoral,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OVER CALORIE TARGET!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StitchAlertCoral
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "+${warning.surplusKcal} kcal above target ceiling",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = StitchDarkSlate
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5).copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Suggested burn offsets:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = StitchAlertCoral
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Walk ${warning.suggestedOffsetSteps} steps (~${warning.suggestedOffsetSteps / 1000}k)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = StitchDarkSlate
                    )
                    Text(
                        text = "• Or jog ${warning.suggestedOffsetRunMiles} miles",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = StitchDarkSlate
                    )
                }
            }

            recoveryQuest?.let { quest ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StitchStepsAmber.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (quest.isCompleted) "🎉 RECOVERY QUEST COMPLETE" else "⚡ PLAYABLE RECOVERY QUEST",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (quest.isCompleted) StitchPrimary else StitchStepsAmber
                            )
                            Text(
                                text = "${quest.currentOffsetSteps} / ${quest.targetOffsetSteps} steps",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = StitchDarkSlate
                            )
                        }
                        LinearProgressIndicator(
                            progress = { quest.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (quest.isCompleted) StitchPrimary else StitchStepsAmber,
                            trackColor = Color(0xFFF1F5F2),
                            strokeCap = StrokeCap.Round
                        )
                        Text(
                            text = if (quest.isCompleted) "Target burned! Snorlax recovering" else "${quest.remainingSteps} offset steps remaining to balance your budget",
                            style = MaterialTheme.typography.bodySmall,
                            color = StitchTextSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Quick Add Calorie Modal Dialog
 */
@Composable
private fun QuickAddCalorieModal(
    onDismiss: () -> Unit,
    onQuickAdd: (Int, String) -> Unit,
    onGoToFullLog: () -> Unit
) {
    var customKcal by remember { mutableStateOf("") }
    var customLabel by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Log Calories",
                fontWeight = FontWeight.Bold,
                color = StitchDarkSlate
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Choose a preset or enter custom amount:",
                    style = MaterialTheme.typography.bodySmall,
                    color = StitchTextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        100 to "Apple",
                        250 to "Snack",
                        500 to "Meal"
                    ).forEach { (cal, name) ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onQuickAdd(cal, name) },
                            color = StitchPrimaryTint,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "+$cal",
                                    fontWeight = FontWeight.Bold,
                                    color = StitchPrimary,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StitchTextSecondary
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = StitchBorderSubtle)

                OutlinedTextField(
                    value = customKcal,
                    onValueChange = { customKcal = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Calories (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    label = { Text("Meal Label (e.g. Oatmeal)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cal = customKcal.toIntOrNull() ?: 0
                    if (cal > 0) {
                        onQuickAdd(cal, customLabel.ifBlank { "Quick Add" })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = StitchPrimary),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Log")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onGoToFullLog,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Full Log Tab")
            }
        },
        containerColor = StitchSurfaceCard,
        shape = RoundedCornerShape(26.dp)
    )
}

/**
 * Buddy Picker Bottom Sheet Content
 */
@Composable
private fun BuddyPickerSheetContent(
    activeBuddy: Pokemon?,
    unlockedBuddies: List<Pokemon>,
    onSelectBuddy: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Select Active Companion",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = StitchDarkSlate
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(unlockedBuddies) { buddy ->
                val isSelected = buddy.pokedexNumber == activeBuddy?.pokedexNumber
                Surface(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSelectBuddy(buddy.pokedexNumber) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) StitchPrimaryTint else StitchSurfaceCard,
                    border = BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) StitchPrimary else StitchBorderSubtle
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val context = LocalContext.current
                        val buddyImageRequest = remember(buddy.spriteAssetPath) {
                            ImageRequest.Builder(context)
                                .data("file:///android_asset/${buddy.spriteAssetPath}")
                                .size(128)
                                .crossfade(120)
                                .build()
                        }
                        AsyncImage(
                            model = buddyImageRequest,
                            contentDescription = buddy.name,
                            modifier = Modifier.size(50.dp)
                        )
                        Text(
                            text = buddy.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = StitchDarkSlate,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}
