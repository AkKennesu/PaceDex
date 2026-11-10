package com.poketch.wear.complication

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.google.common.truth.Truth.assertThat
import com.poketch.core.domain.repository.CurrentWeather
import com.poketch.core.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class PoketchWeatherComplicationServiceTest {

    private lateinit var context: Context
    private lateinit var fakeWeatherRepository: FakeWeatherRepository
    private lateinit var service: PoketchWeatherComplicationService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        fakeWeatherRepository = FakeWeatherRepository()

        service = PoketchWeatherComplicationService().apply {
            val method = android.content.ContextWrapper::class.java.getDeclaredMethod(
                "attachBaseContext",
                Context::class.java
            )
            method.isAccessible = true
            method.invoke(this, context)

            this.weatherRepository = fakeWeatherRepository
        }
    }

    @Test
    fun getPreviewData_returnsValidShortText() {
        val shortText = service.getPreviewData(ComplicationType.SHORT_TEXT) as? ShortTextComplicationData
        assertThat(shortText).isNotNull()
        val textValue = shortText?.text?.getTextAt(context.resources, Instant.now())?.toString()
        assertThat(textValue).isEqualTo("28°C / SUNNY")

        val rangedValue = service.getPreviewData(ComplicationType.RANGED_VALUE)
        assertThat(rangedValue).isNull()
    }

    @Test
    fun onComplicationRequest_returnsFormattedWeatherShortText(): Unit = runBlocking {
        fakeWeatherRepository.currentWeather = CurrentWeather(
            tempCelsius = 22,
            conditionShort = "RAIN",
            badgeText = "22°C / RAINY",
            isDaytime = true,
            conditionText = "RAINY"
        )

        val request = ComplicationRequest(
            complicationInstanceId = 1,
            complicationType = ComplicationType.SHORT_TEXT,
            immediateResponseRequired = true
        )

        val data = service.onComplicationRequest(request) as? ShortTextComplicationData
        assertThat(data).isNotNull()
        val textValue = data?.text?.getTextAt(context.resources, Instant.now())?.toString()
        assertThat(textValue).isEqualTo("22°C / RAINY")
        assertThat(data?.tapAction).isNotNull()
    }

    @Test
    fun onComplicationRequest_returnsCloudyWeather(): Unit = runBlocking {
        fakeWeatherRepository.currentWeather = CurrentWeather(
            tempCelsius = 24,
            conditionShort = "CLDY",
            badgeText = "24°C / CLOUDY",
            isDaytime = true,
            conditionText = "CLOUDY"
        )

        val request = ComplicationRequest(
            complicationInstanceId = 1,
            complicationType = ComplicationType.SHORT_TEXT,
            immediateResponseRequired = true
        )

        val data = service.onComplicationRequest(request) as? ShortTextComplicationData
        assertThat(data).isNotNull()
        val textValue = data?.text?.getTextAt(context.resources, Instant.now())?.toString()
        assertThat(textValue).isEqualTo("24°C / CLOUDY")
    }

    private class FakeWeatherRepository : WeatherRepository {
        var currentWeather = CurrentWeather(
            tempCelsius = 28,
            conditionShort = "SUN",
            badgeText = "28°C / SUNNY",
            isDaytime = true,
            conditionText = "SUNNY"
        )

        override fun observeCurrentWeather(): Flow<CurrentWeather> = flowOf(currentWeather)
        override suspend fun getCurrentWeather(): CurrentWeather = currentWeather
        override suspend fun refreshWeatherFromRemote(): Result<Unit> = Result.success(Unit)
        override suspend fun cacheHourlyForecastJson(jsonString: String): Result<Unit> = Result.success(Unit)
        override fun getLastFetchedJson(): String? = null
    }
}
