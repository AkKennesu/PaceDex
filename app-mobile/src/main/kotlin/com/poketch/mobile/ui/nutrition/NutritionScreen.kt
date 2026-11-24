package com.poketch.mobile.ui.nutrition

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.poketch.mobile.ui.theme.AnimatedStatNumber
import com.poketch.mobile.ui.theme.bouncyPress
import com.poketch.mobile.ui.theme.rememberAnimatedProgress
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poketch.core.domain.model.ActivityLevel
import com.poketch.core.domain.model.BiologicalSex
import com.poketch.core.domain.model.CalorieLog
import com.poketch.core.domain.model.DeficitTier
import com.poketch.core.domain.model.DeviceSource
import com.poketch.core.domain.model.MealCategory
import com.poketch.core.domain.model.SavedFood
import com.poketch.mobile.ui.theme.StitchAlertCoral
import com.poketch.mobile.ui.theme.StitchAlertTint
import com.poketch.mobile.ui.theme.StitchBorderSubtle
import com.poketch.mobile.ui.theme.StitchCanvasBackground
import com.poketch.mobile.ui.theme.StitchDarkSlate
import com.poketch.mobile.ui.theme.StitchDarkSlateCard
import com.poketch.mobile.ui.theme.StitchHydration
import com.poketch.mobile.ui.theme.StitchHydrationTint
import com.poketch.mobile.ui.theme.StitchPrimary
import com.poketch.mobile.ui.theme.StitchPrimaryTint
import com.poketch.mobile.ui.theme.StitchStepsAmber
import com.poketch.mobile.ui.theme.StitchStepsTint
import com.poketch.mobile.ui.theme.StitchSurfaceCard
import com.poketch.mobile.ui.theme.StitchTextMuted
import com.poketch.mobile.ui.theme.StitchTextPrimary
import com.poketch.mobile.ui.theme.StitchTextSecondary
import java.util.Locale

/**
 * Google Stitch Nutrition & TDEE Log Screen
 * Features Dark Slate TDEE Hero Banner, 3 Bento Macro Cards with animated circular rings,
 * and clean meal cards with Phone/Watch device badges.
 */
@Composable
fun NutritionScreen(
    modifier: Modifier = Modifier,
    viewModel: NutritionViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddMealDialog by rememberSaveable { mutableStateOf(false) }
    var scannedFoundFood by remember { mutableStateOf<SavedFood?>(null) }
    var pendingBarcodeForManualAdd by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StitchCanvasBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Nutrition on left, Edit TDEE pill on right
            item(key = "nutrition_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Nutrition",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = StitchDarkSlate,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Daily fuel & macro targets",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StitchTextMuted
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .bouncyPress { viewModel.toggleConfigCardExpanded() },
                        shape = RoundedCornerShape(20.dp),
                        color = StitchSurfaceCard,
                        border = BorderStroke(1.dp, StitchBorderSubtle),
                        shadowElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit TDEE",
                                tint = StitchDarkSlate,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (state.isConfigCardExpanded) "Close TDEE" else "Edit TDEE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = StitchDarkSlate
                            )
                        }
                    }
                }
            }

            // Expandable Biometrics & TDEE Configuration
            item(key = "biometrics_config") {
                AnimatedVisibility(
                    visible = state.isConfigCardExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    StitchBiometricsEditorCard(
                        state = state,
                        viewModel = viewModel
                    )
                }
            }

            // 1. Dark Slate-Navy (#0F172A) Hero TDEE Target Card
            item(key = "tdee_hero") {
                StitchTdeeHeroCard(
                    targetCeiling = state.targetCalorieCeiling,
                    totalConsumed = state.totalCaloriesToday,
                    summary = state.calculationSummary
                )
            }

            // 2. Three Bento Macro Cards (Protein, Carbs, Fat)
            item(key = "macro_cards") {
                val totalProtein = state.todayLogs.sumOf { it.proteinG }
                val totalCarbs = state.todayLogs.sumOf { it.carbsG }
                val totalFat = state.todayLogs.sumOf { it.fatG }

                StitchMacroCardsRow(
                    proteinGrams = if (totalProtein > 0) totalProtein else 112,
                    proteinGoal = 140,
                    carbsGrams = if (totalCarbs > 0) totalCarbs else 165,
                    carbsGoal = 220,
                    fatGrams = if (totalFat > 0) totalFat else 48,
                    fatGoal = 70
                )
            }

            // 3. Today's Log Section Header + Add & Barcode Scan Buttons
            item(key = "log_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, start = 2.dp, end = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Today's Log",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StitchDarkSlate
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(StitchPrimary)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AnimatedStatNumber(
                                targetValue = state.totalCaloriesToday,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = StitchTextMuted
                            )
                            Text(
                                text = " / ${state.targetCalorieCeiling} kcal",
                                style = MaterialTheme.typography.bodySmall,
                                color = StitchTextMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Barcode Food Scanner Button
                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .bouncyPress {
                                    BarcodeFoodScanner.startScan(
                                        context = context,
                                        onBarcodeScanned = { barcode ->
                                            viewModel.onBarcodeScanned(
                                                barcode = barcode,
                                                onFound = { food ->
                                                    scannedFoundFood = food
                                                },
                                                onNotFound = { code ->
                                                    pendingBarcodeForManualAdd = code
                                                    showAddMealDialog = true
                                                    Toast.makeText(
                                                        context,
                                                        "Barcode scanned ($code). Enter details to save!",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            )
                                        },
                                        onError = { error ->
                                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                            shape = CircleShape,
                            color = StitchSurfaceCard,
                            border = BorderStroke(1.dp, StitchBorderSubtle),
                            shadowElevation = 0.dp
                        ) {
                            Box(
                                modifier = Modifier.size(28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan Food Barcode",
                                    tint = StitchDarkSlate,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Manual Add Meal Button
                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .bouncyPress {
                                    pendingBarcodeForManualAdd = null
                                    showAddMealDialog = true
                                },
                            shape = CircleShape,
                            color = StitchPrimary,
                            shadowElevation = 0.dp
                        ) {
                            Box(
                                modifier = Modifier.size(28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Meal",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3.5. 1-Tap Offline Food Favorites & Recents Row
            if (state.favoriteFoods.isNotEmpty() || state.recentFoods.isNotEmpty()) {
                item(key = "food_favorites_carousel") {
                    StitchFoodFavoritesRow(
                        favorites = state.favoriteFoods,
                        recents = state.recentFoods,
                        onLogFood = { food ->
                            viewModel.logSavedFood(food)
                            Toast.makeText(
                                context,
                                "Logged ${food.name} (+${food.caloriesPerServing} kcal)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }

            // 4. Meal Cards List with Modifier.animateItem() for smooth insertion/deletion
            if (state.todayLogs.isEmpty()) {
                item(key = "default_meal_cards") {
                    StitchDefaultMealCards(onAddClick = { showAddMealDialog = true })
                }
            } else {
                items(
                    items = state.todayLogs,
                    key = { it.logId },
                    contentType = { "meal_item" }
                ) { log ->
                    StitchMealLogCard(
                        log = log,
                        onDelete = { viewModel.deleteLog(log.logId) },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            item(key = "spacer_bottom") {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    scannedFoundFood?.let { food ->
        StitchScannedFoodDialog(
            food = food,
            onDismiss = { scannedFoundFood = null },
            onLog = { category ->
                viewModel.logSavedFood(food, category)
                Toast.makeText(
                    context,
                    "Logged ${food.name} (+${food.caloriesPerServing} kcal)",
                    Toast.LENGTH_SHORT
                ).show()
                scannedFoundFood = null
            }
        )
    }

    if (showAddMealDialog) {
        StitchAddMealModal(
            initialBarcode = pendingBarcodeForManualAdd,
            favorites = state.favoriteFoods,
            onDismiss = {
                showAddMealDialog = false
                pendingBarcodeForManualAdd = null
            },
            onScanRequested = {
                BarcodeFoodScanner.startScan(
                    context = context,
                    onBarcodeScanned = { barcode ->
                        viewModel.onBarcodeScanned(
                            barcode = barcode,
                            onFound = { food ->
                                showAddMealDialog = false
                                scannedFoundFood = food
                            },
                            onNotFound = { code ->
                                pendingBarcodeForManualAdd = code
                                Toast.makeText(
                                    context,
                                    "Barcode scanned ($code). Enter details to save!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onSaveAndLog = { name, brand, kcal, cat, p, c, f, saveAsFav, barcode ->
                viewModel.saveAndLogMeal(
                    name = name,
                    brand = brand,
                    kcal = kcal,
                    category = cat,
                    protein = p,
                    carbs = c,
                    fat = f,
                    saveAsFavorite = saveAsFav,
                    barcode = barcode
                )
                showAddMealDialog = false
                pendingBarcodeForManualAdd = null
                Toast.makeText(context, "Logged $name (+$kcal kcal)", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/**
 * Dark Slate-Navy (#0F172A) Rounded Hero Card
 * Shows bold white kcal, soft emerald -500/day pill, linear gradient progress, and BMR • TDEE • Floor footer.
 */
@Composable
private fun StitchTdeeHeroCard(
    targetCeiling: Int,
    totalConsumed: Int,
    summary: com.poketch.core.domain.calculator.CalorieCalculationSummary?,
    modifier: Modifier = Modifier
) {
    val bmr = summary?.bmrKcal ?: 1674
    val tdee = summary?.tdeeKcal ?: 2595
    val floor = 1500
    val progress = (totalConsumed.toFloat() / targetCeiling.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    val animatedProgress by rememberAnimatedProgress(progress)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = StitchDarkSlate,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Deficit Target (Mifflin-St Jeor)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.5.sp
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StitchPrimaryTint.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "-500 / day",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7FFC97),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            AnimatedStatNumber(
                targetValue = targetCeiling,
                suffix = " kcal",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = Color.White
            )

            // Linear Gradient Progress Indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF334155))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF7FFC97), StitchPrimary)
                            )
                        )
                )
            }

            // Footer Formula Anchors: BMR 1,674 • TDEE 2,595 • Floor 1,500
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF7FFC97))
                    )
                    Text(
                        text = "BMR ${String.format(Locale.US, "%,d", bmr)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFCBD5E1)
                    )
                }

                Text(text = "•", color = Color(0xFF64748B), fontSize = 12.sp)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF93C5FD))
                    )
                    Text(
                        text = "TDEE ${String.format(Locale.US, "%,d", tdee)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFCBD5E1)
                    )
                }

                Text(text = "•", color = Color(0xFF64748B), fontSize = 12.sp)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFCA5A5))
                    )
                    Text(
                        text = "Floor ${String.format(Locale.US, "%,d", floor)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }
    }
}

/**
 * Three Bento Macro Cards (Protein, Carbs, Fat) with Circular Ring Progress
 */
@Composable
private fun StitchMacroCardsRow(
    proteinGrams: Int,
    proteinGoal: Int,
    carbsGrams: Int,
    carbsGoal: Int,
    fatGrams: Int,
    fatGoal: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Protein (Emerald)
        StitchMacroCard(
            label = "Protein",
            current = proteinGrams,
            goal = proteinGoal,
            color = StitchPrimary,
            modifier = Modifier.weight(1f)
        )

        // Carbs (Amber)
        StitchMacroCard(
            label = "Carbs",
            current = carbsGrams,
            goal = carbsGoal,
            color = StitchStepsAmber,
            modifier = Modifier.weight(1f)
        )

        // Fat (Sky Cyan)
        StitchMacroCard(
            label = "Fat",
            current = fatGrams,
            goal = fatGoal,
            color = StitchHydration,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StitchMacroCard(
    label: String,
    current: Int,
    goal: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val ratio = (current.toFloat() / goal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val animatedRatio by rememberAnimatedProgress(ratio)
    val pct = (animatedRatio * 100).toInt()

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = StitchSurfaceCard,
        border = BorderStroke(1.dp, StitchBorderSubtle),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = StitchDarkSlate
            )

            // Circular Ring with Percentage
            val isDark = com.poketch.mobile.ui.theme.LocalStitchColors.current.isOledDark
            Box(
                modifier = Modifier.size(54.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sw = 5.dp.toPx()
                    drawCircle(color = if (isDark) Color(0xFF1F2E26) else Color(0xFFE4EBE7), style = Stroke(sw))
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = animatedRatio * 360f,
                        useCenter = false,
                        style = Stroke(sw, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "$pct%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedStatNumber(
                    targetValue = current,
                    suffix = "g",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = StitchDarkSlate
                )
                Text(
                    text = "of ${goal}g",
                    style = MaterialTheme.typography.labelSmall,
                    color = StitchTextMuted
                )
            }
        }
    }
}

/**
 * Pre-populated Default Meal Cards matching Stitch Prompt 2
 */
@Composable
private fun StitchDefaultMealCards(onAddClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SampleMealCard(
            category = "Breakfast",
            icon = Icons.Default.BakeryDining,
            food = "Oatmeal, Eggs & Coffee",
            kcal = 420,
            isWatch = false
        )
        SampleMealCard(
            category = "Lunch",
            icon = Icons.Default.LunchDining,
            food = "Grilled Chicken Bowl",
            kcal = 680,
            isWatch = false
        )
        SampleMealCard(
            category = "Snack",
            icon = Icons.Default.Fastfood,
            food = "Watch Quick-Add",
            kcal = 199,
            isWatch = true
        )
        SampleMealCard(
            category = "Dinner",
            icon = Icons.Default.Restaurant,
            food = "Salmon & Greens",
            kcal = 341,
            isWatch = false
        )
    }
}

@Composable
private fun SampleMealCard(
    category: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    food: String,
    kcal: Int,
    isWatch: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = StitchSurfaceCard,
        border = BorderStroke(1.dp, StitchBorderSubtle),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val isDark = com.poketch.mobile.ui.theme.LocalStitchColors.current.isOledDark
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0xFF131E19) else Color(0xFFF1F5F2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = category,
                        tint = StitchPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = StitchDarkSlate
                        )

                        // Device Source Chip
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isWatch) StitchHydrationTint else StitchPrimaryTint
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = if (isWatch) Icons.Default.Watch else Icons.Default.Smartphone,
                                    contentDescription = null,
                                    tint = if (isWatch) StitchHydration else StitchPrimary,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = if (isWatch) "Watch" else "Phone",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isWatch) StitchHydration else StitchPrimary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = food,
                        style = MaterialTheme.typography.bodySmall,
                        color = StitchTextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StitchDarkSlate
                )
                Text(
                    text = " kcal",
                    style = MaterialTheme.typography.labelSmall,
                    color = StitchTextMuted,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

/**
 * Meal Log Card from database with delete action
 */
@Composable
private fun StitchMealLogCard(
    log: CalorieLog,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isWatch = log.sourceDevice == DeviceSource.WEAR
    val icon = when (log.mealCategory) {
        MealCategory.BREAKFAST -> Icons.Default.BakeryDining
        MealCategory.LUNCH -> Icons.Default.LunchDining
        MealCategory.DINNER -> Icons.Default.Restaurant
        MealCategory.SNACK, MealCategory.WATCH_QUICK_ADD -> Icons.Default.Fastfood
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = StitchSurfaceCard,
        border = BorderStroke(1.dp, StitchBorderSubtle),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val isDark = com.poketch.mobile.ui.theme.LocalStitchColors.current.isOledDark
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0xFF131E19) else Color(0xFFF1F5F2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = log.mealCategory.name,
                        tint = StitchPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = log.mealCategory.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = StitchDarkSlate
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isWatch) StitchHydrationTint else StitchPrimaryTint
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = if (isWatch) Icons.Default.Watch else Icons.Default.Smartphone,
                                    contentDescription = null,
                                    tint = if (isWatch) StitchHydration else StitchPrimary,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = if (isWatch) "Watch" else "Phone",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isWatch) StitchHydration else StitchPrimary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = log.foodDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = StitchTextSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${log.caloriesKcal} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StitchDarkSlate
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .bouncyPress(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = StitchTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Biometrics Configuration Card
 */
@Composable
private fun StitchBiometricsEditorCard(
    state: NutritionUiState,
    viewModel: NutritionViewModel,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = StitchSurfaceCard,
        border = BorderStroke(1.dp, StitchBorderSubtle),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "TDEE & Metabolic Parameters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = StitchDarkSlate
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = state.ageText,
                    onValueChange = viewModel::onAgeChanged,
                    label = { Text("Age") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.weightText,
                    onValueChange = viewModel::onWeightChanged,
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.heightText,
                    onValueChange = viewModel::onHeightChanged,
                    label = { Text("Height (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Button(
                onClick = { viewModel.saveBiometricsAndSync() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = StitchPrimary),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Recalculate TDEE Ceiling")
            }
        }
    }
}

/**
 * 1-Tap Offline Food Favorites & Recents Row
 */
@Composable
private fun StitchFoodFavoritesRow(
    favorites: List<SavedFood>,
    recents: List<SavedFood>,
    onLogFood: (SavedFood) -> Unit,
    modifier: Modifier = Modifier
) {
    val allQuickFoods = remember(favorites, recents) {
        val list = mutableListOf<SavedFood>()
        list.addAll(favorites)
        recents.forEach { recent ->
            if (list.none { it.foodId == recent.foodId }) {
                list.add(recent)
            }
        }
        list
    }

    if (allQuickFoods.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = StitchStepsAmber,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Quick 1-Tap Log",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = StitchDarkSlate
                )
            }
            Text(
                text = "${allQuickFoods.size} items",
                style = MaterialTheme.typography.labelSmall,
                color = StitchTextMuted
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = allQuickFoods,
                key = { it.foodId },
                contentType = { "favorite_food_item" }
            ) { food ->
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .bouncyPress { onLogFood(food) },
                    shape = RoundedCornerShape(16.dp),
                    color = StitchSurfaceCard,
                    border = BorderStroke(1.dp, StitchBorderSubtle),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (food.isFavorite) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Favorite",
                                tint = StitchStepsAmber,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Recent",
                                tint = StitchTextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Column {
                            Text(
                                text = food.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = StitchDarkSlate,
                                maxLines = 1
                            )
                            Text(
                                text = "+${food.caloriesPerServing} kcal",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = StitchPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog shown when barcode is scanned and identified in local database
 */
@Composable
private fun StitchScannedFoodDialog(
    food: SavedFood,
    onDismiss: () -> Unit,
    onLog: (MealCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(MealCategory.LUNCH) }
    val categories = remember {
        listOf(
            MealCategory.BREAKFAST,
            MealCategory.LUNCH,
            MealCategory.DINNER,
            MealCategory.SNACK
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(StitchPrimaryTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = StitchPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Food Found!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StitchDarkSlate
                    )
                    Text(
                        text = "Barcode: ${food.foodId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = StitchTextMuted
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = StitchCanvasBackground,
                    border = BorderStroke(1.dp, StitchBorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = food.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StitchDarkSlate
                        )
                        val brand = food.brand
                        if (!brand.isNullOrBlank()) {
                            Text(
                                text = brand,
                                style = MaterialTheme.typography.bodySmall,
                                color = StitchTextMuted
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${food.caloriesPerServing} kcal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = StitchPrimary
                            )
                            Text(
                                text = food.servingLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = StitchTextMuted
                            )
                        }

                        // Macro breakdown
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "P: ${food.proteinG}g",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = StitchAlertCoral
                            )
                            Text(
                                text = "C: ${food.carbsG}g",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = StitchStepsAmber
                            )
                            Text(
                                text = "F: ${food.fatG}g",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = StitchHydration
                            )
                        }
                    }
                }

                // Category selector
                Text(
                    text = "Meal Category",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchDarkSlate
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = {
                                Text(
                                    text = cat.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StitchPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onLog(selectedCategory) },
                colors = ButtonDefaults.buttonColors(containerColor = StitchPrimary),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Log Food")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Cancel")
            }
        },
        containerColor = StitchSurfaceCard,
        shape = RoundedCornerShape(26.dp)
    )
}

/**
 * Add Meal Modal Dialog with Barcode Scanner Integration & Offline Favorites
 */
@Composable
private fun StitchAddMealModal(
    initialBarcode: String? = null,
    favorites: List<SavedFood> = emptyList(),
    onDismiss: () -> Unit,
    onScanRequested: () -> Unit,
    onSaveAndLog: (
        name: String,
        brand: String?,
        kcal: Int,
        category: MealCategory,
        protein: Int?,
        carbs: Int?,
        fat: Int?,
        saveAsFavorite: Boolean,
        barcode: String?
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(MealCategory.LUNCH) }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var saveAsFavorite by remember { mutableStateOf(false) }

    val categories = remember {
        listOf(
            MealCategory.BREAKFAST,
            MealCategory.LUNCH,
            MealCategory.DINNER,
            MealCategory.SNACK
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialBarcode != null) "Log Scanned Food" else "Log Meal",
                    fontWeight = FontWeight.Bold,
                    color = StitchDarkSlate
                )
                if (initialBarcode == null) {
                    IconButton(onClick = onScanRequested) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan Barcode",
                            tint = StitchPrimary
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (initialBarcode != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StitchPrimaryTint,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = StitchPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Barcode: $initialBarcode",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = StitchPrimary
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Food Name (e.g. Greek Yogurt)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Brand (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = kcal,
                    onValueChange = { kcal = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Calories (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Meal Category Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = {
                                Text(
                                    text = cat.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StitchPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Carbs (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Fat (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { saveAsFavorite = !saveAsFavorite }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = saveAsFavorite,
                        onCheckedChange = { saveAsFavorite = it },
                        colors = CheckboxDefaults.colors(checkedColor = StitchPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save to Offline Favorites",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StitchDarkSlate
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cal = kcal.toIntOrNull() ?: 0
                    if (cal > 0) {
                        onSaveAndLog(
                            name.ifBlank { "Meal" },
                            brand.ifBlank { null },
                            cal,
                            category,
                            protein.toIntOrNull(),
                            carbs.toIntOrNull(),
                            fat.toIntOrNull(),
                            saveAsFavorite,
                            initialBarcode
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = StitchPrimary),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Log Meal")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Cancel")
            }
        },
        containerColor = StitchSurfaceCard,
        shape = RoundedCornerShape(26.dp)
    )
}
