package com.poketch.mobile

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.poketch.core.sync.WeatherSyncWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PoketchMobileApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        runCatching { WeatherSyncWorker.schedulePeriodic(this) }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("pokemon_sprite_cache"))
                    .maxSizeBytes(32L * 1024 * 1024)
                    .build()
            }
            .allowHardware(true)
            .respectCacheHeaders(false)
            .build()
    }
}

