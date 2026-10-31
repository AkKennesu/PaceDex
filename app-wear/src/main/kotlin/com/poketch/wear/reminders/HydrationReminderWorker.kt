package com.poketch.wear.reminders

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.poketch.core.domain.repository.HabitRepository
import com.poketch.core.domain.repository.UserRepository
import com.poketch.wear.R
import com.poketch.wear.WearMainActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HydrationReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface HydrationWorkerEntryPoint {
        fun habitRepository(): HabitRepository
        fun userRepository(): UserRepository
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            HydrationWorkerEntryPoint::class.java
        )
        val habitRepository = entryPoint.habitRepository()
        val userRepository = entryPoint.userRepository()

        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val waterMl = habitRepository.observeDailyWaterMl(todayIso).firstOrNull() ?: 0.0
        val profile = userRepository.getUserProfile()
        val goal = profile?.dailyWaterGoalMl ?: 2500

        // If user hasn't met hydration target for the day yet
        if (waterMl < goal) {
            sendHydrationNotification(applicationContext, waterMl.toInt(), goal)
        }

        return Result.success()
    }

    private fun sendHydrationNotification(context: Context, currentMl: Int, goalMl: Int) {
        val launchIntent = Intent(context, WearMainActivity::class.java).apply {
            putExtra("destination_route", "quick_habit_actions")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            3003,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_HYDRATION)
            .setSmallIcon(R.drawable.splash_icon)
            .setContentTitle("💧 Hydration Check-In")
            .setContentText("Currently at $currentMl / $goalMl ml. Tap +250ml to keep hydrated!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.splash_icon,
                "+250ml Water",
                pendingIntent
            )
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(103, notification)
    }
}
