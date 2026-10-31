package com.poketch.wear.ui.alert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poketch.core.domain.model.Pokemon
import com.poketch.core.domain.repository.CalorieRepository
import com.poketch.core.domain.repository.PokemonRepository
import com.poketch.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class OverTargetRedAlertUiState(
    val consumedKcal: Int = 0,
    val targetCalorieCeiling: Int = 1800,
    val surplusKcal: Int = 0,
    val activeBuddy: Pokemon? = null,
    val isLoading: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OverTargetRedAlertViewModel @Inject constructor(
    userRepository: UserRepository,
    calorieRepository: CalorieRepository,
    pokemonRepository: PokemonRepository,
    private val wearHapticController: WearHapticController,
) : ViewModel() {

    private val todayIso: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private val userProfileFlow = userRepository.observeUserProfile()
    private val totalCaloriesFlow = calorieRepository.observeTotalCaloriesByDate(todayIso)

    private val buddyFlow = userProfileFlow.flatMapLatest { profile ->
        val buddyId = profile?.activeBuddyPokemonId ?: 1
        pokemonRepository.observePokemonById(buddyId)
    }

    val uiState: StateFlow<OverTargetRedAlertUiState> = combine(
        userProfileFlow,
        totalCaloriesFlow,
        buddyFlow,
    ) { profile, consumed, buddy ->
        val ceiling = profile?.targetCalorieCeiling ?: 1800
        val surplus = maxOf(0, consumed - ceiling)

        OverTargetRedAlertUiState(
            consumedKcal = consumed,
            targetCalorieCeiling = ceiling,
            surplusKcal = surplus,
            activeBuddy = buddy,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OverTargetRedAlertUiState(isLoading = true),
    )

    fun triggerAlertHaptic() {
        wearHapticController.playRedAlertWaveform()
    }
}
