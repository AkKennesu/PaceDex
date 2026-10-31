package com.poketch.wear

import android.app.Application
import androidx.work.Configuration
import com.poketch.core.domain.repository.CalorieRepository
import com.poketch.core.domain.repository.HabitRepository
import com.poketch.core.domain.repository.UserRepository
import com.poketch.wear.reminders.ReminderScheduler
import com.poketch.wear.tile.PoketchWatchFaceUpdater
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class PoketchWearApp : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var calorieRepository: CalorieRepository

    @Inject
    lateinit var habitRepository: HabitRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            delay(1000)
            runCatching { reminderScheduler.scheduleAllReminders() }
            runCatching { observeDataChangesForWatchFaceUpdates() }
        }
    }

    private fun observeDataChangesForWatchFaceUpdates() {
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        appScope.launch {
            combine(
                userRepository.observeUserProfile(),
                calorieRepository.observeTotalCaloriesByDate(todayIso),
                habitRepository.observeDailyWaterMl(todayIso)
            ) { _, _, _ ->
                PoketchWatchFaceUpdater.requestAllUpdates(this@PoketchWearApp)
            }.collect()
        }
    }
}
