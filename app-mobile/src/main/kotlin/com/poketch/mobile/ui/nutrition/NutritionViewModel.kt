package com.poketch.mobile.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poketch.core.data.gamification.EvaluateAchievementsUseCase
import com.poketch.core.domain.calculator.CalorieBudgetState
import com.poketch.core.domain.calculator.CalorieCalculationSummary
import com.poketch.core.domain.calculator.CalorieDeficitCalculator
import com.poketch.core.domain.model.ActivityLevel
import com.poketch.core.domain.model.BiologicalSex
import com.poketch.core.domain.model.CalorieLog
import com.poketch.core.domain.model.DeficitTier
import com.poketch.core.domain.model.DeviceSource
import com.poketch.core.domain.model.MealCategory
import com.poketch.core.domain.repository.CalorieRepository
import com.poketch.core.domain.repository.UserRepository
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

data class NutritionUiState(
    val todayIso: String = "",
    // Biometrics Form State
    val ageText: String = "25",
    val sex: BiologicalSex = BiologicalSex.MALE,
    val heightText: String = "175.0",
    val weightText: String = "75.0",
    val activityLevel: ActivityLevel = ActivityLevel.SEDENTARY,
    val deficitTier: DeficitTier = DeficitTier.STANDARD,
    val dailyWaterGoalMl: Int = 2500,
    val activeBuddyPokemonId: Int = 1,
    val calculationSummary: CalorieCalculationSummary? = null,
    val isBiometricsValid: Boolean = true,
    val isConfigCardExpanded: Boolean = false,
    val biometricsSavedMessage: String? = null,
    // Meal Entry Form State
    val inputCategory: MealCategory = MealCategory.BREAKFAST,
    val foodNameText: String = "",
    val caloriesText: String = "",
    val proteinText: String = "",
    val carbsText: String = "",
    val fatText: String = "",
    // List & Filter State
    val filterCategory: MealCategory? = null,
    val todayLogs: List<CalorieLog> = emptyList(),
    val totalCaloriesToday: Int = 0,
    val budgetState: CalorieBudgetState = CalorieBudgetState.OnTrack(0, 1800, 1800, 0.0),
    val isLoading: Boolean = false,
)

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val calorieRepository: CalorieRepository,
    private val evaluateAchievementsUseCase: EvaluateAchievementsUseCase,
) : ViewModel() {

    private val todayIso: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private val _formState = MutableStateFlow(
        NutritionFormState(
            todayIso = todayIso,
            calculationSummary = computeSummary(
                "25",
                BiologicalSex.MALE,
                "175.0",
                "75.0",
                ActivityLevel.SEDENTARY,
                DeficitTier.STANDARD,
            ),
        )
    )

    private val userProfileFlow = userRepository.observeUserProfile()
    private val logsFlow = calorieRepository.observeLogsByDate(todayIso)
    private val totalCaloriesFlow = calorieRepository.observeTotalCaloriesByDate(todayIso)
    private val budgetStateFlow = calorieRepository.observeDailyBudgetState(todayIso)

    private var hasUserModifiedProfileForm = false

    val uiState: StateFlow<NutritionUiState> = combine(
        _formState,
        logsFlow,
        totalCaloriesFlow,
        budgetStateFlow,
    ) { form, logs, totalCalories, budgetState ->
        val filteredLogs = if (form.filterCategory != null) {
            logs.filter { it.mealCategory == form.filterCategory }
        } else {
            logs
        }
        NutritionUiState(
            todayIso = todayIso,
            ageText = form.ageText,
            sex = form.sex,
            heightText = form.heightText,
            weightText = form.weightText,
            activityLevel = form.activityLevel,
            deficitTier = form.deficitTier,
            dailyWaterGoalMl = form.dailyWaterGoalMl,
            activeBuddyPokemonId = form.activeBuddyPokemonId,
            calculationSummary = form.calculationSummary,
            isBiometricsValid = form.isBiometricsValid,
            isConfigCardExpanded = form.isConfigCardExpanded,
            biometricsSavedMessage = form.biometricsSavedMessage,
            inputCategory = form.inputCategory,
            foodNameText = form.foodNameText,
            caloriesText = form.caloriesText,
            proteinText = form.proteinText,
            carbsText = form.carbsText,
            fatText = form.fatText,
            filterCategory = form.filterCategory,
            todayLogs = filteredLogs,
            totalCaloriesToday = totalCalories,
            budgetState = budgetState,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NutritionUiState(todayIso = todayIso)
    )

    init {
        viewModelScope.launch {
            userProfileFlow.collect { profile ->
                if ((profile != null) && !hasUserModifiedProfileForm) {
                    val summary = computeSummary(
                        ageStr = profile.ageYears.toString(),
                        sex = profile.sex,
                        heightStr = profile.heightCm.toString(),
                        weightStr = profile.weightKg.toString(),
                        activityLevel = profile.activityLevel,
                        deficitTier = profile.deficitTier,
                    )
                    _formState.value = _formState.value.copy(
                        ageText = profile.ageYears.toString(),
                        sex = profile.sex,
                        heightText = profile.heightCm.toString(),
                        weightText = profile.weightKg.toString(),
                        activityLevel = profile.activityLevel,
                        deficitTier = profile.deficitTier,
                        dailyWaterGoalMl = profile.dailyWaterGoalMl,
                        activeBuddyPokemonId = profile.activeBuddyPokemonId,
                        calculationSummary = summary,
                        isBiometricsValid = summary != null,
                    )
                }
            }
        }
    }

    // --- Biometrics Configuration Handlers ---

    fun onAgeChanged(newAge: String) {
        hasUserModifiedProfileForm = true
        updateBiometrics { copy(ageText = newAge) }
    }

    fun onSexChanged(newSex: BiologicalSex) {
        hasUserModifiedProfileForm = true
        updateBiometrics { copy(sex = newSex) }
    }

    fun onHeightChanged(newHeight: String) {
        hasUserModifiedProfileForm = true
        updateBiometrics { copy(heightText = newHeight) }
    }

    fun onWeightChanged(newWeight: String) {
        hasUserModifiedProfileForm = true
        updateBiometrics { copy(weightText = newWeight) }
    }

    fun onActivityLevelChanged(newLevel: ActivityLevel) {
        hasUserModifiedProfileForm = true
        updateBiometrics { copy(activityLevel = newLevel) }
    }

    fun onDeficitTierChanged(newTier: DeficitTier) {
        hasUserModifiedProfileForm = true
        updateBiometrics { copy(deficitTier = newTier) }
    }

    fun toggleConfigCardExpanded() {
        _formState.value = _formState.value.copy(
            isConfigCardExpanded = !_formState.value.isConfigCardExpanded
        )
    }

    fun clearSavedMessage() {
        _formState.value = _formState.value.copy(biometricsSavedMessage = null)
    }

    private inline fun updateBiometrics(transform: NutritionFormState.() -> NutritionFormState) {
        val current = _formState.value.transform()
        val summary = computeSummary(
            ageStr = current.ageText,
            sex = current.sex,
            heightStr = current.heightText,
            weightStr = current.weightText,
            activityLevel = current.activityLevel,
            deficitTier = current.deficitTier,
        )
        _formState.value = current.copy(
            calculationSummary = summary,
            isBiometricsValid = summary != null,
            biometricsSavedMessage = null,
        )
    }

    fun saveBiometricsAndSync() {
        val state = _formState.value
        val age = state.ageText.toIntOrNull() ?: return
        val height = state.heightText.toDoubleOrNull() ?: return
        val weight = state.weightText.toDoubleOrNull() ?: return

        if (age !in 13..120 || height !in 100.0..250.0 || weight !in 30.0..300.0) return

        viewModelScope.launch {
            userRepository.saveUserProfile(
                ageYears = age,
                sex = state.sex,
                heightCm = height,
                weightKg = weight,
                activityLevel = state.activityLevel,
                deficitTier = state.deficitTier,
                dailyWaterGoalMl = state.dailyWaterGoalMl,
                activeBuddyPokemonId = state.activeBuddyPokemonId,
            )
            evaluateAchievementsUseCase.evaluate(todayIso)
            _formState.value = _formState.value.copy(
                biometricsSavedMessage = "Target ceiling saved & synced to Wear OS!",
                isConfigCardExpanded = false,
            )
        }
    }

    // --- Meal Logger Handlers ---

    fun onInputCategoryChanged(category: MealCategory) {
        _formState.value = _formState.value.copy(inputCategory = category)
    }

    fun onFoodNameChanged(name: String) {
        _formState.value = _formState.value.copy(foodNameText = name)
    }

    fun onCaloriesChanged(calories: String) {
        _formState.value = _formState.value.copy(caloriesText = calories)
    }

    fun onProteinChanged(protein: String) {
        _formState.value = _formState.value.copy(proteinText = protein)
    }

    fun onCarbsChanged(carbs: String) {
        _formState.value = _formState.value.copy(carbsText = carbs)
    }

    fun onFatChanged(fat: String) {
        _formState.value = _formState.value.copy(fatText = fat)
    }

    fun onFilterCategorySelected(category: MealCategory?) {
        _formState.value = _formState.value.copy(filterCategory = category)
    }

    fun logMeal() {
        val state = _formState.value
        val calories = state.caloriesText.toIntOrNull() ?: return
        if (calories <= 0) return

        val protein = state.proteinText.toIntOrNull() ?: 0
        val carbs = state.carbsText.toIntOrNull() ?: 0
        val fat = state.fatText.toIntOrNull() ?: 0
        val desc = state.foodNameText.trim().ifEmpty {
            state.inputCategory.name.lowercase().replaceFirstChar { it.uppercase() }
        }

        val log = CalorieLog(
            logId = UUID.randomUUID().toString(),
            dateIso = todayIso,
            mealCategory = state.inputCategory,
            foodDescription = desc,
            caloriesKcal = calories,
            proteinG = maxOf(0, protein),
            carbsG = maxOf(0, carbs),
            fatG = maxOf(0, fat),
            sourceDevice = DeviceSource.MOBILE,
            timestampMillis = System.currentTimeMillis(),
            isSynced = false,
        )

        viewModelScope.launch {
            calorieRepository.upsertCalorieLog(log)
            evaluateAchievementsUseCase.evaluate(todayIso)
            _formState.value = _formState.value.copy(
                foodNameText = "",
                caloriesText = "",
                proteinText = "",
                carbsText = "",
                fatText = "",
            )
        }
    }

    fun quickAddMeal(category: MealCategory, calories: Int, defaultName: String, proteinG: Int = 0) {
        val log = CalorieLog(
            logId = UUID.randomUUID().toString(),
            dateIso = todayIso,
            mealCategory = category,
            foodDescription = defaultName,
            caloriesKcal = calories,
            proteinG = proteinG,
            carbsG = 0,
            fatG = 0,
            sourceDevice = DeviceSource.MOBILE,
            timestampMillis = System.currentTimeMillis(),
            isSynced = false,
        )

        viewModelScope.launch {
            calorieRepository.upsertCalorieLog(log)
            evaluateAchievementsUseCase.evaluate(todayIso)
        }
    }

    fun deleteLog(logId: String) {
        viewModelScope.launch {
            calorieRepository.deleteCalorieLog(logId)
            evaluateAchievementsUseCase.evaluate(todayIso)
        }
    }

    private fun computeSummary(
        ageStr: String,
        sex: BiologicalSex,
        heightStr: String,
        weightStr: String,
        activityLevel: ActivityLevel,
        deficitTier: DeficitTier,
    ): CalorieCalculationSummary? {
        val age = ageStr.toIntOrNull() ?: return null
        val height = heightStr.toDoubleOrNull() ?: return null
        val weight = weightStr.toDoubleOrNull() ?: return null
        if (age !in 13..120 || height !in 100.0..250.0 || weight !in 30.0..300.0) return null

        return runCatching {
            CalorieDeficitCalculator.calculateTargetCeiling(
                ageYears = age,
                sex = sex,
                heightCm = height,
                weightKg = weight,
                activityLevel = activityLevel,
                deficitTier = deficitTier,
            )
        }.getOrNull()
    }
}

private data class NutritionFormState(
    val todayIso: String,
    val ageText: String = "25",
    val sex: BiologicalSex = BiologicalSex.MALE,
    val heightText: String = "175.0",
    val weightText: String = "75.0",
    val activityLevel: ActivityLevel = ActivityLevel.SEDENTARY,
    val deficitTier: DeficitTier = DeficitTier.STANDARD,
    val dailyWaterGoalMl: Int = 2500,
    val activeBuddyPokemonId: Int = 1,
    val calculationSummary: CalorieCalculationSummary? = null,
    val isBiometricsValid: Boolean = true,
    val isConfigCardExpanded: Boolean = false,
    val biometricsSavedMessage: String? = null,
    val inputCategory: MealCategory = MealCategory.BREAKFAST,
    val foodNameText: String = "",
    val caloriesText: String = "",
    val proteinText: String = "",
    val carbsText: String = "",
    val fatText: String = "",
    val filterCategory: MealCategory? = null,
)
