package com.poketch.wear.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_MOVEMENT = "poketch_movement"
        const val CHANNEL_MEALS = "poketch_meals"
        const val CHANNEL_HYDRATION = "poketch_hydration"

        const val REQ_CODE_MOVEMENT = 2001
        const val REQ_CODE_BREAKFAST = 2002
        const val REQ_CODE_LUNCH = 2003
        const val REQ_CODE_DINNER = 2004

        const val EXTRA_MEAL_WINDOW = "extra_meal_window"
        const val WINDOW_BREAKFAST = "BREAKFAST"
        const val WINDOW_LUNCH = "LUNCH"
        const val WINDOW_DINNER = "DINNER"

        const val WORK_HYDRATION_PERIODIC = "poketch_hydration_periodic_work"
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val movementChannel = NotificationChannel(
                CHANNEL_MOVEMENT,
                "Movement & Run Nudges",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily 17:00 nudge to complete your 1-mile run and hit step targets."
                enableVibration(true)
            }

            val mealChannel = NotificationChannel(
                CHANNEL_MEALS,
                "Meal Check-Ins",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for Breakfast, Lunch, and Dinner calorie logging."
            }

            val hydrationChannel = NotificationChannel(
                CHANNEL_HYDRATION,
                "Hydration Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Periodic reminders to keep your hydration goal on track."
            }

            notificationManager.createNotificationChannels(
                listOf(movementChannel, mealChannel, hydrationChannel)
            )
        }
    }

    fun scheduleMovementNudge() {
        val triggerMillis = getNextTriggerMillis(hourOfDay = 17, minute = 0)
        val intent = Intent(context, MovementNudgeReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_CODE_MOVEMENT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactOrIdleAlarm(triggerMillis, pendingIntent)
    }

    fun scheduleMealCheckIns() {
        scheduleMealCheckIn(WINDOW_BREAKFAST, hourOfDay = 9, minute = 0, reqCode = REQ_CODE_BREAKFAST)
        scheduleMealCheckIn(WINDOW_LUNCH, hourOfDay = 13, minute = 0, reqCode = REQ_CODE_LUNCH)
        scheduleMealCheckIn(WINDOW_DINNER, hourOfDay = 19, minute = 30, reqCode = REQ_CODE_DINNER)
    }

    fun scheduleMealCheckIn(window: String) {
        when (window) {
            WINDOW_BREAKFAST -> scheduleMealCheckIn(WINDOW_BREAKFAST, hourOfDay = 9, minute = 0, reqCode = REQ_CODE_BREAKFAST)
            WINDOW_LUNCH -> scheduleMealCheckIn(WINDOW_LUNCH, hourOfDay = 13, minute = 0, reqCode = REQ_CODE_LUNCH)
            WINDOW_DINNER -> scheduleMealCheckIn(WINDOW_DINNER, hourOfDay = 19, minute = 30, reqCode = REQ_CODE_DINNER)
        }
    }

    private fun scheduleMealCheckIn(window: String, hourOfDay: Int, minute: Int, reqCode: Int) {
        val triggerMillis = getNextTriggerMillis(hourOfDay = hourOfDay, minute = minute)
        val intent = Intent(context, MealCheckInReceiver::class.java).apply {
            putExtra(EXTRA_MEAL_WINDOW, window)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactOrIdleAlarm(triggerMillis, pendingIntent)
    }

    fun scheduleHydrationWorker() {
        val workRequest = PeriodicWorkRequestBuilder<HydrationReminderWorker>(
            2, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_HYDRATION_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun scheduleAllReminders() {
        createNotificationChannels()
        scheduleMovementNudge()
        scheduleMealCheckIns()
        scheduleHydrationWorker()
    }

    private fun setExactOrIdleAlarm(triggerMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    private fun getNextTriggerMillis(hourOfDay: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }
}
