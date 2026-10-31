package com.poketch.wear.health

import android.content.Context
import androidx.health.services.client.HealthServices
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import com.poketch.core.data.gamification.EvaluateAchievementsUseCase
import com.poketch.core.domain.model.DeviceSource
import com.poketch.core.domain.model.HabitType
import com.poketch.core.domain.repository.HabitRepository
import com.poketch.wear.ui.alert.WearHapticController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.round

enum class RunStatus {
    PREPARING,
    ACTIVE,
    PAUSED,
    COMPLETED
}

data class RunSessionState(
    val status: RunStatus = RunStatus.PREPARING,
    val distanceMiles: Double = 0.00,
    val elapsedSeconds: Long = 0L,
    val heartRateBpm: Int = 0,
    val currentPaceMinPerMile: String = "--'--\"/mi",
    val remainingMilesToPonyta: Double = 1.00,
    val hasCompletedOneMile: Boolean = false,
)

@Singleton
class OneMileRunExerciseManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val habitRepository: HabitRepository,
    private val evaluateAchievementsUseCase: EvaluateAchievementsUseCase,
    private val wearHapticController: WearHapticController,
) {
    private val exerciseClient = HealthServices.getClient(context).exerciseClient
    private val managerScope = CoroutineScope(Dispatchers.Default)

    private val _sessionState = MutableStateFlow(RunSessionState())
    val sessionState: StateFlow<RunSessionState> = _sessionState.asStateFlow()

    private var timerJob: Job? = null
    private var isRegisteredCallback = false
    private var lastMilestoneHapticTriggered = 0.0

    private val exerciseCallback = object : ExerciseUpdateCallback {
        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            val metrics = update.latestMetrics
            val distanceData = metrics.getData(DataType.DISTANCE_TOTAL)
            val distanceMeters = distanceData?.total ?: 0.0
            val miles = round((distanceMeters / 1609.344) * 100.0) / 100.0

            val hrData = metrics.getData(DataType.HEART_RATE_BPM)
            val hrBpm = hrData?.lastOrNull()?.value?.toInt() ?: _sessionState.value.heartRateBpm

            updateMetrics(miles = miles, hrBpm = hrBpm)
        }

        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {}
        override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {}
        override fun onRegistered() {
            isRegisteredCallback = true
        }
        override fun onRegistrationFailed(throwable: Throwable) {
            isRegisteredCallback = false
        }
    }

    fun startRun() {
        if (_sessionState.value.status == RunStatus.ACTIVE) return

        if (_sessionState.value.status == RunStatus.PREPARING) {
            lastMilestoneHapticTriggered = 0.0
        }

        _sessionState.value = _sessionState.value.copy(status = RunStatus.ACTIVE)
        startTimer()

        // Bind HealthServices ExerciseClient
        try {
            exerciseClient.setUpdateCallback(exerciseCallback)
            val config = ExerciseConfig.builder(ExerciseType.RUNNING)
                .setDataTypes(setOf(DataType.DISTANCE_TOTAL, DataType.HEART_RATE_BPM, DataType.PACE))
                .setIsAutoPauseAndResumeEnabled(false)
                .setIsGpsEnabled(true)
                .build()
            exerciseClient.startExerciseAsync(config)
        } catch (_: Exception) {
            // Graceful fallback for emulators without Health Services sensors
        }
    }

    fun pauseRun() {
        if (_sessionState.value.status != RunStatus.ACTIVE) return
        _sessionState.value = _sessionState.value.copy(status = RunStatus.PAUSED)
        stopTimer()
        try {
            exerciseClient.pauseExerciseAsync()
        } catch (_: Exception) {}
    }

    fun resumeRun() {
        if (_sessionState.value.status != RunStatus.PAUSED) return
        _sessionState.value = _sessionState.value.copy(status = RunStatus.ACTIVE)
        startTimer()
        try {
            exerciseClient.resumeExerciseAsync()
        } catch (_: Exception) {}
    }

    fun finishRun() {
        stopTimer()
        val current = _sessionState.value
        _sessionState.value = current.copy(status = RunStatus.COMPLETED)
        try {
            exerciseClient.endExerciseAsync()
            exerciseClient.clearUpdateCallbackAsync(exerciseCallback)
        } catch (_: Exception) {}

        persistRunCompletion(current.distanceMiles, current.elapsedSeconds)
    }

    fun simulateDistance(deltaMiles: Double = 0.25) {
        val current = _sessionState.value
        val newMiles = round((current.distanceMiles + deltaMiles) * 100.0) / 100.0
        val simulatedHr = if (current.heartRateBpm == 0) 142 else current.heartRateBpm + 2

        if (current.status == RunStatus.PREPARING) {
            startRun()
        }

        updateMetrics(miles = newMiles, hrBpm = simulatedHr)
    }

    fun resetSession() {
        stopTimer()
        lastMilestoneHapticTriggered = 0.0
        try {
            exerciseClient.endExerciseAsync()
            exerciseClient.clearUpdateCallbackAsync(exerciseCallback)
        } catch (_: Exception) {}
        _sessionState.value = RunSessionState()
    }

    private fun updateMetrics(miles: Double, hrBpm: Int) {
        val current = _sessionState.value
        val remaining = (1.00 - miles).coerceAtLeast(0.00)
        val roundedRemaining = round(remaining * 100.0) / 100.0

        val pace = computePace(miles, current.elapsedSeconds)

        val isNowComplete = miles >= 1.00 && !current.hasCompletedOneMile

        _sessionState.value = current.copy(
            distanceMiles = miles,
            heartRateBpm = hrBpm,
            currentPaceMinPerMile = pace,
            remainingMilesToPonyta = roundedRemaining,
            hasCompletedOneMile = current.hasCompletedOneMile || isNowComplete,
            status = if (isNowComplete) RunStatus.COMPLETED else current.status
        )

        // Trigger Live Buddy Coaching Haptics at 0.25, 0.50, 0.75, 1.00 mi
        checkAndTriggerMilestoneHaptics(miles)

        // AUTO-COMPLETION TRIGGER
        if (isNowComplete) {
            stopTimer()
            try {
                exerciseClient.endExerciseAsync()
            } catch (_: Exception) {}

            persistRunCompletion(miles, current.elapsedSeconds)
        }
    }

    private fun checkAndTriggerMilestoneHaptics(miles: Double) {
        if (miles >= 1.00 && lastMilestoneHapticTriggered < 1.00) {
            lastMilestoneHapticTriggered = 1.00
            wearHapticController.playUnlockFanfareHaptic()
        } else if (miles >= 0.75 && lastMilestoneHapticTriggered < 0.75) {
            lastMilestoneHapticTriggered = 0.75
            wearHapticController.playRunMilestoneThreeQuartersHaptic()
        } else if (miles >= 0.50 && lastMilestoneHapticTriggered < 0.50) {
            lastMilestoneHapticTriggered = 0.50
            wearHapticController.playRunMilestoneHalfHaptic()
        } else if (miles >= 0.25 && lastMilestoneHapticTriggered < 0.25) {
            lastMilestoneHapticTriggered = 0.25
            wearHapticController.playRunMilestoneQuarterHaptic()
        }
    }

    private fun computePace(miles: Double, elapsedSeconds: Long): String {
        if (miles <= 0.01 || elapsedSeconds <= 0) return "--'--\"/mi"
        val secondsPerMile = (elapsedSeconds / miles).toLong()
        val minutes = secondsPerMile / 60
        val seconds = secondsPerMile % 60
        return "%d'%02d\"/mi".format(minutes, seconds)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = managerScope.launch {
            while (isActive) {
                delay(1000L)
                val current = _sessionState.value
                if (current.status == RunStatus.ACTIVE) {
                    val newElapsed = current.elapsedSeconds + 1L
                    val pace = computePace(current.distanceMiles, newElapsed)
                    _sessionState.value = current.copy(
                        elapsedSeconds = newElapsed,
                        currentPaceMinPerMile = pace
                    )
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun persistRunCompletion(miles: Double, elapsedSeconds: Long) {
        managerScope.launch {
            val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            habitRepository.logHabit(
                dateIso = todayIso,
                habitType = HabitType.RUN_MILES,
                numericValue = miles,
                durationSeconds = elapsedSeconds.toInt(),
                sourceDevice = DeviceSource.WEAR
            )
            evaluateAchievementsUseCase.evaluate(todayIso)
        }
    }
}
