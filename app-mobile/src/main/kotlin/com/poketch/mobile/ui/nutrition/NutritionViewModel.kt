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
import com.poketch.core.domain.model.DailyNutritionSummary
import com.poketch.core.domain.model.DeficitTier
import com.poketch.core.domain.model.DeviceSource
import com.poketch.core.domain.model.MealCategory
import com.poketch.core.domain.model.SavedFood
import com.poketch.core.domain.repository.CalorieRepository
import com.poketch.core.domain.repository.SavedFoodRepository
import com.poketch.core.domain.repository.UserRepository
import com.poketch.core.sync.PoketchSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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

enum class WeightUnit {
    KG,
    LBS
}

enum class HeightUnit {
    CM,
    FT_IN
}

data class NutritionUiState(
    val todayIso: String = "",
    // Biometrics Form State
    val ageText: String = "25",
    val sex: BiologicalSex = BiologicalSex.MALE,
    val heightText: String = "175.0",
    val weightText: String = "75.0",
    val weightUnit: WeightUnit = WeightUnit.KG,
    val heightUnit: HeightUnit = HeightUnit.CM,
    val weightLbsText: String = "165.3",
    val heightFeetText: String = "5",
    val heightInchesText: String = "8.9",
    val activityLevel: ActivityLevel = ActivityLevel.SEDENTARY,
    val deficitTier: DeficitTier = DeficitTier.STANDARD,
    val dailyWaterGoalMl: Int = 2500,
    val activeBuddyPokemonId: Int = 1,
    val calculationSummary: CalorieCalculationSummary? = null,
    val targetCalorieCeiling: Int = 1800,
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
    val todayLogs: ImmutableList<CalorieLog> = persistentListOf(),
    val meals: ImmutableList<CalorieLog> = persistentListOf(),
    val favoriteFoods: ImmutableList<SavedFood> = persistentListOf(),
    val recentFoods: ImmutableList<SavedFood> = persistentListOf(),
    val totalCaloriesToday: Int = 0,
    val budgetState: CalorieBudgetState = CalorieBudgetState.OnTrack(0, 1800, 1800, 0.0),
    val weeklyNutrition: List<DailyNutritionSummary> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val calorieRepository: CalorieRepository,
    private val savedFoodRepository: SavedFoodRepository,
    private val evaluateAchievementsUseCase: EvaluateAchievementsUseCase,
    private val syncManager: PoketchSyncManager,
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
    private val weeklyNutritionFlow = calorieRepository.observeRecentDailyNutrition(7)

    private var hasUserModifiedProfileForm = false

    val uiState: StateFlow<NutritionUiState> = combine(
        _formState,
        logsFlow,
        totalCaloriesFlow,
        budgetStateFlow,
        combine(
            combine(userProfileFlow, weeklyNutritionFlow) { profile, weekly -> Pair(profile, weekly) },
            savedFoodRepository.observeFavorites(),
            savedFoodRepository.observeRecentFoods(10)
        ) { profileWeekly, favorites, recents ->
            Triple(profileWeekly, favorites, recents)
        }
    ) { form, logs, totalCalories, budgetState, (profileWeekly, favorites, recents) ->
        val (profile, weekly) = profileWeekly
        val filteredLogs = if (form.filterCategory != null) {
            logs.filter { it.mealCategory == form.filterCategory }
        } else {
            logs
        }
        val targetCeiling = profile?.targetCalorieCeiling ?: 1800
        NutritionUiState(
            todayIso = todayIso,
            ageText = form.ageText,
            sex = form.sex,
            heightText = form.heightText,
            weightText = form.weightText,
            weightUnit = form.weightUnit,
            heightUnit = form.heightUnit,
            weightLbsText = form.weightLbsText,
            heightFeetText = form.heightFeetText,
            heightInchesText = form.heightInchesText,
            activityLevel = form.activityLevel,
            deficitTier = form.deficitTier,
            dailyWaterGoalMl = form.dailyWaterGoalMl,
            activeBuddyPokemonId = form.activeBuddyPokemonId,
            calculationSummary = form.calculationSummary,
            targetCalorieCeiling = targetCeiling,
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
            todayLogs = filteredLogs.toImmutableList(),
            meals = filteredLogs.toImmutableList(),
            favoriteFoods = favorites.toImmutableList(),
            recentFoods = recents.toImmutableList(),
            totalCaloriesToday = totalCalories,
            budgetState = budgetState,
            weeklyNutrition = weekly,
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
                    val lbsVal = String.format(Locale.US, "%.1f", profile.weightKg * 2.20462262)
                    val totalInches = profile.heightCm / 2.54
                    val feetVal = (totalInches / 12).toInt().toString()
                    val inchesVal = String.format(Locale.US, "%.1f", totalInches % 12)

                    _formState.value = _formState.value.copy(
                        ageText = profile.ageYears.toString(),
                        sex = profile.sex,
                        heightText = profile.heightCm.toString(),
                        weightText = profile.weightKg.toString(),
                        weightLbsText = lbsVal,
                        heightFeetText = feetVal,
                        heightInchesText = inchesVal,
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

    fun onHeightChanged(newHeightCm: String) {
        hasUserModifiedProfileForm = true
        val cm = newHeightCm.toDoubleOrNull()
        val feetVal: String
        val inchesVal: String
        if (cm != null && cm > 0) {
            val totalInches = cm / 2.54
            val ft = (totalInches / 12).toInt()
            feetVal = ft.toString()
            inchesVal = String.format(Locale.US, "%.1f", totalInches - ft * 12)
        } else {
            feetVal = _formState.value.heightFeetText
            inchesVal = _formState.value.heightInchesText
        }
        updateBiometrics {
            copy(
                heightText = newHeightCm,
                heightFeetText = feetVal,
                heightInchesText = inchesVal
            )
        }
    }

    fun onWeightChanged(newWeightKg: String) {
        hasUserModifiedProfileForm = true
        val kg = newWeightKg.toDoubleOrNull()
        val lbsVal = if (kg != null && kg > 0) {
            String.format(Locale.US, "%.1f", kg * 2.20462262)
        } else {
            _formState.value.weightLbsText
        }
        updateBiometrics {
            copy(
                weightText = newWeightKg,
                weightLbsText = lbsVal
            )
        }
    }

    fun onWeightUnitChanged(unit: WeightUnit) {
        hasUserModifiedProfileForm = true
        val state = _formState.value
        if (unit == WeightUnit.LBS && state.weightUnit == WeightUnit.KG) {
            val kg = state.weightText.toDoubleOrNull()
            val lbs = if (kg != null && kg > 0) String.format(Locale.US, "%.1f", kg * 2.20462262) else state.weightLbsText
            _formState.value = state.copy(weightUnit = unit, weightLbsText = lbs)
        } else if (unit == WeightUnit.KG && state.weightUnit == WeightUnit.LBS) {
            val lbs = state.weightLbsText.toDoubleOrNull()
            val kg = if (lbs != null && lbs > 0) String.format(Locale.US, "%.1f", lbs * 0.45359237) else state.weightText
            _formState.value = state.copy(weightUnit = unit, weightText = kg)
        } else {
            _formState.value = state.copy(weightUnit = unit)
        }
    }

    fun onHeightUnitChanged(unit: HeightUnit) {
        hasUserModifiedProfileForm = true
        val state = _formState.value
        if (unit == HeightUnit.FT_IN && state.heightUnit == HeightUnit.CM) {
            val cm = state.heightText.toDoubleOrNull()
            if (cm != null && cm > 0) {
                val totalInches = cm / 2.54
                val ft = (totalInches / 12).toInt()
                val inc = String.format(Locale.US, "%.1f", totalInches - ft * 12)
                _formState.value = state.copy(heightUnit = unit, heightFeetText = ft.toString(), heightInchesText = inc)
            } else {
                _formState.value = state.copy(heightUnit = unit)
            }
        } else if (unit == HeightUnit.CM && state.heightUnit == HeightUnit.FT_IN) {
            val ft = state.heightFeetText.toIntOrNull() ?: 0
            val inc = state.heightInchesText.toDoubleOrNull() ?: 0.0
            val totalInches = ft * 12.0 + inc
            if (totalInches > 0) {
                val cm = String.format(Locale.US, "%.1f", totalInches * 2.54)
                _formState.value = state.copy(heightUnit = unit, heightText = cm)
            } else {
                _formState.value = state.copy(heightUnit = unit)
            }
        } else {
            _formState.value = state.copy(heightUnit = unit)
        }
    }

    fun onWeightLbsChanged(newLbs: String) {
        hasUserModifiedProfileForm = true
        val lbs = newLbs.toDoubleOrNull()
        val kgVal = if (lbs != null && lbs > 0) {
            String.format(Locale.US, "%.1f", lbs * 0.45359237)
        } else {
            ""
        }
        updateBiometrics {
            copy(
                weightLbsText = newLbs,
                weightText = kgVal
            )
        }
    }

    fun onHeightFeetChanged(newFeet: String) {
        hasUserModifiedProfileForm = true
        val ft = newFeet.toIntOrNull() ?: 0
        val inc = _formState.value.heightInchesText.toDoubleOrNull() ?: 0.0
        val totalInches = ft * 12.0 + inc
        val cmVal = if (totalInches > 0) String.format(Locale.US, "%.1f", totalInches * 2.54) else ""
        updateBiometrics {
            copy(
                heightFeetText = newFeet,
                heightText = cmVal
            )
        }
    }

    fun onHeightInchesChanged(newInches: String) {
        hasUserModifiedProfileForm = true
        val ft = _formState.value.heightFeetText.toIntOrNull() ?: 0
        val inc = newInches.toDoubleOrNull() ?: 0.0
        val totalInches = ft * 12.0 + inc
        val cmVal = if (totalInches > 0) String.format(Locale.US, "%.1f", totalInches * 2.54) else ""
        updateBiometrics {
            copy(
                heightInchesText = newInches,
                heightText = cmVal
            )
        }
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

        // Validate bounds in strictly metric values
        if (age !in 13..120 || height !in 100.0..250.0 || weight !in 30.0..300.0) return

        viewModelScope.launch {
            // Strictly metric storage in Room
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
            runCatching {
                syncManager.syncUserProfile()
            }
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

    fun applyMealTemplate(template: KantoMealTemplate) {
        _formState.value = _formState.value.copy(
            inputCategory = template.category,
            foodNameText = template.name,
            caloriesText = template.caloriesKcal.toString(),
            proteinText = template.proteinG.toString(),
            carbsText = template.carbsG.toString(),
            fatText = template.fatG.toString()
        )
    }

    fun quickLogMealTemplate(template: KantoMealTemplate) {
        val log = CalorieLog(
            logId = UUID.randomUUID().toString(),
            dateIso = todayIso,
            mealCategory = template.category,
            foodDescription = template.name,
            caloriesKcal = template.caloriesKcal,
            proteinG = template.proteinG,
            carbsG = template.carbsG,
            fatG = template.fatG,
            sourceDevice = DeviceSource.MOBILE,
            timestampMillis = System.currentTimeMillis(),
            isSynced = false,
        )

        viewModelScope.launch {
            calorieRepository.upsertCalorieLog(log)
            evaluateAchievementsUseCase.evaluate(todayIso)
            runCatching {
                syncManager.coalesceAndSyncCalorie(log)
            }
        }
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
            runCatching {
                syncManager.coalesceAndSyncCalorie(log)
            }
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
            runCatching {
                syncManager.coalesceAndSyncCalorie(log)
            }
        }
    }

    fun logSavedFood(food: SavedFood, category: MealCategory = MealCategory.LUNCH) {
        val log = CalorieLog(
            logId = UUID.randomUUID().toString(),
            dateIso = todayIso,
            mealCategory = category,
            foodDescription = if (!food.brand.isNullOrBlank()) "${food.brand} - ${food.name}" else food.name,
            caloriesKcal = food.caloriesPerServing,
            proteinG = food.proteinG,
            carbsG = food.carbsG,
            fatG = food.fatG,
            sourceDevice = DeviceSource.MOBILE,
            timestampMillis = System.currentTimeMillis(),
            isSynced = false,
        )
        viewModelScope.launch {
            calorieRepository.upsertCalorieLog(log)
            savedFoodRepository.recordFoodUsage(food.foodId)
            evaluateAchievementsUseCase.evaluate(todayIso)
            runCatching {
                syncManager.coalesceAndSyncCalorie(log)
            }
        }
    }

    fun onBarcodeScanned(
        barcode: String,
        onFound: (SavedFood) -> Unit,
        onNotFound: (String) -> Unit
    ) {
        viewModelScope.launch {
            val food = savedFoodRepository.getFoodById(barcode)
            if (food != null) {
                onFound(food)
            } else {
                onNotFound(barcode)
            }
        }
    }

    fun saveAndLogMeal(
        name: String,
        brand: String? = null,
        kcal: Int,
        category: MealCategory,
        protein: Int? = null,
        carbs: Int? = null,
        fat: Int? = null,
        saveAsFavorite: Boolean = false,
        barcode: String? = null
    ) {
        val foodId = barcode ?: ("food_" + UUID.randomUUID().toString().take(8))
        val p = protein ?: 0
        val c = carbs ?: 0
        val f = fat ?: 0

        val log = CalorieLog(
            logId = UUID.randomUUID().toString(),
            dateIso = todayIso,
            mealCategory = category,
            foodDescription = if (!brand.isNullOrBlank()) "$brand - $name" else name,
            caloriesKcal = kcal,
            proteinG = p,
            carbsG = c,
            fatG = f,
            sourceDevice = DeviceSource.MOBILE,
            timestampMillis = System.currentTimeMillis(),
            isSynced = false,
        )

        viewModelScope.launch {
            calorieRepository.upsertCalorieLog(log)
            evaluateAchievementsUseCase.evaluate(todayIso)
            runCatching {
                syncManager.coalesceAndSyncCalorie(log)
            }

            savedFoodRepository.saveFood(
                SavedFood(
                    foodId = foodId,
                    name = name,
                    brand = brand,
                    caloriesPerServing = kcal,
                    proteinG = p,
                    carbsG = c,
                    fatG = f,
                    isFavorite = saveAsFavorite,
                    useCount = 1,
                    lastUsedMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun toggleFavoriteFood(foodId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            savedFoodRepository.toggleFavorite(foodId, isFavorite)
        }
    }

    fun deleteSavedFood(foodId: String) {
        viewModelScope.launch {
            savedFoodRepository.deleteSavedFood(foodId)
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
    val weightUnit: WeightUnit = WeightUnit.KG,
    val heightUnit: HeightUnit = HeightUnit.CM,
    val weightLbsText: String = "165.3",
    val heightFeetText: String = "5",
    val heightInchesText: String = "8.9",
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
