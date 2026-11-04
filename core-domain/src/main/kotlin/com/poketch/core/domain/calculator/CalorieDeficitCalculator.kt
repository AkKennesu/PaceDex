package com.poketch.core.domain.calculator

import com.poketch.core.domain.model.ActivityLevel
import com.poketch.core.domain.model.BiologicalSex
import com.poketch.core.domain.model.DeficitTier
import kotlin.math.round
import kotlin.math.roundToInt

object CalorieDeficitCalculator {

    fun calculateBmr(ageYears: Int, sex: BiologicalSex, heightCm: Double, weightKg: Double): Int {
        require(ageYears in 13..120) { "ageYears must be between 13 and 120, got: $ageYears" }
        require(heightCm in 100.0..250.0) { "heightCm must be between 100.0 and 250.0, got: $heightCm" }
        require(weightKg in 30.0..300.0) { "weightKg must be between 30.0 and 300.0, got: $weightKg" }

        val bmr = (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * ageYears) + sex.sexAdjustment
        return bmr.roundToInt()
    }

    fun calculateTdee(bmrKcal: Int, activityLevel: ActivityLevel): Int {
        require(bmrKcal > 0) { "bmrKcal must be positive, got: $bmrKcal" }
        return (bmrKcal * activityLevel.multiplier).roundToInt()
    }

    fun calculateTargetCeiling(
        ageYears: Int,
        sex: BiologicalSex,
        heightCm: Double,
        weightKg: Double,
        activityLevel: ActivityLevel,
        dailyDeficitKcal: Int
    ): CalorieCalculationSummary {
        val bmrKcal = calculateBmr(ageYears, sex, heightCm, weightKg)
        val tdeeKcal = calculateTdee(bmrKcal, activityLevel)
        val rawDeficitTargetKcal = tdeeKcal - dailyDeficitKcal
        val targetCalorieCeiling = maxOf(sex.safeCalorieFloor, rawDeficitTargetKcal)
        val isClampedToSafetyFloor = rawDeficitTargetKcal < sex.safeCalorieFloor

        return CalorieCalculationSummary(
            bmrKcal = bmrKcal,
            tdeeKcal = tdeeKcal,
            rawDeficitTargetKcal = rawDeficitTargetKcal,
            targetCalorieCeiling = targetCalorieCeiling,
            isClampedToSafetyFloor = isClampedToSafetyFloor
        )
    }

    fun calculateTargetCeiling(
        ageYears: Int,
        sex: BiologicalSex,
        heightCm: Double,
        weightKg: Double,
        activityLevel: ActivityLevel,
        deficitTier: DeficitTier
    ): CalorieCalculationSummary = calculateTargetCeiling(
        ageYears = ageYears,
        sex = sex,
        heightCm = heightCm,
        weightKg = weightKg,
        activityLevel = activityLevel,
        dailyDeficitKcal = deficitTier.dailyDeficitKcal
    )

    fun evaluateBudgetStatus(consumedKcal: Int, targetCeilingKcal: Int): CalorieBudgetState {
        require(consumedKcal >= 0) { "consumedKcal must be non-negative, got: $consumedKcal" }
        require(targetCeilingKcal > 0) { "targetCeilingKcal must be positive, got: $targetCeilingKcal" }

        val ratio = consumedKcal.toDouble() / targetCeilingKcal.toDouble()

        return if (consumedKcal > targetCeilingKcal) {
            val surplusKcal = consumedKcal - targetCeilingKcal
            val suggestedOffsetSteps = (surplusKcal / 0.04).roundToInt()
            val suggestedOffsetRunMiles = round((surplusKcal / 100.0) * 10.0) / 10.0

            CalorieBudgetState.OverTargetWarning(
                consumedKcal = consumedKcal,
                targetCeilingKcal = targetCeilingKcal,
                surplusKcal = surplusKcal,
                suggestedOffsetSteps = suggestedOffsetSteps,
                suggestedOffsetRunMiles = suggestedOffsetRunMiles,
                consumptionRatio = ratio
            )
        } else if (ratio >= 0.90) {
            CalorieBudgetState.ApproachingCeiling(
                consumedKcal = consumedKcal,
                targetCeilingKcal = targetCeilingKcal,
                remainingKcal = targetCeilingKcal - consumedKcal,
                consumptionRatio = ratio
            )
        } else {
            CalorieBudgetState.OnTrack(
                consumedKcal = consumedKcal,
                targetCeilingKcal = targetCeilingKcal,
                remainingKcal = targetCeilingKcal - consumedKcal,
                consumptionRatio = ratio
            )
        }
    }
}
