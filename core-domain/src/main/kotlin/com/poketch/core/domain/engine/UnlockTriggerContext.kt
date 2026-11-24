package com.poketch.core.domain.engine

data class UnlockTriggerContext(
    val starterChoiceId: Int? = null, // 1 (Bulbasaur), 4 (Charmander), or 7 (Squirtle)
    val currentLoggingStreakDays: Int = 0,
    val todayWaterMl: Int = 0,
    val dailyWaterGoalMl: Int = 2500,
    val waterGoalStreakDays: Int = 0,
    val lifetimeWaterLiters: Double = 0.0,
    val todaySteps: Int = 0,
    val latestRunDistanceMiles: Double = 0.0,
    val totalCompletedWorkouts: Int = 0,
    val cumulativeMilesRun: Double = 0.0,
    val todayMealsLoggedCount: Int = 0,
    val latestMealProteinG: Int = 0,
    val isWithinCalorieTargetToday: Boolean = true,
    val calorieDeficitStreakDays: Int = 0,
    val recoveryDaysAfterSurplus: Int = 0,
    val sleepStreakDays: Int = 0,
    val todaySleepHours: Double = 0.0,
    val hasSleepSessionToday: Boolean = false,
    val cumulativeSleepSessionsCount: Int = 0,
    val weatherCode: Int? = null,
    val isNightWindow: Boolean = false,
    val activeWeatherBoostType: String? = null
)
