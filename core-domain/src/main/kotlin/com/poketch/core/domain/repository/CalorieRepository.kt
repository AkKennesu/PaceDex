package com.poketch.core.domain.repository

import com.poketch.core.domain.calculator.CalorieBudgetState
import com.poketch.core.domain.model.CalorieLog
import kotlinx.coroutines.flow.Flow

interface CalorieRepository {
    fun observeLogsByDate(dateIso: String): Flow<List<CalorieLog>>
    fun observeTotalCaloriesByDate(dateIso: String): Flow<Int>
    fun observeDailyBudgetState(dateIso: String): Flow<CalorieBudgetState>
    suspend fun upsertCalorieLog(log: CalorieLog)
    suspend fun upsertCalorieLogs(logs: List<CalorieLog>)
    suspend fun deleteCalorieLog(logId: String)
    suspend fun getUnsyncedLogs(): List<CalorieLog>
    suspend fun markLogsAsSynced(logIds: List<String>)
}
