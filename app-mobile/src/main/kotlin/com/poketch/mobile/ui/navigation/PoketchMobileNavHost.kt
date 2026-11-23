package com.poketch.mobile.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.poketch.mobile.ui.theme.StitchBorderSubtle
import com.poketch.mobile.ui.theme.StitchCanvasBackground
import com.poketch.mobile.ui.theme.StitchDarkSlate
import com.poketch.mobile.ui.theme.StitchPrimary
import com.poketch.mobile.ui.theme.StitchSurfaceCard
import com.poketch.mobile.ui.theme.StitchTextMuted
import com.poketch.mobile.ui.theme.StitchTextSecondary

enum class MobileTab(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    DASHBOARD("dashboard", "Home", Icons.Default.Home),
    NUTRITION("nutrition", "Log", Icons.Default.EditNote),
    POKEDEX("pokedex", "Pokédex", Icons.Default.CatchingPokemon),
    DIAGNOSTICS("diagnostics", "Sync", Icons.Default.Sync),
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
                    .background(StitchCanvasBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = StitchPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Authenticating Trainer...",
                        color = StitchDarkSlate,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        is AuthState.Unauthenticated -> {
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
                containerColor = StitchCanvasBackground,
                bottomBar = {
                    StitchFloatingBottomNav(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        MobileTab.DASHBOARD -> CommandCenterScreen(
                            viewModel = commandCenterViewModel,
                            onOpenProfile = { currentTab = MobileTab.ACCOUNT },
                            onOpenLogMeal = { currentTab = MobileTab.NUTRITION }
                        )
                        MobileTab.NUTRITION -> NutritionScreen(viewModel = nutritionViewModel)
                        MobileTab.POKEDEX -> PokedexScreen(viewModel = pokedexViewModel)
                        MobileTab.DIAGNOSTICS -> SyncDiagnosticsScreen(
                            viewModel = syncDiagnosticsViewModel,
                            onOpenAccount = { currentTab = MobileTab.ACCOUNT }
                        )
                        MobileTab.ACCOUNT -> AuthScreen(viewModel = authViewModel)
                    }
                }
            }

            unlockedPokemonDialog?.let { unlockEvent ->
                EvolutionCeremonyDialog(
                    event = unlockEvent,
                    onSetAsActiveBuddy = { commandCenterViewModel.setActiveBuddy(it) },
                    onDismiss = { unlockedPokemonDialog = null }
                )
            }
        }
    }
}

/**
 * Google Stitch Floating Capsule Bottom Navigation Dock
 * Matches DESIGN.md: Floating white capsule dock with 30px radius, soft drop shadow,
 * and animated solid #0F172A dark slate pill for the active tab.
 */
@Composable
private fun StitchFloatingBottomNav(
    currentTab: MobileTab,
    onTabSelected: (MobileTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val navTabs = listOf(
        MobileTab.DASHBOARD,
        MobileTab.NUTRITION,
        MobileTab.POKEDEX,
        MobileTab.DIAGNOSTICS
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = StitchDarkSlate.copy(alpha = 0.08f),
                    spotColor = StitchDarkSlate.copy(alpha = 0.16f)
                ),
            shape = RoundedCornerShape(32.dp),
            color = StitchSurfaceCard,
            border = BorderStroke(1.dp, StitchBorderSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navTabs.forEach { tab ->
                    val isSelected = currentTab == tab
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.93f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "tab_scale"
                    )

                    val pillBgColor by animateColorAsState(
                        targetValue = if (isSelected) StitchDarkSlate else Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tab_bg"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else StitchTextSecondary,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tab_content_color"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .scale(scale)
                            .clip(RoundedCornerShape(26.dp))
                            .background(pillBgColor)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onTabSelected(tab) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = tab.title,
                                    color = contentColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
