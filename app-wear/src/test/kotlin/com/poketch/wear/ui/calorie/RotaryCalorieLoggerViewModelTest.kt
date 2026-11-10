package com.poketch.wear.ui.calorie

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.wearable.DataMap
import com.google.common.truth.Truth.assertThat
import com.poketch.core.data.gamification.EvaluateAchievementsUseCase
import com.poketch.core.data.gamification.UnlockEventDispatcher
import com.poketch.core.domain.calculator.CalorieBudgetState
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
import com.poketch.core.domain.model.UserProfile
import com.poketch.core.domain.repository.CalorieRepository
import com.poketch.core.domain.repository.HabitRepository
import com.poketch.core.domain.repository.PokemonRepository
import com.poketch.core.domain.repository.UserRepository
import com.poketch.core.sync.ConnectedNodeInfo
import com.poketch.core.sync.PoketchSyncManager
import com.poketch.core.sync.WearableTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class RotaryCalorieLoggerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeUserRepository: FakeUserRepository
    private lateinit var fakeCalorieRepository: FakeCalorieRepository
    private lateinit var fakeHabitRepository: FakeHabitRepository
    private lateinit var fakePokemonRepository: FakePokemonRepository
    private lateinit var evaluateAchievementsUseCase: EvaluateAchievementsUseCase
    private lateinit var syncManager: PoketchSyncManager

    private lateinit var viewModel: RotaryCalorieLoggerViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()

        fakeUserRepository = FakeUserRepository()
        fakeCalorieRepository = FakeCalorieRepository()
        fakeHabitRepository = FakeHabitRepository()
        fakePokemonRepository = FakePokemonRepository()
        val unlockDispatcher = UnlockEventDispatcher()

        evaluateAchievementsUseCase = EvaluateAchievementsUseCase(
            userRepository = fakeUserRepository,
            calorieRepository = fakeCalorieRepository,
            habitRepository = fakeHabitRepository,
            pokemonRepository = fakePokemonRepository,
            unlockEventDispatcher = unlockDispatcher,
        )

        syncManager = PoketchSyncManager(
            context = context,
            transport = FakeWearableTransport(),
            userRepository = fakeUserRepository,
            calorieRepository = fakeCalorieRepository,
            habitRepository = fakeHabitRepository,
            pokemonRepository = fakePokemonRepository,
            unlockEventDispatcher = unlockDispatcher,
        )

        viewModel = RotaryCalorieLoggerViewModel(
            userRepository = fakeUserRepository,
            calorieRepository = fakeCalorieRepository,
            evaluateAchievementsUseCase = evaluateAchievementsUseCase,
            syncManager = syncManager,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun defaultSelectedCalories_is250() = runTest {
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        assertThat(viewModel.uiState.value.selectedCalories).isEqualTo(250)
    }

    @Test
    fun adjustCalories_plusOne_incrementsByOne() = runTest {
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        val changed = viewModel.adjustCalories(1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(changed).isTrue()
        assertThat(viewModel.uiState.value.selectedCalories).isEqualTo(251)
    }

    @Test
    fun adjustCalories_minusOne_decrementsByOne() = runTest {
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        val changed = viewModel.adjustCalories(-1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(changed).isTrue()
        assertThat(viewModel.uiState.value.selectedCalories).isEqualTo(249)
    }

    @Test
    fun adjustCalories_plusFiftyAndMinusFifty_workCorrectly() = runTest {
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.adjustCalories(50)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedCalories).isEqualTo(300)

        viewModel.adjustCalories(-50)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedCalories).isEqualTo(250)
    }

    @Test
    fun adjustCalories_clampsAtOneKcal() = runTest {
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.setCalories(5)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedCalories).isEqualTo(5)

        viewModel.adjustCalories(-10)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedCalories).isEqualTo(1)

        val changed = viewModel.adjustCalories(-1)
        assertThat(changed).isFalse()
        assertThat(viewModel.uiState.value.selectedCalories).isEqualTo(1)
    }

    @Test
    fun setCalories_allowsAccurateSmallValues() = runTest {
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.setCalories(17)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedCalories).isEqualTo(17)
    }

    private class FakeUserRepository : UserRepository {
        private val _profile = MutableStateFlow<UserProfile?>(
            UserProfile(
                id = 1,
                ageYears = 26,
                sex = BiologicalSex.MALE,
                heightCm = 175.0,
                weightKg = 72.0,
                activityLevel = ActivityLevel.MODERATELY_ACTIVE,
                deficitTier = DeficitTier.STANDARD,
                bmrKcal = 1650,
                tdeeKcal = 2300,
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
        var totalCalories = 500

        override fun observeLogsByDate(dateIso: String): Flow<List<CalorieLog>> = flowOf(emptyList())
        override fun observeTotalCaloriesByDate(dateIso: String): Flow<Int> = flowOf(totalCalories)
        override fun observeRecentDailyNutrition(days: Int): Flow<List<DailyNutritionSummary>> = flowOf(emptyList())
        override suspend fun getRecentDailyNutrition(days: Int): List<DailyNutritionSummary> = emptyList()
        override fun observeDailyBudgetState(dateIso: String): Flow<CalorieBudgetState> =
            flowOf(CalorieBudgetState.OnTrack(totalCalories, 1800, 1800 - totalCalories, 0.27))

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

    private class FakePokemonRepository : PokemonRepository {
        private val pokes = MutableStateFlow<List<Pokemon>>(emptyList())

        override fun observeAllPokemon(): Flow<List<Pokemon>> = pokes
        override fun observePokemonById(pokedexNumber: Int): Flow<Pokemon?> = flowOf(null)
        override suspend fun getPokemonByCategory(category: PokemonCategory): List<Pokemon> = emptyList()
        override suspend fun getPokemonByRuleCode(ruleCode: String): List<Pokemon> = emptyList()
        override suspend fun getUnlockedCount(): Int = 0
        override suspend fun updateProgress(pokedexNumber: Int, newProgress: Int): Pokemon? = null
        override suspend fun unlockPokemon(pokedexNumber: Int, timestampMillis: Long): Pokemon? = null
        override suspend fun upsertPokemon(pokemon: Pokemon) {}
        override suspend fun upsertAllPokemon(pokemonList: List<Pokemon>) {}
    }

    private class FakeWearableTransport : WearableTransport {
        override suspend fun putDataMap(path: String, dataMap: DataMap) {}
        override suspend fun sendMessage(path: String, data: ByteArray) {}
        override suspend fun getConnectedNodes(): List<ConnectedNodeInfo> = emptyList()
    }
}
