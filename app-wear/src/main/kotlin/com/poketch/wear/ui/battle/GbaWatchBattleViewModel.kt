package com.poketch.wear.ui.battle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poketch.core.data.gamification.EvaluateAchievementsUseCase
import com.poketch.core.domain.calculator.CalorieBudgetState
import com.poketch.core.domain.model.ActivityLevel
import com.poketch.core.domain.model.BiologicalSex
import com.poketch.core.domain.model.DeficitTier
import com.poketch.core.domain.model.DeviceSource
import com.poketch.core.domain.model.HabitType
import com.poketch.core.domain.model.Pokemon
import com.poketch.core.domain.model.UserProfile
import com.poketch.core.domain.repository.CalorieRepository
import com.poketch.core.domain.repository.HabitRepository
import com.poketch.core.domain.repository.PokemonRepository
import com.poketch.core.domain.repository.UserRepository
import com.poketch.core.sync.PoketchSyncManager
import com.poketch.wear.reminders.ReminderScheduler
import com.poketch.wear.ui.alert.WearHapticController
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val activeBuddy: Pokemon? = null,
    val wildTarget: Pokemon? = null,
    val allPokemon: List<Pokemon> = emptyList(),
    val pinnedTargetPokemonId: Int? = null,
    val userProfile: UserProfile? = null,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val dialogMessage: String? = null,
    val hydrationRemindersEnabled: Boolean = true,
    val runRemindersEnabled: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GbaWatchBattleViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val calorieRepository: CalorieRepository,
    private val habitRepository: HabitRepository,
    private val pokemonRepository: PokemonRepository,
    private val evaluateAchievementsUseCase: EvaluateAchievementsUseCase,
    private val poketchSyncManager: PoketchSyncManager,
    private val wearHapticController: WearHapticController,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val todayIso: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private val userProfileFlow = userRepository.observeUserProfile()
    private val totalCaloriesFlow = calorieRepository.observeTotalCaloriesByDate(todayIso)
    private val budgetStateFlow = calorieRepository.observeDailyBudgetState(todayIso)
    private val dailyWaterFlow = habitRepository.observeDailyWaterMl(todayIso)
    private val dailyRunFlow = habitRepository.observeDailyRunMiles(todayIso)
    private val allPokemonFlow = pokemonRepository.observeAllPokemon()

    private val pinnedTargetIdFlow = MutableStateFlow<Int?>(null)
    private val isSyncingFlow = MutableStateFlow(false)
    private val dialogMessageFlow = MutableStateFlow<String?>(null)
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
        combine(dailyWaterFlow, dailyRunFlow, allPokemonFlow) { water, run, allPokes ->
            Triple(water, run, allPokes)
        },
        combine(buddyPokemonFlow, pinnedTargetIdFlow, isSyncingFlow, dialogMessageFlow) { buddy, pinnedId, syncing, dialog ->
            Quad(buddy, pinnedId, syncing, dialog)
        },
        combine(hydrationRemindersFlow, runRemindersFlow) { hydRem, runRem ->
            Pair(hydRem, runRem)
        }
    ) { (profile, consumed, budget), (water, run, allPokes), (buddy, pinnedId, syncing, dialog), (hydRem, runRem) ->
        val ceiling = profile?.targetCalorieCeiling ?: 1800
        val waterGoal = profile?.dailyWaterGoalMl ?: 2500
        val surplus = maxOf(0, consumed - ceiling)

        // Wild Target selection: Pinned Target > First Locked Pokémon > Fallback Ponyta (#77)
        val target = if (pinnedId != null) {
            allPokes.find { it.pokedexNumber == pinnedId }
        } else {
            allPokes.find { !it.isUnlocked } ?: allPokes.find { it.pokedexNumber == 77 }
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
            activeBuddy = buddy,
            wildTarget = target,
            allPokemon = allPokes,
            pinnedTargetPokemonId = pinnedId,
            userProfile = profile,
            isLoading = false,
            isSyncing = syncing,
            dialogMessage = dialog,
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
        pinnedTargetIdFlow.value = pokedexNumber
    }

    fun setActiveBuddy(pokedexNumber: Int) {
        viewModelScope.launch {
            selectedBuddyIdFlow.value = pokedexNumber
            userRepository.updateActiveBuddy(pokedexNumber)
            poketchSyncManager.syncUserProfile()
            val poke = pokemonRepository.observePokemonById(pokedexNumber).firstOrNull()
            val name = (poke?.name ?: "POKéMON").uppercase()
            showTemporaryDialog("$name is now your Buddy! ★", durationMillis = 1500L)
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
    ) {
        viewModelScope.launch {
            userRepository.saveUserProfile(
                ageYears = age,
                sex = sex,
                heightCm = heightCm,
                weightKg = weightKg,
                activityLevel = activityLevel,
                deficitTier = tier,
                dailyWaterGoalMl = uiState.value.dailyWaterGoalMl,
                activeBuddyPokemonId = uiState.value.activeBuddy?.pokedexNumber ?: 1,
            )
        }
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
