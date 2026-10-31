package com.poketch.wear.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poketch.core.data.gamification.EvaluateAchievementsUseCase
import com.poketch.core.domain.calculator.CalorieBudgetState
import com.poketch.core.domain.model.DeviceSource
import com.poketch.core.domain.model.HabitType
import com.poketch.core.domain.model.Pokemon
import com.poketch.core.domain.repository.CalorieRepository
import com.poketch.core.domain.repository.HabitRepository
import com.poketch.core.domain.repository.PokemonRepository
import com.poketch.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class WatchDashboardUiState(
    val todayIso: String = "",
    val consumedKcal: Int = 0,
    val targetCalorieCeiling: Int = 1800,
    val calorieProgressRatio: Float = 0f,
    val budgetState: CalorieBudgetState = CalorieBudgetState.OnTrack(0, 1800, 1800, 0.0),
    val todayWaterMl: Int = 0,
    val dailyWaterGoalMl: Int = 2500,
    val waterProgressRatio: Float = 0f,
    val activeBuddy: Pokemon? = null,
    val isLoading: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WatchDashboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val calorieRepository: CalorieRepository,
    private val habitRepository: HabitRepository,
    private val pokemonRepository: PokemonRepository,
    private val evaluateAchievementsUseCase: EvaluateAchievementsUseCase,
) : ViewModel() {

    private val todayIso: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private val userProfileFlow = userRepository.observeUserProfile()
    private val totalCaloriesFlow = calorieRepository.observeTotalCaloriesByDate(todayIso)
    private val budgetStateFlow = calorieRepository.observeDailyBudgetState(todayIso)
    private val dailyWaterFlow = habitRepository.observeDailyWaterMl(todayIso)

    private val buddyPokemonFlow = userProfileFlow.flatMapLatest { profile ->
        val buddyId = profile?.activeBuddyPokemonId ?: 1
        pokemonRepository.observePokemonById(buddyId)
    }

    val uiState: StateFlow<WatchDashboardUiState> = combine(
        combine(userProfileFlow, totalCaloriesFlow, budgetStateFlow) { profile, consumed, budget ->
            Triple(profile, consumed, budget)
        },
        combine(dailyWaterFlow, buddyPokemonFlow) { water, buddy ->
            Pair(water, buddy)
        }
    ) { (profile, consumed, budget), (water, buddy) ->
        val ceiling = profile?.targetCalorieCeiling ?: 1800
        val waterGoal = profile?.dailyWaterGoalMl ?: 2500
        val waterInt = water.toInt()
        val calorieRatio = if (ceiling > 0) consumed.toFloat() / ceiling.toFloat() else 0f
        val waterRatio = if (waterGoal > 0) waterInt.toFloat() / waterGoal.toFloat() else 0f

        WatchDashboardUiState(
            todayIso = todayIso,
            consumedKcal = consumed,
            targetCalorieCeiling = ceiling,
            calorieProgressRatio = calorieRatio,
            budgetState = budget,
            todayWaterMl = waterInt,
            dailyWaterGoalMl = waterGoal,
            waterProgressRatio = waterRatio.coerceIn(0f, 1f),
            activeBuddy = buddy,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WatchDashboardUiState(todayIso = todayIso, isLoading = true),
    )

    fun logWaterQuick250() {
        viewModelScope.launch {
            habitRepository.logHabit(
                dateIso = todayIso,
                habitType = HabitType.WATER_ML,
                numericValue = 250.0,
                sourceDevice = DeviceSource.WEAR,
            )
            evaluateAchievementsUseCase.evaluate(todayIso)
        }
    }
}
