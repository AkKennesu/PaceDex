package com.poketch.wear.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poketch.core.data.gamification.EvaluateAchievementsUseCase
import com.poketch.core.data.gamification.UnlockEventDispatcher
import com.poketch.core.domain.engine.UnlockEvent
import com.poketch.core.domain.model.DeviceSource
import com.poketch.core.domain.model.HabitType
import com.poketch.core.domain.repository.HabitRepository
import com.poketch.core.domain.repository.UserRepository
import com.poketch.core.sync.PoketchSyncManager
import com.poketch.wear.ui.alert.WearHapticController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class QuickHabitUiState(
    val todayWaterMl: Int = 0,
    val dailyWaterGoalMl: Int = 2500,
    val todayRunMiles: Double = 0.0,
    val isLoading: Boolean = false,
)

@HiltViewModel
class QuickHabitActionsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val habitRepository: HabitRepository,
    private val evaluateAchievementsUseCase: EvaluateAchievementsUseCase,
    private val unlockEventDispatcher: UnlockEventDispatcher,
    private val wearHapticController: WearHapticController,
    private val syncManager: PoketchSyncManager,
) : ViewModel() {

    private val todayIso: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    val unlockEvents: SharedFlow<UnlockEvent.NewPokemonUnlocked> = unlockEventDispatcher.unlockEvents

    private val userProfileFlow = userRepository.observeUserProfile()
    private val waterFlow = habitRepository.observeDailyWaterMl(todayIso)
    private val runFlow = habitRepository.observeDailyRunMiles(todayIso)

    val uiState: StateFlow<QuickHabitUiState> = combine(
        userProfileFlow,
        waterFlow,
        runFlow,
    ) { profile, water, run ->
        QuickHabitUiState(
            todayWaterMl = water.toInt(),
            dailyWaterGoalMl = profile?.dailyWaterGoalMl ?: 2500,
            todayRunMiles = run,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = QuickHabitUiState(isLoading = true),
    )

    fun logWater(amountMl: Double) {
        viewModelScope.launch {
            val habit = habitRepository.logHabit(
                dateIso = todayIso,
                habitType = HabitType.WATER_ML,
                numericValue = amountMl,
                sourceDevice = DeviceSource.WEAR,
            )
            evaluateAchievementsUseCase.evaluate(todayIso)
            runCatching {
                syncManager.coalesceAndSyncHabit(habit)
            }
        }
    }

    fun playUnlockFanfare() {
        wearHapticController.playUnlockFanfareHaptic()
    }

    fun setActiveBuddy(pokemonId: Int) {
        viewModelScope.launch {
            userRepository.updateActiveBuddy(pokemonId)
            try {
                syncManager.syncUserProfile()
            } catch (_: Exception) {
                // Ignore sync errors offline
            }
        }
    }
}
