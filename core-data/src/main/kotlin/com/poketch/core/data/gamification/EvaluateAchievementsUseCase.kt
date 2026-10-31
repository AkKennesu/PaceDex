package com.poketch.core.data.gamification

import com.poketch.core.domain.calculator.CalorieBudgetState
import com.poketch.core.domain.engine.PokemonUnlockEngine
import com.poketch.core.domain.engine.UnlockEvaluationResult
import com.poketch.core.domain.engine.UnlockTriggerContext
import com.poketch.core.domain.model.HabitType
import com.poketch.core.domain.repository.CalorieRepository
import com.poketch.core.domain.repository.HabitRepository
import com.poketch.core.domain.repository.PokemonRepository
import com.poketch.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvaluateAchievementsUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val calorieRepository: CalorieRepository,
    private val habitRepository: HabitRepository,
    private val pokemonRepository: PokemonRepository,
    private val unlockEventDispatcher: UnlockEventDispatcher
) {

    suspend fun evaluate(
        todayIso: String,
        nowMillis: Long = System.currentTimeMillis()
    ): UnlockEvaluationResult {
        val userProfile = userRepository.getUserProfile()
        val calorieLogs = calorieRepository.observeLogsByDate(todayIso).firstOrNull().orEmpty()
        val budgetState = calorieRepository.observeDailyBudgetState(todayIso).firstOrNull()
        val todayWaterMl = habitRepository.observeDailyWaterMl(todayIso).firstOrNull()?.toInt() ?: 0
        val todaySteps = habitRepository.observeDailySteps(todayIso).firstOrNull()?.toInt() ?: 0
        val latestRunMiles = habitRepository.observeDailyRunMiles(todayIso).firstOrNull() ?: 0.0

        val lifetimeWaterLiters = habitRepository.getLifetimeHabitTotal(HabitType.WATER_ML) / 1000.0
        val cumulativeMilesRun = habitRepository.getLifetimeHabitTotal(HabitType.RUN_MILES)
        val allCatalog = pokemonRepository.observeAllPokemon().firstOrNull().orEmpty()

        val isWithinCalorieTarget = budgetState !is CalorieBudgetState.OverTargetWarning

        val context = UnlockTriggerContext(
            starterChoiceId = userProfile?.activeBuddyPokemonId,
            currentLoggingStreakDays = 1, // Base active day
            todayWaterMl = todayWaterMl,
            dailyWaterGoalMl = userProfile?.dailyWaterGoalMl ?: 2500,
            waterGoalStreakDays = if (todayWaterMl >= (userProfile?.dailyWaterGoalMl ?: 2500)) 1 else 0,
            lifetimeWaterLiters = lifetimeWaterLiters,
            todaySteps = todaySteps,
            latestRunDistanceMiles = latestRunMiles,
            totalCompletedWorkouts = if (latestRunMiles > 0.0) 1 else 0,
            cumulativeMilesRun = cumulativeMilesRun,
            todayMealsLoggedCount = calorieLogs.size,
            latestMealProteinG = calorieLogs.firstOrNull()?.proteinG ?: 0,
            isWithinCalorieTargetToday = isWithinCalorieTarget,
            calorieDeficitStreakDays = if (isWithinCalorieTarget && calorieLogs.isNotEmpty()) 1 else 0,
            recoveryDaysAfterSurplus = 0,
            sleepStreakDays = 0
        )

        val result = PokemonUnlockEngine.evaluate(
            catalog = allCatalog,
            context = context,
            nowMillis = nowMillis
        )

        // Persist updated catalog to Room
        if (result.newlyUnlockedEvents.isNotEmpty() || result.progressEvents.isNotEmpty()) {
            pokemonRepository.upsertAllPokemon(result.updatedCatalog)
        }

        // Emit new unlock celebrations
        if (result.newlyUnlockedEvents.isNotEmpty()) {
            unlockEventDispatcher.emitUnlockEvents(result.newlyUnlockedEvents)
        }

        return result
    }
}
