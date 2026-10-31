package com.poketch.core.domain.repository

import com.poketch.core.domain.model.DeviceSource
import com.poketch.core.domain.model.HabitLog
import com.poketch.core.domain.model.HabitType
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    fun observeHabitsByDate(dateIso: String): Flow<List<HabitLog>>
    fun observeDailyWaterMl(dateIso: String): Flow<Double>
    fun observeDailyRunMiles(dateIso: String): Flow<Double>
    fun observeDailySteps(dateIso: String): Flow<Double>
    fun observeDailySleepHours(dateIso: String): Flow<Double>
    suspend fun getLifetimeHabitTotal(habitType: HabitType): Double
    suspend fun logHabit(
        dateIso: String,
        habitType: HabitType,
        numericValue: Double,
        durationSeconds: Int = 0,
        sourceDevice: DeviceSource = DeviceSource.MOBILE
    ): HabitLog
    suspend fun upsertHabitLog(log: HabitLog)
    suspend fun upsertHabitLogs(logs: List<HabitLog>)
    suspend fun getUnsyncedHabits(): List<HabitLog>
    suspend fun markHabitsAsSynced(logIds: List<String>)
}
