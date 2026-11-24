package com.poketch.core.sync

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.poketch.core.data.gamification.EvaluateAchievementsUseCase
import com.poketch.core.data.gamification.UnlockEventDispatcher
import com.poketch.core.domain.engine.UnlockEvent
import com.poketch.core.domain.model.ActivityLevel
import com.poketch.core.domain.model.BiologicalSex
import com.poketch.core.domain.model.CalorieLog
import com.poketch.core.domain.model.DeficitTier
import com.poketch.core.domain.model.DeviceSource
import com.poketch.core.domain.model.HabitLog
import com.poketch.core.domain.model.HabitType
import com.poketch.core.domain.model.MealCategory
import com.poketch.core.domain.model.RecoveryQuest
import com.poketch.core.domain.model.UserProfile
import com.poketch.core.domain.repository.CalorieRepository
import com.poketch.core.domain.repository.HabitRepository
import com.poketch.core.domain.repository.PokemonRepository
import com.poketch.core.domain.repository.UserRepository
import com.poketch.core.domain.repository.WeatherRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
open class BasePoketchWearableListenerService : WearableListenerService() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var calorieRepository: CalorieRepository

    @Inject
    lateinit var habitRepository: HabitRepository

    @Inject
    lateinit var pokemonRepository: PokemonRepository

    @Inject
    lateinit var weatherRepository: WeatherRepository

    @Inject
    lateinit var evaluateAchievementsUseCase: EvaluateAchievementsUseCase

    @Inject
    lateinit var unlockEventDispatcher: UnlockEventDispatcher

    @Inject
    lateinit var syncManager: PoketchSyncManager

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PoketchWearableListenerServiceEntryPoint {
        fun userRepository(): UserRepository
        fun calorieRepository(): CalorieRepository
        fun habitRepository(): HabitRepository
        fun pokemonRepository(): PokemonRepository
        fun weatherRepository(): WeatherRepository
        fun evaluateAchievementsUseCase(): EvaluateAchievementsUseCase
        fun unlockEventDispatcher(): UnlockEventDispatcher
        fun syncManager(): PoketchSyncManager
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        ensureDependenciesInjected()
    }

    private fun ensureDependenciesInjected() {
        if (!::userRepository.isInitialized) {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                PoketchWearableListenerServiceEntryPoint::class.java
            )
            userRepository = entryPoint.userRepository()
            calorieRepository = entryPoint.calorieRepository()
            habitRepository = entryPoint.habitRepository()
            pokemonRepository = entryPoint.pokemonRepository()
            weatherRepository = entryPoint.weatherRepository()
            evaluateAchievementsUseCase = entryPoint.evaluateAchievementsUseCase()
            unlockEventDispatcher = entryPoint.unlockEventDispatcher()
            syncManager = entryPoint.syncManager()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    private data class SyncDataEvent(val path: String, val dataMap: DataMap)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val extractedEvents = mutableListOf<SyncDataEvent>()
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path ?: ""
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                extractedEvents.add(SyncDataEvent(path, dataMap))
            }
        }

        serviceScope.launch {
            ensureDependenciesInjected()
            for (event in extractedEvents) {
                processDataChanged(event.path, event.dataMap)
            }
        }
    }

    open suspend fun processDataChanged(path: String, dataMap: DataMap) {
        when {
            path == PoketchSyncConstants.PATH_PROFILE_SETTINGS -> {
                handleProfileSettingsSync(dataMap)
            }
            path == PoketchSyncConstants.PATH_LOGS_CALORIES_BATCH -> {
                handleCalorieBatchSync(dataMap)
            }
            path.startsWith(PoketchSyncConstants.PATH_LOGS_CALORIES_PREFIX) -> {
                handleCalorieLogSync(path, dataMap)
            }
            path == PoketchSyncConstants.PATH_LOGS_HABITS_BATCH -> {
                handleHabitBatchSync(dataMap)
            }
            path.startsWith(PoketchSyncConstants.PATH_LOGS_HABITS_PREFIX) -> {
                handleHabitLogSync(path, dataMap)
            }
            path == PoketchSyncConstants.PATH_POKEDEX_UNLOCKS -> {
                handlePokedexUnlocksSync(dataMap)
            }
            path == PoketchSyncConstants.PATH_WEATHER_HOURLY_CACHE -> {
                handleWeatherHourlyCacheSync(dataMap)
            }
            path == PoketchSyncConstants.PATH_LOCATION_TIMEZONE -> {
                handleLocationTimeZoneSync(dataMap)
            }
            path == PoketchSyncConstants.PATH_RECOVERY_QUEST -> {
                handleRecoveryQuestSync(dataMap)
            }
        }
    }

    private fun handleRecoveryQuestSync(dataMap: DataMap) {
        val dateIso = dataMap.getString("dateIso") ?: return
        val surplusKcal = dataMap.getInt("surplusKcal", 0)
        val targetOffsetSteps = dataMap.getInt("targetOffsetSteps", 0)
        val startingStepsSnapshot = dataMap.getInt("startingStepsSnapshot", 0)
        val currentOffsetSteps = dataMap.getInt("currentOffsetSteps", 0)
        val isCompleted = dataMap.getBoolean("isCompleted", false)

        val quest = RecoveryQuest(
            dateIso = dateIso,
            surplusKcal = surplusKcal,
            targetOffsetSteps = targetOffsetSteps,
            startingStepsSnapshot = startingStepsSnapshot,
            currentOffsetSteps = currentOffsetSteps,
            isCompleted = isCompleted
        )
        syncManager.emitRecoveryQuest(quest)
    }

    private fun handleLocationTimeZoneSync(dataMap: DataMap) {
        if (!::weatherRepository.isInitialized) return
        val lat = if (dataMap.containsKey("latitude")) dataMap.getDouble("latitude") else null
        val lon = if (dataMap.containsKey("longitude")) dataMap.getDouble("longitude") else null
        val tz = dataMap.getString("timeZoneId") ?: return
        if (tz.isNotBlank()) {
            weatherRepository.setLocation(lat, lon, tz)
        }
    }

    private suspend fun handleWeatherHourlyCacheSync(dataMap: DataMap) {
        if (!::weatherRepository.isInitialized) return
        val json = dataMap.getString("weather_json") ?: return
        weatherRepository.cacheHourlyForecastJson(json)
    }

    private suspend fun handleProfileSettingsSync(dataMap: DataMap) {
        val remoteUpdatedAt = dataMap.getLong("updatedAtEpochMillis", 0L)
        val localProfile = userRepository.getUserProfile()
        val localUpdatedAt = localProfile?.updatedAtEpochMillis ?: 0L

        // Last-Write-Wins
        if (remoteUpdatedAt > localUpdatedAt || localProfile == null) {
            val age = dataMap.getInt("ageYears", 25)
            val sexStr = dataMap.getString("sex") ?: "MALE"
            val sex = runCatching { BiologicalSex.valueOf(sexStr) }.getOrDefault(BiologicalSex.MALE)
            val height = dataMap.getDouble("heightCm", 175.0)
            val weight = dataMap.getDouble("weightKg", 70.0)
            val activityStr = dataMap.getString("activityLevel") ?: "MODERATELY_ACTIVE"
            val activityLevel = runCatching { ActivityLevel.valueOf(activityStr) }.getOrDefault(ActivityLevel.MODERATELY_ACTIVE)
            val deficitStr = dataMap.getString("deficitTier") ?: "STANDARD"
            val deficitTier = runCatching { DeficitTier.valueOf(deficitStr) }.getOrDefault(DeficitTier.STANDARD)
            val dailyDeficitGoalKcal = dataMap.getInt("dailyDeficitGoalKcal", deficitTier.dailyDeficitKcal)
            val bmr = dataMap.getInt("bmrKcal", 1600)
            val tdee = dataMap.getInt("tdeeKcal", 2200)
            val ceiling = dataMap.getInt("targetCalorieCeiling", 1700)
            val waterGoal = dataMap.getInt("dailyWaterGoalMl", 2500)
            val activeBuddy = dataMap.getInt("activeBuddyPokemonId", 1)

            val updatedProfile = UserProfile(
                id = 1,
                ageYears = age.coerceIn(13, 120),
                sex = sex,
                heightCm = height.coerceIn(100.0, 250.0),
                weightKg = weight.coerceIn(30.0, 300.0),
                activityLevel = activityLevel,
                deficitTier = deficitTier,
                dailyDeficitGoalKcal = dailyDeficitGoalKcal,
                bmrKcal = bmr,
                tdeeKcal = tdee,
                targetCalorieCeiling = ceiling,
                dailyWaterGoalMl = if (waterGoal > 0) waterGoal else 2500,
                activeBuddyPokemonId = activeBuddy.coerceIn(1, 151),
                updatedAtEpochMillis = remoteUpdatedAt
            )
            userRepository.upsertUserProfile(updatedProfile)

            // Recalculating targetCalorieCeiling must re-evaluate all synced calorie logs for today
            // and trigger /alert/over_calorie if intake exceeds the new ceiling.
            val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val totalCalories = calorieRepository.observeTotalCaloriesByDate(todayIso).firstOrNull() ?: 0
            if (totalCalories > updatedProfile.targetCalorieCeiling) {
                val surplus = totalCalories - updatedProfile.targetCalorieCeiling
                syncManager.sendOverCalorieAlert(surplus)
                syncManager.emitOverCalorieAlert(surplus)
            }
        }
    }

    private suspend fun handleCalorieLogSync(path: String, dataMap: DataMap) {
        val logId = dataMap.getString("logId") ?: path.substringAfterLast("/")
        val dateIso = dataMap.getString("dateIso") ?: return
        val mealCategoryStr = dataMap.getString("mealCategory") ?: MealCategory.WATCH_QUICK_ADD.name
        val mealCategory = runCatching { MealCategory.valueOf(mealCategoryStr) }.getOrDefault(MealCategory.WATCH_QUICK_ADD)
        val foodDescription = dataMap.getString("foodDescription") ?: "Logged Meal"
        val caloriesKcal = dataMap.getInt("caloriesKcal", 0)
        if (caloriesKcal <= 0) return

        val proteinG = dataMap.getInt("proteinG", 0).coerceAtLeast(0)
        val carbsG = dataMap.getInt("carbsG", 0).coerceAtLeast(0)
        val fatG = dataMap.getInt("fatG", 0).coerceAtLeast(0)
        val sourceDeviceStr = dataMap.getString("sourceDevice") ?: DeviceSource.WEAR.name
        val sourceDevice = runCatching { DeviceSource.valueOf(sourceDeviceStr) }.getOrDefault(DeviceSource.WEAR)
        val timestampMillis = dataMap.getLong("timestampMillis", System.currentTimeMillis())

        val calorieLog = CalorieLog(
            logId = logId,
            dateIso = dateIso,
            mealCategory = mealCategory,
            foodDescription = foodDescription,
            caloriesKcal = caloriesKcal,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG,
            sourceDevice = sourceDevice,
            timestampMillis = timestampMillis,
            isSynced = true
        )
        // Idempotent upsert
        calorieRepository.upsertCalorieLog(calorieLog)

        // Automatically generate or update recovery quest if surplus exists
        syncManager.reEvaluateDailyCalorieCeiling(dateIso)

        val evalResult = evaluateAchievementsUseCase.evaluate(dateIso)
        if (evalResult.newlyUnlockedEvents.isNotEmpty()) {
            syncManager.syncPokedexCatalog()
            for (event in evalResult.newlyUnlockedEvents) {
                syncManager.sendPokemonUnlocked(event.pokemon.pokedexNumber, event.triggerReason)
            }
        }
    }

    private suspend fun handleCalorieBatchSync(dataMap: DataMap) {
        val remoteList = dataMap.getDataMapArrayList("calories_list") ?: emptyList()
        if (remoteList.isEmpty()) return

        val affectedDates = mutableSetOf<String>()
        val logsToUpsert = mutableListOf<CalorieLog>()

        for (item in remoteList) {
            val logId = item.getString("logId") ?: continue
            val dateIso = item.getString("dateIso") ?: continue
            val mealCategoryStr = item.getString("mealCategory") ?: MealCategory.WATCH_QUICK_ADD.name
            val mealCategory = runCatching { MealCategory.valueOf(mealCategoryStr) }.getOrDefault(MealCategory.WATCH_QUICK_ADD)
            val foodDescription = item.getString("foodDescription") ?: "Logged Meal"
            val caloriesKcal = item.getInt("caloriesKcal", 0)
            if (caloriesKcal <= 0) continue

            val proteinG = item.getInt("proteinG", 0).coerceAtLeast(0)
            val carbsG = item.getInt("carbsG", 0).coerceAtLeast(0)
            val fatG = item.getInt("fatG", 0).coerceAtLeast(0)
            val sourceDeviceStr = item.getString("sourceDevice") ?: DeviceSource.WEAR.name
            val sourceDevice = runCatching { DeviceSource.valueOf(sourceDeviceStr) }.getOrDefault(DeviceSource.WEAR)
            val timestampMillis = item.getLong("timestampMillis", System.currentTimeMillis())

            val calorieLog = CalorieLog(
                logId = logId,
                dateIso = dateIso,
                mealCategory = mealCategory,
                foodDescription = foodDescription,
                caloriesKcal = caloriesKcal,
                proteinG = proteinG,
                carbsG = carbsG,
                fatG = fatG,
                sourceDevice = sourceDevice,
                timestampMillis = timestampMillis,
                isSynced = true
            )
            logsToUpsert.add(calorieLog)
            affectedDates.add(dateIso)
        }

        if (logsToUpsert.isNotEmpty()) {
            calorieRepository.upsertCalorieLogs(logsToUpsert)
        }

        for (dateIso in affectedDates) {
            syncManager.reEvaluateDailyCalorieCeiling(dateIso)

            val evalResult = evaluateAchievementsUseCase.evaluate(dateIso)
            if (evalResult.newlyUnlockedEvents.isNotEmpty()) {
                syncManager.syncPokedexCatalog()
                for (event in evalResult.newlyUnlockedEvents) {
                    syncManager.sendPokemonUnlocked(event.pokemon.pokedexNumber, event.triggerReason)
                }
            }
        }
    }

    private suspend fun handleHabitLogSync(path: String, dataMap: DataMap) {
        val logId = dataMap.getString("logId") ?: path.substringAfterLast("/")
        val dateIso = dataMap.getString("dateIso") ?: return
        val habitTypeStr = dataMap.getString("habitType") ?: return
        val habitType = runCatching { HabitType.valueOf(habitTypeStr) }.getOrNull() ?: return
        val numericValue = dataMap.getDouble("numericValue", 0.0)
        if (numericValue <= 0.0) return

        val durationSeconds = dataMap.getInt("durationSeconds", 0).coerceAtLeast(0)
        val sourceDeviceStr = dataMap.getString("sourceDevice") ?: DeviceSource.WEAR.name
        val sourceDevice = runCatching { DeviceSource.valueOf(sourceDeviceStr) }.getOrDefault(DeviceSource.WEAR)
        val timestampMillis = dataMap.getLong("timestampMillis", System.currentTimeMillis())

        val habitLog = HabitLog(
            logId = logId,
            dateIso = dateIso,
            habitType = habitType,
            numericValue = numericValue,
            durationSeconds = durationSeconds,
            sourceDevice = sourceDevice,
            timestampMillis = timestampMillis,
            isSynced = true
        )
        // Idempotent upsert
        habitRepository.upsertHabitLog(habitLog)
        val evalResult = evaluateAchievementsUseCase.evaluate(dateIso)
        if (evalResult.newlyUnlockedEvents.isNotEmpty()) {
            syncManager.syncPokedexCatalog()
            for (event in evalResult.newlyUnlockedEvents) {
                syncManager.sendPokemonUnlocked(event.pokemon.pokedexNumber, event.triggerReason)
            }
        }
    }

    private suspend fun handleHabitBatchSync(dataMap: DataMap) {
        val remoteList = dataMap.getDataMapArrayList("habits_list") ?: emptyList()
        if (remoteList.isEmpty()) return

        val affectedDates = mutableSetOf<String>()
        val habitsToUpsert = mutableListOf<HabitLog>()

        for (item in remoteList) {
            val logId = item.getString("logId") ?: continue
            val dateIso = item.getString("dateIso") ?: continue
            val habitTypeStr = item.getString("habitType") ?: continue
            val habitType = runCatching { HabitType.valueOf(habitTypeStr) }.getOrNull() ?: continue
            val numericValue = item.getDouble("numericValue", 0.0)
            if (numericValue <= 0.0) continue

            val durationSeconds = item.getInt("durationSeconds", 0).coerceAtLeast(0)
            val sourceDeviceStr = item.getString("sourceDevice") ?: DeviceSource.WEAR.name
            val sourceDevice = runCatching { DeviceSource.valueOf(sourceDeviceStr) }.getOrDefault(DeviceSource.WEAR)
            val timestampMillis = item.getLong("timestampMillis", System.currentTimeMillis())

            val habitLog = HabitLog(
                logId = logId,
                dateIso = dateIso,
                habitType = habitType,
                numericValue = numericValue,
                durationSeconds = durationSeconds,
                sourceDevice = sourceDevice,
                timestampMillis = timestampMillis,
                isSynced = true
            )
            habitsToUpsert.add(habitLog)
            affectedDates.add(dateIso)
        }

        if (habitsToUpsert.isNotEmpty()) {
            habitRepository.upsertHabitLogs(habitsToUpsert)
        }

        for (dateIso in affectedDates) {
            val evalResult = evaluateAchievementsUseCase.evaluate(dateIso)
            if (evalResult.newlyUnlockedEvents.isNotEmpty()) {
                syncManager.syncPokedexCatalog()
                for (event in evalResult.newlyUnlockedEvents) {
                    syncManager.sendPokemonUnlocked(event.pokemon.pokedexNumber, event.triggerReason)
                }
            }
        }
    }

    private suspend fun handlePokedexUnlocksSync(dataMap: DataMap) {
        val remoteList = dataMap.getDataMapArrayList("pokemon_list") ?: emptyList()
        if (remoteList.isEmpty()) return

        val localCatalog = pokemonRepository.observeAllPokemon().firstOrNull().orEmpty()
        val localMap = localCatalog.associateBy { it.pokedexNumber }.toMutableMap()

        val newlyUnlockedToCelebrate = mutableListOf<UnlockEvent.NewPokemonUnlocked>()
        var hasChanges = false

        for (item in remoteList) {
            val pokedexNumber = item.getInt("pokedexNumber")
            val remoteProgress = item.getInt("currentProgress")
            val remoteUnlocked = item.getBoolean("isUnlocked")
            val remoteUnlockedAt = if (item.containsKey("unlockedAtMillis")) item.getLong("unlockedAtMillis") else null

            val existing = localMap[pokedexNumber]
            if (existing != null) {
                // Monotonic Union Merge
                val mergedProgress = maxOf(existing.currentProgress, remoteProgress)
                val mergedUnlocked = existing.isUnlocked || remoteUnlocked || (mergedProgress >= existing.unlockRule.targetThreshold)
                val mergedUnlockedAt = existing.unlockedAtMillis ?: remoteUnlockedAt ?: if (mergedUnlocked) System.currentTimeMillis() else null

                if (mergedProgress != existing.currentProgress || mergedUnlocked != existing.isUnlocked) {
                    val updated = existing.copy(
                        currentProgress = mergedProgress,
                        isUnlocked = mergedUnlocked,
                        unlockedAtMillis = mergedUnlockedAt
                    )
                    localMap[pokedexNumber] = updated
                    hasChanges = true

                    if (!existing.isUnlocked && mergedUnlocked) {
                        newlyUnlockedToCelebrate.add(
                            UnlockEvent.NewPokemonUnlocked(
                                pokemon = updated,
                                triggerReason = "Cross-Device Pokédex Sync",
                                timestampMillis = mergedUnlockedAt ?: System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }

        if (hasChanges) {
            pokemonRepository.upsertAllPokemon(localMap.values.toList())
        }
        if (newlyUnlockedToCelebrate.isNotEmpty()) {
            unlockEventDispatcher.emitUnlockEvents(newlyUnlockedToCelebrate)
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val data = messageEvent.data

        serviceScope.launch {
            ensureDependenciesInjected()
            processMessageReceived(path, data)
        }
    }

    open suspend fun processMessageReceived(path: String, data: ByteArray) {
        when (path) {
            PoketchSyncConstants.PATH_ALERT_OVER_CALORIE -> {
                val surplus = String(data, Charsets.UTF_8).toIntOrNull() ?: 0
                syncManager.emitOverCalorieAlert(surplus)
            }
            PoketchSyncConstants.PATH_HANDSHAKE -> {
                syncManager.refreshConnectedNodes()
                syncManager.flushOfflineQueue()
            }
            PoketchSyncConstants.PATH_EVENT_NEW_POKEMON -> {
                val payload = String(data, Charsets.UTF_8)
                val parts = payload.split("|")
                val pokemonNumber = parts.getOrNull(0)?.toIntOrNull()
                val reason = parts.getOrNull(1) ?: "Cross-Device Unlock Event"
                if (pokemonNumber != null) {
                    val pokemon = pokemonRepository.observePokemonById(pokemonNumber).firstOrNull()
                    if (pokemon != null) {
                        unlockEventDispatcher.emitUnlockEvents(
                            listOf(
                                UnlockEvent.NewPokemonUnlocked(
                                    pokemon = pokemon,
                                    triggerReason = reason,
                                    timestampMillis = System.currentTimeMillis()
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    override fun onPeerConnected(peer: com.google.android.gms.wearable.Node) {
        super.onPeerConnected(peer)
        serviceScope.launch {
            ensureDependenciesInjected()
            syncManager.onPeerStatusChanged(peer.id, isConnected = true)
        }
    }

    override fun onPeerDisconnected(peer: com.google.android.gms.wearable.Node) {
        super.onPeerDisconnected(peer)
        serviceScope.launch {
            ensureDependenciesInjected()
            syncManager.onPeerStatusChanged(peer.id, isConnected = false)
        }
    }
}
