package com.poketch.core.domain.calculator

import com.google.common.truth.Truth.assertThat
import com.poketch.core.domain.model.ActivityLevel
import com.poketch.core.domain.model.BiologicalSex
import com.poketch.core.domain.model.DeficitTier
import org.junit.Assert.assertThrows
import org.junit.Test

class CalorieDeficitCalculatorTest {

    @Test
    fun calculateBmr_maleStandardInputs_returnsExactMifflinStJeorValue() {
        // 26yo Male, 175.0 cm, 78.0 kg -> (10*78) + (6.25*175) - (5*26) + 5 = 780 + 1093.75 - 130 + 5 = 1748.75 -> 1749 kcal
        val bmr = CalorieDeficitCalculator.calculateBmr(
            ageYears = 26,
            sex = BiologicalSex.MALE,
            heightCm = 175.0,
            weightKg = 78.0
        )
        assertThat(bmr).isEqualTo(1749)
    }

    @Test
    fun calculateBmr_femaleStandardInputs_returnsExactMifflinStJeorValue() {
        // 26yo Female, 165.0 cm, 60.0 kg -> (10*60) + (6.25*165) - (5*26) - 161 = 600 + 1031.25 - 130 - 161 = 1340.25 -> 1340 kcal
        val bmr = CalorieDeficitCalculator.calculateBmr(
            ageYears = 26,
            sex = BiologicalSex.FEMALE,
            heightCm = 165.0,
            weightKg = 60.0
        )
        assertThat(bmr).isEqualTo(1340)
    }

    @Test
    fun calculateTdee_allActivityMultipliers_scalesCorrectly() {
        val bmr = 2000
        assertThat(CalorieDeficitCalculator.calculateTdee(bmr, ActivityLevel.SEDENTARY)).isEqualTo(2400) // 2000 * 1.2
        assertThat(CalorieDeficitCalculator.calculateTdee(bmr, ActivityLevel.LIGHTLY_ACTIVE)).isEqualTo(2750) // 2000 * 1.375
        assertThat(CalorieDeficitCalculator.calculateTdee(bmr, ActivityLevel.MODERATELY_ACTIVE)).isEqualTo(3100) // 2000 * 1.55
        assertThat(CalorieDeficitCalculator.calculateTdee(bmr, ActivityLevel.VERY_ACTIVE)).isEqualTo(3450) // 2000 * 1.725
        assertThat(CalorieDeficitCalculator.calculateTdee(bmr, ActivityLevel.EXTRA_ACTIVE)).isEqualTo(3800) // 2000 * 1.9
    }

    @Test
    fun calculateTargetCeiling_allDeficitTiers_subtractsExpectedDeficit() {
        // Male: 25yo, 180cm, 85kg -> BMR = (10*85) + (6.25*180) - (5*25) + 5 = 850 + 1125 - 125 + 5 = 1855
        // TDEE (Moderately active, 1.55) = (1855 * 1.55).roundToInt() = 2875 kcal
        val age = 25
        val sex = BiologicalSex.MALE
        val height = 180.0
        val weight = 85.0
        val activity = ActivityLevel.MODERATELY_ACTIVE

        val mildSummary = CalorieDeficitCalculator.calculateTargetCeiling(
            ageYears = age, sex = sex, heightCm = height, weightKg = weight,
            activityLevel = activity, deficitTier = DeficitTier.MILD
        )
        assertThat(mildSummary.bmrKcal).isEqualTo(1855)
        assertThat(mildSummary.tdeeKcal).isEqualTo(2875)
        assertThat(mildSummary.rawDeficitTargetKcal).isEqualTo(2625) // 2875 - 250
        assertThat(mildSummary.targetCalorieCeiling).isEqualTo(2625)
        assertThat(mildSummary.isClampedToSafetyFloor).isFalse()

        val standardSummary = CalorieDeficitCalculator.calculateTargetCeiling(
            ageYears = age, sex = sex, heightCm = height, weightKg = weight,
            activityLevel = activity, deficitTier = DeficitTier.STANDARD
        )
        assertThat(standardSummary.rawDeficitTargetKcal).isEqualTo(2375) // 2875 - 500
        assertThat(standardSummary.targetCalorieCeiling).isEqualTo(2375)
        assertThat(standardSummary.isClampedToSafetyFloor).isFalse()

        val aggressiveSummary = CalorieDeficitCalculator.calculateTargetCeiling(
            ageYears = age, sex = sex, heightCm = height, weightKg = weight,
            activityLevel = activity, deficitTier = DeficitTier.AGGRESSIVE
        )
        assertThat(aggressiveSummary.rawDeficitTargetKcal).isEqualTo(2125) // 2875 - 750
        assertThat(aggressiveSummary.targetCalorieCeiling).isEqualTo(2125)
        assertThat(aggressiveSummary.isClampedToSafetyFloor).isFalse()
    }

    @Test
    fun calculateTargetCeiling_whenBelowMaleSafetyFloor_clampsTo1500Kcal() {
        // Low weight sedentary male: 30yo, 150cm, 40kg -> BMR = (10*40) + (6.25*150) - (5*30) + 5 = 400 + 937.5 - 150 + 5 = 1192.5 -> 1193 kcal
        // TDEE = 1193 * 1.2 = 1431.6 -> 1432 kcal
        // Raw deficit (-750) = 1432 - 750 = 682 kcal < 1500 safe floor
        val summary = CalorieDeficitCalculator.calculateTargetCeiling(
            ageYears = 30,
            sex = BiologicalSex.MALE,
            heightCm = 150.0,
            weightKg = 40.0,
            activityLevel = ActivityLevel.SEDENTARY,
            deficitTier = DeficitTier.AGGRESSIVE
        )
        assertThat(summary.rawDeficitTargetKcal).isEqualTo(682)
        assertThat(summary.targetCalorieCeiling).isEqualTo(1500)
        assertThat(summary.isClampedToSafetyFloor).isTrue()
    }

    @Test
    fun calculateTargetCeiling_whenBelowFemaleSafetyFloor_clampsTo1200Kcal() {
        // Low weight sedentary female: 30yo, 150cm, 40kg -> BMR = (10*40) + (6.25*150) - (5*30) - 161 = 400 + 937.5 - 150 - 161 = 1026.5 -> 1027 kcal
        // TDEE = 1027 * 1.2 = 1232.4 -> 1232 kcal
        // Raw deficit (-750) = 1232 - 750 = 482 kcal < 1200 safe floor
        val summary = CalorieDeficitCalculator.calculateTargetCeiling(
            ageYears = 30,
            sex = BiologicalSex.FEMALE,
            heightCm = 150.0,
            weightKg = 40.0,
            activityLevel = ActivityLevel.SEDENTARY,
            deficitTier = DeficitTier.AGGRESSIVE
        )
        assertThat(summary.rawDeficitTargetKcal).isEqualTo(482)
        assertThat(summary.targetCalorieCeiling).isEqualTo(1200)
        assertThat(summary.isClampedToSafetyFloor).isTrue()
    }

    @Test
    fun evaluateBudgetStatus_below90Percent_returnsOnTrack() {
        val ceiling = 2000
        val zeroConsumed = CalorieDeficitCalculator.evaluateBudgetStatus(0, ceiling)
        assertThat(zeroConsumed).isInstanceOf(CalorieBudgetState.OnTrack::class.java)
        val onTrackZero = zeroConsumed as CalorieBudgetState.OnTrack
        assertThat(onTrackZero.consumedKcal).isEqualTo(0)
        assertThat(onTrackZero.remainingKcal).isEqualTo(2000)
        assertThat(onTrackZero.consumptionRatio).isEqualTo(0.0)

        val justBelow90 = CalorieDeficitCalculator.evaluateBudgetStatus(1799, ceiling)
        assertThat(justBelow90).isInstanceOf(CalorieBudgetState.OnTrack::class.java)
        val onTrackBelow = justBelow90 as CalorieBudgetState.OnTrack
        assertThat(onTrackBelow.consumedKcal).isEqualTo(1799)
        assertThat(onTrackBelow.remainingKcal).isEqualTo(201)
        assertThat(onTrackBelow.consumptionRatio).isLessThan(0.90)
    }

    @Test
    fun evaluateBudgetStatus_between90And100Percent_returnsApproachingCeiling() {
        val ceiling = 2000
        val exact90 = CalorieDeficitCalculator.evaluateBudgetStatus(1800, ceiling)
        assertThat(exact90).isInstanceOf(CalorieBudgetState.ApproachingCeiling::class.java)
        val approaching90 = exact90 as CalorieBudgetState.ApproachingCeiling
        assertThat(approaching90.consumedKcal).isEqualTo(1800)
        assertThat(approaching90.remainingKcal).isEqualTo(200)
        assertThat(approaching90.consumptionRatio).isEqualTo(0.90)

        val exact100 = CalorieDeficitCalculator.evaluateBudgetStatus(2000, ceiling)
        assertThat(exact100).isInstanceOf(CalorieBudgetState.ApproachingCeiling::class.java)
        val approaching100 = exact100 as CalorieBudgetState.ApproachingCeiling
        assertThat(approaching100.consumedKcal).isEqualTo(2000)
        assertThat(approaching100.remainingKcal).isEqualTo(0)
        assertThat(approaching100.consumptionRatio).isEqualTo(1.0)
    }

    @Test
    fun evaluateBudgetStatus_above100Percent_returnsOverTargetWarningWithExactSurplusAndOffsets() {
        val ceiling = 2000
        val overByOne = CalorieDeficitCalculator.evaluateBudgetStatus(2001, ceiling)
        assertThat(overByOne).isInstanceOf(CalorieBudgetState.OverTargetWarning::class.java)
        val warningOne = overByOne as CalorieBudgetState.OverTargetWarning
        assertThat(warningOne.surplusKcal).isEqualTo(1)

        val overBy250 = CalorieDeficitCalculator.evaluateBudgetStatus(2250, ceiling)
        assertThat(overBy250).isInstanceOf(CalorieBudgetState.OverTargetWarning::class.java)
        val warning250 = overBy250 as CalorieBudgetState.OverTargetWarning
        assertThat(warning250.consumedKcal).isEqualTo(2250)
        assertThat(warning250.targetCeilingKcal).isEqualTo(2000)
        assertThat(warning250.surplusKcal).isEqualTo(250)
        // 250 / 0.04 = 6250 steps
        assertThat(warning250.suggestedOffsetSteps).isEqualTo(6250)
        // 250 / 100.0 = 2.5 miles
        assertThat(warning250.suggestedOffsetRunMiles).isEqualTo(2.5)
        assertThat(warning250.consumptionRatio).isEqualTo(1.125)
    }

    @Test
    fun calculator_invalidBiometricInputs_throwsIllegalArgumentException() {
        // Age validations
        assertThrows(IllegalArgumentException::class.java) {
            CalorieDeficitCalculator.calculateBmr(12, BiologicalSex.MALE, 175.0, 70.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            CalorieDeficitCalculator.calculateBmr(121, BiologicalSex.MALE, 175.0, 70.0)
        }

        // Height validations
        assertThrows(IllegalArgumentException::class.java) {
            CalorieDeficitCalculator.calculateBmr(25, BiologicalSex.MALE, 99.9, 70.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            CalorieDeficitCalculator.calculateBmr(25, BiologicalSex.MALE, 250.1, 70.0)
        }

        // Weight validations
        assertThrows(IllegalArgumentException::class.java) {
            CalorieDeficitCalculator.calculateBmr(25, BiologicalSex.MALE, 175.0, 29.9)
        }
        assertThrows(IllegalArgumentException::class.java) {
            CalorieDeficitCalculator.calculateBmr(25, BiologicalSex.MALE, 175.0, 300.1)
        }

        // BMR validation for TDEE
        assertThrows(IllegalArgumentException::class.java) {
            CalorieDeficitCalculator.calculateTdee(0, ActivityLevel.SEDENTARY)
        }

        // Budget evaluations invalid arguments
        assertThrows(IllegalArgumentException::class.java) {
            CalorieDeficitCalculator.evaluateBudgetStatus(-1, 2000)
        }
        assertThrows(IllegalArgumentException::class.java) {
            CalorieDeficitCalculator.evaluateBudgetStatus(500, 0)
        }
    }
}
