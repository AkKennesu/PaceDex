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
import com.poketch.core.domain.calculator.CalorieBudgetState
import com.poketch.core.domain.engine.UnlockEvent
import com.poketch.core.domain.model.ActivityLevel
import com.poketch.core.domain.model.BiologicalSex
import com.poketch.core.domain.model.CalorieLog
import com.poketch.core.domain.model.DeficitTier
import com.poketch.core.domain.model.DeviceSource
import com.poketch.core.domain.model.HabitType
import com.poketch.core.domain.model.MealCategory
import com.poketch.core.domain.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class EndToEndSyncScenariosTest {

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

    // In-Memory Bidirectional Transport
    private lateinit var fakeTransport: FakeWearableTransport

    private val todayIso = "2026-10-31"

    @Before
    fun setup() = runTest {
        context = ApplicationProvider.getApplicationContext()

        // 1. Initialize Watch Database and repositories
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
            unlockEventDispatcher = watchUnlockEventDispatcher,
            recoveryQuestDao = watchDatabase.recoveryQuestDao()
        )

        // 2. Initialize Phone Database and repositories
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
            unlockEventDispatcher = phoneUnlockEventDispatcher,
            recoveryQuestDao = phoneDatabase.recoveryQuestDao()
        )

        // 3. Connect via FakeWearableTransport
        fakeTransport = FakeWearableTransport(isConnected = true)

        watchSyncManager = PoketchSyncManager(
            context = context,
            transport = fakeTransport,
            userRepository = watchUserRepository,
            calorieRepository = watchCalorieRepository,
            habitRepository = watchHabitRepository,
            pokemonRepository = watchPokemonRepository,
            unlockEventDispatcher = watchUnlockEventDispatcher,
            recoveryQuestDao = watchDatabase.recoveryQuestDao(),
            evaluateAchievementsUseCase = watchEvaluateAchievementsUseCase
        )

        phoneSyncManager = PoketchSyncManager(
            context = context,
            transport = fakeTransport.reverseEndpoint,
            userRepository = phoneUserRepository,
            calorieRepository = phoneCalorieRepository,
            habitRepository = phoneHabitRepository,
            pokemonRepository = phonePokemonRepository,
            unlockEventDispatcher = phoneUnlockEventDispatcher,
            recoveryQuestDao = phoneDatabase.recoveryQuestDao(),
            evaluateAchievementsUseCase = phoneEvaluateAchievementsUseCase
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
            phoneService.processDataChanged(path, dataMap)
        }
        fakeTransport.peerMessageListener = { path, data ->
            phoneService.processMessageReceived(path, data)
        }

        // Bridge Phone -> Watch
        fakeTransport.reverseListener = { path, dataMap ->
            watchService.processDataChanged(path, dataMap)
        }
        fakeTransport.reverseMessageListener = { path, data ->
            watchService.processMessageReceived(path, data)
        }
    }

    @After
    fun tearDown() {
        watchDatabase.close()
        phoneDatabase.close()
    }

    @Test
    fun watchQuickAdd600Kcal_exceedsTarget_triggersWatchRedAlertAndSyncsPhoneWarningBanner() = runTest {
        // Given a daily target ceiling of 1,900 kcal and 1,500 kcal already consumed
        val profile = UserProfile(
            id = 1,
            ageYears = 26,
            sex = BiologicalSex.MALE,
            heightCm = 175.0,
            weightKg = 75.0,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            deficitTier = DeficitTier.STANDARD,
            bmrKcal = 1700,
            tdeeKcal = 2400,
            targetCalorieCeiling = 1900,
            dailyWaterGoalMl = 2500,
            activeBuddyPokemonId = 1,
            updatedAtEpochMillis = System.currentTimeMillis()
        )
        watchUserRepository.upsertUserProfile(profile)
        phoneUserRepository.upsertUserProfile(profile)

        val baseLog = CalorieLog(
            logId = UUID.randomUUID().toString(),
            dateIso = todayIso,
            mealCategory = MealCategory.LUNCH,
            foodDescription = "Base Meals",
            caloriesKcal = 1500,
            sourceDevice = DeviceSource.WEAR,
            timestampMillis = System.currentTimeMillis(),
            isSynced = true
        )
        watchCalorieRepository.upsertCalorieLog(baseLog)
        phoneCalorieRepository.upsertCalorieLog(baseLog)

        val phoneAlerts = mutableListOf<Int>()
        val alertCollectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            phoneSyncManager.overCalorieAlerts.collect { phoneAlerts.add(it) }
        }

        // When the user quick-adds +600 kcal on the Watch (2,100 / 1,900 kcal)
        val quickAddLog = CalorieLog(
            logId = UUID.randomUUID().toString(),
            dateIso = todayIso,
            mealCategory = MealCategory.WATCH_QUICK_ADD,
            foodDescription = "Watch Quick Add",
            caloriesKcal = 600,
            sourceDevice = DeviceSource.WEAR,
            timestampMillis = System.currentTimeMillis(),
            isSynced = false
        )
        watchCalorieRepository.upsertCalorieLog(quickAddLog)

        // 1. Watch immediately enters CalorieBudgetState.OverTargetWarning(surplusKcal = 200)
        val watchBudget = watchCalorieRepository.observeDailyBudgetState(todayIso).first()
        assertThat(watchBudget).isInstanceOf(CalorieBudgetState.OverTargetWarning::class.java)
        val surplusKcal = (watchBudget as CalorieBudgetState.OverTargetWarning).surplusKcal
        assertThat(surplusKcal).isEqualTo(200)

        // 2. Watch pushes /logs/calories/{uuid} and /alert/over_calorie over the sync transport
        watchSyncManager.syncCalorieLog(quickAddLog)
        watchSyncManager.sendOverCalorieAlert(surplusKcal)

        // 3. Phone database receives the log idempotently, updates consumed total to 2,100 kcal
        val phoneTotalCalories = phoneCalorieRepository.observeTotalCaloriesByDate(todayIso).first()
        assertThat(phoneTotalCalories).isEqualTo(2100)

        // and emits OverTargetWarning(surplusKcal = 200) on the mobile dashboard
        val phoneBudget = phoneCalorieRepository.observeDailyBudgetState(todayIso).first()
        assertThat(phoneBudget).isInstanceOf(CalorieBudgetState.OverTargetWarning::class.java)
        val phoneSurplus = (phoneBudget as CalorieBudgetState.OverTargetWarning).surplusKcal
        assertThat(phoneSurplus).isEqualTo(200)

        // Cross-device alert received
        assertThat(phoneAlerts).contains(200)

        // Verify idempotent deduplication: re-syncing produces zero duplicate calories
        watchSyncManager.syncCalorieLog(quickAddLog)
        val phoneTotalAfterResync = phoneCalorieRepository.observeTotalCaloriesByDate(todayIso).first()
        assertThat(phoneTotalAfterResync).isEqualTo(2100)

        alertCollectJob.cancel()
    }

    @Test
    fun watchCompletesOneMileRun_syncsToPhone_unlocks077PonytaAndBroadcastsFanfareToWatch() = runTest {
        val watchEvents = mutableListOf<UnlockEvent.NewPokemonUnlocked>()
        val watchCollectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            watchUnlockEventDispatcher.unlockEvents.collect { watchEvents.add(it) }
        }

        val phoneEvents = mutableListOf<UnlockEvent.NewPokemonUnlocked>()
        val phoneCollectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            phoneUnlockEventDispatcher.unlockEvents.collect { phoneEvents.add(it) }
        }

        // Initially #077 Ponyta is locked on both
        val initialWatchPonyta = watchPokemonRepository.observePokemonById(77).first()
        assertThat(initialWatchPonyta?.isUnlocked).isFalse()
        val initialPhonePonyta = phonePokemonRepository.observePokemonById(77).first()
        assertThat(initialPhonePonyta?.isUnlocked).isFalse()

        // When the Watch logs a 1.02 mile run via ExerciseClient
        val runLog = watchHabitRepository.logHabit(
            dateIso = todayIso,
            habitType = HabitType.RUN_MILES,
            numericValue = 1.02,
            durationSeconds = 540,
            sourceDevice = DeviceSource.WEAR
        )

        // Watch syncs /logs/habits/{uuid} to the Phone
        watchSyncManager.syncHabitLog(runLog)

        // Phone evaluates PokemonUnlockEngine, unlocks #077 Ponyta, persists isUnlocked = true
        val updatedPhonePonyta = phonePokemonRepository.observePokemonById(77).first()
        assertThat(updatedPhonePonyta?.isUnlocked).isTrue()

        // and broadcasts /event/new_pokemon + /pokedex/unlocks back to the Watch
        // Both Watch and Phone have #077 Ponyta unlocked
        val updatedWatchPonyta = watchPokemonRepository.observePokemonById(77).first()
        assertThat(updatedWatchPonyta?.isUnlocked).isTrue()

        // and receive the NewPokemonUnlocked celebration event
        assertThat(phoneEvents.any { it.pokemon.pokedexNumber == 77 }).isTrue()
        assertThat(watchEvents.any { it.pokemon.pokedexNumber == 77 }).isTrue()

        watchCollectJob.cancel()
        phoneCollectJob.cancel()
    }

    @Test
    fun offlineWatchUsage_queuesLogsInRoomAndFlushesIdempotentlyUponReconnection() = runTest {
        // Disconnect FakeWearableTransport (isConnected = false)
        fakeTransport.isConnected = false

        // Log +250 ml water and +400 kcal meal on the Watch
        val waterLog = watchHabitRepository.logHabit(
            dateIso = todayIso,
            habitType = HabitType.WATER_ML,
            numericValue = 250.0,
            sourceDevice = DeviceSource.WEAR
        )
        val mealLog = CalorieLog(
            logId = UUID.randomUUID().toString(),
            dateIso = todayIso,
            mealCategory = MealCategory.SNACK,
            foodDescription = "Protein Bar",
            caloriesKcal = 400,
            sourceDevice = DeviceSource.WEAR,
            timestampMillis = System.currentTimeMillis(),
            isSynced = false
        )
        watchCalorieRepository.upsertCalorieLog(mealLog)

        // Verify both are stored in watchDatabase with isSynced = false
        val unsyncedCaloriesBefore = watchCalorieRepository.getUnsyncedLogs()
        assertThat(unsyncedCaloriesBefore.map { it.logId }).contains(mealLog.logId)
        assertThat(unsyncedCaloriesBefore.first { it.logId == mealLog.logId }.isSynced).isFalse()

        val unsyncedHabitsBefore = watchHabitRepository.getUnsyncedHabits()
        assertThat(unsyncedHabitsBefore.map { it.logId }).contains(waterLog.logId)
        assertThat(unsyncedHabitsBefore.first { it.logId == waterLog.logId }.isSynced).isFalse()

        // Attempt flush while disconnected -> should safely keep them unsynced
        watchSyncManager.flushOfflineQueue()
        assertThat(watchCalorieRepository.getUnsyncedLogs().map { it.logId }).contains(mealLog.logId)
        assertThat(watchHabitRepository.getUnsyncedHabits().map { it.logId }).contains(waterLog.logId)

        // Reconnect FakeWearableTransport and invoke poketchSyncManager.flushOfflineQueue()
        fakeTransport.isConnected = true
        watchSyncManager.flushOfflineQueue()

        // Verify both records arrive in phoneDatabase
        val phoneLogs = phoneCalorieRepository.observeLogsByDate(todayIso).first()
        assertThat(phoneLogs.map { it.logId }).contains(mealLog.logId)
        val phoneWater = phoneHabitRepository.observeDailyWaterMl(todayIso).first()
        assertThat(phoneWater).isEqualTo(250.0)

        // isSynced flips to true on watchDatabase
        val unsyncedCaloriesAfter = watchCalorieRepository.getUnsyncedLogs()
        assertThat(unsyncedCaloriesAfter).isEmpty()
        val unsyncedHabitsAfter = watchHabitRepository.getUnsyncedHabits()
        assertThat(unsyncedHabitsAfter).isEmpty()

        // and re-flushing produces zero duplicate calorie or water totals
        watchSyncManager.flushOfflineQueue()

        val phoneTotalCaloriesAfter = phoneCalorieRepository.observeTotalCaloriesByDate(todayIso).first()
        assertThat(phoneTotalCaloriesAfter).isEqualTo(400)

        val phoneTotalWaterAfter = phoneHabitRepository.observeDailyWaterMl(todayIso).first()
        assertThat(phoneTotalWaterAfter).isEqualTo(250.0)
    }

    @Test
    fun overCalorieSurplus_generatesRecoveryQuest_andStepsCompletion_clearsAlertAndAdvancesSnorlax() = runTest {
        // Given target ceiling of 1800 kcal
        val profile = UserProfile(
            id = 1,
            ageYears = 26,
            sex = BiologicalSex.MALE,
            heightCm = 175.0,
            weightKg = 75.0,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            deficitTier = DeficitTier.STANDARD,
            bmrKcal = 1700,
            tdeeKcal = 2400,
            targetCalorieCeiling = 1800,
            dailyWaterGoalMl = 2500,
            activeBuddyPokemonId = 1,
            updatedAtEpochMillis = System.currentTimeMillis()
        )
        phoneUserRepository.upsertUserProfile(profile)
        watchUserRepository.upsertUserProfile(profile)

        // 1. Initial baseline: user has 2000 steps logged
        phoneHabitRepository.logHabit(
            dateIso = todayIso,
            habitType = HabitType.DAILY_STEPS,
            numericValue = 2000.0,
            sourceDevice = DeviceSource.MOBILE
        )

        val completedQuests = mutableListOf<com.poketch.core.domain.model.RecoveryQuest>()
        val collectJob = launch(UnconfinedTestDispatcher()) {
            phoneSyncManager.recoveryQuestCompletedEvents.collect {
                completedQuests.add(it)
            }
        }

        // 2. User logs a 2100 kcal meal (300 kcal over the 1800 target ceiling)
        val overageLog = CalorieLog(
            logId = UUID.randomUUID().toString(),
            dateIso = todayIso,
            mealCategory = MealCategory.DINNER,
            foodDescription = "Large Pizza Feast",
            caloriesKcal = 2100,
            sourceDevice = DeviceSource.MOBILE,
            timestampMillis = System.currentTimeMillis(),
            isSynced = false
        )
        phoneCalorieRepository.upsertCalorieLog(overageLog)

        // Sync triggers reEvaluateDailyCalorieCeiling
        phoneSyncManager.syncCalorieLog(overageLog)

        // 3. Verify RecoveryQuest automatically generated with ~22 steps per kcal
        val generatedQuest = phoneSyncManager.recoveryQuestFlow.value
        assertThat(generatedQuest).isNotNull()
        assertThat(generatedQuest?.surplusKcal).isEqualTo(300)
        assertThat(generatedQuest?.startingStepsSnapshot).isEqualTo(2000)
        assertThat(generatedQuest?.targetOffsetSteps).isEqualTo(6667) // 300 / 0.045
        assertThat(generatedQuest?.isCompleted).isFalse()

        // Verify it synced to watch
        val watchQuest = watchSyncManager.recoveryQuestFlow.value
        assertThat(watchQuest).isNotNull()
        assertThat(watchQuest?.surplusKcal).isEqualTo(300)
        assertThat(watchQuest?.targetOffsetSteps).isEqualTo(6667)

        // 4. As steps increase (from 2000 to 5000), update currentOffsetSteps
        phoneSyncManager.updateRecoveryQuestProgress(5000)
        val partialQuest = phoneSyncManager.recoveryQuestFlow.value
        assertThat(partialQuest?.currentOffsetSteps).isEqualTo(3000)
        assertThat(partialQuest?.isCompleted).isFalse()
        assertThat(completedQuests).isEmpty()

        // 5. User reaches 8667 steps (2000 starting + 6667 targetOffsetSteps)
        phoneSyncManager.updateRecoveryQuestProgress(8667)
        val completedQuest = phoneSyncManager.recoveryQuestFlow.value
        assertThat(completedQuest?.currentOffsetSteps).isEqualTo(6667)
        assertThat(completedQuest?.isCompleted).isTrue()
        assertThat(completedQuests).hasSize(1)

        // 6. Verify Cubone and Snorlax (#143) progression
        val cubone = phonePokemonRepository.observePokemonById(104).first()
        assertThat(cubone?.isUnlocked).isTrue()
        val snorlax = phonePokemonRepository.observePokemonById(143).first()
        assertThat(snorlax?.currentProgress).isAtLeast(1)

        collectJob.cancel()
    }
}
