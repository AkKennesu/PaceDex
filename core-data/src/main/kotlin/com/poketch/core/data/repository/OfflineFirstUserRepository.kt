package com.poketch.core.data.repository

import com.poketch.core.data.local.dao.UserDao
import com.poketch.core.data.local.entity.toDomain
import com.poketch.core.data.local.entity.toEntity
import com.poketch.core.domain.calculator.CalorieDeficitCalculator
import com.poketch.core.domain.model.ActivityLevel
import com.poketch.core.domain.model.BiologicalSex
import com.poketch.core.domain.model.DeficitTier
import com.poketch.core.domain.model.UserProfile
import com.poketch.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstUserRepository @Inject constructor(
    private val userDao: UserDao
) : UserRepository {

    override fun observeUserProfile(): Flow<UserProfile?> {
        return userDao.observeUserProfile().map { it?.toDomain() }
    }

    override suspend fun getUserProfile(): UserProfile? {
        return userDao.getUserProfileOnce()?.toDomain()
    }

    override suspend fun saveUserProfile(
        ageYears: Int,
        sex: BiologicalSex,
        heightCm: Double,
        weightKg: Double,
        activityLevel: ActivityLevel,
        deficitTier: DeficitTier,
        dailyWaterGoalMl: Int,
        activeBuddyPokemonId: Int
    ) {
        val summary = CalorieDeficitCalculator.calculateTargetCeiling(
            ageYears = ageYears,
            sex = sex,
            heightCm = heightCm,
            weightKg = weightKg,
            activityLevel = activityLevel,
            deficitTier = deficitTier
        )

        val updatedProfile = UserProfile(
            id = 1,
            ageYears = ageYears,
            sex = sex,
            heightCm = heightCm,
            weightKg = weightKg,
            activityLevel = activityLevel,
            deficitTier = deficitTier,
            bmrKcal = summary.bmrKcal,
            tdeeKcal = summary.tdeeKcal,
            targetCalorieCeiling = summary.targetCalorieCeiling,
            dailyWaterGoalMl = dailyWaterGoalMl,
            activeBuddyPokemonId = activeBuddyPokemonId,
            updatedAtEpochMillis = System.currentTimeMillis()
        )

        userDao.upsertUserProfile(updatedProfile.toEntity())
    }

    override suspend fun upsertUserProfile(profile: UserProfile) {
        userDao.upsertUserProfile(profile.toEntity())
    }

    override suspend fun updateActiveBuddy(pokemonId: Int) {
        require(pokemonId in 1..151) { "Buddy Pokemon must be between 1 and 151, got: $pokemonId" }
        val existing = userDao.getUserProfileOnce()
        if (existing != null) {
            userDao.updateActiveBuddy(pokemonId, System.currentTimeMillis())
        } else {
            val defaultProfile = UserProfile(
                id = 1,
                ageYears = 25,
                sex = BiologicalSex.MALE,
                heightCm = 175.0,
                weightKg = 70.0,
                activityLevel = ActivityLevel.MODERATELY_ACTIVE,
                deficitTier = DeficitTier.STANDARD,
                bmrKcal = 1650,
                tdeeKcal = 2300,
                targetCalorieCeiling = 1800,
                dailyWaterGoalMl = 2500,
                activeBuddyPokemonId = pokemonId,
                updatedAtEpochMillis = System.currentTimeMillis()
            )
            userDao.upsertUserProfile(defaultProfile.toEntity())
        }
    }
}
