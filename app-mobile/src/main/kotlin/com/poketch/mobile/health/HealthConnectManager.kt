package com.poketch.mobile.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.poketch.core.data.gamification.EvaluateAchievementsUseCase
import com.poketch.core.domain.model.DeviceSource
import com.poketch.core.domain.model.HabitType
import com.poketch.core.domain.repository.HabitRepository
import com.poketch.core.sync.PoketchSyncManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class HealthSyncSummary(
    val dateIso: String,
    val sleepHours: Double = 0.0,
    val stepsCount: Long = 0L,
    val distanceMiles: Double = 0.0,
    val isSuccess: Boolean = true,
    val message: String? = null
)

@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val habitRepository: HabitRepository,
    private val evaluateAchievementsUseCase: EvaluateAchievementsUseCase,
    private val syncManager: PoketchSyncManager? = null
) {

    val permissions = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class)
    )

    fun isHealthConnectAvailable(): Boolean {
        return runCatching {
            val status = HealthConnectClient.getSdkStatus(context)
            status == HealthConnectClient.SDK_AVAILABLE
        }.getOrDefault(false)
    }

    private fun getClient(): HealthConnectClient? {
        return if (isHealthConnectAvailable()) {
            runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
        } else {
            null
        }
    }

    suspend fun hasAllPermissions(): Boolean {
        val client = getClient() ?: return false
        return runCatching {
            val granted = client.permissionController.getGrantedPermissions()
            granted.containsAll(permissions)
        }.getOrDefault(false)
    }

    suspend fun syncHealthDataForDate(date: LocalDate = LocalDate.now()): HealthSyncSummary = withContext(Dispatchers.IO) {
        val dateIso = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val client = getClient()
        if (client == null) {
            return@withContext HealthSyncSummary(
                dateIso = dateIso,
                isSuccess = false,
                message = "Health Connect SDK is not available on this device."
            )
        }

        try {
            val zoneId = ZoneId.systemDefault()
            val startOfDay = date.atStartOfDay(zoneId).toInstant()
            val endOfDay = date.plusDays(1).atStartOfDay(zoneId).toInstant()

            // 1. Read Sleep Sessions (SleepSessionRecord)
            // Look from 18:00 previous day to 18:00 current day for typical sleep night
            val sleepStartTime = date.minusDays(1).atTime(18, 0).atZone(zoneId).toInstant()
            val sleepEndTime = date.atTime(18, 0).atZone(zoneId).toInstant()

            val sleepRequest = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(sleepStartTime, sleepEndTime)
            )
            val sleepResponse = client.readRecords(sleepRequest)

            var totalSleepSeconds = 0L
            for (session in sleepResponse.records) {
                val duration = Duration.between(session.startTime, session.endTime).seconds
                if (duration > 0) {
                    totalSleepSeconds += duration
                }
            }
            val totalSleepHours = totalSleepSeconds / 3600.0

            if (totalSleepHours > 0.0) {
                habitRepository.logHabit(
                    dateIso = dateIso,
                    habitType = HabitType.SLEEP_HOURS,
                    numericValue = totalSleepHours,
                    durationSeconds = totalSleepSeconds.toInt(),
                    sourceDevice = DeviceSource.MOBILE
                )
            }

            // 2. Read Steps (StepsRecord)
            val stepsAggregate = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                )
            )
            val totalSteps = stepsAggregate[StepsRecord.COUNT_TOTAL] ?: 0L
            if (totalSteps > 0) {
                habitRepository.logHabit(
                    dateIso = dateIso,
                    habitType = HabitType.DAILY_STEPS,
                    numericValue = totalSteps.toDouble(),
                    sourceDevice = DeviceSource.MOBILE
                )
                syncManager?.updateRecoveryQuestProgress(totalSteps.toInt())
            }

            // 3. Read Distance (DistanceRecord)
            val distanceAggregate = client.aggregate(
                AggregateRequest(
                    metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                )
            )
            val distanceMeters = distanceAggregate[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
            val distanceMiles = distanceMeters * 0.000621371
            if (distanceMiles > 0.0) {
                habitRepository.logHabit(
                    dateIso = dateIso,
                    habitType = HabitType.RUN_MILES,
                    numericValue = distanceMiles,
                    sourceDevice = DeviceSource.MOBILE
                )
            }

            // Trigger gamification / Pokédex unlock evaluation
            evaluateAchievementsUseCase.evaluate(dateIso)

            HealthSyncSummary(
                dateIso = dateIso,
                sleepHours = totalSleepHours,
                stepsCount = totalSteps,
                distanceMiles = distanceMiles,
                isSuccess = true,
                message = "Synced $totalSteps steps, ${String.format(Locale.US, "%.1f", totalSleepHours)}h sleep from Health Connect."
            )
        } catch (e: Exception) {
            HealthSyncSummary(
                dateIso = dateIso,
                isSuccess = false,
                message = "Sync failed: ${e.message}"
            )
        }
    }

    suspend fun syncPastDays(daysCount: Int = 7): List<HealthSyncSummary> {
        val today = LocalDate.now()
        val results = mutableListOf<HealthSyncSummary>()
        for (i in (daysCount - 1) downTo 0) {
            results.add(syncHealthDataForDate(today.minusDays(i.toLong())))
        }
        return results
    }
}
