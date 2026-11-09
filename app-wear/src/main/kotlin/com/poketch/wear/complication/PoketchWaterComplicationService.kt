package com.poketch.wear.complication

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.poketch.core.domain.repository.HabitRepository
import com.poketch.core.domain.repository.UserRepository
import com.poketch.wear.WearMainActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class PoketchWaterComplicationService : SuspendingComplicationDataSourceService() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var habitRepository: HabitRepository

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WaterComplicationEntryPoint {
        fun userRepository(): UserRepository
        fun habitRepository(): HabitRepository
    }

    private fun ensureDependenciesInjected() {
        if (!::userRepository.isInitialized) {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                WaterComplicationEntryPoint::class.java
            )
            userRepository = entryPoint.userRepository()
            habitRepository = entryPoint.habitRepository()
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("1500 ml").build(),
                    contentDescription = PlainComplicationText.Builder("Daily Hydration").build()
                )
                    .setTitle(PlainComplicationText.Builder("WTR").build())
                    .build()
            }
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = 1500f,
                    min = 0f,
                    max = 2500f,
                    contentDescription = PlainComplicationText.Builder("Water Progress").build()
                )
                    .setText(PlainComplicationText.Builder("1500").build())
                    .setTitle(PlainComplicationText.Builder("WTR").build())
                    .build()
            }
            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        ensureDependenciesInjected()

        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val profile = userRepository.getUserProfile()
        val goalMl = (profile?.dailyWaterGoalMl ?: 2500).toFloat().coerceAtLeast(1f)
        val waterMl = (habitRepository.observeDailyWaterMl(todayIso).firstOrNull() ?: 0.0).toFloat().coerceAtLeast(0f)

        val tapIntent = PendingIntent.getActivity(
            this,
            2,
            Intent(this, WearMainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("destination_route", "quick_habit_actions")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("${waterMl.toInt()} ml").build(),
                    contentDescription = PlainComplicationText.Builder(
                        "${waterMl.toInt()} of ${goalMl.toInt()} ml water logged today"
                    ).build()
                )
                    .setTitle(PlainComplicationText.Builder("WTR").build())
                    .setTapAction(tapIntent)
                    .build()
            }
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = waterMl,
                    min = 0f,
                    max = goalMl,
                    contentDescription = PlainComplicationText.Builder(
                        "${waterMl.toInt()} of ${goalMl.toInt()} ml water logged today"
                    ).build()
                )
                    .setText(PlainComplicationText.Builder("${waterMl.toInt()}").build())
                    .setTitle(PlainComplicationText.Builder("WTR").build())
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
                    ComponentName(context, PoketchWaterComplicationService::class.java)
                )
                requester.requestUpdateAll()
            } catch (_: Exception) {
            }
        }
    }
}
