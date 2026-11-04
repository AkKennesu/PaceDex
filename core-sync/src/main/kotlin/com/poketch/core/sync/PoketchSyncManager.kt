package com.poketch.core.sync

import android.content.Context
import com.google.android.gms.wearable.DataMap
import com.poketch.core.data.gamification.UnlockEventDispatcher
import com.poketch.core.domain.model.CalorieLog
import com.poketch.core.domain.model.HabitLog
import com.poketch.core.domain.repository.CalorieRepository
import com.poketch.core.domain.repository.HabitRepository
import com.poketch.core.domain.repository.PokemonRepository
import com.poketch.core.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PoketchSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    val transport: WearableTransport,
    private val userRepository: UserRepository,
    private val calorieRepository: CalorieRepository,
    private val habitRepository: HabitRepository,
    private val pokemonRepository: PokemonRepository,
    private val unlockEventDispatcher: UnlockEventDispatcher
) {

    private val _overCalorieAlerts = MutableSharedFlow<Int>(extraBufferCapacity = 16)
    val overCalorieAlerts: SharedFlow<Int> = _overCalorieAlerts.asSharedFlow()

    var syncDispatcher: CoroutineDispatcher = Dispatchers.IO
        set(value) {
            field = value
            syncScope = CoroutineScope(SupervisorJob() + value)
        }
    private var syncScope = CoroutineScope(SupervisorJob() + syncDispatcher)

    // Debounce coalescing for rapid habit taps (e.g., +250 ml water tapped 3 times)
    private val habitCoalesceMutex = Mutex()
    private val pendingHabitBurst = mutableListOf<HabitLog>()
    private var habitCoalesceJob: Job? = null

    // Debounce coalescing for rapid calorie entries
    private val calorieCoalesceMutex = Mutex()
    private val pendingCalorieBurst = mutableListOf<CalorieLog>()
    private var calorieCoalesceJob: Job? = null

    suspend fun emitOverCalorieAlert(surplusKcal: Int) {
        _overCalorieAlerts.emit(surplusKcal)
    }

    suspend fun sendOverCalorieAlert(surplusKcal: Int) {
        val payload = surplusKcal.toString().toByteArray(Charsets.UTF_8)
        runCatching {
            transport.sendMessage(PoketchSyncConstants.PATH_ALERT_OVER_CALORIE, payload)
        }
    }

    suspend fun sendPokemonUnlocked(pokemonNumber: Int, triggerReason: String) {
        val payload = "$pokemonNumber|$triggerReason".toByteArray(Charsets.UTF_8)
        runCatching {
            transport.sendMessage(PoketchSyncConstants.PATH_EVENT_NEW_POKEMON, payload)
        }
    }

    suspend fun reEvaluateDailyCalorieCeiling(dateIso: String? = null): Int {
        val profile = userRepository.getUserProfile() ?: return 0
        val ceiling = profile.targetCalorieCeiling
        if (ceiling <= 0) return 0
        val targetDate = dateIso ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val totalCalories = calorieRepository.observeTotalCaloriesByDate(targetDate).firstOrNull() ?: 0
        if (totalCalories > ceiling) {
            val surplus = totalCalories - ceiling
            sendOverCalorieAlert(surplus)
            emitOverCalorieAlert(surplus)
            return surplus
        }
        return 0
    }

    suspend fun syncUserProfile() {
        val profile = userRepository.getUserProfile() ?: return
        val dataMap = DataMap().apply {
            putInt("id", profile.id)
            putInt("ageYears", profile.ageYears)
            putString("sex", profile.sex.name)
            putDouble("heightCm", profile.heightCm)
            putDouble("weightKg", profile.weightKg)
            putString("activityLevel", profile.activityLevel.name)
            putString("deficitTier", profile.deficitTier.name)
            putInt("dailyDeficitGoalKcal", profile.dailyDeficitGoalKcal)
            putInt("bmrKcal", profile.bmrKcal)
            putInt("tdeeKcal", profile.tdeeKcal)
            putInt("targetCalorieCeiling", profile.targetCalorieCeiling)
            putInt("dailyWaterGoalMl", profile.dailyWaterGoalMl)
            putInt("activeBuddyPokemonId", profile.activeBuddyPokemonId)
            putLong("updatedAtEpochMillis", profile.updatedAtEpochMillis)
            putLong("syncTimestamp", System.currentTimeMillis())
        }
        transport.putDataMap(PoketchSyncConstants.PATH_PROFILE_SETTINGS, dataMap)
        reEvaluateDailyCalorieCeiling()
    }

    suspend fun syncPokedexCatalog() {
        val catalog = pokemonRepository.observeAllPokemon().firstOrNull().orEmpty()
        val pokemonDataMaps = ArrayList<DataMap>()
        for (pokemon in catalog) {
            val item = DataMap().apply {
                putInt("pokedexNumber", pokemon.pokedexNumber)
                putInt("currentProgress", pokemon.currentProgress)
                putBoolean("isUnlocked", pokemon.isUnlocked)
                pokemon.unlockedAtMillis?.let { putLong("unlockedAtMillis", it) }
            }
            pokemonDataMaps.add(item)
        }

        val dataMap = DataMap().apply {
            putDataMapArrayList("pokemon_list", pokemonDataMaps)
            putLong("syncTimestamp", System.currentTimeMillis())
        }
        transport.putDataMap(PoketchSyncConstants.PATH_POKEDEX_UNLOCKS, dataMap)
    }

    suspend fun syncCalorieLog(log: CalorieLog) {
        val dataMap = DataMap().apply {
            putString("logId", log.logId)
            putString("dateIso", log.dateIso)
            putString("mealCategory", log.mealCategory.name)
            putString("foodDescription", log.foodDescription)
            putInt("caloriesKcal", log.caloriesKcal)
            putInt("proteinG", log.proteinG)
            putInt("carbsG", log.carbsG)
            putInt("fatG", log.fatG)
            putString("sourceDevice", log.sourceDevice.name)
            putLong("timestampMillis", log.timestampMillis)
            putBoolean("isSynced", true)
            putLong("syncTimestamp", System.currentTimeMillis())
        }
        transport.putDataMap("${PoketchSyncConstants.PATH_LOGS_CALORIES_PREFIX}${log.logId}", dataMap)
    }

    suspend fun syncCaloriesBatch(logs: List<CalorieLog>) {
        if (logs.isEmpty()) return
        val calorieDataMaps = ArrayList<DataMap>()
        for (log in logs) {
            val item = DataMap().apply {
                putString("logId", log.logId)
                putString("dateIso", log.dateIso)
                putString("mealCategory", log.mealCategory.name)
                putString("foodDescription", log.foodDescription)
                putInt("caloriesKcal", log.caloriesKcal)
                putInt("proteinG", log.proteinG)
                putInt("carbsG", log.carbsG)
                putInt("fatG", log.fatG)
                putString("sourceDevice", log.sourceDevice.name)
                putLong("timestampMillis", log.timestampMillis)
                putBoolean("isSynced", true)
            }
            calorieDataMaps.add(item)
        }

        val dataMap = DataMap().apply {
            putDataMapArrayList("calories_list", calorieDataMaps)
            putLong("syncTimestamp", System.currentTimeMillis())
        }
        transport.putDataMap(PoketchSyncConstants.PATH_LOGS_CALORIES_BATCH, dataMap)
    }

    suspend fun coalesceAndSyncCalorie(log: CalorieLog, debounceMillis: Long = 300L) {
        calorieCoalesceMutex.withLock {
            pendingCalorieBurst.add(log)
            calorieCoalesceJob?.cancel()
            calorieCoalesceJob = syncScope.launch {
                delay(debounceMillis)
                val batch = calorieCoalesceMutex.withLock {
                    val items = ArrayList(pendingCalorieBurst)
                    pendingCalorieBurst.clear()
                    items
                }
                if (batch.isNotEmpty()) {
                    runCatching {
                        syncCaloriesBatch(batch)
                        calorieRepository.markLogsAsSynced(batch.map { it.logId })
                    }
                }
            }
        }
    }

    suspend fun flushCoalescedCalories() {
        val batch = calorieCoalesceMutex.withLock {
            calorieCoalesceJob?.cancel()
            calorieCoalesceJob = null
            val items = ArrayList(pendingCalorieBurst)
            pendingCalorieBurst.clear()
            items
        }
        if (batch.isNotEmpty()) {
            syncCaloriesBatch(batch)
            calorieRepository.markLogsAsSynced(batch.map { it.logId })
        }
    }

    suspend fun syncHabitLog(habit: HabitLog) {
        val dataMap = DataMap().apply {
            putString("logId", habit.logId)
            putString("dateIso", habit.dateIso)
            putString("habitType", habit.habitType.name)
            putDouble("numericValue", habit.numericValue)
            putInt("durationSeconds", habit.durationSeconds)
            putString("sourceDevice", habit.sourceDevice.name)
            putLong("timestampMillis", habit.timestampMillis)
            putBoolean("isSynced", true)
            putLong("syncTimestamp", System.currentTimeMillis())
        }
        transport.putDataMap("${PoketchSyncConstants.PATH_LOGS_HABITS_PREFIX}${habit.logId}", dataMap)
    }

    suspend fun syncHabitsBatch(habits: List<HabitLog>) {
        if (habits.isEmpty()) return
        val habitDataMaps = ArrayList<DataMap>()
        for (habit in habits) {
            val item = DataMap().apply {
                putString("logId", habit.logId)
                putString("dateIso", habit.dateIso)
                putString("habitType", habit.habitType.name)
                putDouble("numericValue", habit.numericValue)
                putInt("durationSeconds", habit.durationSeconds)
                putString("sourceDevice", habit.sourceDevice.name)
                putLong("timestampMillis", habit.timestampMillis)
                putBoolean("isSynced", true)
            }
            habitDataMaps.add(item)
        }

        val dataMap = DataMap().apply {
            putDataMapArrayList("habits_list", habitDataMaps)
            putLong("syncTimestamp", System.currentTimeMillis())
        }
        transport.putDataMap(PoketchSyncConstants.PATH_LOGS_HABITS_BATCH, dataMap)
    }

    suspend fun coalesceAndSyncHabit(habit: HabitLog, debounceMillis: Long = 300L) {
        habitCoalesceMutex.withLock {
            pendingHabitBurst.add(habit)
            habitCoalesceJob?.cancel()
            habitCoalesceJob = syncScope.launch {
                delay(debounceMillis)
                val batch = habitCoalesceMutex.withLock {
                    val items = ArrayList(pendingHabitBurst)
                    pendingHabitBurst.clear()
                    items
                }
                if (batch.isNotEmpty()) {
                    runCatching {
                        syncHabitsBatch(batch)
                        habitRepository.markHabitsAsSynced(batch.map { it.logId })
                    }
                }
            }
        }
    }

    suspend fun flushCoalescedHabits() {
        val batch = habitCoalesceMutex.withLock {
            habitCoalesceJob?.cancel()
            habitCoalesceJob = null
            val items = ArrayList(pendingHabitBurst)
            pendingHabitBurst.clear()
            items
        }
        if (batch.isNotEmpty()) {
            syncHabitsBatch(batch)
            habitRepository.markHabitsAsSynced(batch.map { it.logId })
        }
    }

    suspend fun flushOfflineQueue(): Boolean {
        var hasError = false

        // Flush any debounced coalesced bursts first
        runCatching { flushCoalescedCalories() }
        runCatching { flushCoalescedHabits() }

        // 1. Flush unsynced calorie logs in batch
        val unsyncedCalories = calorieRepository.getUnsyncedLogs()
        if (unsyncedCalories.isNotEmpty()) {
            try {
                syncCaloriesBatch(unsyncedCalories)
                calorieRepository.markLogsAsSynced(unsyncedCalories.map { it.logId })
            } catch (e: Exception) {
                hasError = true
            }
        }

        // 2. Flush unsynced habit logs in batch
        val unsyncedHabits = habitRepository.getUnsyncedHabits()
        if (unsyncedHabits.isNotEmpty()) {
            try {
                syncHabitsBatch(unsyncedHabits)
                habitRepository.markHabitsAsSynced(unsyncedHabits.map { it.logId })
            } catch (e: Exception) {
                hasError = true
            }
        }

        // 3. Sync User Profile & Pokédex state
        val profileResult = runCatching { syncUserProfile() }
        if (profileResult.isFailure) hasError = true

        val pokedexResult = runCatching { syncPokedexCatalog() }
        if (pokedexResult.isFailure) hasError = true

        _lastSyncTimestampMillis = System.currentTimeMillis()
        return !hasError
    }

    private var _lastSyncTimestampMillis: Long = System.currentTimeMillis()
    val lastSyncTimestampMillis: Long get() = _lastSyncTimestampMillis

    suspend fun syncWeatherHourlyCache(jsonString: String) {
        val dataMap = DataMap().apply {
            putString("weather_json", jsonString)
            putLong("timestamp", System.currentTimeMillis())
        }
        transport.putDataMap(PoketchSyncConstants.PATH_WEATHER_HOURLY_CACHE, dataMap)
    }

    suspend fun getSyncDiagnostics(): SyncDiagnosticsInfo {
        val nodes = runCatching { transport.getConnectedNodes() }.getOrElse { emptyList() }
        val unsyncedCalories = runCatching { calorieRepository.getUnsyncedLogs().size }.getOrElse { 0 }
        val unsyncedHabits = runCatching { habitRepository.getUnsyncedHabits().size }.getOrElse { 0 }

        return SyncDiagnosticsInfo(
            connectedNodes = nodes,
            pendingCaloriesCount = unsyncedCalories,
            pendingHabitsCount = unsyncedHabits,
            totalPendingCount = unsyncedCalories + unsyncedHabits,
            lastSyncTimestampMillis = _lastSyncTimestampMillis
        )
    }

    suspend fun forceFullSync(): SyncDiagnosticsInfo {
        flushOfflineQueue()
        return getSyncDiagnostics()
    }
}

data class SyncDiagnosticsInfo(
    val connectedNodes: List<ConnectedNodeInfo> = emptyList(),
    val pendingCaloriesCount: Int = 0,
    val pendingHabitsCount: Int = 0,
    val totalPendingCount: Int = 0,
    val lastSyncTimestampMillis: Long = 0L
)
