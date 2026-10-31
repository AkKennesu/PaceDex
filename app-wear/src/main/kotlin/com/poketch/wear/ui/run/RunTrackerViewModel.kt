package com.poketch.wear.ui.run

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poketch.core.domain.model.Pokemon
import com.poketch.core.domain.repository.PokemonRepository
import com.poketch.core.domain.repository.UserRepository
import com.poketch.wear.health.OneMileRunExerciseManager
import com.poketch.wear.health.RunSessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RunTrackerViewModel @Inject constructor(
    private val exerciseManager: OneMileRunExerciseManager,
    private val userRepository: UserRepository,
    private val pokemonRepository: PokemonRepository,
) : ViewModel() {

    val runState: StateFlow<RunSessionState> = exerciseManager.sessionState

    val activeBuddy: StateFlow<Pokemon?> = userRepository.observeUserProfile()
        .flatMapLatest { profile ->
            val buddyId = profile?.activeBuddyPokemonId ?: 77 // Default to #077 Ponyta
            pokemonRepository.observePokemonById(buddyId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun startRun() {
        exerciseManager.startRun()
    }

    fun pauseRun() {
        exerciseManager.pauseRun()
    }

    fun resumeRun() {
        exerciseManager.resumeRun()
    }

    fun finishRun() {
        exerciseManager.finishRun()
    }

    fun simulateProgress(deltaMiles: Double = 0.25) {
        exerciseManager.simulateDistance(deltaMiles)
    }

    fun resetSession() {
        exerciseManager.resetSession()
    }
}
