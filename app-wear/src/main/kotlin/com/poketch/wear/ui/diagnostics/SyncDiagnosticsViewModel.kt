package com.poketch.wear.ui.diagnostics

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

data class WearSyncUiState(
    val isSyncing: Boolean = false,
    val diagnostics: SyncDiagnosticsInfo = SyncDiagnosticsInfo(),
    val feedbackMessage: String? = null
)

@HiltViewModel
class SyncDiagnosticsViewModel @Inject constructor(
    private val syncManager: PoketchSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WearSyncUiState())
    val uiState: StateFlow<WearSyncUiState> = _uiState.asStateFlow()

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
            _uiState.update { it.copy(isSyncing = true, feedbackMessage = "Syncing with phone...") }
            try {
                val updated = syncManager.forceFullSync()
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        diagnostics = updated,
                        feedbackMessage = "Sync Complete!"
                    )
                }
            } catch (e: Exception) {
                val current = syncManager.getSyncDiagnostics()
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        diagnostics = current,
                        feedbackMessage = "Sync Error: ${e.message}"
                    )
                }
            }
        }
    }
}
