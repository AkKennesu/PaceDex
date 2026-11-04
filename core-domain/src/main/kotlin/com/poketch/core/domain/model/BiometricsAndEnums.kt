package com.poketch.core.domain.model

enum class BiologicalSex(val sexAdjustment: Int, val safeCalorieFloor: Int) {
    MALE(sexAdjustment = 5, safeCalorieFloor = 1500),
    FEMALE(sexAdjustment = -161, safeCalorieFloor = 1200)
}

enum class ActivityLevel(val multiplier: Double, val displayName: String) {
    SEDENTARY(1.2, "Sedentary"),
    LIGHTLY_ACTIVE(1.375, "Lightly Active"),
    MODERATELY_ACTIVE(1.55, "Moderately Active"),
    VERY_ACTIVE(1.725, "Very Active"),
    EXTRA_ACTIVE(1.9, "Extra Active")
}

enum class DeficitTier(val dailyDeficitKcal: Int, val expectedWeeklyLossKg: Double) {
    MILD(250, 0.25),
    STANDARD(500, 0.50),
    AGGRESSIVE(750, 0.75),
    CUSTOM(0, 0.0)
}

enum class DeviceSource {
    WEAR,
    MOBILE
}

enum class HabitType {
    WATER_ML,
    RUN_MILES,
    DAILY_STEPS,
    SLEEP_HOURS
}

enum class MealCategory {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK,
    WATCH_QUICK_ADD
}
