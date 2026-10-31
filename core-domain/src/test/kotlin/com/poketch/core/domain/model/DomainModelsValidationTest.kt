package com.poketch.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class DomainModelsValidationTest {

    private val sampleUnlockRule = UnlockRule(
        ruleCode = "RUN_1_MILE_ONCE",
        category = PokemonCategory.RUN_STEPS,
        description = "Complete your first 1-mile run",
        targetThreshold = 100
    )

    @Test
    fun pokemon_enforcesPokedexNumberBounds_andComputesProgressRatioClamped() {
        // Valid Pokemon within 1..151
        val bulbasaur = Pokemon(
            pokedexNumber = 1,
            name = "Bulbasaur",
            primaryType = "Grass",
            secondaryType = "Poison",
            unlockRule = sampleUnlockRule,
            currentProgress = 50,
            spriteAssetPath = "sprites/001.png"
        )
        assertThat(bulbasaur.pokedexNumber).isEqualTo(1)
        assertThat(bulbasaur.progressRatio).isEqualTo(0.5)

        // Progress over threshold clamps to 1.0
        val overProgress = bulbasaur.copy(currentProgress = 150)
        assertThat(overProgress.progressRatio).isEqualTo(1.0)

        // Progress zero
        val zeroProgress = bulbasaur.copy(currentProgress = 0)
        assertThat(zeroProgress.progressRatio).isEqualTo(0.0)

        // Lower bound violation (< 1)
        val exLower = assertThrows(IllegalArgumentException::class.java) {
            bulbasaur.copy(pokedexNumber = 0)
        }
        assertThat(exLower).hasMessageThat().contains("pokedexNumber")

        // Upper bound violation (> 151)
        val exUpper = assertThrows(IllegalArgumentException::class.java) {
            bulbasaur.copy(pokedexNumber = 152)
        }
        assertThat(exUpper).hasMessageThat().contains("pokedexNumber")

        // Blank name or type
        assertThrows(IllegalArgumentException::class.java) {
            bulbasaur.copy(name = "  ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            bulbasaur.copy(primaryType = "")
        }
        assertThrows(IllegalArgumentException::class.java) {
            bulbasaur.copy(currentProgress = -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            bulbasaur.copy(spriteAssetPath = "")
        }

        // Unlock rule validation
        assertThrows(IllegalArgumentException::class.java) {
            sampleUnlockRule.copy(ruleCode = "")
        }
        assertThrows(IllegalArgumentException::class.java) {
            sampleUnlockRule.copy(description = " ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            sampleUnlockRule.copy(targetThreshold = 0)
        }
    }

    @Test
    fun calorieLog_validatesInputsAndRejectsInvalidValues() {
        val validLog = CalorieLog(
            logId = "550e8400-e29b-41d4-a716-446655440000",
            dateIso = "2026-10-30",
            mealCategory = MealCategory.LUNCH,
            foodDescription = "Grilled Chicken Salad",
            caloriesKcal = 550,
            proteinG = 45,
            carbsG = 20,
            fatG = 15,
            sourceDevice = DeviceSource.MOBILE,
            timestampMillis = 1730300000000L
        )
        assertThat(validLog.caloriesKcal).isEqualTo(550)

        // Blank logId
        assertThrows(IllegalArgumentException::class.java) {
            validLog.copy(logId = "")
        }
        // Blank dateIso
        assertThrows(IllegalArgumentException::class.java) {
            validLog.copy(dateIso = "  ")
        }
        // Non-positive calories
        assertThrows(IllegalArgumentException::class.java) {
            validLog.copy(caloriesKcal = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            validLog.copy(caloriesKcal = -100)
        }
        // Negative macros
        assertThrows(IllegalArgumentException::class.java) {
            validLog.copy(proteinG = -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            validLog.copy(carbsG = -5)
        }
        assertThrows(IllegalArgumentException::class.java) {
            validLog.copy(fatG = -10)
        }
    }

    @Test
    fun habitLog_validatesInputsAndRejectsInvalidValues() {
        val validHabit = HabitLog(
            logId = "123e4567-e89b-12d3-a456-426614174000",
            dateIso = "2026-10-30",
            habitType = HabitType.WATER_ML,
            numericValue = 250.0,
            durationSeconds = 0,
            sourceDevice = DeviceSource.WEAR,
            timestampMillis = 1730300000000L
        )
        assertThat(validHabit.numericValue).isEqualTo(250.0)

        // Blank logId or date
        assertThrows(IllegalArgumentException::class.java) {
            validHabit.copy(logId = " ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            validHabit.copy(dateIso = "")
        }
        // Zero or negative numericValue
        assertThrows(IllegalArgumentException::class.java) {
            validHabit.copy(numericValue = 0.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            validHabit.copy(numericValue = -1.5)
        }
        // Negative durationSeconds
        assertThrows(IllegalArgumentException::class.java) {
            validHabit.copy(durationSeconds = -1)
        }
    }

    @Test
    fun userProfile_validatesBiometricsWaterGoalAndBuddyId() {
        val validProfile = UserProfile(
            id = 1,
            ageYears = 28,
            sex = BiologicalSex.MALE,
            heightCm = 178.0,
            weightKg = 75.0,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            deficitTier = DeficitTier.STANDARD,
            bmrKcal = 1720,
            tdeeKcal = 2666,
            targetCalorieCeiling = 2166,
            dailyWaterGoalMl = 2500,
            activeBuddyPokemonId = 25, // Pikachu
            updatedAtEpochMillis = 1730300000000L
        )
        assertThat(validProfile.activeBuddyPokemonId).isEqualTo(25)

        // Age bounds (13..120)
        assertThrows(IllegalArgumentException::class.java) {
            validProfile.copy(ageYears = 12)
        }
        assertThrows(IllegalArgumentException::class.java) {
            validProfile.copy(ageYears = 121)
        }

        // Height bounds (100.0..250.0)
        assertThrows(IllegalArgumentException::class.java) {
            validProfile.copy(heightCm = 99.9)
        }
        assertThrows(IllegalArgumentException::class.java) {
            validProfile.copy(heightCm = 250.1)
        }

        // Weight bounds (30.0..300.0)
        assertThrows(IllegalArgumentException::class.java) {
            validProfile.copy(weightKg = 29.9)
        }
        assertThrows(IllegalArgumentException::class.java) {
            validProfile.copy(weightKg = 300.1)
        }

        // Water goal (> 0)
        assertThrows(IllegalArgumentException::class.java) {
            validProfile.copy(dailyWaterGoalMl = 0)
        }

        // Buddy Pokemon ID (1..151)
        assertThrows(IllegalArgumentException::class.java) {
            validProfile.copy(activeBuddyPokemonId = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            validProfile.copy(activeBuddyPokemonId = 152)
        }
    }
}
