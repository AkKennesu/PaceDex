package com.poketch.core.domain.repository

import kotlinx.coroutines.flow.Flow

data class CurrentWeather(
    val tempCelsius: Int = 28,
    val conditionShort: String = "SUN",
    val badgeText: String = "28°C / SUNNY",
    val isDaytime: Boolean = true,
    val conditionText: String = "SUNNY",
    val wmoCode: Int? = null
)

interface WeatherRepository {
    fun observeCurrentWeather(): Flow<CurrentWeather>
    suspend fun getCurrentWeather(): CurrentWeather
    suspend fun refreshWeatherFromRemote(): Result<Unit>
    suspend fun cacheHourlyForecastJson(jsonString: String): Result<Unit>
    fun getLastFetchedJson(): String?
    fun setLocation(latitude: Double?, longitude: Double?, timeZoneId: String) {}
    fun getResolvedTimeZoneId(): String = "UTC"
}
