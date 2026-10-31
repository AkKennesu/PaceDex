package com.poketch.core.data.repository

import com.poketch.core.data.local.dao.HabitDao
import com.poketch.core.data.local.entity.toDomain
import com.poketch.core.data.local.entity.toEntity
import com.poketch.core.domain.model.DeviceSource
import com.poketch.core.domain.model.HabitLog
import com.poketch.core.domain.model.HabitType
import com.poketch.core.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstHabitRepository @Inject constructor(
    private val habitDao: HabitDao
) : HabitRepository {

    override fun observeHabitsByDate(dateIso: String): Flow<List<HabitLog>> {
        return habitDao.observeHabitsByDate(dateIso).map { list -> list.map { it.toDomain() } }
    }

    override fun observeDailyWaterMl(dateIso: String): Flow<Double> {
        return habitDao.observeDailyHabitTotal(dateIso, HabitType.WATER_ML.name)
    }

    override fun observeDailyRunMiles(dateIso: String): Flow<Double> {
        return habitDao.observeDailyHabitTotal(dateIso, HabitType.RUN_MILES.name)
    }

    override fun observeDailySteps(dateIso: String): Flow<Double> {
        return habitDao.observeDailyHabitTotal(dateIso, HabitType.DAILY_STEPS.name)
    }

    override fun observeDailySleepHours(dateIso: String): Flow<Double> {
        return habitDao.observeDailyHabitTotal(dateIso, HabitType.SLEEP_HOURS.name)
    }

    override suspend fun getLifetimeHabitTotal(habitType: HabitType): Double {
        return habitDao.getLifetimeHabitTotal(habitType.name)
    }

    override suspend fun logHabit(
        dateIso: String,
        habitType: HabitType,
        numericValue: Double,
        durationSeconds: Int,
        sourceDevice: DeviceSource
    ): HabitLog {
        val newLog = HabitLog(
            logId = UUID.randomUUID().toString(),
            dateIso = dateIso,
            habitType = habitType,
            numericValue = numericValue,
            durationSeconds = durationSeconds,
            sourceDevice = sourceDevice,
            timestampMillis = System.currentTimeMillis(),
            isSynced = false
        )
        habitDao.upsertHabitLog(newLog.toEntity())
        return newLog
    }

    override suspend fun upsertHabitLog(log: HabitLog) {
        habitDao.upsertHabitLog(log.toEntity())
    }

    override suspend fun upsertHabitLogs(logs: List<HabitLog>) {
        habitDao.upsertHabitLogs(logs.map { it.toEntity() })
    }

    override suspend fun getUnsyncedHabits(): List<HabitLog> {
        return habitDao.getUnsyncedHabits().map { it.toDomain() }
    }

    override suspend fun markHabitsAsSynced(logIds: List<String>) {
        if (logIds.isNotEmpty()) {
            habitDao.markHabitsAsSynced(logIds)
        }
    }
}
