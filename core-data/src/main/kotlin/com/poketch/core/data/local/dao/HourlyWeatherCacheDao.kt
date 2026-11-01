package com.poketch.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.poketch.core.data.local.entity.HourlyWeatherCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HourlyWeatherCacheDao {

    @Query("SELECT * FROM offline_weather_cache WHERE hourIso = :hourIso LIMIT 1")
    suspend fun getWeatherForHour(hourIso: String): HourlyWeatherCacheEntity?

    @Query("SELECT * FROM offline_weather_cache WHERE hourIso = :hourIso LIMIT 1")
    fun observeWeatherForHour(hourIso: String): Flow<HourlyWeatherCacheEntity?>

    @Query("SELECT * FROM offline_weather_cache ORDER BY fetchedAtMillis DESC LIMIT 1")
    suspend fun getLatestCachedWeather(): HourlyWeatherCacheEntity?

    @Query("SELECT * FROM offline_weather_cache")
    suspend fun getAll(): List<HourlyWeatherCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<HourlyWeatherCacheEntity>)

    @Query("DELETE FROM offline_weather_cache WHERE hourIso < :beforeHourIso")
    suspend fun deleteExpired(beforeHourIso: String)

    @Query("DELETE FROM offline_weather_cache")
    suspend fun clearAll()
}
