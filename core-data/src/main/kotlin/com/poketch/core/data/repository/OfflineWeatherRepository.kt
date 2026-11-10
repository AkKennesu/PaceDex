package com.poketch.core.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.poketch.core.data.local.dao.HourlyWeatherCacheDao
import com.poketch.core.data.local.entity.HourlyWeatherCacheEntity
import com.poketch.core.domain.repository.CurrentWeather
import com.poketch.core.domain.repository.WeatherRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class OfflineWeatherRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hourlyWeatherCacheDao: HourlyWeatherCacheDao,
) : WeatherRepository, SensorEventListener {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastFetchedRawJson: String? = null

    // Hardware barometer reading in hPa
    @Volatile
    private var lastBarometerHpa: Float? = null

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val pressureSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PRESSURE)

    private val currentWeatherState = MutableStateFlow(CurrentWeather())

    init {
        pressureSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Cold-start evaluation and initial remote sync attempt in background
        repositoryScope.launch {
            updateCurrentWeather()
            // Attempt remote refresh when network allows
            runCatching { refreshWeatherFromRemote() }
            updateCurrentWeather()
        }

        // Periodic ticker to keep hourly weather in sync across hour changes
        repositoryScope.launch {
            while (true) {
                delay(60_000L) // Check every minute
                updateCurrentWeather()
            }
        }
    }

    override fun observeCurrentWeather(): Flow<CurrentWeather> = currentWeatherState.asStateFlow()

    override suspend fun getCurrentWeather(): CurrentWeather = withContext(Dispatchers.IO) {
        evaluateCurrentWeather()
    }

    override fun getLastFetchedJson(): String? = lastFetchedRawJson

    private suspend fun updateCurrentWeather() {
        val weather = evaluateCurrentWeather()
        currentWeatherState.value = weather
    }

    /**
     * 3-Tier Offline-First Weather Resolution:
     * Tier 2: Check 168-hour Room cache for the exact current hour.
     * Tier 1: If empty and online, fetch 7-day forecast from Open-Meteo.
     * Tier 3: Zero-network fallback using hardware barometer + diurnal temperature curve.
     */
    private suspend fun evaluateCurrentWeather(): CurrentWeather {
        val now = LocalDateTime.now()
        val currentHourIso = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00", Locale.US))
        val hourOfDay = now.hour
        val isDaytime = hourOfDay in 6..17

        // --- TIER 2: Hourly Lookup from Local Room Cache ---
        val cachedHour = runCatching { hourlyWeatherCacheDao.getWeatherForHour(currentHourIso) }.getOrNull()
        if (cachedHour != null) {
            val condition = WeatherCodeMapper.mapWmoToConditionShort(cachedHour.wmoCode, isDaytime)
            val conditionFull = WeatherCodeMapper.mapWmoToConditionText(cachedHour.wmoCode, isDaytime)
            val badge = "${cachedHour.tempCelsius}°C / $conditionFull"
            return CurrentWeather(
                tempCelsius = cachedHour.tempCelsius,
                conditionShort = condition,
                badgeText = badge,
                isDaytime = isDaytime,
                conditionText = conditionFull
            )
        }

        // If current hour missing, check if any recent cached data can provide base temperature
        val latestCached = runCatching { hourlyWeatherCacheDao.getLatestCachedWeather() }.getOrNull()
        val basePeak = latestCached?.tempCelsius?.coerceIn(10, 45) ?: 28
        val baseLow = (basePeak - 5).coerceIn(5, 40)

        // --- TIER 3: Hardware Barometer + Diurnal Curve Fallback ---
        val estimatedTemp = calculateDiurnalTemp(hourOfDay, basePeak, baseLow)
        val condition = determineBarometerCondition(isDaytime)
        val conditionFull = WeatherCodeMapper.mapConditionShortToText(condition, isDaytime)
        val badge = "${estimatedTemp}°C / $conditionFull"

        return CurrentWeather(
            tempCelsius = estimatedTemp,
            conditionShort = condition,
            badgeText = badge,
            isDaytime = isDaytime,
            conditionText = conditionFull
        )
    }

    /**
     * Tier 1: Free zero-key Open-Meteo 7-day hourly forecast fetch.
     */
    override suspend fun refreshWeatherFromRemote(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Default coordinates (Tokyo/Kanto baseline: 35.6762, 139.6503; works everywhere worldwide without API key)
            val urlString = "https://api.open-meteo.com/v1/forecast?latitude=35.6762&longitude=139.6503&current=temperature_2m,weather_code&hourly=temperature_2m,weather_code&forecast_days=7&timezone=auto"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.requestMethod = "GET"

            try {
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseJson = connection.inputStream.bufferedReader().use { it.readText() }
                    cacheHourlyForecastJson(responseJson).getOrThrow()
                    updateCurrentWeather()
                } else {
                    throw IllegalStateException("Open-Meteo returned HTTP ${connection.responseCode}")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    /**
     * Parses the 168-hour forecast JSON and stores it into Room.
     */
    override suspend fun cacheHourlyForecastJson(jsonString: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject(jsonString)
            val hourly = root.getJSONObject("hourly")
            val times = hourly.getJSONArray("time")
            val temps = hourly.getJSONArray("temperature_2m")
            val codes = hourly.getJSONArray("weather_code")

            val count = minOf(times.length(), temps.length(), codes.length())
            val nowMillis = System.currentTimeMillis()
            val entities = ArrayList<HourlyWeatherCacheEntity>(count)

            for (i in 0 until count) {
                val hourIso = times.getString(i)
                val tempC = temps.getDouble(i).roundToInt()
                val code = codes.getInt(i)
                entities.add(
                    HourlyWeatherCacheEntity(
                        hourIso = hourIso,
                        tempCelsius = tempC,
                        wmoCode = code,
                        fetchedAtMillis = nowMillis
                    )
                )
            }

            if (entities.isNotEmpty()) {
                hourlyWeatherCacheDao.insertAll(entities)
                lastFetchedRawJson = jsonString
            }
        }
    }

    /**
     * Smooth mathematical diurnal curve:
     * Minimum temperature at 05:00, peak temperature at 14:00.
     */
    private fun calculateDiurnalTemp(hour: Int, basePeak: Int = 28, baseLow: Int = 23): Int {
        val temp = if (hour in 5..14) {
            val progress = (hour - 5) / 9.0
            baseLow + (basePeak - baseLow) * (1.0 - Math.cos(progress * Math.PI)) / 2.0
        } else {
            val hoursSincePeak = if (hour > 14) hour - 14 else hour + 10
            val progress = hoursSincePeak / 15.0
            baseLow + (basePeak - baseLow) * (1.0 + Math.cos(progress * Math.PI)) / 2.0
        }
        return temp.roundToInt()
    }

    /**
     * Reads atmospheric pressure (hPa) from Sensor.TYPE_PRESSURE:
     * < 1006 hPa -> "RAIN"
     * 1006..1014 hPa -> "CLDY"
     * > 1014 hPa -> "SUN" (or "CLR" at night)
     */
    private fun determineBarometerCondition(isDaytime: Boolean): String {
        val pressure = lastBarometerHpa
        return when {
            pressure != null && pressure < 1006f -> "RAIN"
            pressure != null && pressure <= 1014f -> "CLDY"
            else -> if (isDaytime) "SUN" else "CLR"
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PRESSURE) {
            val hpa = event.values.firstOrNull() ?: return
            lastBarometerHpa = hpa
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
