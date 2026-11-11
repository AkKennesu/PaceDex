package com.poketch.core.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.poketch.core.domain.model.ActivityLevel
import com.poketch.core.domain.model.BiologicalSex
import com.poketch.core.domain.model.CalorieLog
import com.poketch.core.domain.model.DeficitTier
import com.poketch.core.domain.model.DeviceSource
import com.poketch.core.domain.model.HabitLog
import com.poketch.core.domain.model.HabitType
import com.poketch.core.domain.model.MealCategory
import com.poketch.core.domain.model.UserProfile
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun uploadUserProfile(userId: String, profile: UserProfile): Result<Unit> {
        return runCatching {
            val data = hashMapOf(
                "uid" to userId,
                "ageYears" to profile.ageYears,
                "sex" to profile.sex.name,
                "heightCm" to profile.heightCm,
                "weightKg" to profile.weightKg,
                "activityLevel" to profile.activityLevel.name,
                "deficitTier" to profile.deficitTier.name,
                "dailyDeficitGoalKcal" to profile.dailyDeficitGoalKcal,
                "bmrKcal" to profile.bmrKcal,
                "tdeeKcal" to profile.tdeeKcal,
                "targetCalorieCeiling" to profile.targetCalorieCeiling,
                "dailyWaterGoalMl" to profile.dailyWaterGoalMl,
                "activeBuddyPokemonId" to profile.activeBuddyPokemonId,
                "updatedAtEpochMillis" to profile.updatedAtEpochMillis,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            firestore.collection("users").document(userId)
                .set(data, SetOptions.merge())
                .await()
        }
    }

    suspend fun downloadUserProfile(userId: String): Result<UserProfile?> {
        return runCatching {
            val snapshot = firestore.collection("users").document(userId).get().await()
            if (!snapshot.exists()) return@runCatching null

            UserProfile(
                id = 1,
                ageYears = (snapshot.getLong("ageYears") ?: 25).toInt(),
                sex = BiologicalSex.valueOf(snapshot.getString("sex") ?: BiologicalSex.MALE.name),
                heightCm = snapshot.getDouble("heightCm") ?: 170.0,
                weightKg = snapshot.getDouble("weightKg") ?: 70.0,
                activityLevel = ActivityLevel.valueOf(snapshot.getString("activityLevel") ?: ActivityLevel.LIGHTLY_ACTIVE.name),
                deficitTier = DeficitTier.valueOf(snapshot.getString("deficitTier") ?: DeficitTier.STANDARD.name),
                dailyDeficitGoalKcal = (snapshot.getLong("dailyDeficitGoalKcal") ?: 500).toInt(),
                bmrKcal = (snapshot.getLong("bmrKcal") ?: 1600).toInt(),
                tdeeKcal = (snapshot.getLong("tdeeKcal") ?: 2200).toInt(),
                targetCalorieCeiling = (snapshot.getLong("targetCalorieCeiling") ?: 1700).toInt(),
                dailyWaterGoalMl = (snapshot.getLong("dailyWaterGoalMl") ?: 2500).toInt(),
                activeBuddyPokemonId = (snapshot.getLong("activeBuddyPokemonId") ?: 1).toInt(),
                updatedAtEpochMillis = snapshot.getLong("updatedAtEpochMillis") ?: System.currentTimeMillis()
            )
        }
    }

    suspend fun uploadCalorieLog(userId: String, log: CalorieLog): Result<Unit> {
        return runCatching {
            val data = hashMapOf(
                "logId" to log.logId,
                "dateIso" to log.dateIso,
                "mealCategory" to log.mealCategory.name,
                "foodDescription" to log.foodDescription,
                "caloriesKcal" to log.caloriesKcal,
                "proteinG" to log.proteinG,
                "carbsG" to log.carbsG,
                "fatG" to log.fatG,
                "sourceDevice" to log.sourceDevice.name,
                "timestampMillis" to log.timestampMillis,
                "syncedAt" to com.google.firebase.Timestamp.now()
            )
            firestore.collection("users").document(userId)
                .collection("calorie_logs").document(log.logId)
                .set(data, SetOptions.merge())
                .await()
        }
    }

    suspend fun uploadHabitLog(userId: String, habit: HabitLog): Result<Unit> {
        return runCatching {
            val data = hashMapOf(
                "logId" to habit.logId,
                "dateIso" to habit.dateIso,
                "habitType" to habit.habitType.name,
                "numericValue" to habit.numericValue,
                "durationSeconds" to habit.durationSeconds,
                "sourceDevice" to habit.sourceDevice.name,
                "timestampMillis" to habit.timestampMillis,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            firestore.collection("users").document(userId)
                .collection("habit_logs").document(habit.logId)
                .set(data, SetOptions.merge())
                .await()
        }
    }
}
