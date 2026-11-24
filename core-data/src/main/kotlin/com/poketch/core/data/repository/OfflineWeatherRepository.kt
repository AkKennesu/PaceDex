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
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

data class ResolvedLocation(
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String
)

@Singleton
class OfflineWeatherRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hourlyWeatherCacheDao: HourlyWeatherCacheDao,
) : WeatherRepository, SensorEventListener {

    companion object {
        private const val PREFS_WEATHER = "poketch_weather_prefs"
        private const val KEY_LATITUDE = "pref_lat"
        private const val KEY_LONGITUDE = "pref_lon"
        private const val KEY_TIMEZONE_ID = "pref_tz_id"
    }

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastFetchedRawJson: String? = null

    @Volatile
    private var userLocation: ResolvedLocation? = null

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

    override fun setLocation(latitude: Double?, longitude: Double?, timeZoneId: String) {
        val resolved = if (latitude != null && longitude != null && !latitude.isNaN() && !longitude.isNaN()) {
            ResolvedLocation(latitude, longitude, timeZoneId)
        } else {
            lookupCoordinatesForZone(timeZoneId)
        }
        userLocation = resolved
        val prefs = context.getSharedPreferences(PREFS_WEATHER, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LATITUDE, resolved.latitude.toString())
            .putString(KEY_LONGITUDE, resolved.longitude.toString())
            .putString(KEY_TIMEZONE_ID, resolved.timeZoneId)
            .apply()

        repositoryScope.launch {
            runCatching { hourlyWeatherCacheDao.clearAll() }
            runCatching { refreshWeatherFromRemote() }
            updateCurrentWeather()
        }
    }

    override fun getResolvedTimeZoneId(): String {
        return resolveActiveLocation().timeZoneId
    }

    private fun lookupCoordinatesForZone(zoneId: String): ResolvedLocation {
        val cleanZone = zoneId.trim()
        val coords = when (cleanZone) {
            "Asia/Singapore" -> Pair(1.3521, 103.8198)
            "Asia/Manila" -> Pair(14.5995, 120.9842)
            "Asia/Tokyo" -> Pair(35.6762, 139.6503)
            "Asia/Seoul" -> Pair(37.5665, 126.9780)
            "Asia/Hong_Kong" -> Pair(22.3193, 114.1694)
            "Asia/Taipei" -> Pair(25.0330, 121.5654)
            "Asia/Bangkok" -> Pair(13.7563, 100.5018)
            "Asia/Kuala_Lumpur" -> Pair(3.1390, 101.6869)
            "Asia/Jakarta" -> Pair(-6.2088, 106.8456)
            "Asia/Shanghai", "Asia/Chongqing", "Asia/Beijing" -> Pair(31.2304, 121.4737)
            "Asia/Kolkata", "Asia/Calcutta" -> Pair(28.6139, 77.2090)
            "Asia/Dubai" -> Pair(25.2048, 55.2708)
            "Asia/Riyadh" -> Pair(24.7136, 46.6753)
            "Europe/London" -> Pair(51.5074, -0.1278)
            "Europe/Paris" -> Pair(48.8566, 2.3522)
            "Europe/Berlin" -> Pair(52.5200, 13.4050)
            "Europe/Rome" -> Pair(41.9028, 12.4964)
            "Europe/Madrid" -> Pair(40.4168, -3.7038)
            "Europe/Amsterdam" -> Pair(52.3676, 4.9041)
            "America/New_York" -> Pair(40.7128, -74.0060)
            "America/Chicago" -> Pair(41.8781, -87.6298)
            "America/Denver" -> Pair(39.7392, -104.9903)
            "America/Los_Angeles" -> Pair(34.0522, -118.2437)
            "America/Phoenix" -> Pair(33.4484, -112.0740)
            "America/Toronto" -> Pair(43.6532, -79.3832)
            "America/Vancouver" -> Pair(49.2827, -123.1207)
            "America/Sao_Paulo" -> Pair(-23.5505, -46.6333)
            "Australia/Sydney" -> Pair(-33.8688, 151.2093)
            "Australia/Melbourne" -> Pair(-37.8136, 144.9631)
            "Australia/Perth" -> Pair(-31.9505, 115.8605)
            "Pacific/Auckland" -> Pair(-36.8485, 174.7633)
            "Pacific/Honolulu" -> Pair(21.3069, -157.8583)
            else -> {
                try {
                    val zone = ZoneId.of(cleanZone)
                    val offsetSeconds = zone.rules.getOffset(java.time.Instant.now()).totalSeconds
                    val approxLon = (offsetSeconds / 3600.0) * 15.0
                    Pair(1.3521, approxLon.coerceIn(-180.0, 180.0))
                } catch (_: Exception) {
                    Pair(1.3521, 103.8198)
                }
            }
        }
        return ResolvedLocation(coords.first, coords.second, cleanZone)
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun getLastKnownLocationIfAvailable(): Pair<Double, Double>? {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager ?: return null
            val loc = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(android.location.LocationManager.PASSIVE_PROVIDER)
            if (loc != null) Pair(loc.latitude, loc.longitude) else null
        } catch (_: Throwable) {
            null
        }
    }

    fun resolveActiveLocation(): ResolvedLocation {
        userLocation?.let { return it }

        val prefs = context.getSharedPreferences(PREFS_WEATHER, Context.MODE_PRIVATE)
        val savedLat = prefs.getString(KEY_LATITUDE, null)?.toDoubleOrNull()
        val savedLon = prefs.getString(KEY_LONGITUDE, null)?.toDoubleOrNull()
        val savedTz = prefs.getString(KEY_TIMEZONE_ID, null)

        if (savedLat != null && savedLon != null && !savedTz.isNullOrBlank()) {
            val loc = ResolvedLocation(savedLat, savedLon, savedTz)
            userLocation = loc
            return loc
        }

        val lastGps = getLastKnownLocationIfAvailable()
        val systemZoneId = try {
            ZoneId.systemDefault().id
        } catch (_: Exception) {
            "Asia/Singapore"
        }

        if (lastGps != null) {
            val loc = ResolvedLocation(lastGps.first, lastGps.second, systemZoneId)
            userLocation = loc
            return loc
        }

        val resolved = lookupCoordinatesForZone(systemZoneId)
        userLocation = resolved
        return resolved
    }

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
        val loc = resolveActiveLocation()
        val zone = runCatching { ZoneId.of(loc.timeZoneId) }.getOrElse { ZoneId.systemDefault() }
        val now = LocalDateTime.now(zone)
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
                conditionText = conditionFull,
                wmoCode = cachedHour.wmoCode
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
            conditionText = conditionFull,
            wmoCode = latestCached?.wmoCode
        )
    }

    /**
     * Tier 1: Free zero-key Open-Meteo 7-day hourly forecast fetch.
     */
    override suspend fun refreshWeatherFromRemote(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val loc = resolveActiveLocation()
            val encodedTz = URLEncoder.encode(loc.timeZoneId, "UTF-8")
            val urlString = "https://api.open-meteo.com/v1/forecast?latitude=${loc.latitude}&longitude=${loc.longitude}&current=temperature_2m,weather_code&hourly=temperature_2m,weather_code&forecast_days=7&timezone=$encodedTz"
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

    suspend fun getLatestCachedWeatherWmoCode(): Int? = withContext(Dispatchers.IO) {
        val loc = resolveActiveLocation()
        val zone = runCatching { ZoneId.of(loc.timeZoneId) }.getOrElse { ZoneId.systemDefault() }
        val now = LocalDateTime.now(zone)
        val currentHourIso = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00", Locale.US))
        hourlyWeatherCacheDao.getWeatherForHour(currentHourIso)?.wmoCode
            ?: hourlyWeatherCacheDao.getLatestCachedWeather()?.wmoCode
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PRESSURE) {
            val hpa = event.values.firstOrNull() ?: return
            lastBarometerHpa = hpa
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
