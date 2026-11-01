package com.poketch.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_weather_cache")
data class HourlyWeatherCacheEntity(
    @PrimaryKey val hourIso: String,
    val tempCelsius: Int,
    val wmoCode: Int,
    val fetchedAtMillis: Long
)
