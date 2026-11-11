package com.poketch.mobile.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poketch.core.data.repository.FirestoreSyncRepository
import com.poketch.core.domain.model.AuthState
import com.poketch.core.domain.repository.AuthRepository
import com.poketch.core.domain.repository.CalorieRepository
import com.poketch.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isSignUp: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isSyncing: Boolean = false,
    val lastSyncTime: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firestoreSyncRepository: FirestoreSyncRepository,
    private val userRepository: UserRepository,
    private val calorieRepository: CalorieRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthState.Loading)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun toggleMode() {
        _uiState.update { it.copy(isSignUp = !it.isSignUp, errorMessage = null, infoMessage = null) }
    }

    fun submitEmailAuth() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        val isSignUp = _uiState.value.isSignUp

        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter both email and password") }
            return
        }

        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = if (isSignUp) {
                authRepository.signUpWithEmail(email, password)
            } else {
                authRepository.signInWithEmail(email, password)
            }

            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            email = "",
                            password = "",
                            infoMessage = "Welcome, ${user.email ?: "Trainer"}!"
                        )
                    }
                    // Auto-sync profile to cloud upon sign in
                    userRepository.getUserProfile()?.let { profile ->
                        firestoreSyncRepository.uploadUserProfile(user.uid, profile)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Authentication failed"
                        )
                    }
                }
            )
        }
    }

    fun signInWithGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.signInWithGoogleIdToken(idToken)
            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            infoMessage = "Signed in with Google as ${user.displayName ?: user.email}!"
                        )
                    }
                    userRepository.getUserProfile()?.let { profile ->
                        firestoreSyncRepository.uploadUserProfile(user.uid, profile)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Google sign-in failed"
                        )
                    }
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.update { it.copy(infoMessage = "Signed out successfully", errorMessage = null) }
        }
    }

    fun syncToCloud() {
        val user = authRepository.currentUser ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, errorMessage = null) }
            try {
                val profile = userRepository.getUserProfile()
                if (profile != null) {
                    firestoreSyncRepository.uploadUserProfile(user.uid, profile)
                }

                val nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        lastSyncTime = nowStr,
                        infoMessage = "Cloud backup completed at $nowStr"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        errorMessage = "Sync failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun restoreFromCloud() {
        val user = authRepository.currentUser ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, errorMessage = null) }
            firestoreSyncRepository.downloadUserProfile(user.uid).fold(
                onSuccess = { remoteProfile ->
                    if (remoteProfile != null) {
                        userRepository.upsertUserProfile(remoteProfile)
                        _uiState.update {
                            it.copy(
                                isSyncing = false,
                                infoMessage = "Restored profile from Cloud Firestore!"
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isSyncing = false,
                                infoMessage = "No remote profile found in Cloud Firestore."
                            )
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            errorMessage = "Restore failed: ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }
}
