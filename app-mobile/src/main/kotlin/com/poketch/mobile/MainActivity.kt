package com.poketch.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import com.poketch.core.data.gamification.UnlockEventDispatcher
import com.poketch.mobile.ui.dashboard.CommandCenterViewModel
import com.poketch.mobile.ui.navigation.PoketchMobileNavHost
import com.poketch.mobile.ui.nutrition.NutritionViewModel
import com.poketch.mobile.ui.pokedex.PokedexViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var unlockEventDispatcher: UnlockEventDispatcher

    private val commandCenterViewModel: CommandCenterViewModel by viewModels()
    private val nutritionViewModel: NutritionViewModel by viewModels()
    private val pokedexViewModel: PokedexViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                PoketchMobileNavHost(
                    unlockEventDispatcher = unlockEventDispatcher,
                    commandCenterViewModel = commandCenterViewModel,
                    nutritionViewModel = nutritionViewModel,
                    pokedexViewModel = pokedexViewModel,
                )
            }
        }
    }
}
