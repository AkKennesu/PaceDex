package com.poketch.mobile.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.poketch.mobile.ui.theme.PoketchMotion
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.poketch.mobile.ui.theme.StitchTextSecondary
import kotlinx.coroutines.launch

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
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

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
            var currentTabIndex by rememberSaveable { mutableIntStateOf(0) }
            var isAccountOpen by rememberSaveable { mutableStateOf(false) }
            var unlockedPokemonDialog by remember { mutableStateOf<UnlockEvent.NewPokemonUnlocked?>(null) }

            BackHandler(enabled = isAccountOpen) {
                isAccountOpen = false
            }

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
                        selectedTabIndex = currentTabIndex,
                        isAccountOpen = isAccountOpen,
                        onTabSelected = { pageIndex ->
                            isAccountOpen = false
                            currentTabIndex = pageIndex
                        }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Crossfade(
                        targetState = currentTabIndex,
                        animationSpec = tween(durationMillis = 150, easing = PoketchMotion.NarrativeEaseOut),
                        label = "tab_crossfade"
                    ) { page ->
                        when (page) {
                            0 -> CommandCenterScreen(
                                viewModel = commandCenterViewModel,
                                onOpenProfile = { isAccountOpen = true },
                                onOpenLogMeal = { currentTabIndex = 1 }
                            )
                            1 -> NutritionScreen(viewModel = nutritionViewModel)
                            2 -> PokedexScreen(viewModel = pokedexViewModel)
                            3 -> SyncDiagnosticsScreen(
                                viewModel = syncDiagnosticsViewModel,
                                onOpenAccount = { isAccountOpen = true }
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isAccountOpen,
                        enter = fadeIn(animationSpec = PoketchMotion.EnterSpec) + slideInVertically(animationSpec = tween(220, easing = PoketchMotion.NarrativeEaseOut)) { it / 28 },
                        exit = fadeOut(animationSpec = PoketchMotion.ExitSpec),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AuthScreen(
                            viewModel = authViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
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
 * and single continuous sliding pill driven by graphicsLayer translationX for 120fps physics.
 */
@Composable
private fun StitchFloatingBottomNav(
    selectedTabIndex: Int,
    isAccountOpen: Boolean,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val navTabs = listOf(
        MobileTab.DASHBOARD,
        MobileTab.NUTRITION,
        MobileTab.POKEDEX,
        MobileTab.DIAGNOSTICS
    )

    val isDark = com.poketch.mobile.ui.theme.LocalStitchColors.current.isOledDark
    val pillColor = if (isDark) Color(0xFF34D399) else StitchDarkSlate

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
                    elevation = if (isDark) 0.dp else 14.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = StitchDarkSlate.copy(alpha = 0.08f),
                    spotColor = StitchDarkSlate.copy(alpha = 0.16f)
                ),
            shape = RoundedCornerShape(32.dp),
            color = if (isDark) Color(0xE6090F0D) else StitchSurfaceCard,
            border = BorderStroke(1.dp, if (isDark) Color(0xFF1F2E26) else StitchBorderSubtle)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp)
            ) {
                val tabWidth = maxWidth / navTabs.size
                val density = LocalDensity.current
                val tabWidthPx = with(density) { tabWidth.toPx() }

                val animatedPillOffsetPx by animateFloatAsState(
                    targetValue = selectedTabIndex * tabWidthPx,
                    animationSpec = PoketchMotion.SnapSpring,
                    label = "pill_offset"
                )

                // Continuous sliding pill driven by RenderThread / graphicsLayer (zero recomposition on swipe)
                Box(
                    modifier = Modifier
                        .width(tabWidth)
                        .height(44.dp)
                        .graphicsLayer {
                            translationX = animatedPillOffsetPx
                            alpha = if (isAccountOpen) 0f else 1f
                        }
                        .clip(RoundedCornerShape(26.dp))
                        .background(pillColor)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navTabs.forEachIndexed { index, tab ->
                        val isSelected = !isAccountOpen && selectedTabIndex == index
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) PoketchMotion.PressedScale else 1f,
                            animationSpec = PoketchMotion.PressSpring,
                            label = "tab_scale"
                        )

                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                if (isDark) Color(0xFF022C22) else Color.White
                            } else StitchTextSecondary,
                            animationSpec = tween(120, easing = PoketchMotion.NarrativeEaseOut),
                            label = "tab_content_color"
                        )

                        Box(
                            modifier = Modifier
                                .width(tabWidth)
                                .height(44.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clip(RoundedCornerShape(26.dp))
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { onTabSelected(index) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = contentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = fadeIn(animationSpec = tween(140, easing = PoketchMotion.NarrativeEaseOut)) + expandHorizontally(
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
                                        expandFrom = Alignment.Start
                                    ),
                                    exit = fadeOut(animationSpec = tween(100, easing = PoketchMotion.NarrativeEaseOut)) + shrinkHorizontally(
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
                                        shrinkTowards = Alignment.Start
                                    )
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
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
    }
}
