package com.poketch.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.poketch.core.domain.model.DeviceSource
import com.poketch.core.domain.model.HabitLog
import com.poketch.core.domain.model.HabitType

@Entity(
    tableName = "habit_logs",
    indices = [
        Index(value = ["dateIso", "habitType"]),
        Index(value = ["isSynced"])
    ]
)
data class HabitLogEntity(
    @PrimaryKey val logId: String,
    val dateIso: String,
    val habitType: String, // "WATER_ML" | "RUN_MILES" | "DAILY_STEPS" | "SLEEP_HOURS"
    val numericValue: Double,
    val durationSeconds: Int = 0,
    val sourceDevice: String, // "WEAR" | "MOBILE"
    val timestampMillis: Long,
    val isSynced: Boolean = false
)

fun HabitLogEntity.toDomain(): HabitLog {
    return HabitLog(
        logId = logId,
        dateIso = dateIso,
        habitType = HabitType.valueOf(habitType),
        numericValue = numericValue,
        durationSeconds = durationSeconds,
        sourceDevice = DeviceSource.valueOf(sourceDevice),
        timestampMillis = timestampMillis,
        isSynced = isSynced
    )
}

fun HabitLog.toEntity(): HabitLogEntity {
    return HabitLogEntity(
        logId = logId,
        dateIso = dateIso,
        habitType = habitType.name,
        numericValue = numericValue,
        durationSeconds = durationSeconds,
        sourceDevice = sourceDevice.name,
        timestampMillis = timestampMillis,
        isSynced = isSynced
    )
}
