package com.poketch.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.poketch.core.data.local.dao.HourlyWeatherCacheDao
import com.poketch.core.data.local.db.PoketchDatabase
import com.poketch.core.data.local.entity.HourlyWeatherCacheEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class OfflineWeatherRepositoryTest {

    private lateinit var database: PoketchDatabase
    private lateinit var dao: HourlyWeatherCacheDao
    private lateinit var context: Context
    private lateinit var repository: OfflineWeatherRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, PoketchDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.hourlyWeatherCacheDao()
        repository = OfflineWeatherRepository(context, dao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun weatherCodeMapper_mapsCorrectly() {
        // 0, 1 -> SUN (day) / CLR (night)
        assertThat(WeatherCodeMapper.mapWmoToConditionShort(0, isDaytime = true)).isEqualTo("SUN")
        assertThat(WeatherCodeMapper.mapWmoToConditionShort(1, isDaytime = false)).isEqualTo("CLR")

        // 2, 3 -> CLDY
        assertThat(WeatherCodeMapper.mapWmoToConditionShort(2, isDaytime = true)).isEqualTo("CLDY")
        assertThat(WeatherCodeMapper.mapWmoToConditionShort(3, isDaytime = false)).isEqualTo("CLDY")

        // 45, 48 -> FOG
        assertThat(WeatherCodeMapper.mapWmoToConditionShort(45, isDaytime = true)).isEqualTo("FOG")
        assertThat(WeatherCodeMapper.mapWmoToConditionShort(48, isDaytime = true)).isEqualTo("FOG")

        // 51..67, 80..82 -> RAIN
        assertThat(WeatherCodeMapper.mapWmoToConditionShort(61, isDaytime = true)).isEqualTo("RAIN")
        assertThat(WeatherCodeMapper.mapWmoToConditionShort(80, isDaytime = true)).isEqualTo("RAIN")

        // 71..77, 85, 86 -> SNOW
        assertThat(WeatherCodeMapper.mapWmoToConditionShort(71, isDaytime = true)).isEqualTo("SNOW")
        assertThat(WeatherCodeMapper.mapWmoToConditionShort(85, isDaytime = true)).isEqualTo("SNOW")

        // 95..99 -> STRM
        assertThat(WeatherCodeMapper.mapWmoToConditionShort(95, isDaytime = true)).isEqualTo("STRM")

        // mapWmoToConditionText
        assertThat(WeatherCodeMapper.mapWmoToConditionText(0, isDaytime = true)).isEqualTo("SUNNY")
        assertThat(WeatherCodeMapper.mapWmoToConditionText(0, isDaytime = false)).isEqualTo("CLEAR")
        assertThat(WeatherCodeMapper.mapWmoToConditionText(2, isDaytime = true)).isEqualTo("CLOUDY")
        assertThat(WeatherCodeMapper.mapWmoToConditionText(45, isDaytime = true)).isEqualTo("FOGGY")
        assertThat(WeatherCodeMapper.mapWmoToConditionText(61, isDaytime = true)).isEqualTo("RAINY")
        assertThat(WeatherCodeMapper.mapWmoToConditionText(71, isDaytime = true)).isEqualTo("SNOWY")
        assertThat(WeatherCodeMapper.mapWmoToConditionText(95, isDaytime = true)).isEqualTo("STORMY")

        // mapConditionShortToText
        assertThat(WeatherCodeMapper.mapConditionShortToText("SUN", isDaytime = true)).isEqualTo("SUNNY")
        assertThat(WeatherCodeMapper.mapConditionShortToText("CLR", isDaytime = false)).isEqualTo("CLEAR")
        assertThat(WeatherCodeMapper.mapConditionShortToText("RAIN")).isEqualTo("RAINY")
        assertThat(WeatherCodeMapper.mapConditionShortToText("CLDY")).isEqualTo("CLOUDY")
    }

    @Test
    fun cacheHourlyForecastJson_parsesAndInsertsEntities() = runTest {
        val sampleJson = """
            {
                "hourly": {
                    "time": ["2026-11-01T10:00", "2026-11-01T11:00"],
                    "temperature_2m": [28.4, 29.1],
                    "weather_code": [0, 2]
                }
            }
        """.trimIndent()

        val result = repository.cacheHourlyForecastJson(sampleJson)
        assertThat(result.isSuccess).isTrue()

        val item1 = dao.getWeatherForHour("2026-11-01T10:00")
        assertThat(item1).isNotNull()
        assertThat(item1?.tempCelsius).isEqualTo(28)
        assertThat(item1?.wmoCode).isEqualTo(0)

        val item2 = dao.getWeatherForHour("2026-11-01T11:00")
        assertThat(item2).isNotNull()
        assertThat(item2?.tempCelsius).isEqualTo(29)
        assertThat(item2?.wmoCode).isEqualTo(2)
    }

    @Test
    fun getCurrentWeather_usesTier2WhenCached() = runTest {
        val currentHourIso = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00", Locale.US))
        dao.insertAll(
            listOf(
                HourlyWeatherCacheEntity(
                    hourIso = currentHourIso,
                    tempCelsius = 29,
                    wmoCode = 0,
                    fetchedAtMillis = System.currentTimeMillis()
                )
            )
        )

        val weather = repository.getCurrentWeather()
        assertThat(weather.tempCelsius).isEqualTo(29)
        assertThat(weather.badgeText).contains("29°C")
    }

    @Test
    fun getCurrentWeather_fallsBackToTier3WhenCacheEmpty() = runTest {
        // Database is empty
        val weather = repository.getCurrentWeather()
        assertThat(weather.badgeText).isNotEmpty()
        assertThat(weather.tempCelsius).isAtLeast(15)
        assertThat(weather.tempCelsius).isAtMost(40)
    }
}
