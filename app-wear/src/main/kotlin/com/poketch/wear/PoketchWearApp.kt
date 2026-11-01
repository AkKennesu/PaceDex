package com.poketch.wear

import android.app.Application
import android.os.Build
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
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
class PoketchWearApp : Application(), Configuration.Provider, ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(64L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
    }

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
        runCatching { com.poketch.core.sync.WeatherSyncWorker.schedulePeriodic(this) }
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
