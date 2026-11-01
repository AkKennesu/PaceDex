package com.poketch.core.domain.repository

import kotlinx.coroutines.flow.Flow

data class CurrentWeather(
    val tempCelsius: Int = 28,
    val conditionShort: String = "SUN",
    val badgeText: String = "28°C SUN",
    val isDaytime: Boolean = true
)

interface WeatherRepository {
    fun observeCurrentWeather(): Flow<CurrentWeather>
    suspend fun getCurrentWeather(): CurrentWeather
    suspend fun refreshWeatherFromRemote(): Result<Unit>
    suspend fun cacheHourlyForecastJson(jsonString: String): Result<Unit>
    fun getLastFetchedJson(): String?
}
