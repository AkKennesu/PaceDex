package com.poketch.mobile.ui.diagnostics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poketch.mobile.ui.theme.LocalStitchColors
import com.poketch.mobile.ui.theme.LocalThemeIsOledDark
import com.poketch.mobile.ui.theme.StitchAlertCoral
import com.poketch.mobile.ui.theme.StitchBorderSubtle
import com.poketch.mobile.ui.theme.StitchCanvasBackground
import com.poketch.mobile.ui.theme.StitchDarkSlate
import com.poketch.mobile.ui.theme.StitchHydration
import com.poketch.mobile.ui.theme.StitchHydrationTint
import com.poketch.mobile.ui.theme.StitchPrimary
import com.poketch.mobile.ui.theme.StitchPrimaryTint
import com.poketch.mobile.ui.theme.StitchStepsAmber
import com.poketch.mobile.ui.theme.StitchStepsTint
import com.poketch.mobile.ui.theme.StitchSurfaceCard
import com.poketch.mobile.ui.theme.StitchTextMuted
import com.poketch.mobile.ui.theme.StitchTextPrimary
import com.poketch.mobile.ui.theme.StitchTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Google Stitch Watch & Sync Settings Screen
 * Features Bento Wear OS status card with sync button, smart reminder switches,
 * and appearance / trainer account card with Botanical Light vs Tactical Mint OLED Dark switcher.
 */
@Composable
fun SyncDiagnosticsScreen(
    modifier: Modifier = Modifier,
    viewModel: SyncDiagnosticsViewModel = viewModel(),
    onOpenAccount: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    var hydrationNudgeOn by rememberSaveable { mutableStateOf(true) }
    var runPromptOn by rememberSaveable { mutableStateOf(true) }
    var mealCheckInOn by rememberSaveable { mutableStateOf(true) }

    val oledDarkState = LocalThemeIsOledDark.current
    val isDark = LocalStitchColors.current.isOledDark

    LaunchedEffect(Unit) {
        viewModel.refreshDiagnostics()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StitchCanvasBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header: Watch & Sync
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = "Watch & Sync",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = StitchDarkSlate,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Wear OS telemetry & background sync",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StitchTextMuted
                )
            }

            // 1. Elevated White / Tactical Deep Mint "Wear OS Watch" Status Card
            StitchWearOsStatusCard(
                isSyncing = state.isSyncing,
                hasNodes = state.diagnostics.connectedNodes.isNotEmpty(),
                nodeName = state.diagnostics.connectedNodes.firstOrNull()?.displayName,
                lastSyncTime = state.diagnostics.lastSyncTimestampMillis,
                onSyncClick = { viewModel.forceFullSyncNow() }
            )

            // Feedback Toast / Banner
            state.feedbackMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = StitchPrimaryTint,
                    border = BorderStroke(1.dp, StitchPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = StitchPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = StitchDarkSlate,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 2. Smart Reminders Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Reminders",
                            tint = StitchPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Smart Reminders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StitchDarkSlate
                        )
                    }
                    Text(
                        text = "HAPTIC ALERTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = StitchTextMuted,
                        letterSpacing = 1.sp
                    )
                }

                // Reminder 1: Hydration Nudges
                StitchReminderToggleCard(
                    title = "Hydration Nudges",
                    subtitle = "Every 90 min • 08:00–21:00",
                    icon = Icons.Default.WaterDrop,
                    iconTint = StitchHydration,
                    iconBg = StitchHydrationTint,
                    checked = hydrationNudgeOn,
                    onCheckedChange = { hydrationNudgeOn = it }
                )

                // Reminder 2: 1-Mile Run Prompt
                StitchReminderToggleCard(
                    title = "1-Mile Run Prompt",
                    subtitle = "Daily at 17:00 if under 50% steps",
                    icon = Icons.Default.DirectionsRun,
                    iconTint = StitchStepsAmber,
                    iconBg = StitchStepsTint,
                    checked = runPromptOn,
                    onCheckedChange = { runPromptOn = it }
                )

                // Reminder 3: Meal Check-Ins
                StitchReminderToggleCard(
                    title = "Meal Check-Ins",
                    subtitle = "Breakfast, Lunch & Dinner",
                    icon = Icons.Default.Restaurant,
                    iconTint = StitchPrimary,
                    iconBg = StitchPrimaryTint,
                    checked = mealCheckInOn,
                    onCheckedChange = { mealCheckInOn = it }
                )
            }

            // 3. Appearance & Trainer Account Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = if (isDark) 0.dp else 6.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = StitchDarkSlate.copy(alpha = 0.04f),
                        spotColor = StitchDarkSlate.copy(alpha = 0.06f)
                    ),
                shape = RoundedCornerShape(22.dp),
                color = StitchSurfaceCard,
                border = BorderStroke(1.dp, StitchBorderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Appearance & Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StitchDarkSlate
                    )

                    // Light / OLED Dark Segmented Switcher
                    val switcherBg = if (isDark) Color(0xFF090F0D) else Color(0xFFF1F5F2)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(switcherBg)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = { oledDarkState.value = false },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp)),
                            color = if (!isDark) Color.White else Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = if (!isDark) 2.dp else 0.dp
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Botanical Light",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (!isDark) FontWeight.Bold else FontWeight.Normal,
                                    color = if (!isDark) Color(0xFF0F172A) else StitchTextSecondary
                                )
                            }
                        }

                        Surface(
                            onClick = { oledDarkState.value = true },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp)),
                            color = if (isDark) Color(0xFF131E19) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            border = if (isDark) BorderStroke(1.dp, StitchBorderSubtle) else null,
                            shadowElevation = 0.dp
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "OLED Dark",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isDark) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isDark) Color(0xFF34D399) else StitchTextSecondary
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = StitchBorderSubtle)

                    // Account & Sign Out Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "trainer@poketch.app",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = StitchDarkSlate
                            )
                            Text(
                                text = "Google Account Linked",
                                style = MaterialTheme.typography.bodySmall,
                                color = StitchTextMuted
                            )
                        }

                        Text(
                            text = "Trainer Account",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = StitchPrimary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenAccount() }
                                .padding(8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

/**
 * Elevated White / Tactical Deep Mint "Wear OS Watch" Status Card
 */
@Composable
private fun StitchWearOsStatusCard(
    isSyncing: Boolean,
    hasNodes: Boolean,
    nodeName: String?,
    lastSyncTime: Long,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalStitchColors.current.isOledDark

    val syncInteraction = remember { MutableInteractionSource() }
    val isPressed by syncInteraction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "sync_scale"
    )

    // Infinite rotation for sync icon during sync
    val infiniteTransition = rememberInfiniteTransition(label = "sync_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    val lastSyncFormatted = remember(lastSyncTime) {
        if (lastSyncTime > 0) {
            SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(lastSyncTime))
        } else {
            "Just now"
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 0.dp else 10.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = StitchDarkSlate.copy(alpha = 0.04f),
                spotColor = StitchDarkSlate.copy(alpha = 0.08f)
            ),
        shape = RoundedCornerShape(26.dp),
        color = StitchSurfaceCard,
        border = BorderStroke(1.dp, StitchBorderSubtle)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Poké Ball Icon inside circular container
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF141C18) else Color(0xFFF1F5F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "◓", fontSize = 26.sp, color = StitchAlertCoral)
                    }

                    Column {
                        Text(
                            text = "Wear OS Watch",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StitchDarkSlate
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = "Sensors",
                                tint = StitchPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Last synced $lastSyncFormatted via Data Layer",
                                style = MaterialTheme.typography.bodySmall,
                                color = StitchTextMuted
                            )
                        }
                    }
                }

                // Connected Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StitchPrimaryTint
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(StitchPrimary)
                        )
                        Text(
                            text = if (hasNodes) "92%" else "Linked",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = StitchPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Full-Width CTA "Sync Now" (Tactical Mint in OLED Dark, Deep Slate in Light)
            val syncBtnColor = if (isDark) StitchPrimary else StitchDarkSlate
            val syncTextColor = if (isDark) Color(0xFF022C22) else Color.White

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = syncInteraction,
                        indication = null,
                        onClick = onSyncClick
                    ),
                shape = RoundedCornerShape(24.dp),
                color = syncBtnColor
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync",
                        tint = syncTextColor,
                        modifier = Modifier
                            .size(18.dp)
                            .then(if (isSyncing) Modifier.rotate(rotation) else Modifier)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSyncing) "Syncing with Watch..." else "Sync Now",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = syncTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Companion Telemetry Tiny Glance Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF090F0D) else Color(0xFFF8FAFC))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = nodeName ?: "Galaxy Watch6",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchTextSecondary
                )
                Text(
                    text = "Latency < 140ms",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = StitchPrimary
                )
            }
        }
    }
}

/**
 * Smart Reminder Toggle Card with Emerald Switch
 */
@Composable
private fun StitchReminderToggleCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalStitchColors.current.isOledDark

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 0.dp else 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = StitchDarkSlate.copy(alpha = 0.03f),
                spotColor = StitchDarkSlate.copy(alpha = 0.06f)
            ),
        shape = RoundedCornerShape(20.dp),
        color = StitchSurfaceCard,
        border = BorderStroke(1.dp, StitchBorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = StitchDarkSlate
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = StitchTextMuted
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = if (isDark) Color(0xFF022C22) else Color.White,
                    checkedTrackColor = StitchPrimary,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = if (isDark) Color(0xFF1F2E26) else Color(0xFFE2E8F0)
                )
            )
        }
    }
}
