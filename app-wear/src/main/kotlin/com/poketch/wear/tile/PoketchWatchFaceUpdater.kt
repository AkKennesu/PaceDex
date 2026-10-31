package com.poketch.wear.tile

import android.content.ComponentName
import android.content.Context
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester

object PoketchWatchFaceUpdater {
    fun requestAllUpdates(context: Context) {
        runCatching {
            TileService.getUpdater(context).requestUpdate(PoketchTileService::class.java)
        }
        runCatching {
            ComplicationDataSourceUpdateRequester.create(
                context,
                ComponentName(context, "com.poketch.wear.complication.PoketchCalorieComplicationService")
            ).requestUpdateAll()
        }
    }
}
