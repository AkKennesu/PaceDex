package com.poketch.wear.reminders

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.poketch.core.domain.repository.HabitRepository
import com.poketch.wear.R
import com.poketch.wear.WearMainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MovementNudgeReceiver : BroadcastReceiver() {

    @Inject
    lateinit var habitRepository: HabitRepository

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val steps = habitRepository.observeDailySteps(todayIso).firstOrNull() ?: 0.0
                val runMiles = habitRepository.observeDailyRunMiles(todayIso).firstOrNull() ?: 0.0

                // If steps are < 5,000 (<50% of 10k target) and no 1-mile run logged today
                if ((steps < 5000.0) && (runMiles < 1.0)) {
                    sendRunNudgeNotification(context)
                }

                // Reschedule for tomorrow 17:00
                reminderScheduler.scheduleMovementNudge()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun sendRunNudgeNotification(context: Context) {
        val launchIntent = Intent(context, WearMainActivity::class.java).apply {
            putExtra("destination_route", "run_tracker")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            3001,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_MOVEMENT)
            .setSmallIcon(R.drawable.splash_icon)
            .setContentTitle("🏃 Time for a 1-Mile Run!")
            .setContentText("Unlock #077 Ponyta and keep your buddy energized!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.splash_icon,
                "Start 1-Mile Run",
                pendingIntent
            )
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(101, notification)
    }
}
