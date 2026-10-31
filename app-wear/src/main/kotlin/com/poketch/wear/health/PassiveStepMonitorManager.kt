package com.poketch.wear.health

import android.content.Context
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.PassiveListenerConfig
import com.poketch.core.data.gamification.EvaluateAchievementsUseCase
import com.poketch.core.domain.model.DeviceSource
import com.poketch.core.domain.model.HabitType
import com.poketch.core.domain.repository.HabitRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PassiveStepMonitorManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val habitRepository: HabitRepository,
    private val evaluateAchievementsUseCase: EvaluateAchievementsUseCase,
) {
    private val passiveClient = HealthServices.getClient(context).passiveMonitoringClient

    suspend fun registerPassiveStepMonitoring(): Boolean {
        return withContext(Dispatchers.IO) {
            runCatching {
                val config = PassiveListenerConfig.builder()
                    .setDataTypes(setOf(DataType.STEPS_DAILY))
                    .build()
                passiveClient.setPassiveListenerServiceAsync(
                    PassiveDataReceiver::class.java,
                    config
                )
                true
            }.getOrDefault(false)
        }
    }

    suspend fun unregisterPassiveStepMonitoring() {
        withContext(Dispatchers.IO) {
            try {
                passiveClient.clearPassiveListenerServiceAsync()
            } catch (_: Exception) {
            }
        }
    }

    suspend fun recordManualSteps(steps: Long) {
        withContext(Dispatchers.IO) {
            val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            habitRepository.logHabit(
                dateIso = todayIso,
                habitType = HabitType.DAILY_STEPS,
                numericValue = steps.toDouble(),
                sourceDevice = DeviceSource.WEAR
            )
            evaluateAchievementsUseCase.evaluate(todayIso)
        }
    }
}
