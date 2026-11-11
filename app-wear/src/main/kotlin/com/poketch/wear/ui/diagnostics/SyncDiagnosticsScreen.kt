package com.poketch.wear.ui.diagnostics

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
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
import com.poketch.wear.ui.layout.rememberResponsiveColumnState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SyncDiagnosticsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SyncDiagnosticsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val columnState = rememberResponsiveColumnState()

    LaunchedEffect(Unit) {
        viewModel.refreshDiagnostics()
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
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header
            item {
                Text(
                    text = "Sync & Pairing",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary,
                    textAlign = TextAlign.Center
                )
            }

            // QR Code Pairing Card (Visible when disconnected or awaiting pairing; gone once connected and synced)
            if (state.showQrCode && state.qrBitmap != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.94f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "PAIR WITH PHONE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFB74D),
                                textAlign = TextAlign.Center
                            )
                            Box(
                                modifier = Modifier
                                    .size(116.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = state.qrBitmap!!.asImageBitmap(),
                                    contentDescription = "Pokétch Pairing QR Code",
                                    filterQuality = FilterQuality.None,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Text(
                                text = "Scan in Phone App > Profile",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Connection Status Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF263238))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "PEER CONNECTION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF90A4AE)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        if (state.diagnostics.connectedNodes.isNotEmpty()) {
                            val node = state.diagnostics.connectedNodes.first()
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (node.isNearby) Color(0xFF4CAF50) else Color(0xFFFFB300))
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text(
                                    text = node.displayName.ifBlank { "Phone (${node.id.take(6)})" },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "● Connected & Linked",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF81C784)
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE53935))
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text(
                                    text = "No Phone Connected",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFFFCDD2)
                                )
                            }
                            Text(
                                text = "QR code displayed above",
                                fontSize = 8.5.sp,
                                color = Color(0xFFFFCC80)
                            )
                        }
                    }
                }
            }

            // Queue & Outbox Stats
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E272C))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "OFFLINE OUTBOX QUEUE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF90A4AE)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val totalPending = state.diagnostics.totalPendingCount
                        Text(
                            text = if (totalPending == 0) "All Changes Synced (0)" else "$totalPending Pending Changes",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalPending == 0) Color(0xFF81C784) else Color(0xFFFFB74D)
                        )
                    }
                }
            }

            // Last Sync Timestamp
            item {
                val timeStr = if (state.diagnostics.lastSyncTimestampMillis > 0L) {
                    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    "Last: ${sdf.format(Date(state.diagnostics.lastSyncTimestampMillis))}"
                } else {
                    "Last: Not synced yet"
                }
                Text(
                    text = timeStr,
                    fontSize = 10.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
            }

            // Feedback Message
            if (state.feedbackMessage != null) {
                item {
                    Text(
                        text = state.feedbackMessage ?: "",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF80D8FF),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Force Full Sync Button
            item {
                CompactChip(
                    onClick = { viewModel.forceFullSyncNow() },
                    enabled = !state.isSyncing,
                    label = {
                        if (state.isSyncing) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    indicatorColor = Color.White
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = "Syncing...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = "Sync Now",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    colors = ChipDefaults.primaryChipColors(
                        backgroundColor = Color(0xFF1976D2),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(34.dp)
                )
            }

            // Back Button
            item {
                CompactChip(
                    onClick = onNavigateBack,
                    label = {
                        Text(
                            text = "Back",
                            fontSize = 11.sp
                        )
                    },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(30.dp)
                )
            }
        }
    }
}
