package com.poketch.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.poketch.core.domain.model.ActivityLevel
import com.poketch.core.domain.model.BiologicalSex
import com.poketch.core.domain.model.DeficitTier
import com.poketch.core.domain.model.UserProfile

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val ageYears: Int,
    val sex: String, // "MALE" | "FEMALE"
    val heightCm: Double,
    val weightKg: Double,
    val activityLevel: String,
    val deficitTier: String,
    val bmrKcal: Int,
    val tdeeKcal: Int,
    val dailyDeficitGoalKcal: Int,
    val targetCalorieCeiling: Int,
    val dailyWaterGoalMl: Int = 2500,
    val activeBuddyPokemonId: Int = 1,
    val updatedAtEpochMillis: Long
)

fun UserProfileEntity.toDomain(): UserProfile {
    return UserProfile(
        id = id,
        ageYears = ageYears,
        sex = BiologicalSex.valueOf(sex),
        heightCm = heightCm,
        weightKg = weightKg,
        activityLevel = ActivityLevel.valueOf(activityLevel),
        deficitTier = DeficitTier.valueOf(deficitTier),
        bmrKcal = bmrKcal,
        tdeeKcal = tdeeKcal,
        targetCalorieCeiling = targetCalorieCeiling,
        dailyWaterGoalMl = dailyWaterGoalMl,
        activeBuddyPokemonId = activeBuddyPokemonId,
        updatedAtEpochMillis = updatedAtEpochMillis
    )
}

fun UserProfile.toEntity(): UserProfileEntity {
    return UserProfileEntity(
        id = id,
        ageYears = ageYears,
        sex = sex.name,
        heightCm = heightCm,
        weightKg = weightKg,
        activityLevel = activityLevel.name,
        deficitTier = deficitTier.name,
        bmrKcal = bmrKcal,
        tdeeKcal = tdeeKcal,
        dailyDeficitGoalKcal = deficitTier.dailyDeficitKcal,
        targetCalorieCeiling = targetCalorieCeiling,
        dailyWaterGoalMl = dailyWaterGoalMl,
        activeBuddyPokemonId = activeBuddyPokemonId,
        updatedAtEpochMillis = updatedAtEpochMillis
    )
}
