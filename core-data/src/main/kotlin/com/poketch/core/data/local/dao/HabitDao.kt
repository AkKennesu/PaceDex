package com.poketch.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.poketch.core.data.local.entity.HabitLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habit_logs WHERE dateIso = :dateIso ORDER BY timestampMillis DESC")
    fun observeHabitsByDate(dateIso: String): Flow<List<HabitLogEntity>>

    @Query("SELECT COALESCE(SUM(numericValue), 0.0) FROM habit_logs WHERE dateIso = :dateIso AND habitType = :habitType")
    fun observeDailyHabitTotal(dateIso: String, habitType: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(numericValue), 0.0) FROM habit_logs WHERE habitType = :habitType")
    suspend fun getLifetimeHabitTotal(habitType: String): Double

    @Upsert
    suspend fun upsertHabitLog(log: HabitLogEntity)

    @Upsert
    suspend fun upsertHabitLogs(logs: List<HabitLogEntity>)

    @Query("SELECT * FROM habit_logs WHERE isSynced = 0")
    suspend fun getUnsyncedHabits(): List<HabitLogEntity>

    @Query("UPDATE habit_logs SET isSynced = 1 WHERE logId IN (:logIds)")
    suspend fun markHabitsAsSynced(logIds: List<String>)
}
