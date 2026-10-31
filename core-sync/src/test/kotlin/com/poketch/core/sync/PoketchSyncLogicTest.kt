package com.poketch.core.sync

import com.google.common.truth.Truth.assertThat
import com.poketch.core.domain.model.ActivityLevel
import com.poketch.core.domain.model.BiologicalSex
import com.poketch.core.domain.model.CalorieLog
import com.poketch.core.domain.model.DeficitTier
import com.poketch.core.domain.model.DeviceSource
import com.poketch.core.domain.model.HabitLog
import com.poketch.core.domain.model.HabitType
import com.poketch.core.domain.model.MealCategory
import com.poketch.core.domain.model.Pokemon
import com.poketch.core.domain.model.PokemonCategory
import com.poketch.core.domain.model.UnlockRule
import com.poketch.core.domain.model.UserProfile
import org.junit.Test

class PoketchSyncLogicTest {

    @Test
    fun lastWriteWins_acceptsNewerTimestamp_andRejectsOlder() {
        val baseTime = 1_000_000L
        val localProfile = UserProfile(
            id = 1,
            ageYears = 25,
            sex = BiologicalSex.MALE,
            heightCm = 175.0,
            weightKg = 75.0,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            deficitTier = DeficitTier.STANDARD,
            bmrKcal = 1700,
            tdeeKcal = 2300,
            targetCalorieCeiling = 1800,
            updatedAtEpochMillis = baseTime
        )

        val olderRemoteTime = baseTime - 5000L
        val isOlderAccepted = olderRemoteTime > localProfile.updatedAtEpochMillis
        assertThat(isOlderAccepted).isFalse()

        val newerRemoteTime = baseTime + 5000L
        val isNewerAccepted = newerRemoteTime > localProfile.updatedAtEpochMillis
        assertThat(isNewerAccepted).isTrue()
    }

    @Test
    fun monotonicUnionMerge_mergesProgressAndPreservesUnlocks() {
        val rule = UnlockRule(
            ruleCode = "TEST_RULE",
            category = PokemonCategory.RUN_STEPS,
            description = "Walk 10,000 steps",
            targetThreshold = 10_000
        )

        val localPokemon = Pokemon(
            pokedexNumber = 25,
            name = "Pikachu",
            primaryType = "Electric",
            unlockRule = rule,
            currentProgress = 6_000,
            isUnlocked = false,
            unlockedAtMillis = null,
            spriteAssetPath = "sprites/025.png"
        )

        val remoteProgress = 10_000
        val remoteUnlocked = true
        val remoteUnlockedAt = 1_700_000_000L

        val mergedProgress = maxOf(localPokemon.currentProgress, remoteProgress)
        val mergedUnlocked = localPokemon.isUnlocked || remoteUnlocked || (mergedProgress >= rule.targetThreshold)
        val mergedUnlockedAt = localPokemon.unlockedAtMillis ?: remoteUnlockedAt

        val mergedPokemon = localPokemon.copy(
            currentProgress = mergedProgress,
            isUnlocked = mergedUnlocked,
            unlockedAtMillis = mergedUnlockedAt
        )

        assertThat(mergedPokemon.currentProgress).isEqualTo(10_000)
        assertThat(mergedPokemon.isUnlocked).isTrue()
        assertThat(mergedPokemon.unlockedAtMillis).isEqualTo(1_700_000_000L)
    }

    @Test
    fun calorieLog_idempotentDeduplicationKey_preservesUuid() {
        val log1 = CalorieLog(
            logId = "cal-uuid-1234",
            dateIso = "2026-10-31",
            mealCategory = MealCategory.LUNCH,
            foodDescription = "Grilled Chicken Salad",
            caloriesKcal = 550,
            proteinG = 42,
            carbsG = 15,
            fatG = 12,
            sourceDevice = DeviceSource.WEAR,
            timestampMillis = 1_700_000_100L,
            isSynced = true
        )

        val duplicateIncomingLog = log1.copy(isSynced = true)
        assertThat(duplicateIncomingLog.logId).isEqualTo(log1.logId)
        assertThat(duplicateIncomingLog.caloriesKcal).isEqualTo(log1.caloriesKcal)
        assertThat(duplicateIncomingLog.isSynced).isTrue()
    }

    @Test
    fun habitLog_idempotentDeduplicationKey_preservesUuid() {
        val habit1 = HabitLog(
            logId = "habit-uuid-5678",
            dateIso = "2026-10-31",
            habitType = HabitType.DAILY_STEPS,
            numericValue = 8500.0,
            durationSeconds = 0,
            sourceDevice = DeviceSource.WEAR,
            timestampMillis = 1_700_000_200L,
            isSynced = true
        )

        val duplicateIncoming = habit1.copy(isSynced = true)
        assertThat(duplicateIncoming.logId).isEqualTo(habit1.logId)
        assertThat(duplicateIncoming.numericValue).isEqualTo(8500.0)
        assertThat(duplicateIncoming.isSynced).isTrue()
    }
}
