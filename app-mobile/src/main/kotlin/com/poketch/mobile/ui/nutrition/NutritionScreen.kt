package com.poketch.mobile.ui.nutrition

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poketch.core.domain.calculator.CalorieBudgetState
import com.poketch.core.domain.calculator.CalorieCalculationSummary
import com.poketch.core.domain.model.ActivityLevel
import com.poketch.core.domain.model.BiologicalSex
import com.poketch.core.domain.model.CalorieLog
import com.poketch.core.domain.model.DeficitTier
import com.poketch.core.domain.model.MealCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NutritionScreen(
    modifier: Modifier = Modifier,
    viewModel: NutritionViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Calorie & TDEE Logger",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // 1. Mifflin-St Jeor Biometrics Card
        MifflinStJeorConfigCard(
            state = state,
            onAgeChanged = viewModel::onAgeChanged,
            onSexChanged = viewModel::onSexChanged,
            onHeightChanged = viewModel::onHeightChanged,
            onWeightChanged = viewModel::onWeightChanged,
            onActivityLevelChanged = viewModel::onActivityLevelChanged,
            onDeficitTierChanged = viewModel::onDeficitTierChanged,
            onToggleExpand = viewModel::toggleConfigCardExpanded,
            onSaveAndSync = viewModel::saveBiometricsAndSync,
            onDismissSavedMessage = viewModel::clearSavedMessage
        )

        // 2. Meal Quick-Add Buttons
        QuickAddMealSection(
            onQuickAdd = viewModel::quickAddMeal
        )

        // 3. New Meal Entry Form
        MealEntryFormCard(
            state = state,
            onCategoryChanged = viewModel::onInputCategoryChanged,
            onFoodNameChanged = viewModel::onFoodNameChanged,
            onCaloriesChanged = viewModel::onCaloriesChanged,
            onProteinChanged = viewModel::onProteinChanged,
            onCarbsChanged = viewModel::onCarbsChanged,
            onFatChanged = viewModel::onFatChanged,
            onSubmit = viewModel::logMeal
        )

        // 4. Today's Meals Chronological List
        LoggedMealsSection(
            logs = state.todayLogs,
            totalCalories = state.totalCaloriesToday,
            budgetState = state.budgetState,
            selectedFilter = state.filterCategory,
            onFilterSelected = viewModel::onFilterCategorySelected,
            onDeleteLog = viewModel::deleteLog
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MifflinStJeorConfigCard(
    state: NutritionUiState,
    onAgeChanged: (String) -> Unit,
    onSexChanged: (BiologicalSex) -> Unit,
    onHeightChanged: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
    onActivityLevelChanged: (ActivityLevel) -> Unit,
    onDeficitTierChanged: (DeficitTier) -> Unit,
    onToggleExpand: () -> Unit,
    onSaveAndSync: () -> Unit,
    onDismissSavedMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "⚙️ Mifflin-St Jeor TDEE Calculator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (state.isConfigCardExpanded) "Tap to collapse settings" else "Tap to adjust height, weight & deficit goals",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (state.isConfigCardExpanded) "▲" else "▼",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Live preview summary banner always visible
            state.calculationSummary?.let { summary ->
                LiveCalculationBanner(
                    summary = summary,
                    sex = state.sex
                )
            }

            state.biometricsSavedMessage?.let { msg ->
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✓ $msg",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        IconButton(
                            onClick = onDismissSavedMessage,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = state.isConfigCardExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    HorizontalDivider()

                    // Biological Sex Selector
                    Text(
                        text = "Biological Sex",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BiologicalSex.entries.forEach { sex ->
                            val isSelected = state.sex == sex
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSexChanged(sex) },
                                label = {
                                    Text(
                                        text = if (sex == BiologicalSex.MALE) "Male (Floor 1,500)" else "Female (Floor 1,200)"
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }

                    // Age, Height, Weight
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = state.ageText,
                            onValueChange = onAgeChanged,
                            label = { Text("Age (yrs)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.heightText,
                            onValueChange = onHeightChanged,
                            label = { Text("Height (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.weightText,
                            onValueChange = onWeightChanged,
                            label = { Text("Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Activity Level
                    Text(
                        text = "Activity Level",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ActivityLevel.entries.forEach { level ->
                            val isSelected = state.activityLevel == level
                            FilterChip(
                                selected = isSelected,
                                onClick = { onActivityLevelChanged(level) },
                                label = { Text("${level.displayName} (×${level.multiplier})") }
                            )
                        }
                    }

                    // Deficit Tier
                    Text(
                        text = "Calorie Deficit Goal",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DeficitTier.entries.forEach { tier ->
                            val isSelected = state.deficitTier == tier
                            FilterChip(
                                selected = isSelected,
                                onClick = { onDeficitTierChanged(tier) },
                                label = {
                                    Text(
                                        text = "${tier.name.lowercase().replaceFirstChar { it.uppercase() }} (-${tier.dailyDeficitKcal})"
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Save & Sync Button
                    Button(
                        onClick = onSaveAndSync,
                        enabled = state.isBiometricsValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save & Sync to Watch",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveCalculationBanner(
    summary: CalorieCalculationSummary,
    sex: BiologicalSex,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CalculationMetric(label = "BMR", value = "${summary.bmrKcal} kcal")
            CalculationMetric(label = "TDEE", value = "${summary.tdeeKcal} kcal")
            CalculationMetric(
                label = "Target Ceiling",
                value = "${summary.targetCalorieCeiling} kcal",
                valueColor = Color(0xFF1B5E20),
                isEmphasized = true
            )
        }

        if (summary.isClampedToSafetyFloor) {
            Surface(
                color = Color(0xFFFFF3E0),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Safety Notice: Target clamped to ${sex.safeCalorieFloor} kcal floor to maintain metabolic health.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFBF360C)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalculationMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified,
    isEmphasized: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (isEmphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun QuickAddMealSection(
    onQuickAdd: (MealCategory, Int, String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "⚡ Quick-Add Meals",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { onQuickAdd(MealCategory.SNACK, 150, "Quick Snack (Apple / Shake)", 5) },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "+150 kcal\nSnack",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedButton(
                onClick = { onQuickAdd(MealCategory.BREAKFAST, 400, "Quick Breakfast (Eggs & Toast)", 20) },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "+400 kcal\nBreakfast",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedButton(
                onClick = { onQuickAdd(MealCategory.DINNER, 600, "Quick Dinner (Balanced Bowl)", 35) },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "+600 kcal\nDinner",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MealEntryFormCard(
    state: NutritionUiState,
    onCategoryChanged: (MealCategory) -> Unit,
    onFoodNameChanged: (String) -> Unit,
    onCaloriesChanged: (String) -> Unit,
    onProteinChanged: (String) -> Unit,
    onCarbsChanged: (String) -> Unit,
    onFatChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "📝 Log Meal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Category Selection Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    MealCategory.BREAKFAST to "Breakfast",
                    MealCategory.LUNCH to "Lunch",
                    MealCategory.DINNER to "Dinner",
                    MealCategory.SNACK to "Snack"
                ).forEach { (cat, name) ->
                    val isSelected = state.inputCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategoryChanged(cat) },
                        label = { Text(name) }
                    )
                }
            }

            // Food Name & Calories
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = state.foodNameText,
                    onValueChange = onFoodNameChanged,
                    label = { Text("Food Name") },
                    placeholder = { Text("e.g. Grilled Chicken") },
                    modifier = Modifier.weight(2f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.caloriesText,
                    onValueChange = onCaloriesChanged,
                    label = { Text("Calories") },
                    placeholder = { Text("kcal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.2f),
                    singleLine = true
                )
            }

            // Macros (Protein, Carbs, Fat)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = state.proteinText,
                    onValueChange = onProteinChanged,
                    label = { Text("Protein (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.carbsText,
                    onValueChange = onCarbsChanged,
                    label = { Text("Carbs (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.fatText,
                    onValueChange = onFatChanged,
                    label = { Text("Fat (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Button(
                onClick = onSubmit,
                enabled = state.caloriesText.toIntOrNull()?.let { it > 0 } == true,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Add Meal Entry",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LoggedMealsSection(
    logs: List<CalorieLog>,
    totalCalories: Int,
    budgetState: CalorieBudgetState,
    selectedFilter: MealCategory?,
    onFilterSelected: (MealCategory?) -> Unit,
    onDeleteLog: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today's Logged Meals",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Total: $totalCalories kcal",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = when (budgetState) {
                    is CalorieBudgetState.OverTargetWarning -> Color(0xFFB71C1C)
                    is CalorieBudgetState.ApproachingCeiling -> Color(0xFFF57F17)
                    is CalorieBudgetState.OnTrack -> Color(0xFF2E7D32)
                }
            )
        }

        // Filter chips: All, Breakfast, Lunch, Dinner, Snack
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { onFilterSelected(null) },
                label = { Text("All (${logs.size})") }
            )
            listOf(
                MealCategory.BREAKFAST to "Breakfast",
                MealCategory.LUNCH to "Lunch",
                MealCategory.DINNER to "Dinner",
                MealCategory.SNACK to "Snack"
            ).forEach { (cat, name) ->
                FilterChip(
                    selected = selectedFilter == cat,
                    onClick = { onFilterSelected(if (selectedFilter == cat) null else cat) },
                    label = { Text(name) }
                )
            }
        }

        if (logs.isEmpty()) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No meals logged for this category yet today.\nUse the form or quick-add above!",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                logs.forEach { log ->
                    MealLogItemCard(
                        log = log,
                        onDelete = { onDeleteLog(log.logId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MealLogItemCard(
    log: CalorieLog,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(log.timestampMillis))

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.foodDescription,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    // Sync badge
                    SyncStatusBadge(isSynced = log.isSynced)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${log.mealCategory.name.lowercase().replaceFirstChar { it.uppercase() }} • $timeFormatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Macros
                if ((log.proteinG > 0) || (log.carbsG > 0) || (log.fatG > 0)) {
                    Text(
                        text = "P: ${log.proteinG}g  •  C: ${log.carbsG}g  •  F: ${log.fatG}g",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${log.caloriesKcal} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete entry",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncStatusBadge(
    isSynced: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isSynced) {
        Surface(
            color = Color(0xFFE8F5E9),
            shape = CircleShape,
            modifier = modifier
        ) {
            Text(
                text = "✓ Synced to Watch",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF2E7D32),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    } else {
        Surface(
            color = Color(0xFFFFF3E0),
            shape = CircleShape,
            modifier = modifier
        ) {
            Text(
                text = "📱 Local",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFE65100),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
