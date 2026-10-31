package com.poketch.core.data.repository

import com.poketch.core.data.local.dao.CalorieDao
import com.poketch.core.data.local.dao.UserDao
import com.poketch.core.data.local.entity.toDomain
import com.poketch.core.data.local.entity.toEntity
import com.poketch.core.domain.calculator.CalorieBudgetState
import com.poketch.core.domain.calculator.CalorieDeficitCalculator
import com.poketch.core.domain.model.CalorieLog
import com.poketch.core.domain.repository.CalorieRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstCalorieRepository @Inject constructor(
    private val calorieDao: CalorieDao,
    private val userDao: UserDao
) : CalorieRepository {

    override fun observeLogsByDate(dateIso: String): Flow<List<CalorieLog>> {
        return calorieDao.observeLogsByDate(dateIso).map { list -> list.map { it.toDomain() } }
    }

    override fun observeTotalCaloriesByDate(dateIso: String): Flow<Int> {
        return calorieDao.observeTotalCaloriesByDate(dateIso)
    }

    override fun observeDailyBudgetState(dateIso: String): Flow<CalorieBudgetState> {
        val totalCaloriesFlow = calorieDao.observeTotalCaloriesByDate(dateIso)
        val userProfileFlow = userDao.observeUserProfile()

        return combine(totalCaloriesFlow, userProfileFlow) { consumed, userEntity ->
            val ceiling = userEntity?.targetCalorieCeiling ?: 2000
            CalorieDeficitCalculator.evaluateBudgetStatus(
                consumedKcal = consumed,
                targetCeilingKcal = ceiling
            )
        }
    }

    override suspend fun upsertCalorieLog(log: CalorieLog) {
        calorieDao.upsertCalorieLog(log.toEntity())
    }

    override suspend fun upsertCalorieLogs(logs: List<CalorieLog>) {
        calorieDao.upsertCalorieLogs(logs.map { it.toEntity() })
    }

    override suspend fun deleteCalorieLog(logId: String) {
        calorieDao.deleteCalorieLog(logId)
    }

    override suspend fun getUnsyncedLogs(): List<CalorieLog> {
        return calorieDao.getUnsyncedLogs().map { it.toDomain() }
    }

    override suspend fun markLogsAsSynced(logIds: List<String>) {
        if (logIds.isNotEmpty()) {
            calorieDao.markLogsAsSynced(logIds)
        }
    }
}
