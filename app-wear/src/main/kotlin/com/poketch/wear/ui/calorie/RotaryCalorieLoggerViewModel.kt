package com.poketch.wear.ui.calorie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poketch.core.data.gamification.EvaluateAchievementsUseCase
import com.poketch.core.domain.calculator.CalorieBudgetState
import com.poketch.core.domain.calculator.CalorieDeficitCalculator
import com.poketch.core.domain.model.CalorieLog
import com.poketch.core.domain.model.DeviceSource
import com.poketch.core.domain.model.MealCategory
import com.poketch.core.domain.repository.CalorieRepository
import com.poketch.core.domain.repository.UserRepository
import com.poketch.core.sync.PoketchSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class RotaryCalorieUiState(
    val todayIso: String = "",
    val currentConsumedKcal: Int = 0,
    val targetCalorieCeiling: Int = 1800,
    val selectedCalories: Int = 250,
    val newTotalCalories: Int = 250,
    val willExceedCeiling: Boolean = false,
    val surplusKcal: Int = 0,
    val isSaving: Boolean = false,
)

@HiltViewModel
class RotaryCalorieLoggerViewModel @Inject constructor(
    userRepository: UserRepository,
    private val calorieRepository: CalorieRepository,
    private val evaluateAchievementsUseCase: EvaluateAchievementsUseCase,
    private val syncManager: PoketchSyncManager,
) : ViewModel() {

    private val todayIso: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private val _selectedCalories = MutableStateFlow(250)
    private val userProfileFlow = userRepository.observeUserProfile()
    private val totalCaloriesFlow = calorieRepository.observeTotalCaloriesByDate(todayIso)

    val uiState: StateFlow<RotaryCalorieUiState> = combine(
        userProfileFlow,
        totalCaloriesFlow,
        _selectedCalories,
    ) { profile, consumed, selected ->
        val ceiling = profile?.targetCalorieCeiling ?: 1800
        val newTotal = consumed + selected
        val willExceed = newTotal > ceiling
        val surplus = if (willExceed) newTotal - ceiling else 0

        RotaryCalorieUiState(
            todayIso = todayIso,
            currentConsumedKcal = consumed,
            targetCalorieCeiling = ceiling,
            selectedCalories = selected,
            newTotalCalories = newTotal,
            willExceedCeiling = willExceed,
            surplusKcal = surplus,
            isSaving = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RotaryCalorieUiState(todayIso = todayIso),
    )

    fun adjustCalories(delta: Int): Boolean {
        val current = _selectedCalories.value
        val updated = (current + delta).coerceIn(1, 2500)
        return if (updated != current) {
            _selectedCalories.value = updated
            true
        } else {
            false
        }
    }

    fun setCalories(calories: Int) {
        _selectedCalories.value = calories.coerceIn(1, 2500)
    }

    fun confirmCalorieLog(
        onOverTarget: () -> Unit,
        onSuccess: () -> Unit,
    ) {
        val state = uiState.value
        val log = CalorieLog(
            logId = UUID.randomUUID().toString(),
            dateIso = todayIso,
            mealCategory = MealCategory.WATCH_QUICK_ADD,
            foodDescription = "Watch Crown +${state.selectedCalories} kcal",
            caloriesKcal = state.selectedCalories,
            proteinG = 0,
            carbsG = 0,
            fatG = 0,
            sourceDevice = DeviceSource.WEAR,
            timestampMillis = System.currentTimeMillis(),
            isSynced = false,
        )

        viewModelScope.launch {
            calorieRepository.upsertCalorieLog(log)
            evaluateAchievementsUseCase.evaluate(todayIso)
            runCatching {
                syncManager.coalesceAndSyncCalorie(log)
            }

            val budgetStatus = CalorieDeficitCalculator.evaluateBudgetStatus(
                consumedKcal = state.newTotalCalories,
                targetCeilingKcal = state.targetCalorieCeiling,
            )

            if (budgetStatus is CalorieBudgetState.OverTargetWarning) {
                runCatching {
                    syncManager.sendOverCalorieAlert(budgetStatus.surplusKcal)
                }
                onOverTarget()
            } else {
                onSuccess()
            }
        }
    }
}
