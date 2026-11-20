package com.poketch.mobile.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.poketch.core.data.gamification.UnlockEventDispatcher
import com.poketch.core.domain.engine.UnlockEvent
import com.poketch.core.domain.model.AuthState
import com.poketch.mobile.ui.auth.AuthScreen
import com.poketch.mobile.ui.auth.AuthViewModel
import com.poketch.mobile.ui.dashboard.CommandCenterScreen
import com.poketch.mobile.ui.dashboard.CommandCenterViewModel
import com.poketch.mobile.ui.diagnostics.SyncDiagnosticsScreen
import com.poketch.mobile.ui.diagnostics.SyncDiagnosticsViewModel
import com.poketch.mobile.ui.evolution.EvolutionCeremonyDialog
import com.poketch.mobile.ui.nutrition.NutritionScreen
import com.poketch.mobile.ui.nutrition.NutritionViewModel
import com.poketch.mobile.ui.pokedex.PokedexScreen
import com.poketch.mobile.ui.pokedex.PokedexViewModel

enum class MobileTab(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    DASHBOARD("dashboard", "Home", Icons.Default.Home),
    NUTRITION("nutrition", "Meals", Icons.Default.Restaurant),
    POKEDEX("pokedex", "Pokédex", Icons.Default.Star),
    DIAGNOSTICS("diagnostics", "Watch Link", Icons.Default.Watch),
    ACCOUNT("account", "Trainer", Icons.Default.AccountCircle),
}

@Composable
fun PoketchMobileNavHost(
    unlockEventDispatcher: UnlockEventDispatcher,
    modifier: Modifier = Modifier,
    commandCenterViewModel: CommandCenterViewModel = viewModel(),
    nutritionViewModel: NutritionViewModel = viewModel(),
    pokedexViewModel: PokedexViewModel = viewModel(),
    syncDiagnosticsViewModel: SyncDiagnosticsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
) {
    val authState by authViewModel.authState.collectAsState()

    when (authState) {
        is AuthState.Loading -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFFE11D48))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Authenticating Trainer...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        is AuthState.Unauthenticated -> {
            // User MUST log in or register before they can access the app
            AuthScreen(
                viewModel = authViewModel,
                modifier = modifier.fillMaxSize()
            )
        }
        is AuthState.Authenticated -> {
            var currentTab by rememberSaveable { mutableStateOf(MobileTab.DASHBOARD) }
            var unlockedPokemonDialog by remember { mutableStateOf<UnlockEvent.NewPokemonUnlocked?>(null) }

            LaunchedEffect(unlockEventDispatcher) {
                unlockEventDispatcher.unlockEvents.collect { event ->
                    unlockedPokemonDialog = event
                }
            }

            Scaffold(
                modifier = modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp,
                        shadowElevation = 8.dp
                    ) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp
                        ) {
                            MobileTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.title,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        MobileTab.DASHBOARD -> CommandCenterScreen(viewModel = commandCenterViewModel)
                        MobileTab.NUTRITION -> NutritionScreen(viewModel = nutritionViewModel)
                        MobileTab.POKEDEX -> PokedexScreen(viewModel = pokedexViewModel)
                        MobileTab.DIAGNOSTICS -> SyncDiagnosticsScreen(viewModel = syncDiagnosticsViewModel)
                        MobileTab.ACCOUNT -> AuthScreen(viewModel = authViewModel)
                    }
                }
            }

            unlockedPokemonDialog?.let { unlockEvent ->
                if (unlockEvent.isEvolution && unlockEvent.preEvolution != null) {
                    EvolutionCeremonyDialog(
                        event = unlockEvent,
                        onSetAsActiveBuddy = { buddyId ->
                            commandCenterViewModel.setActiveBuddy(buddyId)
                        },
                        onDismiss = { unlockedPokemonDialog = null }
                    )
                } else {
                    UnlockedPokemonCelebrationDialog(
                        event = unlockEvent,
                        onDismiss = { unlockedPokemonDialog = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun UnlockedPokemonCelebrationDialog(
    event: UnlockEvent.NewPokemonUnlocked,
    onDismiss: () -> Unit
) {
    val pokemon = event.pokemon
    val context = LocalContext.current
    val imageUri = "file:///android_asset/${pokemon.spriteAssetPath}"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉 POKÉMON UNLOCKED!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFD32F2F),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "#%03d ${pokemon.name.uppercase()}".format(pokemon.pokedexNumber),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = pokemon.name,
                        modifier = Modifier.size(110.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = pokemon.primaryType,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    pokemon.secondaryType?.let { sec ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = sec,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Achievement Unlocked",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = event.triggerReason,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Claim to Pokédex!",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
