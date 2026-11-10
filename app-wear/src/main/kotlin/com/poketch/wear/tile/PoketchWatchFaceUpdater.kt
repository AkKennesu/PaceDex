package com.poketch.wear.tile

import android.content.Context
import androidx.wear.tiles.TileService
import com.poketch.wear.complication.PoketchBuddyPokemonComplicationService
import com.poketch.wear.complication.PoketchCalorieComplicationService
import com.poketch.wear.complication.PoketchWaterComplicationService

object PoketchWatchFaceUpdater {
    fun requestAllUpdates(context: Context) {
        runCatching {
            TileService.getUpdater(context).requestUpdate(PoketchTileService::class.java)
        }
        runCatching {
            PoketchCalorieComplicationService.requestUpdate(context)
        }
        runCatching {
            PoketchWaterComplicationService.requestUpdate(context)
        }
        runCatching {
            PoketchBuddyPokemonComplicationService.requestUpdate(context)
        }
        runCatching {
            com.poketch.wear.complication.PoketchWeatherComplicationService.requestUpdate(context)
        }
    }
}
