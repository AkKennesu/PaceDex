package com.poketch.wear.ui.battle

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.wearable.DataMap
import com.google.common.truth.Truth.assertThat
import com.poketch.core.data.gamification.EvaluateAchievementsUseCase
import com.poketch.core.data.gamification.UnlockEventDispatcher
import com.poketch.core.domain.calculator.CalorieBudgetState
import com.poketch.core.domain.engine.UnlockEvent
import com.poketch.core.domain.model.ActivityLevel
import com.poketch.core.domain.model.BiologicalSex
import com.poketch.core.domain.model.CalorieLog
import com.poketch.core.domain.model.DailyNutritionSummary
import com.poketch.core.domain.model.DeficitTier
import com.poketch.core.domain.model.DeviceSource
import com.poketch.core.domain.model.HabitLog
import com.poketch.core.domain.model.HabitType
import com.poketch.core.domain.model.Pokemon
import com.poketch.core.domain.model.PokemonCategory
import com.poketch.core.domain.model.UnlockRule
import com.poketch.core.domain.model.UserProfile
import com.poketch.core.domain.repository.CalorieRepository
import com.poketch.core.domain.repository.CurrentWeather
import com.poketch.core.domain.repository.HabitRepository
import com.poketch.core.domain.repository.PokemonRepository
import com.poketch.core.domain.repository.UserRepository
import com.poketch.core.domain.repository.WeatherRepository
import com.poketch.core.sync.ConnectedNodeInfo
import com.poketch.core.sync.PoketchSyncManager
import com.poketch.core.sync.WearableTransport
import com.poketch.wear.reminders.ReminderScheduler
import com.poketch.wear.ui.alert.WearHapticController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class GbaWatchBattleViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakeUserRepository: FakeUserRepository
    private lateinit var fakeCalorieRepository: FakeCalorieRepository
    private lateinit var fakeHabitRepository: FakeHabitRepository
    private lateinit var fakePokemonRepository: FakePokemonRepository
    private lateinit var fakeWeatherRepository: FakeWeatherRepository
    private lateinit var evaluateAchievementsUseCase: EvaluateAchievementsUseCase
    private lateinit var unlockEventDispatcher: UnlockEventDispatcher
    private lateinit var syncManager: PoketchSyncManager

    private val sampleBulbasaur = Pokemon(
        pokedexNumber = 1,
        name = "Bulbasaur",
        primaryType = "Grass",
        unlockRule = UnlockRule("STARTER", PokemonCategory.STARTER, "Choose as starter", 1),
        currentProgress = 1,
        isUnlocked = true,
        spriteAssetPath = "sprites/pokemon_001.png"
    )

    private val sampleIvysaur = Pokemon(
        pokedexNumber = 2,
        name = "Ivysaur",
        primaryType = "Grass",
        unlockRule = UnlockRule("STREAK_7", PokemonCategory.STARTER, "7-day streak", 7),
        currentProgress = 0,
        isUnlocked = false,
        spriteAssetPath = "sprites/pokemon_002.png"
    )

    private val samplePoliwag = Pokemon(
        pokedexNumber = 60,
        name = "Poliwag",
        primaryType = "Water",
        unlockRule = UnlockRule("WATER_LOG_3", PokemonCategory.WATER_HYDRATION, "Log water 3 times", 3),
        currentProgress = 1,
        isUnlocked = false,
        spriteAssetPath = "sprites/pokemon_060.png"
    )

    private val samplePonyta = Pokemon(
        pokedexNumber = 77,
        name = "Ponyta",
        primaryType = "Fire",
        unlockRule = UnlockRule("RUN_1_MILE", PokemonCategory.RUN_STEPS, "Run 1 mile", 1),
        currentProgress = 0,
        isUnlocked = false,
        spriteAssetPath = "sprites/pokemon_077.png"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()

        fakeUserRepository = FakeUserRepository()
        fakeCalorieRepository = FakeCalorieRepository()
        fakeHabitRepository = FakeHabitRepository()
        fakePokemonRepository = FakePokemonRepository(
            listOf(sampleBulbasaur, sampleIvysaur, samplePoliwag, samplePonyta)
        )
        fakeWeatherRepository = FakeWeatherRepository()
        unlockEventDispatcher = UnlockEventDispatcher()

        evaluateAchievementsUseCase = EvaluateAchievementsUseCase(
            userRepository = fakeUserRepository,
            calorieRepository = fakeCalorieRepository,
            habitRepository = fakeHabitRepository,
            pokemonRepository = fakePokemonRepository,
            unlockEventDispatcher = unlockEventDispatcher
        )

        syncManager = PoketchSyncManager(
            context = context,
            transport = FakeWearableTransport(),
            userRepository = fakeUserRepository,
            calorieRepository = fakeCalorieRepository,
            habitRepository = fakeHabitRepository,
            pokemonRepository = fakePokemonRepository,
            unlockEventDispatcher = unlockEventDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): GbaWatchBattleViewModel {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return GbaWatchBattleViewModel(
            userRepository = fakeUserRepository,
            calorieRepository = fakeCalorieRepository,
            habitRepository = fakeHabitRepository,
            pokemonRepository = fakePokemonRepository,
            weatherRepository = fakeWeatherRepository,
            evaluateAchievementsUseCase = evaluateAchievementsUseCase,
            unlockEventDispatcher = unlockEventDispatcher,
            poketchSyncManager = syncManager,
            wearHapticController = WearHapticController(context),
            reminderScheduler = ReminderScheduler(context),
        )
    }

    private fun TestScope.createSubscribedViewModel(): GbaWatchBattleViewModel {
        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect { }
        }
        return vm
    }

    @Test
    fun wildTarget_prioritizesLockedPokemonWithActiveProgress() = testScope.runTest {
        val viewModel = createSubscribedViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Poliwag (#60) has 1/3 progress (33%), whereas Ivysaur (#2) has 0/7 (0%).
        // Dynamic target selection must pick Poliwag so the player sees active damage!
        assertThat(state.wildTarget?.pokedexNumber).isEqualTo(60)
        assertThat(state.wildTarget?.progressRatio).isWithin(0.01).of(0.333)
    }

    @Test
    fun wildTarget_respectsUserPinnedTarget() = testScope.runTest {
        val viewModel = createSubscribedViewModel()
        advanceUntilIdle()

        viewModel.pinWildTarget(77)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.wildTarget?.pokedexNumber).isEqualTo(77)
        assertThat(state.pinnedTargetPokemonId).isEqualTo(77)
    }

    @Test
    fun unlockEvent_emitsCelebrationPopupAndClearsPin() = testScope.runTest {
        val viewModel = createSubscribedViewModel()
        advanceUntilIdle()

        viewModel.pinWildTarget(60)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.pinnedTargetPokemonId).isEqualTo(60)

        // Simulate unlocking Poliwag
        val unlockedPoliwag = samplePoliwag.copy(isUnlocked = true, currentProgress = 3)
        val event = UnlockEvent.NewPokemonUnlocked(
            pokemon = unlockedPoliwag,
            triggerReason = "Logged 1,000ml water!",
            timestampMillis = System.currentTimeMillis()
        )

        unlockEventDispatcher.emitUnlockEvents(listOf(event))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.newlyUnlockedPokemon).isEqualTo(event)
        // Pin should auto-clear once target unlocks so the battle screen advances!
        assertThat(state.pinnedTargetPokemonId).isNull()

        // Dismiss celebration popup
        viewModel.dismissUnlockCelebration()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.newlyUnlockedPokemon).isNull()
    }

    @Test
    fun setUnlockedAsActiveBuddy_setsBuddyAndDismissesPopup() = testScope.runTest {
        val viewModel = createSubscribedViewModel()
        advanceUntilIdle()

        val unlockedPoliwag = samplePoliwag.copy(isUnlocked = true, currentProgress = 3)
        val event = UnlockEvent.NewPokemonUnlocked(
            pokemon = unlockedPoliwag,
            triggerReason = "Logged 1,000ml water!",
            timestampMillis = System.currentTimeMillis()
        )

        unlockEventDispatcher.emitUnlockEvents(listOf(event))
        advanceUntilIdle()

        viewModel.setUnlockedAsActiveBuddy(60)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.newlyUnlockedPokemon).isNull()
        assertThat(state.activeBuddy?.pokedexNumber).isEqualTo(60)
    }

    @Test
    fun unlockedPokemon_alwaysHasFullProgressRatio() {
        val lockedPoliwag = samplePoliwag.copy(isUnlocked = false, currentProgress = 0)
        assertThat(lockedPoliwag.progressRatio).isEqualTo(0.0)

        val partiallyProgressed = samplePoliwag.copy(isUnlocked = false, currentProgress = 2)
        assertThat(partiallyProgressed.progressRatio).isWithin(0.01).of(0.666)

        // When unlocked, progressRatio must always be 1.0 even if raw progress differed
        val unlocked = samplePoliwag.copy(isUnlocked = true, currentProgress = 1)
        assertThat(unlocked.progressRatio).isEqualTo(1.0)
    }

    // --- Fakes ---
    private class FakeUserRepository : UserRepository {
        private val _profile = MutableStateFlow<UserProfile?>(
            UserProfile(
                id = 1,
                ageYears = 25,
                sex = BiologicalSex.MALE,
                heightCm = 175.0,
                weightKg = 70.0,
                activityLevel = ActivityLevel.SEDENTARY,
                deficitTier = DeficitTier.STANDARD,
                bmrKcal = 1600,
                tdeeKcal = 2200,
                targetCalorieCeiling = 1800,
                dailyWaterGoalMl = 2500,
                activeBuddyPokemonId = 1,
                updatedAtEpochMillis = System.currentTimeMillis()
            )
        )

        override fun observeUserProfile(): Flow<UserProfile?> = _profile
        override suspend fun getUserProfile(): UserProfile? = _profile.value
        override suspend fun saveUserProfile(
            ageYears: Int,
            sex: BiologicalSex,
            heightCm: Double,
            weightKg: Double,
            activityLevel: ActivityLevel,
            deficitTier: DeficitTier,
            dailyWaterGoalMl: Int,
            activeBuddyPokemonId: Int
        ) {
            _profile.value = _profile.value?.copy(
                activeBuddyPokemonId = activeBuddyPokemonId,
                dailyWaterGoalMl = dailyWaterGoalMl
            )
        }
        override suspend fun upsertUserProfile(profile: UserProfile) { _profile.value = profile }
        override suspend fun updateActiveBuddy(pokemonId: Int) {
            _profile.value = _profile.value?.copy(activeBuddyPokemonId = pokemonId)
        }
    }

    private class FakeCalorieRepository : CalorieRepository {
        override fun observeLogsByDate(dateIso: String): Flow<List<CalorieLog>> = flowOf(emptyList())
        override fun observeTotalCaloriesByDate(dateIso: String): Flow<Int> = flowOf(500)
        override fun observeRecentDailyNutrition(days: Int): Flow<List<DailyNutritionSummary>> = flowOf(emptyList())
        override suspend fun getRecentDailyNutrition(days: Int): List<DailyNutritionSummary> = emptyList()
        override fun observeDailyBudgetState(dateIso: String): Flow<CalorieBudgetState> =
            flowOf(CalorieBudgetState.OnTrack(500, 1800, 1300, 0.27))
        override suspend fun upsertCalorieLog(log: CalorieLog) {}
        override suspend fun upsertCalorieLogs(logs: List<CalorieLog>) {}
        override suspend fun deleteCalorieLog(logId: String) {}
        override suspend fun getUnsyncedLogs(): List<CalorieLog> = emptyList()
        override suspend fun markLogsAsSynced(logIds: List<String>) {}
    }

    private class FakeHabitRepository : HabitRepository {
        private val waterFlow = MutableStateFlow(250.0)
        private val stepsFlow = MutableStateFlow(3500.0)
        private val runFlow = MutableStateFlow(0.0)

        override fun observeDailyWaterMl(dateIso: String): Flow<Double> = waterFlow
        override fun observeDailySteps(dateIso: String): Flow<Double> = stepsFlow
        override fun observeDailyRunMiles(dateIso: String): Flow<Double> = runFlow
        override fun observeDailySleepHours(dateIso: String): Flow<Double> = flowOf(8.0)
        override fun observeHabitsByDate(dateIso: String): Flow<List<HabitLog>> = flowOf(emptyList())
        override suspend fun getLifetimeHabitTotal(habitType: HabitType): Double = 1000.0
        override suspend fun logHabit(
            dateIso: String,
            habitType: HabitType,
            numericValue: Double,
            durationSeconds: Int,
            sourceDevice: DeviceSource
        ): HabitLog {
            if (habitType == HabitType.WATER_ML) waterFlow.value += numericValue
            return HabitLog("h1", dateIso, habitType, numericValue, durationSeconds, sourceDevice, System.currentTimeMillis(), true)
        }
        override suspend fun upsertHabitLog(log: HabitLog) {}
        override suspend fun upsertHabitLogs(logs: List<HabitLog>) {}
        override suspend fun getUnsyncedHabits(): List<HabitLog> = emptyList()
        override suspend fun markHabitsAsSynced(logIds: List<String>) {}
    }

    private class FakePokemonRepository(initial: List<Pokemon>) : PokemonRepository {
        private val pokes = MutableStateFlow(initial)

        override fun observeAllPokemon(): Flow<List<Pokemon>> = pokes
        override fun observePokemonById(pokedexNumber: Int): Flow<Pokemon?> =
            flowOf(pokes.value.find { it.pokedexNumber == pokedexNumber })
        override suspend fun getPokemonByCategory(category: PokemonCategory): List<Pokemon> =
            pokes.value.filter { it.unlockRule.category == category }
        override suspend fun getPokemonByRuleCode(ruleCode: String): List<Pokemon> =
            pokes.value.filter { it.unlockRule.ruleCode == ruleCode }
        override suspend fun getUnlockedCount(): Int = pokes.value.count { it.isUnlocked }
        override suspend fun updateProgress(pokedexNumber: Int, newProgress: Int): Pokemon? = null
        override suspend fun unlockPokemon(pokedexNumber: Int, timestampMillis: Long): Pokemon? = null
        override suspend fun upsertPokemon(pokemon: Pokemon) {
            pokes.value = pokes.value.map { if (it.pokedexNumber == pokemon.pokedexNumber) pokemon else it }
        }
        override suspend fun upsertAllPokemon(pokemonList: List<Pokemon>) {
            pokes.value = pokemonList
        }
    }

    private class FakeWeatherRepository : WeatherRepository {
        override fun observeCurrentWeather(): Flow<CurrentWeather> = flowOf(CurrentWeather(tempCelsius = 22, badgeText = "22°C CLR"))
        override suspend fun getCurrentWeather(): CurrentWeather = CurrentWeather(tempCelsius = 22, badgeText = "22°C CLR")
        override suspend fun refreshWeatherFromRemote(): Result<Unit> = Result.success(Unit)
        override suspend fun cacheHourlyForecastJson(jsonString: String): Result<Unit> = Result.success(Unit)
        override fun getLastFetchedJson(): String? = null
    }

    private class FakeWearableTransport : WearableTransport {
        override suspend fun putDataMap(path: String, dataMap: DataMap) {}
        override suspend fun sendMessage(path: String, data: ByteArray) {}
        override suspend fun getConnectedNodes(): List<ConnectedNodeInfo> = emptyList()
    }
}
