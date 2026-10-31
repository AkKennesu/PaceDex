package com.poketch.core.domain.calculator

data class CalorieCalculationSummary(
    val bmrKcal: Int,
    val tdeeKcal: Int,
    val rawDeficitTargetKcal: Int,
    val targetCalorieCeiling: Int,
    val isClampedToSafetyFloor: Boolean
)
