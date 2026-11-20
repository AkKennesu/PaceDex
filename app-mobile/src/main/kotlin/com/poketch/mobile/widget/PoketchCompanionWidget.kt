package com.poketch.mobile.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.color.ColorProvider
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.poketch.core.domain.model.CalorieLog
import com.poketch.core.domain.model.DeviceSource
import com.poketch.core.domain.model.HabitType
import com.poketch.core.domain.model.MealCategory
import com.poketch.core.domain.repository.CalorieRepository
import com.poketch.core.domain.repository.HabitRepository
import com.poketch.core.domain.repository.PokemonRepository
import com.poketch.core.domain.repository.UserRepository
import com.poketch.core.sync.PoketchSyncManager
import com.poketch.mobile.MainActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GlanceWidgetEntryPoint {
    fun habitRepository(): HabitRepository
    fun calorieRepository(): CalorieRepository
    fun userRepository(): UserRepository
    fun pokemonRepository(): PokemonRepository
    fun syncManager(): PoketchSyncManager
}

private fun glanceColor(color: Color): androidx.glance.unit.ColorProvider =
    androidx.glance.color.ColorProvider(color, color)

class PoketchCompanionWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            GlanceWidgetEntryPoint::class.java
        )
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val profile = entryPoint.userRepository().getUserProfile()
        val targetCeiling = profile?.targetCalorieCeiling ?: 2164
        val waterGoal = profile?.dailyWaterGoalMl ?: 2500
        val buddyId = profile?.activeBuddyPokemonId ?: 1
        val buddyName = entryPoint.pokemonRepository().observePokemonById(buddyId).firstOrNull()?.name ?: "Bulbasaur"

        val consumedCalories = entryPoint.calorieRepository().observeTotalCaloriesByDate(todayIso).firstOrNull() ?: 0
        val waterMl = entryPoint.habitRepository().observeDailyWaterMl(todayIso).firstOrNull() ?: 0.0

        val nodes = entryPoint.syncManager().refreshConnectedNodes()
        val isWatchConnected = nodes.isNotEmpty()
        val watchName = nodes.firstOrNull()?.displayName?.ifBlank { "Pokétch Watch" } ?: "Pokétch Watch"

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A))
                        .cornerRadius(16.dp)
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.Top
                    ) {
                        // 1. Header with App Title & Watch Connection Status
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .clickable(actionStartActivity<MainActivity>()),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚡ POKÉTCH",
                                style = TextStyle(
                                    color = glanceColor(Color(0xFFE11D48)),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Text(
                                text = if (isWatchConnected) "⌚ $watchName" else "⌚ Disconnected",
                                style = TextStyle(
                                    color = glanceColor(if (isWatchConnected) Color(0xFF22C55E) else Color(0xFF94A3B8)),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(8.dp))

                        // 2. Buddy & Daily Progress Summary
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .clickable(actionStartActivity<MainActivity>()),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = "Buddy: $buddyName",
                                    style = TextStyle(
                                        color = glanceColor(Color(0xFF38BDF8)),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = GlanceModifier.height(2.dp))
                                val remaining = targetCeiling - consumedCalories
                                val calStatus = if (remaining >= 0) "$remaining kcal left" else "${-remaining} kcal over"
                                Text(
                                    text = "$consumedCalories / $targetCeiling kcal ($calStatus)",
                                    style = TextStyle(
                                        color = glanceColor(Color.White),
                                        fontSize = 12.sp
                                    )
                                )
                                Spacer(modifier = GlanceModifier.height(2.dp))
                                val waterPct = ((waterMl / waterGoal.coerceAtLeast(1)) * 100).toInt()
                                Text(
                                    text = "💧 ${waterMl.toInt()} / $waterGoal ml ($waterPct%)",
                                    style = TextStyle(
                                        color = glanceColor(Color(0xFF60A5FA)),
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(10.dp))

                        // 3. Quick Action Buttons Row (Controls Watch & Mobile Instantly)
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .background(Color(0xFF2563EB))
                                    .cornerRadius(8.dp)
                                    .padding(vertical = 6.dp)
                                    .clickable(actionRunCallback<QuickAddWaterAction>()),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "💧 +250ml",
                                    style = TextStyle(
                                        color = glanceColor(Color.White),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Spacer(modifier = GlanceModifier.width(6.dp))

                            Box(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .background(Color(0xFFD97706))
                                    .cornerRadius(8.dp)
                                    .padding(vertical = 6.dp)
                                    .clickable(actionRunCallback<QuickAddCalorieAction>()),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🍎 +100kcal",
                                    style = TextStyle(
                                        color = glanceColor(Color.White),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Spacer(modifier = GlanceModifier.width(6.dp))

                            Box(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .background(Color(0xFF0D9488))
                                    .cornerRadius(8.dp)
                                    .padding(vertical = 6.dp)
                                    .clickable(actionRunCallback<SyncWatchAction>()),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🔄 Sync",
                                    style = TextStyle(
                                        color = glanceColor(Color.White),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class QuickAddWaterAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            GlanceWidgetEntryPoint::class.java
        )
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val habit = entryPoint.habitRepository().logHabit(
            dateIso = todayIso,
            habitType = HabitType.WATER_ML,
            numericValue = 250.0,
            durationSeconds = 0,
            sourceDevice = DeviceSource.MOBILE
        )
        runCatching {
            entryPoint.syncManager().coalesceAndSyncHabit(habit)
        }
        PoketchCompanionWidget().update(context, glanceId)
    }
}

class QuickAddCalorieAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            GlanceWidgetEntryPoint::class.java
        )
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val log = CalorieLog(
            logId = UUID.randomUUID().toString(),
            dateIso = todayIso,
            mealCategory = MealCategory.SNACK,
            foodDescription = "Quick Snack",
            caloriesKcal = 100,
            sourceDevice = DeviceSource.MOBILE,
            timestampMillis = System.currentTimeMillis()
        )
        entryPoint.calorieRepository().upsertCalorieLog(log)
        runCatching {
            entryPoint.syncManager().coalesceAndSyncCalorie(log)
        }
        PoketchCompanionWidget().update(context, glanceId)
    }
}

class SyncWatchAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            GlanceWidgetEntryPoint::class.java
        )
        runCatching {
            entryPoint.syncManager().flushOfflineQueue()
            entryPoint.syncManager().refreshConnectedNodes()
            entryPoint.syncManager().syncUserProfile()
        }
        PoketchCompanionWidget().update(context, glanceId)
    }
}

class PoketchCompanionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PoketchCompanionWidget()
}
