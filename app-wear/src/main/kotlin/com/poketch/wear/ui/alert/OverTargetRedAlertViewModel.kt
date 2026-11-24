package com.poketch.wear.ui.alert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poketch.core.domain.model.Pokemon
import com.poketch.core.domain.model.RecoveryQuest
import com.poketch.core.domain.repository.CalorieRepository
import com.poketch.core.domain.repository.HabitRepository
import com.poketch.core.domain.repository.PokemonRepository
import com.poketch.core.domain.repository.UserRepository
import com.poketch.core.sync.PoketchSyncManager
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
    val recoveryQuest: RecoveryQuest? = null,
    val isLoading: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OverTargetRedAlertViewModel @Inject constructor(
    userRepository: UserRepository,
    calorieRepository: CalorieRepository,
    pokemonRepository: PokemonRepository,
    habitRepository: HabitRepository,
    syncManager: PoketchSyncManager,
    private val wearHapticController: WearHapticController,
) : ViewModel() {

    private val todayIso: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private val userProfileFlow = userRepository.observeUserProfile()
    private val totalCaloriesFlow = calorieRepository.observeTotalCaloriesByDate(todayIso)
    private val stepsFlow = habitRepository.observeDailySteps(todayIso)
    private val recoveryQuestFlow = syncManager.recoveryQuestFlow

    private val buddyFlow = userProfileFlow.flatMapLatest { profile ->
        val buddyId = profile?.activeBuddyPokemonId ?: 1
        pokemonRepository.observePokemonById(buddyId)
    }

    val uiState: StateFlow<OverTargetRedAlertUiState> = combine(
        userProfileFlow,
        totalCaloriesFlow,
        buddyFlow,
        stepsFlow,
        recoveryQuestFlow
    ) { profile, consumed, buddy, steps, syncedQuest ->
        val ceiling = profile?.targetCalorieCeiling ?: 1800
        val surplus = maxOf(0, consumed - ceiling)
        val currentSteps = steps.toInt()

        val quest = if (surplus > 0) {
            if (syncedQuest != null && syncedQuest.dateIso == todayIso) {
                val offset = maxOf(0, currentSteps - syncedQuest.startingStepsSnapshot)
                syncedQuest.copy(
                    surplusKcal = surplus,
                    targetOffsetSteps = RecoveryQuest.calculateTargetOffsetSteps(surplus),
                    currentOffsetSteps = offset,
                    isCompleted = offset >= syncedQuest.targetOffsetSteps
                )
            } else {
                RecoveryQuest.create(
                    dateIso = todayIso,
                    surplusKcal = surplus,
                    startingStepsSnapshot = currentSteps,
                    currentTotalSteps = currentSteps
                )
            }
        } else null

        OverTargetRedAlertUiState(
            consumedKcal = consumed,
            targetCalorieCeiling = ceiling,
            surplusKcal = surplus,
            activeBuddy = buddy,
            recoveryQuest = quest,
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

    fun triggerCelebrationHaptic() {
        wearHapticController.playUnlockFanfareHaptic()
    }
}
