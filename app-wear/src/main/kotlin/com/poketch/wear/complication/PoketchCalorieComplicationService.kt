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
import com.poketch.core.domain.repository.CalorieRepository
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
class PoketchCalorieComplicationService : SuspendingComplicationDataSourceService() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var calorieRepository: CalorieRepository

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ComplicationEntryPoint {
        fun userRepository(): UserRepository
        fun calorieRepository(): CalorieRepository
    }

    private fun ensureDependenciesInjected() {
        if (!::userRepository.isInitialized) {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                ComplicationEntryPoint::class.java
            )
            userRepository = entryPoint.userRepository()
            calorieRepository = entryPoint.calorieRepository()
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("450 kcal").build(),
                    contentDescription = PlainComplicationText.Builder("Remaining Calories").build()
                )
                    .setTitle(PlainComplicationText.Builder("REMAIN").build())
                    .build()
            }
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = 1450f,
                    min = 0f,
                    max = 1900f,
                    contentDescription = PlainComplicationText.Builder("Calorie Progress").build()
                )
                    .setText(PlainComplicationText.Builder("450").build())
                    .setTitle(PlainComplicationText.Builder("KCAL").build())
                    .build()
            }
            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        ensureDependenciesInjected()

        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val profile = userRepository.getUserProfile()
        val ceiling = profile?.targetCalorieCeiling ?: 1900
        val consumed = calorieRepository.observeTotalCaloriesByDate(todayIso).firstOrNull() ?: 0

        val isOverTarget = consumed > ceiling
        val surplus = consumed - ceiling
        val remaining = (ceiling - consumed).coerceAtLeast(0)

        val tapIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, WearMainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(
                    "destination_route",
                    if (isOverTarget) "over_calorie_red_alert" else "rotary_calorie_logger"
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                val displayText = if (isOverTarget) "+$surplus OVER" else "$remaining kcal"
                val titleText = if (isOverTarget) "ALERT" else "REMAIN"
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(displayText).build(),
                    contentDescription = PlainComplicationText.Builder(
                        if (isOverTarget) "+$surplus kcal over target" else "$remaining kcal remaining"
                    ).build()
                )
                    .setTitle(PlainComplicationText.Builder(titleText).build())
                    .setTapAction(tapIntent)
                    .build()
            }
            ComplicationType.RANGED_VALUE -> {
                val displayText = if (isOverTarget) "+$surplus" else "$remaining"
                val titleText = if (isOverTarget) "OVER" else "KCAL"
                val safeMax = ceiling.toFloat().coerceAtLeast(1f)
                val safeValue = consumed.toFloat().coerceIn(0f, safeMax)
                RangedValueComplicationData.Builder(
                    value = safeValue,
                    min = 0f,
                    max = safeMax,
                    contentDescription = PlainComplicationText.Builder(
                        if (isOverTarget) "+$surplus kcal over target" else "$remaining kcal remaining"
                    ).build()
                )
                    .setText(PlainComplicationText.Builder(displayText).build())
                    .setTitle(PlainComplicationText.Builder(titleText).build())
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
                    ComponentName(context, PoketchCalorieComplicationService::class.java)
                )
                requester.requestUpdateAll()
            } catch (_: Exception) {
            }
        }
    }
}
