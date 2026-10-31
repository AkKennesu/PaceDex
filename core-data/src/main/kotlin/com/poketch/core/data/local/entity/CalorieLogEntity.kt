package com.poketch.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.poketch.core.domain.model.CalorieLog
import com.poketch.core.domain.model.DeviceSource
import com.poketch.core.domain.model.MealCategory

@Entity(
    tableName = "calorie_logs",
    indices = [
        Index(value = ["dateIso"]),
        Index(value = ["isSynced"])
    ]
)
data class CalorieLogEntity(
    @PrimaryKey val logId: String,
    val dateIso: String,
    val mealCategory: String,
    val foodDescription: String,
    val caloriesKcal: Int,
    val proteinG: Int = 0,
    val carbsG: Int = 0,
    val fatG: Int = 0,
    val sourceDevice: String, // "WEAR" | "MOBILE"
    val timestampMillis: Long,
    val isSynced: Boolean = false
)

fun CalorieLogEntity.toDomain(): CalorieLog {
    return CalorieLog(
        logId = logId,
        dateIso = dateIso,
        mealCategory = MealCategory.valueOf(mealCategory),
        foodDescription = foodDescription,
        caloriesKcal = caloriesKcal,
        proteinG = proteinG,
        carbsG = carbsG,
        fatG = fatG,
        sourceDevice = DeviceSource.valueOf(sourceDevice),
        timestampMillis = timestampMillis,
        isSynced = isSynced
    )
}

fun CalorieLog.toEntity(): CalorieLogEntity {
    return CalorieLogEntity(
        logId = logId,
        dateIso = dateIso,
        mealCategory = mealCategory.name,
        foodDescription = foodDescription,
        caloriesKcal = caloriesKcal,
        proteinG = proteinG,
        carbsG = carbsG,
        fatG = fatG,
        sourceDevice = sourceDevice.name,
        timestampMillis = timestampMillis,
        isSynced = isSynced
    )
}
