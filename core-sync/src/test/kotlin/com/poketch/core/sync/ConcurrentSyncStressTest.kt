package com.poketch.core.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.poketch.core.data.gamification.EvaluateAchievementsUseCase
import com.poketch.core.data.gamification.UnlockEventDispatcher
import com.poketch.core.data.local.db.PoketchDatabase
import com.poketch.core.data.local.seed.PokemonSeedData
import com.poketch.core.data.repository.OfflineFirstCalorieRepository
import com.poketch.core.data.repository.OfflineFirstHabitRepository
import com.poketch.core.data.repository.OfflineFirstPokemonRepository
import com.poketch.core.data.repository.OfflineFirstUserRepository
import com.poketch.core.domain.model.ActivityLevel
import com.poketch.core.domain.model.BiologicalSex
import com.poketch.core.domain.model.CalorieLog
import com.poketch.core.domain.model.DeficitTier
import com.poketch.core.domain.model.DeviceSource
import com.poketch.core.domain.model.HabitLog
import com.poketch.core.domain.model.HabitType
import com.poketch.core.domain.model.MealCategory
import com.poketch.core.domain.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Collections
import java.util.UUID

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ConcurrentSyncStressTest {

    private lateinit var context: Context

    // Watch Node
    private lateinit var watchDatabase: PoketchDatabase
    private lateinit var watchUserRepository: OfflineFirstUserRepository
    private lateinit var watchCalorieRepository: OfflineFirstCalorieRepository
    private lateinit var watchHabitRepository: OfflineFirstHabitRepository
    private lateinit var watchPokemonRepository: OfflineFirstPokemonRepository
    private lateinit var watchUnlockEventDispatcher: UnlockEventDispatcher
    private lateinit var watchEvaluateAchievementsUseCase: EvaluateAchievementsUseCase
    private lateinit var watchSyncManager: PoketchSyncManager
    private lateinit var watchService: BasePoketchWearableListenerService

    // Phone Node
    private lateinit var phoneDatabase: PoketchDatabase
    private lateinit var phoneUserRepository: OfflineFirstUserRepository
    private lateinit var phoneCalorieRepository: OfflineFirstCalorieRepository
    private lateinit var phoneHabitRepository: OfflineFirstHabitRepository
    private lateinit var phonePokemonRepository: OfflineFirstPokemonRepository
    private lateinit var phoneUnlockEventDispatcher: UnlockEventDispatcher
    private lateinit var phoneEvaluateAchievementsUseCase: EvaluateAchievementsUseCase
    private lateinit var phoneSyncManager: PoketchSyncManager
    private lateinit var phoneService: BasePoketchWearableListenerService

    private lateinit var fakeTransport: FakeWearableTransport
    private val transmittedPaths = Collections.synchronizedList(mutableListOf<String>())

    private val todayIso = "2026-10-31"

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()

        // 1. Watch DB & Repositories
        watchDatabase = Room.inMemoryDatabaseBuilder(context, PoketchDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        watchDatabase.pokemonDao().upsertAllPokemon(PokemonSeedData.ALL_151)

        watchUserRepository = OfflineFirstUserRepository(watchDatabase.userDao())
        watchCalorieRepository = OfflineFirstCalorieRepository(watchDatabase.calorieDao(), watchDatabase.userDao())
        watchHabitRepository = OfflineFirstHabitRepository(watchDatabase.habitDao())
        watchPokemonRepository = OfflineFirstPokemonRepository(watchDatabase.pokemonDao())
        watchUnlockEventDispatcher = UnlockEventDispatcher()
        watchEvaluateAchievementsUseCase = EvaluateAchievementsUseCase(
            userRepository = watchUserRepository,
            calorieRepository = watchCalorieRepository,
            habitRepository = watchHabitRepository,
            pokemonRepository = watchPokemonRepository,
            unlockEventDispatcher = watchUnlockEventDispatcher
        )

        // 2. Phone DB & Repositories
        phoneDatabase = Room.inMemoryDatabaseBuilder(context, PoketchDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        phoneDatabase.pokemonDao().upsertAllPokemon(PokemonSeedData.ALL_151)

        phoneUserRepository = OfflineFirstUserRepository(phoneDatabase.userDao())
        phoneCalorieRepository = OfflineFirstCalorieRepository(phoneDatabase.calorieDao(), phoneDatabase.userDao())
        phoneHabitRepository = OfflineFirstHabitRepository(phoneDatabase.habitDao())
        phonePokemonRepository = OfflineFirstPokemonRepository(phoneDatabase.pokemonDao())
        phoneUnlockEventDispatcher = UnlockEventDispatcher()
        phoneEvaluateAchievementsUseCase = EvaluateAchievementsUseCase(
            userRepository = phoneUserRepository,
            calorieRepository = phoneCalorieRepository,
            habitRepository = phoneHabitRepository,
            pokemonRepository = phonePokemonRepository,
            unlockEventDispatcher = phoneUnlockEventDispatcher
        )

        // 3. Fake transport with thread-safe recording
        fakeTransport = FakeWearableTransport(isConnected = true)

        watchSyncManager = PoketchSyncManager(
            context = context,
            transport = fakeTransport,
            userRepository = watchUserRepository,
            calorieRepository = watchCalorieRepository,
            habitRepository = watchHabitRepository,
            pokemonRepository = watchPokemonRepository,
            unlockEventDispatcher = watchUnlockEventDispatcher
        )

        phoneSyncManager = PoketchSyncManager(
            context = context,
            transport = fakeTransport.reverseEndpoint,
            userRepository = phoneUserRepository,
            calorieRepository = phoneCalorieRepository,
            habitRepository = phoneHabitRepository,
            pokemonRepository = phonePokemonRepository,
            unlockEventDispatcher = phoneUnlockEventDispatcher
        )

        watchService = BasePoketchWearableListenerService().apply {
            userRepository = watchUserRepository
            calorieRepository = watchCalorieRepository
            habitRepository = watchHabitRepository
            pokemonRepository = watchPokemonRepository
            evaluateAchievementsUseCase = watchEvaluateAchievementsUseCase
            unlockEventDispatcher = watchUnlockEventDispatcher
            syncManager = watchSyncManager
        }

        phoneService = BasePoketchWearableListenerService().apply {
            userRepository = phoneUserRepository
            calorieRepository = phoneCalorieRepository
            habitRepository = phoneHabitRepository
            pokemonRepository = phonePokemonRepository
            evaluateAchievementsUseCase = phoneEvaluateAchievementsUseCase
            unlockEventDispatcher = phoneUnlockEventDispatcher
            syncManager = phoneSyncManager
        }

        // Bridge Watch -> Phone
        fakeTransport.peerListener = { path, dataMap ->
            transmittedPaths.add(path)
            phoneService.processDataChanged(path, dataMap)
        }
        fakeTransport.peerMessageListener = { path, data ->
            transmittedPaths.add(path)
            phoneService.processMessageReceived(path, data)
        }

        // Bridge Phone -> Watch
        fakeTransport.reverseListener = { path, dataMap ->
            transmittedPaths.add(path)
            watchService.processDataChanged(path, dataMap)
        }
        fakeTransport.reverseMessageListener = { path, data ->
            transmittedPaths.add(path)
            watchService.processMessageReceived(path, data)
        }

        val baseProfile = UserProfile(
            id = 1,
            ageYears = 28,
            sex = BiologicalSex.MALE,
            heightCm = 178.0,
            weightKg = 76.0,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            deficitTier = DeficitTier.STANDARD,
            bmrKcal = 1750,
            tdeeKcal = 2400,
            targetCalorieCeiling = 2000,
            dailyWaterGoalMl = 2500,
            activeBuddyPokemonId = 1,
            updatedAtEpochMillis = System.currentTimeMillis()
        )
        watchUserRepository.upsertUserProfile(baseProfile)
        phoneUserRepository.upsertUserProfile(baseProfile)
    }

    @After
    fun tearDown() {
        watchDatabase.close()
        phoneDatabase.close()
    }

    @Test
    fun concurrentSyncStress_50ConcurrentWatchAndPhoneLogs_zeroDataLossAndZeroDuplicates(): Unit = runBlocking(Dispatchers.IO) {
        val totalOperations = 50
        val watchOperations = 25
        val phoneOperations = 25

        val createdCalorieIds = Collections.synchronizedSet(mutableSetOf<String>())
        val createdHabitIds = Collections.synchronizedSet(mutableSetOf<String>())

        val jobs = (0 until totalOperations).map { index ->
            async(Dispatchers.IO) {
                val isWatch = index < watchOperations
                val logUuid = "stress-log-$index-${UUID.randomUUID()}"
                val habitUuid = "stress-habit-$index-${UUID.randomUUID()}"

                if (isWatch) {
                    val calorieLog = CalorieLog(
                        logId = logUuid,
                        dateIso = todayIso,
                        mealCategory = MealCategory.WATCH_QUICK_ADD,
                        foodDescription = "Watch Quick Add #$index",
                        caloriesKcal = 50 + (index * 5),
                        sourceDevice = DeviceSource.WEAR,
                        timestampMillis = System.currentTimeMillis() + index,
                        isSynced = false
                    )
                    createdCalorieIds.add(logUuid)
                    watchCalorieRepository.upsertCalorieLog(calorieLog)
                    watchSyncManager.syncCalorieLog(calorieLog)

                    val habitLog = HabitLog(
                        logId = habitUuid,
                        dateIso = todayIso,
                        habitType = HabitType.WATER_ML,
                        numericValue = 100.0 + index,
                        durationSeconds = 0,
                        sourceDevice = DeviceSource.WEAR,
                        timestampMillis = System.currentTimeMillis() + index,
                        isSynced = false
                    )
                    createdHabitIds.add(habitUuid)
                    watchHabitRepository.upsertHabitLog(habitLog)
                    watchSyncManager.syncHabitLog(habitLog)
                } else {
                    val calorieLog = CalorieLog(
                        logId = logUuid,
                        dateIso = todayIso,
                        mealCategory = MealCategory.LUNCH,
                        foodDescription = "Phone Meal #$index",
                        caloriesKcal = 100 + (index * 5),
                        sourceDevice = DeviceSource.MOBILE,
                        timestampMillis = System.currentTimeMillis() + index,
                        isSynced = false
                    )
                    createdCalorieIds.add(logUuid)
                    phoneCalorieRepository.upsertCalorieLog(calorieLog)
                    phoneSyncManager.syncCalorieLog(calorieLog)

                    val habitLog = HabitLog(
                        logId = habitUuid,
                        dateIso = todayIso,
                        habitType = HabitType.DAILY_STEPS,
                        numericValue = 200.0 + index,
                        durationSeconds = 0,
                        sourceDevice = DeviceSource.MOBILE,
                        timestampMillis = System.currentTimeMillis() + index,
                        isSynced = false
                    )
                    createdHabitIds.add(habitUuid)
                    phoneHabitRepository.upsertHabitLog(habitLog)
                    phoneSyncManager.syncHabitLog(habitLog)
                }
            }
        }

        jobs.awaitAll()

        // 1. Verify Zero Data Loss: All 50 calorie logs exist on Phone
        val phoneCalorieLogs = phoneCalorieRepository.observeLogsByDate(todayIso).first()
        val phoneCalorieLogIds = phoneCalorieLogs.map { it.logId }.toSet()
        assertThat(phoneCalorieLogIds).containsAtLeastElementsIn(createdCalorieIds)
        assertThat(createdCalorieIds).hasSize(totalOperations)

        // 2. Verify Zero Duplicate Rows: Unique IDs match total count
        val allPhoneCalorieIdsList = phoneCalorieLogs.map { it.logId }
        val distinctPhoneCalorieIds = allPhoneCalorieIdsList.distinct()
        assertThat(allPhoneCalorieIdsList.size).isEqualTo(distinctPhoneCalorieIds.size)

        // 3. Verify Zero Data Loss on Habit Logs
        val phoneHabitLogs = phoneHabitRepository.observeHabitsByDate(todayIso).first()
        val phoneHabitLogIds = phoneHabitLogs.map { it.logId }.toSet()
        assertThat(phoneHabitLogIds).containsAtLeastElementsIn(createdHabitIds)
        assertThat(createdHabitIds).hasSize(totalOperations)

        // 4. Verify Zero Duplicate Rows on Habit Logs
        val allPhoneHabitIdsList = phoneHabitLogs.map { it.logId }
        val distinctPhoneHabitIds = allPhoneHabitIdsList.distinct()
        assertThat(allPhoneHabitIdsList.size).isEqualTo(distinctPhoneHabitIds.size)
    }

    @Test
    fun simultaneousOfflineEdits_recalculationTriggersOverCalorieAlertOnWatch() = runTest {
        // Disconnect transport (simulating offline state on both devices)
        fakeTransport.isConnected = false

        // 1. Watch logs 3 meals offline totaling 1,800 kcal
        val watchMeals = listOf(
            CalorieLog(
                logId = "watch-offline-1",
                dateIso = todayIso,
                mealCategory = MealCategory.BREAKFAST,
                foodDescription = "Watch Omelette",
                caloriesKcal = 500,
                sourceDevice = DeviceSource.WEAR,
                timestampMillis = 1_000_000L,
                isSynced = false
            ),
            CalorieLog(
                logId = "watch-offline-2",
                dateIso = todayIso,
                mealCategory = MealCategory.LUNCH,
                foodDescription = "Watch Salad",
                caloriesKcal = 600,
                sourceDevice = DeviceSource.WEAR,
                timestampMillis = 1_001_000L,
                isSynced = false
            ),
            CalorieLog(
                logId = "watch-offline-3",
                dateIso = todayIso,
                mealCategory = MealCategory.SNACK,
                foodDescription = "Watch Shake",
                caloriesKcal = 700,
                sourceDevice = DeviceSource.WEAR,
                timestampMillis = 1_002_000L,
                isSynced = false
            )
        )
        watchMeals.forEach { watchCalorieRepository.upsertCalorieLog(it) }

        // Watch total is 1,800 kcal (ceiling was 2,000 kcal, so NOT over target yet)
        val watchTotalBefore = watchCalorieRepository.observeTotalCaloriesByDate(todayIso).first()
        assertThat(watchTotalBefore).isEqualTo(1800)

        // 2. Phone simultaneously offline:
        // User tightens deficit tier, lowering targetCalorieCeiling from 2,000 to 1,600 kcal
        val updatedProfile = UserProfile(
            id = 1,
            ageYears = 28,
            sex = BiologicalSex.MALE,
            heightCm = 178.0,
            weightKg = 76.0,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            deficitTier = DeficitTier.AGGRESSIVE,
            bmrKcal = 1750,
            tdeeKcal = 2400,
            targetCalorieCeiling = 1600,
            dailyWaterGoalMl = 2500,
            activeBuddyPokemonId = 1,
            updatedAtEpochMillis = System.currentTimeMillis() + 5000L
        )
        phoneUserRepository.upsertUserProfile(updatedProfile)

        // User also logs a 500 kcal meal on Phone offline
        val phoneMeal = CalorieLog(
            logId = "phone-offline-1",
            dateIso = todayIso,
            mealCategory = MealCategory.DINNER,
            foodDescription = "Phone Steak",
            caloriesKcal = 500,
            sourceDevice = DeviceSource.MOBILE,
            timestampMillis = 1_003_000L,
            isSynced = false
        )
        phoneCalorieRepository.upsertCalorieLog(phoneMeal)

        // Phone total alone is 500 kcal (not over 1,600 ceiling)
        val phoneTotalBefore = phoneCalorieRepository.observeTotalCaloriesByDate(todayIso).first()
        assertThat(phoneTotalBefore).isEqualTo(500)

        // 3. Collect Watch alerts
        val receivedWatchAlerts = mutableListOf<Int>()
        val alertCollectJob = launch {
            watchSyncManager.overCalorieAlerts.collect { receivedWatchAlerts.add(it) }
        }

        // 4. Reconnect transport & Flush both queues concurrently
        fakeTransport.isConnected = true

        val flushWatch = async(Dispatchers.IO) { watchSyncManager.flushOfflineQueue() }
        val flushPhone = async(Dispatchers.IO) { phoneSyncManager.flushOfflineQueue() }
        flushWatch.await()
        flushPhone.await()

        // 5. Total combined intake on Phone is now 1,800 + 500 = 2,300 kcal
        val phoneTotalAfter = phoneCalorieRepository.observeTotalCaloriesByDate(todayIso).first()
        assertThat(phoneTotalAfter).isEqualTo(2300)

        // Phone ceiling is 1,600 kcal. Surplus = 2,300 - 1,600 = 700 kcal.
        // Recalculating targetCalorieCeiling re-evaluates all synced Watch logs and triggers /alert/over_calorie
        assertThat(receivedWatchAlerts).contains(700)

        // Both devices have all 4 meals with zero duplicate UUIDs
        val allPhoneLogs = phoneCalorieRepository.observeLogsByDate(todayIso).first()
        assertThat(allPhoneLogs.map { it.logId }).containsExactly(
            "watch-offline-1", "watch-offline-2", "watch-offline-3", "phone-offline-1"
        )

        alertCollectJob.cancel()
    }

    @Test
    fun rapidHabitTaps_coalesceIntoSingleBatchSyncBurst() = runTest {
        transmittedPaths.clear()
        val testDispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler)
        watchSyncManager.syncDispatcher = testDispatcher

        // Rapidly tap +250 ml water 3 times in quick succession
        val habit1 = HabitLog(
            logId = "burst-water-1",
            dateIso = todayIso,
            habitType = HabitType.WATER_ML,
            numericValue = 250.0,
            sourceDevice = DeviceSource.WEAR,
            timestampMillis = System.currentTimeMillis()
        )
        val habit2 = HabitLog(
            logId = "burst-water-2",
            dateIso = todayIso,
            habitType = HabitType.WATER_ML,
            numericValue = 250.0,
            sourceDevice = DeviceSource.WEAR,
            timestampMillis = System.currentTimeMillis() + 10
        )
        val habit3 = HabitLog(
            logId = "burst-water-3",
            dateIso = todayIso,
            habitType = HabitType.WATER_ML,
            numericValue = 250.0,
            sourceDevice = DeviceSource.WEAR,
            timestampMillis = System.currentTimeMillis() + 20
        )

        watchHabitRepository.upsertHabitLog(habit1)
        watchHabitRepository.upsertHabitLog(habit2)
        watchHabitRepository.upsertHabitLog(habit3)

        watchSyncManager.coalesceAndSyncHabit(habit1, debounceMillis = 100L)
        testScheduler.advanceTimeBy(20L)
        testScheduler.runCurrent()

        watchSyncManager.coalesceAndSyncHabit(habit2, debounceMillis = 100L)
        testScheduler.advanceTimeBy(20L)
        testScheduler.runCurrent()

        watchSyncManager.coalesceAndSyncHabit(habit3, debounceMillis = 100L)

        // Wait for debounce window (100ms) to elapse
        testScheduler.advanceTimeBy(200L)
        testScheduler.runCurrent()

        // 1. Verify single batch path was transmitted
        val batchSyncTransmissions = transmittedPaths.filter { it == PoketchSyncConstants.PATH_LOGS_HABITS_BATCH }
        assertThat(batchSyncTransmissions).hasSize(1)

        // 2. Phone received and persisted all 3 habits
        val phoneWaterTotal = phoneHabitRepository.observeDailyWaterMl(todayIso).first()
        assertThat(phoneWaterTotal).isEqualTo(750.0)

        val phoneHabitLogs = phoneHabitRepository.observeHabitsByDate(todayIso).first()
        assertThat(phoneHabitLogs.map { it.logId }).containsAtLeast(
            "burst-water-1", "burst-water-2", "burst-water-3"
        )
    }
}
