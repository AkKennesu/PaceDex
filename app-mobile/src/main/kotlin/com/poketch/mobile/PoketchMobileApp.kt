package com.poketch.mobile

import android.app.Application
import com.poketch.core.sync.WeatherSyncWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PoketchMobileApp : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching { WeatherSyncWorker.schedulePeriodic(this) }
    }
}
