package com.poketch.mobile.ui.nutrition

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import com.poketch.core.domain.model.DailyNutritionSummary
import com.poketch.mobile.ui.theme.PoketchBorder
import com.poketch.mobile.ui.theme.PoketchSurface
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeeklyDeficitChart(
    dailySummaries: List<DailyNutritionSummary>,
    targetCeilingKcal: Int,
    modifier: Modifier = Modifier
) {
    // Generate normalized 7 days (6 days ago -> today)
    val past7Days = rememberPast7Days(dailySummaries)

    val onTrackColor = Color(0xFF10B981)
    val approachingColor = Color(0xFFF59E0B)
    val overTargetColor = Color(0xFFEF4444)
    val ceilingLineColor = Color(0xFF64748B)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PoketchSurface),
        border = BorderStroke(1.dp, PoketchBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "7-Day Calorie & Deficit Trend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Target Ceiling: $targetCeilingKcal kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val avgCalories = if (past7Days.isNotEmpty()) {
                    val activeDays = past7Days.filter { it.totalCalories > 0 }
                    if (activeDays.isNotEmpty()) activeDays.sumOf { it.totalCalories } / activeDays.size else 0
                } else 0

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Avg: $avgCalories kcal/d",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Canvas Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(vertical = 8.dp)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    val topPadding = 28f
                    val bottomPadding = 32f
                    val chartHeight = canvasHeight - topPadding - bottomPadding

                    val maxKcal = maxOf(
                        targetCeilingKcal * 1.35f,
                        (past7Days.maxOfOrNull { it.totalCalories } ?: 2000).toFloat() * 1.15f
                    )

                    // Draw Target Ceiling Line
                    val ceilingY = topPadding + chartHeight * (1f - (targetCeilingKcal / maxKcal))
                    drawLine(
                        color = ceilingLineColor,
                        start = Offset(0f, ceilingY),
                        end = Offset(canvasWidth, ceilingY),
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )

                    // Native paint for labels
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    val valuePaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 26f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                        isAntiAlias = true
                    }

                    // Draw Ceiling Label text
                    val ceilingTextPaint = android.graphics.Paint().apply {
                        color = ceilingLineColor.toArgb()
                        textSize = 22f
                        textAlign = android.graphics.Paint.Align.RIGHT
                        isAntiAlias = true
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        "Ceiling $targetCeilingKcal",
                        canvasWidth - 8f,
                        ceilingY - 6f,
                        ceilingTextPaint
                    )

                    val slotWidth = canvasWidth / past7Days.size
                    val barWidth = slotWidth * 0.58f

                    past7Days.forEachIndexed { index, day ->
                        val centerX = slotWidth * index + slotWidth / 2f
                        val left = centerX - barWidth / 2f

                        val consumed = day.totalCalories
                        val barHeight = if (consumed > 0) {
                            (consumed / maxKcal) * chartHeight
                        } else {
                            4f // minimal baseline indicator
                        }
                        val top = topPadding + (chartHeight - barHeight)

                        // Color determination based on target percentage
                        val ratio = consumed.toFloat() / targetCeilingKcal.toFloat()
                        val barColor = when {
                            consumed == 0 -> Color(0xFF616161)
                            ratio <= 0.90f -> onTrackColor
                            ratio <= 1.0f -> approachingColor
                            else -> overTargetColor
                        }

                        // Draw Bar
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(8f, 8f)
                        )

                        // Draw Kcal text above bar
                        if (consumed > 0) {
                            val textY = top - 8f
                            drawContext.canvas.nativeCanvas.drawText(
                                "$consumed",
                                centerX,
                                maxOf(textY, 24f),
                                valuePaint
                            )
                        }

                        // Draw Day label below bar
                        val dayLabel = formatDayLabel(day.dateIso)
                        drawContext.canvas.nativeCanvas.drawText(
                            dayLabel,
                            centerX,
                            canvasHeight - 6f,
                            textPaint
                        )
                    }
                }
            }

            // Legend
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LegendItem(color = onTrackColor, label = "On Track (≤90%)")
                LegendItem(color = approachingColor, label = "Approaching (90-100%)")
                LegendItem(color = overTargetColor, label = "Over Target (>100%)")
                LegendItem(
                    color = ceilingLineColor,
                    label = "Ceiling Line",
                    isDashed = true
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // 7-Day Macro Analytics & Distribution
            WeeklyMacroAnalytics(past7Days = past7Days)
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    isDashed: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isDashed) {
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(2.dp)
                    .background(color)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeeklyMacroAnalytics(
    past7Days: List<DailyNutritionSummary>,
    modifier: Modifier = Modifier
) {
    val totalProtein = past7Days.sumOf { it.totalProtein }
    val totalCarbs = past7Days.sumOf { it.totalCarbs }
    val totalFat = past7Days.sumOf { it.totalFat }

    val activeDays = maxOf(1, past7Days.count { it.totalCalories > 0 })
    val avgProtein = totalProtein / activeDays
    val avgCarbs = totalCarbs / activeDays
    val avgFat = totalFat / activeDays

    val proteinKcal = avgProtein * 4
    val carbsKcal = avgCarbs * 4
    val fatKcal = avgFat * 9
    val totalMacroKcal = maxOf(1, proteinKcal + carbsKcal + fatKcal)

    val proteinPct = (proteinKcal * 100) / totalMacroKcal
    val carbsPct = (carbsKcal * 100) / totalMacroKcal
    val fatPct = (fatKcal * 100) / totalMacroKcal

    val proteinColor = Color(0xFF42A5F5) // Blue
    val carbsColor = Color(0xFFFFB74D)   // Amber
    val fatColor = Color(0xFFEF5350)     // Coral

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Macro Trends & Daily Average",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        // Segmented Macro Ratio Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
        ) {
            if (proteinPct > 0) {
                Box(
                    modifier = Modifier
                        .weight(proteinPct.coerceAtLeast(1).toFloat())
                        .height(10.dp)
                        .background(proteinColor)
                )
            }
            if (carbsPct > 0) {
                Box(
                    modifier = Modifier
                        .weight(carbsPct.coerceAtLeast(1).toFloat())
                        .height(10.dp)
                        .background(carbsColor)
                )
            }
            if (fatPct > 0) {
                Box(
                    modifier = Modifier
                        .weight(fatPct.coerceAtLeast(1).toFloat())
                        .height(10.dp)
                        .background(fatColor)
                )
            }
            if (proteinPct == 0 && carbsPct == 0 && fatPct == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .background(Color.Gray.copy(alpha = 0.3f))
                )
            }
        }

        // Macro Statistics Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MacroStatPill(
                label = "Protein",
                grams = avgProtein,
                percentage = proteinPct,
                dotColor = proteinColor,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            MacroStatPill(
                label = "Carbs",
                grams = avgCarbs,
                percentage = carbsPct,
                dotColor = carbsColor,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            MacroStatPill(
                label = "Fat",
                grams = avgFat,
                percentage = fatPct,
                dotColor = fatColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MacroStatPill(
    label: String,
    grams: Int,
    percentage: Int,
    dotColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${grams}g ($percentage%)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun rememberPast7Days(summaries: List<DailyNutritionSummary>): List<DailyNutritionSummary> {
    val map = summaries.associateBy { it.dateIso }
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val result = mutableListOf<DailyNutritionSummary>()
    // 6 days ago up to today (total 7 days)
    for (i in 6 downTo 0) {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -i)
        }
        val dateIso = sdf.format(cal.time)
        val existing = map[dateIso]
        if (existing != null) {
            result.add(existing)
        } else {
            result.add(
                DailyNutritionSummary(
                    dateIso = dateIso,
                    totalCalories = 0,
                    totalProtein = 0,
                    totalCarbs = 0,
                    totalFat = 0
                )
            )
        }
    }
    return result
}

private fun formatDayLabel(dateIso: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = sdf.parse(dateIso) ?: return dateIso.takeLast(2)
        val today = sdf.format(Date())
        if (dateIso == today) {
            "Today"
        } else {
            val dayFormat = SimpleDateFormat("EEE", Locale.US)
            dayFormat.format(date)
        }
    } catch (_: Exception) {
        dateIso.takeLast(2)
    }
}
