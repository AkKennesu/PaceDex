package com.poketch.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.poketch.core.data.local.entity.CalorieLogEntity
import com.poketch.core.domain.model.DailyNutritionSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface CalorieDao {

    @Query("SELECT * FROM calorie_logs WHERE dateIso = :dateIso ORDER BY timestampMillis DESC")
    fun observeLogsByDate(dateIso: String): Flow<List<CalorieLogEntity>>

    @Query("SELECT COALESCE(SUM(caloriesKcal), 0) FROM calorie_logs WHERE dateIso = :dateIso")
    fun observeTotalCaloriesByDate(dateIso: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(caloriesKcal), 0) FROM calorie_logs WHERE dateIso = :dateIso")
    suspend fun getTotalCaloriesByDateOnce(dateIso: String): Int

    @Query("""
        SELECT dateIso,
               COALESCE(SUM(caloriesKcal), 0) AS totalCalories,
               COALESCE(SUM(proteinG), 0) AS totalProtein,
               COALESCE(SUM(carbsG), 0) AS totalCarbs,
               COALESCE(SUM(fatG), 0) AS totalFat
        FROM calorie_logs
        GROUP BY dateIso
        ORDER BY dateIso DESC
        LIMIT :days
    """)
    fun observeRecentDailyNutrition(days: Int): Flow<List<DailyNutritionSummary>>

    @Query("""
        SELECT dateIso,
               COALESCE(SUM(caloriesKcal), 0) AS totalCalories,
               COALESCE(SUM(proteinG), 0) AS totalProtein,
               COALESCE(SUM(carbsG), 0) AS totalCarbs,
               COALESCE(SUM(fatG), 0) AS totalFat
        FROM calorie_logs
        GROUP BY dateIso
        ORDER BY dateIso DESC
        LIMIT :days
    """)
    suspend fun getRecentDailyNutrition(days: Int): List<DailyNutritionSummary>

    @Upsert
    suspend fun upsertCalorieLog(log: CalorieLogEntity)

    @Upsert
    suspend fun upsertCalorieLogs(logs: List<CalorieLogEntity>)

    @Query("DELETE FROM calorie_logs WHERE logId = :logId")
    suspend fun deleteCalorieLog(logId: String)

    @Query("SELECT * FROM calorie_logs WHERE isSynced = 0")
    suspend fun getUnsyncedLogs(): List<CalorieLogEntity>

    @Query("UPDATE calorie_logs SET isSynced = 1 WHERE logId IN (:logIds)")
    suspend fun markLogsAsSynced(logIds: List<String>)
}
