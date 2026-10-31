package com.poketch.wear.reminders

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.poketch.core.domain.model.MealCategory
import com.poketch.core.domain.repository.CalorieRepository
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
class MealCheckInReceiver : BroadcastReceiver() {

    @Inject
    lateinit var calorieRepository: CalorieRepository

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val window = intent.getStringExtra(ReminderScheduler.EXTRA_MEAL_WINDOW) ?: ReminderScheduler.WINDOW_BREAKFAST
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val logs = calorieRepository.observeLogsByDate(todayIso).firstOrNull().orEmpty()

                val hasLoggedWindow = when (window) {
                    ReminderScheduler.WINDOW_BREAKFAST -> logs.any { it.mealCategory == MealCategory.BREAKFAST }
                    ReminderScheduler.WINDOW_LUNCH -> logs.any { it.mealCategory == MealCategory.LUNCH }
                    ReminderScheduler.WINDOW_DINNER -> logs.any { it.mealCategory == MealCategory.DINNER }
                    else -> logs.isNotEmpty()
                }

                if (!hasLoggedWindow) {
                    sendMealReminderNotification(context, window)
                }

                // Reschedule for tomorrow's corresponding window
                reminderScheduler.scheduleMealCheckIn(window)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun sendMealReminderNotification(context: Context, window: String) {
        val launchIntent = Intent(context, WearMainActivity::class.java).apply {
            putExtra("destination_route", "rotary_calorie_logger")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            3002 + window.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val windowTitle = window.lowercase().replaceFirstChar { it.uppercase() }

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_MEALS)
            .setSmallIcon(R.drawable.splash_icon)
            .setContentTitle("🍽️ $windowTitle Check-In")
            .setContentText("Dial in your meal to stay within your daily calorie ceiling.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.splash_icon,
                "Log kcal",
                pendingIntent
            )
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(102 + window.hashCode(), notification)
    }
}
