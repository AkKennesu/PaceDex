package com.poketch.mobile.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poketch.core.data.gamification.EvaluateAchievementsUseCase
import com.poketch.core.domain.calculator.CalorieBudgetState
import com.poketch.core.domain.model.ActivityLevel
import com.poketch.core.domain.model.BiologicalSex
import com.poketch.core.domain.model.DeficitTier
import com.poketch.core.domain.model.DeviceSource
import com.poketch.core.domain.model.HabitType
import com.poketch.core.domain.model.Pokemon
import com.poketch.core.domain.repository.CalorieRepository
import com.poketch.core.domain.repository.HabitRepository
import com.poketch.core.domain.repository.PokemonRepository
import com.poketch.core.domain.repository.UserRepository
import com.poketch.core.sync.PoketchSyncManager
import com.poketch.mobile.health.HealthConnectManager
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

data class SnorlaxRecoveryState(
    val isUnlocked: Boolean = false,
    val recoveryDays: Int = 0,
    val isCurrentlyInSurplus: Boolean = false,
    val surplusAmountKcal: Int = 0,
    val progressFraction: Float = 0f
)

data class CommandCenterUiState(
    val todayIso: String = "",
    val tdeeKcal: Int = 2000,
    val targetCeilingKcal: Int = 1800,
    val consumedKcal: Int = 0,
    val budgetState: CalorieBudgetState = CalorieBudgetState.OnTrack(0, 1800, 1800, 0.0),
    val activeBuddy: Pokemon? = null,
    val snorlaxRecovery: SnorlaxRecoveryState = SnorlaxRecoveryState(),
    val todayWaterMl: Int = 0,
    val dailyWaterGoalMl: Int = 2500,
    val todaySteps: Int = 0,
    val latestRunMiles: Double = 0.0,
    val isLoading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CommandCenterViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val calorieRepository: CalorieRepository,
    private val habitRepository: HabitRepository,
    private val pokemonRepository: PokemonRepository,
    private val evaluateAchievementsUseCase: EvaluateAchievementsUseCase,
    private val healthConnectManager: HealthConnectManager,
    private val syncManager: PoketchSyncManager
) : ViewModel() {

    private val todayIso: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private val userProfileFlow = userRepository.observeUserProfile()
    private val totalCaloriesFlow = calorieRepository.observeTotalCaloriesByDate(todayIso)
    private val budgetStateFlow = calorieRepository.observeDailyBudgetState(todayIso)
    private val dailyWaterFlow = habitRepository.observeDailyWaterMl(todayIso)
    private val dailyStepsFlow = habitRepository.observeDailySteps(todayIso)
    private val dailyRunFlow = habitRepository.observeDailyRunMiles(todayIso)
    private val snorlaxFlow = pokemonRepository.observePokemonById(143)
    private val recentNutritionFlow = calorieRepository.observeRecentDailyNutrition(14)

    private val activeBuddyFlow = userProfileFlow.flatMapLatest { profile ->
        val buddyId = profile?.activeBuddyPokemonId ?: 1
        pokemonRepository.observePokemonById(buddyId)
    }

    private val calorieAndProfileFlow = combine(
        userProfileFlow,
        totalCaloriesFlow,
        budgetStateFlow,
        recentNutritionFlow,
        snorlaxFlow
    ) { profile, consumed, budgetState, recentNutrition, snorlax ->
        val targetCeiling = profile?.targetCalorieCeiling ?: 1800
        val isSurplus = budgetState is CalorieBudgetState.OverTargetWarning
        val surplusKcal = if (budgetState is CalorieBudgetState.OverTargetWarning) budgetState.surplusKcal else 0

        val recoveryDays = EvaluateAchievementsUseCase.calculateRecoveryDays(
            recentDaily = recentNutrition,
            targetCeilingKcal = targetCeiling,
            todayIso = todayIso
        )

        val snorlaxRecovery = SnorlaxRecoveryState(
            isUnlocked = snorlax?.isUnlocked == true,
            recoveryDays = if (snorlax?.isUnlocked == true) 3 else recoveryDays,
            isCurrentlyInSurplus = isSurplus,
            surplusAmountKcal = surplusKcal,
            progressFraction = if (snorlax?.isUnlocked == true) 1.0f else (recoveryDays / 3f).coerceIn(0f, 1f)
        )

        Tuple5(profile, consumed, budgetState, snorlaxRecovery)
    }

    private val habitFlow = combine(
        dailyWaterFlow,
        dailyStepsFlow,
        dailyRunFlow
    ) { water, steps, run ->
        Triple(water, steps, run)
    }

    val uiState: StateFlow<CommandCenterUiState> = combine(
        calorieAndProfileFlow,
        habitFlow,
        activeBuddyFlow
    ) { (profile, consumed, budgetState, snorlaxRecovery), (waterMl, steps, runMiles), buddy ->
        CommandCenterUiState(
            todayIso = todayIso,
            tdeeKcal = profile?.tdeeKcal ?: 2200,
            targetCeilingKcal = profile?.targetCalorieCeiling ?: 1800,
            consumedKcal = consumed,
            budgetState = budgetState,
            activeBuddy = buddy,
            snorlaxRecovery = snorlaxRecovery,
            todayWaterMl = waterMl.toInt(),
            dailyWaterGoalMl = profile?.dailyWaterGoalMl ?: 2500,
            todaySteps = steps.toInt(),
            latestRunMiles = runMiles,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CommandCenterUiState(todayIso = todayIso, isLoading = true)
    )

    init {
        viewModelScope.launch {
            if (userRepository.getUserProfile() == null) {
                // Initialize default profile on first launch: Male 26, 175cm, 75kg, Moderately Active, Standard Deficit
                userRepository.saveUserProfile(
                    ageYears = 26,
                    sex = BiologicalSex.MALE,
                    heightCm = 175.0,
                    weightKg = 75.0,
                    activityLevel = ActivityLevel.MODERATELY_ACTIVE,
                    deficitTier = DeficitTier.STANDARD,
                    dailyWaterGoalMl = 2500,
                    activeBuddyPokemonId = 1 // Bulbasaur
                )
            }
            evaluateAchievementsUseCase.evaluate(todayIso)
        }
    }

    fun quickAddWater(amountMl: Double = 250.0) {
        viewModelScope.launch {
            val habit = habitRepository.logHabit(
                dateIso = todayIso,
                habitType = HabitType.WATER_ML,
                numericValue = amountMl,
                sourceDevice = DeviceSource.MOBILE
            )
            evaluateAchievementsUseCase.evaluate(todayIso)
            runCatching {
                syncManager.coalesceAndSyncHabit(habit)
            }
        }
    }

    fun quickLogRun(miles: Double = 1.0) {
        viewModelScope.launch {
            val habit = habitRepository.logHabit(
                dateIso = todayIso,
                habitType = HabitType.RUN_MILES,
                numericValue = miles,
                durationSeconds = 600, // 10 minutes default
                sourceDevice = DeviceSource.MOBILE
            )
            evaluateAchievementsUseCase.evaluate(todayIso)
            runCatching {
                syncManager.coalesceAndSyncHabit(habit)
            }
        }
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

    fun syncHealthConnect() {
        viewModelScope.launch {
            healthConnectManager.syncPastDays(7)
        }
    }
}

private data class Tuple5<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
