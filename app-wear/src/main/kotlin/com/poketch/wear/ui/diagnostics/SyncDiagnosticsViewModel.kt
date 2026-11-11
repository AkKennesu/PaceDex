package com.poketch.wear.ui.diagnostics

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poketch.core.sync.ConnectedNodeInfo
import com.poketch.core.sync.PoketchSyncConstants
import com.poketch.core.sync.PoketchSyncManager
import com.poketch.core.sync.SyncDiagnosticsInfo
import com.poketch.wear.ui.qr.QrCodeGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.URLEncoder
import javax.inject.Inject

data class WearSyncUiState(
    val isSyncing: Boolean = false,
    val diagnostics: SyncDiagnosticsInfo = SyncDiagnosticsInfo(),
    val feedbackMessage: String? = null,
    val showQrCode: Boolean = true,
    val qrBitmap: Bitmap? = null,
    val localNode: ConnectedNodeInfo? = null
)

@HiltViewModel
class SyncDiagnosticsViewModel @Inject constructor(
    private val syncManager: PoketchSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WearSyncUiState())
    val uiState: StateFlow<WearSyncUiState> = _uiState.asStateFlow()

    init {
        initializePairingAndMonitor()
    }

    private fun initializePairingAndMonitor() {
        viewModelScope.launch {
            // 1. Fetch local node info to build pairing QR code
            val local = runCatching { syncManager.getLocalNode() }.getOrNull()
            val nodeId = local?.id ?: "poketch_wear_node"
            val deviceName = local?.displayName ?: "Pokétch Watch"
            val pairingPayload = buildPairingUri(nodeId, deviceName)
            val bitmap = QrCodeGenerator.generateQrBitmap(pairingPayload, sizePx = 200)

            _uiState.update {
                it.copy(
                    localNode = local,
                    qrBitmap = bitmap
                )
            }

            // 2. Initial diagnostics
            refreshDiagnostics()

            // 3. Monitor connection status continuously
            while (isActive) {
                val nodes = syncManager.refreshConnectedNodes()
                val isConnected = nodes.isNotEmpty()
                _uiState.update { current ->
                    current.copy(
                        diagnostics = current.diagnostics.copy(connectedNodes = nodes),
                        // Once connected, hide QR code. Once disconnected, show QR code again!
                        showQrCode = !isConnected
                    )
                }
                delay(2000L)
            }
        }
    }

    private fun buildPairingUri(nodeId: String, name: String): String {
        val encodedId = runCatching { URLEncoder.encode(nodeId, "UTF-8") }.getOrDefault(nodeId)
        val encodedName = runCatching { URLEncoder.encode(name, "UTF-8") }.getOrDefault(name)
        return "${PoketchSyncConstants.URI_SCHEME_PAIR}?nodeId=$encodedId&name=$encodedName"
    }

    fun refreshDiagnostics() {
        viewModelScope.launch {
            val diag = syncManager.getSyncDiagnostics()
            val isConnected = diag.connectedNodes.isNotEmpty()
            _uiState.update {
                it.copy(
                    diagnostics = diag,
                    showQrCode = !isConnected
                )
            }
        }
    }

    fun forceFullSyncNow() {
        viewModelScope.launch {
            val diagBefore = syncManager.getSyncDiagnostics()
            val isConnected = diagBefore.connectedNodes.isNotEmpty()

            if (!isConnected) {
                // If not connected, show the QR code and alert user
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        showQrCode = true,
                        diagnostics = diagBefore,
                        feedbackMessage = "Disconnected. Scan QR code on Phone."
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(isSyncing = true, feedbackMessage = "Syncing with phone...") }
            try {
                val updated = syncManager.forceFullSync()
                val stillConnected = updated.connectedNodes.isNotEmpty()
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        diagnostics = updated,
                        // Gone once synced and connected! If disconnected, shows QR again.
                        showQrCode = !stillConnected,
                        feedbackMessage = if (stillConnected) "Sync Complete!" else "Disconnected. Scan QR code."
                    )
                }
            } catch (e: Exception) {
                val current = syncManager.getSyncDiagnostics()
                val stillConnected = current.connectedNodes.isNotEmpty()
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        diagnostics = current,
                        showQrCode = !stillConnected,
                        feedbackMessage = "Sync Error: ${e.message}"
                    )
                }
            }
        }
    }
}
