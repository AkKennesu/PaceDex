package com.poketch.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.poketch.core.data.local.entity.CalorieLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalorieDao {

    @Query("SELECT * FROM calorie_logs WHERE dateIso = :dateIso ORDER BY timestampMillis DESC")
    fun observeLogsByDate(dateIso: String): Flow<List<CalorieLogEntity>>

    @Query("SELECT COALESCE(SUM(caloriesKcal), 0) FROM calorie_logs WHERE dateIso = :dateIso")
    fun observeTotalCaloriesByDate(dateIso: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(caloriesKcal), 0) FROM calorie_logs WHERE dateIso = :dateIso")
    suspend fun getTotalCaloriesByDateOnce(dateIso: String): Int

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
