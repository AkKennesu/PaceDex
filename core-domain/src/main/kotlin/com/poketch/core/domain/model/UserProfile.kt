package com.poketch.core.domain.model

data class UserProfile(
    val id: Int = 1,
    val ageYears: Int,
    val sex: BiologicalSex,
    val heightCm: Double,
    val weightKg: Double,
    val activityLevel: ActivityLevel,
    val deficitTier: DeficitTier,
    val bmrKcal: Int,
    val tdeeKcal: Int,
    val targetCalorieCeiling: Int,
    val dailyWaterGoalMl: Int = 2500,
    val activeBuddyPokemonId: Int = 1,
    val updatedAtEpochMillis: Long
) {
    init {
        require(ageYears in 13..120) { "ageYears must be between 13 and 120, got: $ageYears" }
        require(heightCm in 100.0..250.0) { "heightCm must be between 100.0 and 250.0, got: $heightCm" }
        require(weightKg in 30.0..300.0) { "weightKg must be between 30.0 and 300.0, got: $weightKg" }
        require(dailyWaterGoalMl > 0) { "dailyWaterGoalMl must be positive, got: $dailyWaterGoalMl" }
        require(activeBuddyPokemonId in 1..151) { "activeBuddyPokemonId must be between 1 and 151, got: $activeBuddyPokemonId" }
    }
}
