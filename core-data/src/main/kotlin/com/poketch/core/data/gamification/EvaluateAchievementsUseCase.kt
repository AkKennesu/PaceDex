package com.poketch.core.data.gamification

import com.poketch.core.data.repository.OfflineWeatherRepository
import com.poketch.core.domain.calculator.CalorieBudgetState
import com.poketch.core.domain.engine.PokemonUnlockEngine
import com.poketch.core.domain.engine.UnlockEvaluationResult
import com.poketch.core.domain.engine.UnlockTriggerContext
import com.poketch.core.domain.model.DailyNutritionSummary
import com.poketch.core.domain.model.HabitType
import com.poketch.core.domain.repository.CalorieRepository
import com.poketch.core.domain.repository.HabitRepository
import com.poketch.core.domain.repository.PokemonRepository
import com.poketch.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvaluateAchievementsUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val calorieRepository: CalorieRepository,
    private val habitRepository: HabitRepository,
    private val pokemonRepository: PokemonRepository,
    private val unlockEventDispatcher: UnlockEventDispatcher,
    private val offlineWeatherRepository: OfflineWeatherRepository? = null
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
        val todaySleepHours = habitRepository.observeDailySleepHours(todayIso).firstOrNull() ?: 0.0

        val lifetimeWaterLiters = habitRepository.getLifetimeHabitTotal(HabitType.WATER_ML) / 1000.0
        val cumulativeMilesRun = habitRepository.getLifetimeHabitTotal(HabitType.RUN_MILES)
        val allCatalog = pokemonRepository.observeAllPokemon().firstOrNull().orEmpty()

        val isWithinCalorieTarget = budgetState !is CalorieBudgetState.OverTargetWarning

        val targetCeiling = userProfile?.targetCalorieCeiling ?: 2000
        val recentNutrition = calorieRepository.getRecentDailyNutrition(14)
        val recoveryDays = calculateRecoveryDays(recentNutrition, targetCeiling, todayIso)

        val weatherCode = runCatching { offlineWeatherRepository?.getLatestCachedWeatherWmoCode() }.getOrNull()
        val zoneId = runCatching { ZoneId.of(offlineWeatherRepository?.getResolvedTimeZoneId().orEmpty()) }.getOrElse { ZoneId.systemDefault() }
        val localHour = LocalDateTime.now(zoneId).hour
        val isNight = localHour >= 20 || localHour < 5
        val activeWeatherBoostType = resolveWeatherBoostType(weatherCode, isNight)

        val context = UnlockTriggerContext(
            starterChoiceId = userProfile?.activeBuddyPokemonId ?: 1,
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
            recoveryDaysAfterSurplus = recoveryDays,
            sleepStreakDays = if (todaySleepHours >= 7.0) 1 else if (todaySleepHours > 0.0) 1 else 0,
            todaySleepHours = todaySleepHours,
            hasSleepSessionToday = todaySleepHours > 0.0,
            weatherCode = weatherCode,
            isNightWindow = isNight,
            activeWeatherBoostType = activeWeatherBoostType,
            weatherBoostMultiplier = 1.5
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

    companion object {
        fun resolveWeatherBoostType(weatherCode: Int?, isNightWindow: Boolean): String {
            return when {
                weatherCode != null && (weatherCode in 51..67 || weatherCode in 80..82 || weatherCode in 95..99) -> "WATER_ELECTRIC"
                isNightWindow -> "GHOST_FAIRY"
                else -> "FIRE_GRASS"
            }
        }

        fun calculateRecoveryDays(
            recentDaily: List<DailyNutritionSummary>,
            targetCeilingKcal: Int,
            todayIso: String
        ): Int {
            val sorted = recentDaily.filter { it.totalCalories > 0 }.sortedBy { it.dateIso }
            if (sorted.isEmpty()) return 0

            val todayItem = sorted.find { it.dateIso == todayIso }
            if (todayItem != null && todayItem.totalCalories > targetCeilingKcal) {
                return 0 // Today is in active surplus
            }

            val lastSurplusIndex = sorted.indexOfLast { it.totalCalories > targetCeilingKcal }
            if (lastSurplusIndex == -1) return 0 // No past surplus day recorded

            val daysAfterSurplus = sorted.subList(lastSurplusIndex + 1, sorted.size)
            var consecutiveRecoveryDays = 0
            for (day in daysAfterSurplus) {
                if (day.totalCalories in 1..targetCeilingKcal) {
                    consecutiveRecoveryDays++
                } else {
                    break
                }
            }
            return consecutiveRecoveryDays
        }
    }
}
