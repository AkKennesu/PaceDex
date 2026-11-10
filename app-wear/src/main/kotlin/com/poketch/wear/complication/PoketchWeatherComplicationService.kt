package com.poketch.wear.complication

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.poketch.core.domain.repository.WeatherRepository
import com.poketch.wear.WearMainActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

@AndroidEntryPoint
class PoketchWeatherComplicationService : SuspendingComplicationDataSourceService() {

    @Inject
    lateinit var weatherRepository: WeatherRepository

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WeatherComplicationEntryPoint {
        fun weatherRepository(): WeatherRepository
    }

    private fun ensureDependenciesInjected() {
        if (!::weatherRepository.isInitialized) {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                WeatherComplicationEntryPoint::class.java
            )
            weatherRepository = entryPoint.weatherRepository()
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("28°C / SUNNY").build(),
            contentDescription = PlainComplicationText.Builder("Weather: 28°C SUNNY").build()
        )
            .setTitle(PlainComplicationText.Builder("WEATHER").build())
            .build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        ensureDependenciesInjected()

        val weather = weatherRepository.getCurrentWeather()
        val formatted = "${weather.tempCelsius}°C / ${weather.conditionText}"

        val tapIntent = PendingIntent.getActivity(
            this,
            5,
            Intent(this, WearMainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("destination_route", "watch_dashboard")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(formatted).build(),
                    contentDescription = PlainComplicationText.Builder("Weather: $formatted").build()
                )
                    .setTitle(PlainComplicationText.Builder("WEATHER").build())
                    .setTapAction(tapIntent)
                    .build()
            }
            else -> null
        }
    }

    companion object {
        fun requestUpdate(context: Context) {
            try {
                val requester = ComplicationDataSourceUpdateRequester.create(
                    context,
                    ComponentName(context, PoketchWeatherComplicationService::class.java)
                )
                requester.requestUpdateAll()
            } catch (_: Exception) {
            }
        }
    }
}
