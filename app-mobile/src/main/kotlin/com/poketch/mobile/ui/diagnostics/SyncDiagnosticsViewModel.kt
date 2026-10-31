package com.poketch.mobile.ui.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poketch.core.sync.PoketchSyncManager
import com.poketch.core.sync.SyncDiagnosticsInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MobileSyncUiState(
    val isSyncing: Boolean = false,
    val diagnostics: SyncDiagnosticsInfo = SyncDiagnosticsInfo(),
    val feedbackMessage: String? = null
)

@HiltViewModel
class SyncDiagnosticsViewModel @Inject constructor(
    private val syncManager: PoketchSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MobileSyncUiState())
    val uiState: StateFlow<MobileSyncUiState> = _uiState.asStateFlow()

    init {
        refreshDiagnostics()
    }

    fun refreshDiagnostics() {
        viewModelScope.launch {
            val diag = syncManager.getSyncDiagnostics()
            _uiState.update { it.copy(diagnostics = diag) }
        }
    }

    fun forceFullSyncNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, feedbackMessage = "Flushing offline queue and synchronizing with watch...") }
            try {
                val updated = syncManager.forceFullSync()
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        diagnostics = updated,
                        feedbackMessage = "Full synchronization completed successfully!"
                    )
                }
            } catch (e: Exception) {
                val current = syncManager.getSyncDiagnostics()
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        diagnostics = current,
                        feedbackMessage = "Sync failed: ${e.message}"
                    )
                }
            }
        }
    }
}
