package com.poketch.wear.ui.battle

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poketch.core.data.gamification.EvaluateAchievementsUseCase
import com.poketch.core.data.gamification.UnlockEventDispatcher
import com.poketch.core.domain.calculator.CalorieBudgetState
import com.poketch.core.domain.engine.UnlockEvent
import com.poketch.core.domain.model.ActivityLevel
import com.poketch.core.domain.model.BiologicalSex
import com.poketch.core.domain.model.DeficitTier
import com.poketch.core.domain.model.DeviceSource
import com.poketch.core.domain.model.HabitType
import com.poketch.core.domain.model.Pokemon
import com.poketch.core.domain.model.UserProfile
import com.poketch.core.domain.repository.CalorieRepository
import com.poketch.core.domain.repository.CurrentWeather
import com.poketch.core.domain.repository.HabitRepository
import com.poketch.core.domain.repository.PokemonRepository
import com.poketch.core.domain.repository.UserRepository
import com.poketch.core.domain.repository.WeatherRepository
import com.poketch.core.sync.PoketchSyncManager
import com.poketch.wear.reminders.ReminderScheduler
import com.poketch.wear.ui.alert.WearHapticController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class GbaWatchBattleUiState(
    val todayIso: String = "",
    val consumedKcal: Int = 0,
    val targetCalorieCeiling: Int = 1800,
    val surplusKcal: Int = 0,
    val budgetState: CalorieBudgetState = CalorieBudgetState.OnTrack(0, 1800, 1800, 0.0),
    val todayWaterMl: Int = 0,
    val dailyWaterGoalMl: Int = 2500,
    val todayRunMiles: Double = 0.0,
    val todaySteps: Int = 0,
    val dailyStepGoal: Int = 10000,
    val activeBuddy: Pokemon? = null,
    val wildTarget: Pokemon? = null,
    val allPokemon: List<Pokemon> = emptyList(),
    val pinnedTargetPokemonId: Int? = null,
    val userProfile: UserProfile? = null,
    val currentWeather: CurrentWeather = CurrentWeather(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val dialogMessage: String? = null,
    val newlyUnlockedPokemon: UnlockEvent.NewPokemonUnlocked? = null,
    val hydrationRemindersEnabled: Boolean = true,
    val runRemindersEnabled: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GbaWatchBattleViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val calorieRepository: CalorieRepository,
    private val habitRepository: HabitRepository,
    private val pokemonRepository: PokemonRepository,
    private val weatherRepository: WeatherRepository,
    private val evaluateAchievementsUseCase: EvaluateAchievementsUseCase,
    private val unlockEventDispatcher: UnlockEventDispatcher,
    private val poketchSyncManager: PoketchSyncManager,
    private val wearHapticController: WearHapticController,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val todayIso: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private val prefs by lazy { context.getSharedPreferences("poketch_gba_battle_prefs", Context.MODE_PRIVATE) }
    private val initialPinnedId: Int? = prefs.getInt("pinned_target_id", -1).takeIf { it in 1..151 }

    private val userProfileFlow = userRepository.observeUserProfile()
    private val totalCaloriesFlow = calorieRepository.observeTotalCaloriesByDate(todayIso)
    private val budgetStateFlow = calorieRepository.observeDailyBudgetState(todayIso)
    private val dailyWaterFlow = habitRepository.observeDailyWaterMl(todayIso)
    private val dailyRunFlow = habitRepository.observeDailyRunMiles(todayIso)
    private val dailyStepsFlow = habitRepository.observeDailySteps(todayIso)
    private val allPokemonFlow = pokemonRepository.observeAllPokemon()
    private val currentWeatherFlow = weatherRepository.observeCurrentWeather()

    private val pinnedTargetIdFlow = MutableStateFlow<Int?>(initialPinnedId)
    private val isSyncingFlow = MutableStateFlow(false)
    private val dialogMessageFlow = MutableStateFlow<String?>(null)
    private val newlyUnlockedPokemonFlow = MutableStateFlow<UnlockEvent.NewPokemonUnlocked?>(null)
    private val hydrationRemindersFlow = MutableStateFlow(true)
    private val runRemindersFlow = MutableStateFlow(true)

    val selectedRootOption = MutableStateFlow(GbaRootOption.FIGHT)
    val selectedFightOption = MutableStateFlow(GbaFightOption.LOG_CAL)
    val commandMenuState = MutableStateFlow(GbaCommandMenuState.ROOT_MENU)

    fun setSelectedRootOption(option: GbaRootOption) {
        selectedRootOption.value = option
    }

    fun setSelectedFightOption(option: GbaFightOption) {
        selectedFightOption.value = option
    }

    fun setCommandMenuState(menuState: GbaCommandMenuState) {
        commandMenuState.value = menuState
    }

    init {
        viewModelScope.launch {
            evaluateAchievementsUseCase.evaluate(todayIso)
        }
        viewModelScope.launch {
            unlockEventDispatcher.unlockEvents.collect { event ->
                wearHapticController.playUnlockFanfareHaptic()
                newlyUnlockedPokemonFlow.value = event
                if (pinnedTargetIdFlow.value == event.pokemon.pokedexNumber) {
                    pinnedTargetIdFlow.value = null
                    prefs.edit().remove("pinned_target_id").apply()
                }
            }
        }
    }

    private val selectedBuddyIdFlow = MutableStateFlow<Int?>(null)

    private val buddyPokemonFlow = combine(userProfileFlow, selectedBuddyIdFlow) { profile, selectedId ->
        selectedId ?: profile?.activeBuddyPokemonId ?: 1
    }.flatMapLatest { buddyId ->
        pokemonRepository.observePokemonById(buddyId)
    }

    val uiState: StateFlow<GbaWatchBattleUiState> = combine(
        combine(userProfileFlow, totalCaloriesFlow, budgetStateFlow) { profile, consumed, budget ->
            Triple(profile, consumed, budget)
        },
        combine(dailyWaterFlow, dailyRunFlow, dailyStepsFlow, allPokemonFlow) { water, run, steps, allPokes ->
            Quad(water, run, steps, allPokes)
        },
        combine(buddyPokemonFlow, pinnedTargetIdFlow, isSyncingFlow, dialogMessageFlow, newlyUnlockedPokemonFlow) { buddy, pinnedId, syncing, dialog, newlyUnlocked ->
            Penta(buddy, pinnedId, syncing, dialog, newlyUnlocked)
        },
        combine(hydrationRemindersFlow, runRemindersFlow, currentWeatherFlow) { hydRem, runRem, weather ->
            Triple(hydRem, runRem, weather)
        }
    ) { (profile, consumed, budget), (water, run, steps, allPokes), (buddy, pinnedId, syncing, dialog, newlyUnlocked), (hydRem, runRem, weather) ->
        val ceiling = profile?.targetCalorieCeiling ?: 1800
        val waterGoal = profile?.dailyWaterGoalMl ?: 2500
        val surplus = maxOf(0, consumed - ceiling)

        // Wild Target selection:
        // 1. Explicit pinned target if set and still locked
        // 2. Locked Pokémon with active non-zero progress (> 0%)
        // 3. Engaging daily actionable targets (Poliwag #60, Pidgey #16, Ponyta #77, Sandshrew #27)
        // 4. First locked Pokémon fallback
        val lockedPokes = allPokes.filter { !it.isUnlocked }
        val target = if (pinnedId != null) {
            lockedPokes.find { it.pokedexNumber == pinnedId }
                ?: allPokes.find { it.pokedexNumber == pinnedId }
        } else null

        val finalTarget = target ?: run {
            val withProgress = lockedPokes.filter { it.currentProgress > 0 }.maxByOrNull { it.progressRatio }
            withProgress
                ?: lockedPokes.find { it.pokedexNumber == 60 } // Poliwag (water)
                ?: lockedPokes.find { it.pokedexNumber == 16 } // Pidgey (steps)
                ?: lockedPokes.find { it.pokedexNumber == 77 } // Ponyta (run)
                ?: lockedPokes.find { it.pokedexNumber == 27 } // Sandshrew (meals)
                ?: lockedPokes.firstOrNull()
                ?: allPokes.find { it.pokedexNumber == 77 }
        }

        GbaWatchBattleUiState(
            todayIso = todayIso,
            consumedKcal = consumed,
            targetCalorieCeiling = ceiling,
            surplusKcal = surplus,
            budgetState = budget,
            todayWaterMl = water.toInt(),
            dailyWaterGoalMl = waterGoal,
            todayRunMiles = run,
            todaySteps = steps.toInt(),
            dailyStepGoal = 10000,
            activeBuddy = buddy,
            wildTarget = finalTarget,
            allPokemon = allPokes,
            pinnedTargetPokemonId = pinnedId,
            userProfile = profile,
            currentWeather = weather,
            isLoading = false,
            isSyncing = syncing,
            dialogMessage = dialog,
            newlyUnlockedPokemon = newlyUnlocked,
            hydrationRemindersEnabled = hydRem,
            runRemindersEnabled = runRem,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GbaWatchBattleUiState(todayIso = todayIso, isLoading = true),
    )

    fun logWaterQuick250() {
        viewModelScope.launch {
            wearHapticController.playRunMilestoneQuarterHaptic()
            habitRepository.logHabit(
                dateIso = todayIso,
                habitType = HabitType.WATER_ML,
                numericValue = 250.0,
                sourceDevice = DeviceSource.WEAR,
            )
            evaluateAchievementsUseCase.evaluate(todayIso)
            showTemporaryDialog("LOGGED +250ml WATER!", durationMillis = 1000L)
        }
    }

    fun logWaterQuick500() {
        viewModelScope.launch {
            wearHapticController.playRunMilestoneQuarterHaptic()
            habitRepository.logHabit(
                dateIso = todayIso,
                habitType = HabitType.WATER_ML,
                numericValue = 500.0,
                sourceDevice = DeviceSource.WEAR,
            )
            evaluateAchievementsUseCase.evaluate(todayIso)
            showTemporaryDialog("LOGGED +500ml WATER!", durationMillis = 1000L)
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            isSyncingFlow.value = true
            showTemporaryDialog("SYNCING LINK CABLE...", durationMillis = 800L)
            try {
                poketchSyncManager.flushOfflineQueue()
                showTemporaryDialog("LINK CABLE SYNC OK!", durationMillis = 1500L)
                wearHapticController.playRunMilestoneQuarterHaptic()
            } catch (e: Exception) {
                showTemporaryDialog("SYNC ERROR: ${e.message?.take(16)}", durationMillis = 1500L)
            } finally {
                isSyncingFlow.value = false
            }
        }
    }

    fun pinWildTarget(pokedexNumber: Int) {
        if (pokedexNumber in 1..151) {
            pinnedTargetIdFlow.value = pokedexNumber
            prefs.edit().putInt("pinned_target_id", pokedexNumber).apply()
            viewModelScope.launch {
                val poke = pokemonRepository.observePokemonById(pokedexNumber).firstOrNull()
                val name = (poke?.name ?: "POKéMON").uppercase()
                showTemporaryDialog("TARGET: $name PINNED! ★", durationMillis = 3000L)
                wearHapticController.playRunMilestoneQuarterHaptic()
            }
        }
    }

    fun unpinWildTarget() {
        pinnedTargetIdFlow.value = null
        prefs.edit().remove("pinned_target_id").apply()
        viewModelScope.launch {
            showTemporaryDialog("TARGET UNPINNED", durationMillis = 2000L)
            wearHapticController.playRunMilestoneQuarterHaptic()
        }
    }

    fun setActiveBuddy(pokedexNumber: Int) {
        viewModelScope.launch {
            selectedBuddyIdFlow.value = pokedexNumber
            userRepository.updateActiveBuddy(pokedexNumber)
            poketchSyncManager.syncUserProfile()
            val poke = pokemonRepository.observePokemonById(pokedexNumber).firstOrNull()
            val name = (poke?.name ?: "POKéMON").uppercase()
            showTemporaryDialog("$name is now your Buddy! ★", durationMillis = 3000L)
            wearHapticController.playUnlockFanfareHaptic()
        }
    }

    fun showTemporaryDialog(message: String, durationMillis: Long = 1200L) {
        viewModelScope.launch {
            dialogMessageFlow.value = message
            kotlinx.coroutines.delay(durationMillis)
            if (dialogMessageFlow.value == message) {
                dialogMessageFlow.value = null
            }
        }
    }

    fun clearDialog() {
        dialogMessageFlow.value = null
    }

    fun playOverTargetHaptic() {
        wearHapticController.playRedAlertWaveform()
    }

    fun toggleHydrationReminders() {
        val next = !hydrationRemindersFlow.value
        hydrationRemindersFlow.value = next
        if (next) {
            reminderScheduler.scheduleHydrationWorker()
        }
    }

    fun toggleRunReminders() {
        val next = !runRemindersFlow.value
        runRemindersFlow.value = next
        if (next) {
            reminderScheduler.scheduleMovementNudge()
        }
    }

    fun updateProfileBiometrics(
        age: Int,
        sex: BiologicalSex,
        heightCm: Double,
        weightKg: Double,
        activityLevel: ActivityLevel,
        tier: DeficitTier,
        deficitKcal: Int = tier.dailyDeficitKcal,
    ) {
        viewModelScope.launch {
            userRepository.saveUserProfile(
                ageYears = age,
                sex = sex,
                heightCm = heightCm,
                weightKg = weightKg,
                activityLevel = activityLevel,
                deficitTier = tier,
                dailyDeficitKcal = deficitKcal,
                dailyWaterGoalMl = uiState.value.dailyWaterGoalMl,
                activeBuddyPokemonId = uiState.value.activeBuddy?.pokedexNumber ?: 1,
            )
        }
    }

    fun updateProfileDeficit(deficitKcal: Int) {
        val p = uiState.value.userProfile ?: return
        val clampedDeficit = deficitKcal.coerceIn(0, 1500)
        viewModelScope.launch {
            val tier = when (clampedDeficit) {
                250 -> DeficitTier.MILD
                500 -> DeficitTier.STANDARD
                750 -> DeficitTier.AGGRESSIVE
                else -> DeficitTier.CUSTOM
            }
            userRepository.saveUserProfile(
                ageYears = p.ageYears,
                sex = p.sex,
                heightCm = p.heightCm,
                weightKg = p.weightKg,
                activityLevel = p.activityLevel,
                deficitTier = tier,
                dailyDeficitKcal = clampedDeficit,
                dailyWaterGoalMl = uiState.value.dailyWaterGoalMl,
                activeBuddyPokemonId = uiState.value.activeBuddy?.pokedexNumber ?: 1,
            )
        }
    }

    fun dismissUnlockCelebration() {
        newlyUnlockedPokemonFlow.value = null
    }

    fun setUnlockedAsActiveBuddy(pokedexNumber: Int) {
        newlyUnlockedPokemonFlow.value = null
        setActiveBuddy(pokedexNumber)
    }

    fun triggerDemoUnlock(pokemonId: Int = 54) {
        viewModelScope.launch {
            val poke = uiState.value.allPokemon.find { it.pokedexNumber == pokemonId }
                ?: com.poketch.core.domain.model.Pokemon(
                    pokedexNumber = pokemonId,
                    name = "Psyduck",
                    primaryType = "Water",
                    unlockRule = com.poketch.core.domain.model.UnlockRule(
                        ruleCode = "HYDRATION_3",
                        category = com.poketch.core.domain.model.PokemonCategory.WATER_HYDRATION,
                        description = "Log water 3 times in a day",
                        targetThreshold = 3
                    ),
                    isUnlocked = true,
                    currentProgress = 3,
                    spriteAssetPath = "sprites/pokemon_054.png"
                )
            newlyUnlockedPokemonFlow.value = com.poketch.core.domain.engine.UnlockEvent.NewPokemonUnlocked(
                pokemon = poke,
                triggerReason = "Hydration Goal Reached! 3/3 Logs Complete!",
                timestampMillis = System.currentTimeMillis()
            )
        }
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
    private data class Penta<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
}
