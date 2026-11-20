package com.poketch.mobile.ui.diagnostics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poketch.mobile.ui.theme.PoketchBorder
import com.poketch.mobile.ui.theme.PoketchBorderSubtle
import com.poketch.mobile.ui.theme.PoketchCrimson
import com.poketch.mobile.ui.theme.PoketchCyan
import com.poketch.mobile.ui.theme.PoketchEmerald
import com.poketch.mobile.ui.theme.PoketchSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SyncDiagnosticsScreen(
    modifier: Modifier = Modifier,
    viewModel: SyncDiagnosticsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.refreshDiagnostics()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                                .background(PoketchCyan)
                        )
                        Text(
                            text = "WEAR OS TELEMETRY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            color = PoketchCyan
                        )
                    }
                    Text(
                        text = "Watch Link & Sync",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, PoketchBorder)
                ) {
                    Button(
                        onClick = { viewModel.refreshDiagnostics() },
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                        colors = ButtonDefaults.textButtonColors()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Connection Guide Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, PoketchBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PoketchCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⌚", fontSize = 16.sp)
                        }
                        Text(
                            text = "How Watch Link Works",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "• Physical Watch: Automatically connects via Google Play Services Wearable when Bluetooth is active.\n• Emulators: Scan the QR code from the watch's Bag menu or use the Trainer tab to link directly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Card 1: Connected Wear Watch Nodes
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PoketchSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, PoketchBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "CONNECTED WEAR OS NODES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = PoketchCyan,
                        letterSpacing = 1.sp
                    )

                    if (state.diagnostics.connectedNodes.isNotEmpty()) {
                        state.diagnostics.connectedNodes.forEach { node ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(1.dp, PoketchBorderSubtle, RoundedCornerShape(14.dp))
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (node.isNearby) PoketchEmerald else Color(0xFFFFB300))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = node.displayName.ifBlank { "Wear OS Device" },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Node ID: ${node.id.take(12)}...",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (node.isNearby) PoketchEmerald.copy(alpha = 0.15f) else Color(0xFFFFB300).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (node.isNearby) "Nearby (BT/WiFi)" else "Cloud Relay",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (node.isNearby) PoketchEmerald else Color(0xFFFFB300),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                                .border(1.dp, PoketchBorderSubtle, RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = PoketchCrimson
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "No Wear OS watch currently connected via Google Play Services.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }

            // Card 2: Offline Outbox Queue & Sync State
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PoketchSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, PoketchBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "OFFLINE OUTBOX QUEUE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = PoketchCyan,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QueueMetricItem(
                            label = "Pending Calories",
                            count = state.diagnostics.pendingCaloriesCount,
                            modifier = Modifier.weight(1f)
                        )
                        QueueMetricItem(
                            label = "Pending Habits",
                            count = state.diagnostics.pendingHabitsCount,
                            modifier = Modifier.weight(1f)
                        )
                        QueueMetricItem(
                            label = "Total Unsynced",
                            count = state.diagnostics.totalPendingCount,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = PoketchBorderSubtle, modifier = Modifier.padding(vertical = 4.dp))

                    val timeFormatted = if (state.diagnostics.lastSyncTimestampMillis > 0L) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        sdf.format(Date(state.diagnostics.lastSyncTimestampMillis))
                    } else {
                        "No synchronizations performed this session"
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = PoketchEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Last Sync: $timeFormatted",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // Card 3: Synchronization Protocol Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PoketchSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, PoketchBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = PoketchCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SYNC PROTOCOL & DATA PATHS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = PoketchCyan,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = "• Conflict Resolution: Last-Write-Wins (LWW) via UTC Epoch Milliseconds\n" +
                               "• /profile/settings: User target calorie ceiling, target water ml, active buddy\n" +
                               "• /catalog/pokedex: 151 Pokémon unlock and shiny bitmasks\n" +
                               "• /logs/calories/: UUID-keyed immutable food and meal logs\n" +
                               "• /logs/habits/: UUID-keyed daily water, step milestones, and runs",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Feedback Status Text
            if (state.feedbackMessage != null) {
                Text(
                    text = state.feedbackMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PoketchCyan,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // Action Button: Force Full Sync Now
            Button(
                onClick = { viewModel.forceFullSyncNow() },
                enabled = !state.isSyncing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
            ) {
                if (state.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Synchronizing Full Dataset...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Force Full Sync Now",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueMetricItem(
    label: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, PoketchBorderSubtle),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp)
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = if (count == 0) PoketchEmerald else Color(0xFFFF9800)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
