package com.poketch.core.domain.repository

import com.poketch.core.domain.model.ActivityLevel
import com.poketch.core.domain.model.BiologicalSex
import com.poketch.core.domain.model.DeficitTier
import com.poketch.core.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeUserProfile(): Flow<UserProfile?>
    suspend fun getUserProfile(): UserProfile?
    suspend fun saveUserProfile(
        ageYears: Int,
        sex: BiologicalSex,
        heightCm: Double,
        weightKg: Double,
        activityLevel: ActivityLevel,
        deficitTier: DeficitTier,
        dailyWaterGoalMl: Int = 2500,
        activeBuddyPokemonId: Int = 1
    ) {
        saveUserProfile(
            ageYears = ageYears,
            sex = sex,
            heightCm = heightCm,
            weightKg = weightKg,
            activityLevel = activityLevel,
            deficitTier = deficitTier,
            dailyDeficitKcal = deficitTier.dailyDeficitKcal,
            dailyWaterGoalMl = dailyWaterGoalMl,
            activeBuddyPokemonId = activeBuddyPokemonId
        )
    }

    suspend fun saveUserProfile(
        ageYears: Int,
        sex: BiologicalSex,
        heightCm: Double,
        weightKg: Double,
        activityLevel: ActivityLevel,
        deficitTier: DeficitTier,
        dailyDeficitKcal: Int,
        dailyWaterGoalMl: Int = 2500,
        activeBuddyPokemonId: Int = 1
    ) {
        saveUserProfile(
            ageYears = ageYears,
            sex = sex,
            heightCm = heightCm,
            weightKg = weightKg,
            activityLevel = activityLevel,
            deficitTier = deficitTier,
            dailyWaterGoalMl = dailyWaterGoalMl,
            activeBuddyPokemonId = activeBuddyPokemonId
        )
    }
    suspend fun upsertUserProfile(profile: UserProfile)
    suspend fun updateActiveBuddy(pokemonId: Int)
}
