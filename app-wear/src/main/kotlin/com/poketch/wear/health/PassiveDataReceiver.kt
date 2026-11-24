package com.poketch.wear.health

import androidx.health.services.client.PassiveListenerService
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import com.poketch.core.data.gamification.EvaluateAchievementsUseCase
import com.poketch.core.domain.model.DeviceSource
import com.poketch.core.domain.model.HabitType
import com.poketch.core.domain.repository.HabitRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class PassiveDataReceiver : PassiveListenerService() {

    @Inject
    lateinit var habitRepository: HabitRepository

    @Inject
    lateinit var evaluateAchievementsUseCase: EvaluateAchievementsUseCase

    @Inject
    lateinit var syncManager: com.poketch.core.sync.PoketchSyncManager

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
        val stepDataPoints = dataPoints.getData(DataType.STEPS_DAILY)
        val latestSteps = stepDataPoints.lastOrNull()?.value ?: return

        serviceScope.launch {
            val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            habitRepository.logHabit(
                dateIso = todayIso,
                habitType = HabitType.DAILY_STEPS,
                numericValue = latestSteps.toDouble(),
                sourceDevice = DeviceSource.WEAR
            )
            syncManager.updateRecoveryQuestProgress(latestSteps.toInt())
            evaluateAchievementsUseCase.evaluate(todayIso)
        }
    }
}
