package com.poketch.core.data.gamification

import com.google.common.truth.Truth.assertThat
import com.poketch.core.domain.model.DailyNutritionSummary
import org.junit.Test

class EvaluateAchievementsUseCaseTest {

    @Test
    fun calculateRecoveryDays_returnsZeroWhenNoSurplusInHistory() {
        val summaries = listOf(
            DailyNutritionSummary("2026-10-28", totalCalories = 1700, 100, 150, 50),
            DailyNutritionSummary("2026-10-29", totalCalories = 1650, 110, 140, 45),
            DailyNutritionSummary("2026-10-30", totalCalories = 1750, 120, 160, 55)
        )
        val recoveryDays = EvaluateAchievementsUseCase.calculateRecoveryDays(
            recentDaily = summaries,
            targetCeilingKcal = 1800,
            todayIso = "2026-10-30"
        )
        assertThat(recoveryDays).isEqualTo(0)
    }

    @Test
    fun calculateRecoveryDays_returnsZeroWhenTodayIsInActiveSurplus() {
        val summaries = listOf(
            DailyNutritionSummary("2026-10-28", totalCalories = 1700, 100, 150, 50),
            DailyNutritionSummary("2026-10-29", totalCalories = 1650, 110, 140, 45),
            DailyNutritionSummary("2026-10-30", totalCalories = 2200, 120, 160, 55)
        )
        val recoveryDays = EvaluateAchievementsUseCase.calculateRecoveryDays(
            recentDaily = summaries,
            targetCeilingKcal = 1800,
            todayIso = "2026-10-30"
        )
        assertThat(recoveryDays).isEqualTo(0)
    }

    @Test
    fun calculateRecoveryDays_countsConsecutiveDaysFollowingLastSurplus() {
        val summaries = listOf(
            DailyNutritionSummary("2026-10-26", totalCalories = 2300, 80, 250, 80), // Surplus Day N
            DailyNutritionSummary("2026-10-27", totalCalories = 1600, 110, 150, 50), // Day N+1 (Recovered 1)
            DailyNutritionSummary("2026-10-28", totalCalories = 1750, 120, 140, 45), // Day N+2 (Recovered 2)
            DailyNutritionSummary("2026-10-29", totalCalories = 1700, 130, 130, 50)  // Day N+3 (Recovered 3)
        )

        val daysOnDay29 = EvaluateAchievementsUseCase.calculateRecoveryDays(
            recentDaily = summaries,
            targetCeilingKcal = 1800,
            todayIso = "2026-10-29"
        )
        assertThat(daysOnDay29).isEqualTo(3)

        val daysOnDay28 = EvaluateAchievementsUseCase.calculateRecoveryDays(
            recentDaily = summaries.take(3),
            targetCeilingKcal = 1800,
            todayIso = "2026-10-28"
        )
        assertThat(daysOnDay28).isEqualTo(2)

        val daysOnDay27 = EvaluateAchievementsUseCase.calculateRecoveryDays(
            recentDaily = summaries.take(2),
            targetCeilingKcal = 1800,
            todayIso = "2026-10-27"
        )
        assertThat(daysOnDay27).isEqualTo(1)
    }

    @Test
    fun calculateRecoveryDays_resetsIfAnotherSurplusOccurs() {
        val summaries = listOf(
            DailyNutritionSummary("2026-10-25", totalCalories = 2300, 80, 250, 80), // Surplus 1
            DailyNutritionSummary("2026-10-26", totalCalories = 1600, 110, 150, 50), // Recovered 1
            DailyNutritionSummary("2026-10-27", totalCalories = 2400, 80, 260, 85),  // Surplus 2 (Reset!)
            DailyNutritionSummary("2026-10-28", totalCalories = 1750, 120, 140, 45)  // Recovered 1 after Surplus 2
        )

        val recoveryDays = EvaluateAchievementsUseCase.calculateRecoveryDays(
            recentDaily = summaries,
            targetCeilingKcal = 1800,
            todayIso = "2026-10-28"
        )
        assertThat(recoveryDays).isEqualTo(1)
    }

    @Test
    fun resolveWeatherBoostType_mapsRainAndStormToWaterElectric() {
        assertThat(EvaluateAchievementsUseCase.resolveWeatherBoostType(weatherCode = 61, isNightWindow = false))
            .isEqualTo("WATER_ELECTRIC")
        assertThat(EvaluateAchievementsUseCase.resolveWeatherBoostType(weatherCode = 80, isNightWindow = false))
            .isEqualTo("WATER_ELECTRIC")
        assertThat(EvaluateAchievementsUseCase.resolveWeatherBoostType(weatherCode = 95, isNightWindow = true))
            .isEqualTo("WATER_ELECTRIC")
    }

    @Test
    fun resolveWeatherBoostType_mapsNightWindowToGhostFairy() {
        assertThat(EvaluateAchievementsUseCase.resolveWeatherBoostType(weatherCode = 0, isNightWindow = true))
            .isEqualTo("GHOST_FAIRY")
        assertThat(EvaluateAchievementsUseCase.resolveWeatherBoostType(weatherCode = null, isNightWindow = true))
            .isEqualTo("GHOST_FAIRY")
    }

    @Test
    fun resolveWeatherBoostType_mapsClearDaytimeToFireGrass() {
        assertThat(EvaluateAchievementsUseCase.resolveWeatherBoostType(weatherCode = 0, isNightWindow = false))
            .isEqualTo("FIRE_GRASS")
        assertThat(EvaluateAchievementsUseCase.resolveWeatherBoostType(weatherCode = 1, isNightWindow = false))
            .isEqualTo("FIRE_GRASS")
        assertThat(EvaluateAchievementsUseCase.resolveWeatherBoostType(weatherCode = null, isNightWindow = false))
            .isEqualTo("FIRE_GRASS")
    }
}
