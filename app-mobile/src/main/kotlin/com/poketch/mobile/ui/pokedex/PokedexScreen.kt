package com.poketch.mobile.ui.pokedex

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.poketch.core.domain.model.Pokemon
import com.poketch.mobile.ui.theme.StitchAlertCoral
import com.poketch.mobile.ui.theme.StitchBorderSubtle
import com.poketch.mobile.ui.theme.StitchCanvasBackground
import com.poketch.mobile.ui.theme.StitchDarkSlate
import com.poketch.mobile.ui.theme.StitchHydration
import com.poketch.mobile.ui.theme.StitchHydrationTint
import com.poketch.mobile.ui.theme.StitchPrimary
import com.poketch.mobile.ui.theme.StitchPrimaryTint
import com.poketch.mobile.ui.theme.StitchStepsAmber
import com.poketch.mobile.ui.theme.StitchStepsTint
import com.poketch.mobile.ui.theme.StitchSurfaceCard
import com.poketch.mobile.ui.theme.StitchTextMuted
import com.poketch.mobile.ui.theme.StitchTextPrimary
import com.poketch.mobile.ui.theme.StitchTextSecondary

/**
 * Google Stitch Pokédex Collection Screen
 * Features 3-column collector Bento grid, custom filter chips, active mission card,
 * and detailed companion bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokedexScreen(
    modifier: Modifier = Modifier,
    viewModel: PokedexViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StitchCanvasBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header: Pokédex on left, dark slate pill 18 / 151 on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pokédex",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = StitchDarkSlate,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Habit-driven Kanto field registry",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StitchTextMuted
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = StitchDarkSlate,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = "${state.unlockedCount} / ${state.totalCount}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // 2. Horizontal Filter Chips
            StitchPokedexFilterChips(
                selectedFilter = state.selectedFilter,
                onFilterSelected = viewModel::onFilterSelected
            )

            // 3. 3-Column Collector Bento Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(
                    items = state.filteredPokemon,
                    key = { it.pokedexNumber }
                ) { pokemon ->
                    StitchPokemonCollectorCard(
                        pokemon = pokemon,
                        isPartner = pokemon.pokedexNumber == state.activeBuddyId,
                        onClick = { viewModel.onPokemonClicked(pokemon) }
                    )
                }
            }

            // 4. Elevated White "Active Mission" Detail Card
            StitchActiveMissionCard()
        }
    }

    // Detail Bottom Sheet
    state.selectedPokemon?.let { selected ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.onDismissBottomSheet() },
            sheetState = sheetState,
            containerColor = StitchSurfaceCard,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            StitchPokemonDetailSheet(
                pokemon = selected,
                isPartner = selected.pokedexNumber == state.activeBuddyId,
                onSetAsPartner = {
                    viewModel.setActiveBuddy(selected.pokedexNumber)
                    viewModel.onDismissBottomSheet()
                }
            )
        }
    }
}

/**
 * Filter Chips: All, Water, Steps, Deficit, etc.
 */
@Composable
private fun StitchPokedexFilterChips(
    selectedFilter: PokedexFilter,
    onFilterSelected: (PokedexFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        PokedexFilter.ALL to "All",
        PokedexFilter.HYDRATION to "Water",
        PokedexFilter.RUNNING_STEPS to "Steps",
        PokedexFilter.CALORIE_DEFICIT to "Deficit",
        PokedexFilter.UNLOCKED to "Unlocked",
        PokedexFilter.LOCKED to "Locked"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val isDark = com.poketch.mobile.ui.theme.LocalStitchColors.current.isOledDark
        filters.forEach { (filter, label) ->
            val isSelected = selectedFilter == filter
            val activeBg = if (isDark) StitchPrimary else StitchDarkSlate
            val activeText = if (isDark) Color(0xFF022C22) else Color.White
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onFilterSelected(filter) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) activeBg else StitchSurfaceCard,
                border = BorderStroke(1.dp, if (isSelected) activeBg else StitchBorderSubtle),
                shadowElevation = if (isSelected && !isDark) 2.dp else 0.dp
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) activeText else StitchTextSecondary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}

/**
 * 3-Column Collector Card (20dp radius)
 */
@Composable
private fun StitchPokemonCollectorCard(
    pokemon: Pokemon,
    isPartner: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_scale"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = StitchDarkSlate.copy(alpha = 0.03f),
                spotColor = StitchDarkSlate.copy(alpha = 0.06f)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        color = StitchSurfaceCard,
        border = BorderStroke(
            if (isPartner) 2.dp else 1.dp,
            if (isPartner) StitchPrimary else StitchBorderSubtle
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header: #001 & status pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#%03d".format(pokemon.pokedexNumber),
                    style = MaterialTheme.typography.labelSmall,
                    color = StitchTextMuted,
                    fontSize = 10.sp
                )

                if (isPartner) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StitchPrimaryTint
                    ) {
                        Text(
                            text = "Partner",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = StitchPrimary,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                } else if (pokemon.isUnlocked) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StitchPrimaryTint
                    ) {
                        Text(
                            text = "Unlocked",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = StitchPrimary,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F2)
                    ) {
                        Text(
                            text = "Locked",
                            style = MaterialTheme.typography.labelSmall,
                            color = StitchTextMuted,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Creature Pedestal
            val isDark = com.poketch.mobile.ui.theme.LocalStitchColors.current.isOledDark
            val pedestalBg = if (isDark) Color(0xFF090F0D) else (if (pokemon.isUnlocked) Color(0xFFF7FAF8) else Color(0xFFF1F4F2))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(pedestalBg),
                contentAlignment = Alignment.Center
            ) {
                if (pokemon.isUnlocked) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data("file:///android_asset/${pokemon.spriteAssetPath}")
                            .crossfade(true)
                            .build(),
                        contentDescription = pokemon.name,
                        modifier = Modifier.size(54.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = StitchTextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Text(
                text = pokemon.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = StitchDarkSlate,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Progress bar or subtitle
            if (pokemon.isUnlocked) {
                Text(
                    text = "${pokemon.primaryType.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = StitchPrimary,
                    fontSize = 10.sp
                )
            } else {
                LinearProgressIndicator(
                    progress = { 0.45f },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = StitchStepsAmber,
                    trackColor = Color(0xFFE2E8F0),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Elevated White "Active Mission" Bottom Detail Card
 */
@Composable
private fun StitchActiveMissionCard(
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btn_scale"
    )

    val isDark = com.poketch.mobile.ui.theme.LocalStitchColors.current.isOledDark

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 0.dp else 8.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = StitchDarkSlate.copy(alpha = 0.05f),
                spotColor = StitchDarkSlate.copy(alpha = 0.08f)
            ),
        shape = RoundedCornerShape(24.dp),
        color = StitchSurfaceCard,
        border = BorderStroke(1.dp, StitchBorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StitchStepsTint
                ) {
                    Text(
                        text = "ACTIVE MISSION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = StitchStepsAmber,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = "Fast Track",
                    style = MaterialTheme.typography.labelSmall,
                    color = StitchTextMuted
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(StitchStepsTint.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = "Run",
                        tint = StitchStepsAmber,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = "Unlock #077 Ponyta",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StitchDarkSlate
                    )
                    Text(
                        text = "Run 1.0 mile in a single session on your Wear OS watch to capture Ponyta",
                        style = MaterialTheme.typography.bodySmall,
                        color = StitchTextSecondary
                    )
                }
            }

            // Progress Meter
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { 0.64f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = StitchStepsAmber,
                    trackColor = if (isDark) Color(0xFF1F2E26) else Color(0xFFF1F5F2),
                    strokeCap = StrokeCap.Round
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Progress",
                        style = MaterialTheme.typography.labelSmall,
                        color = StitchTextMuted
                    )
                    Text(
                        text = "0.64 / 1.00 miles completed",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = StitchDarkSlate
                    )
                }
            }

            // Full-Width Emerald CTA Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(24.dp),
                color = StitchPrimary
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Watch,
                        contentDescription = "Watch",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Send Run Prompt to Watch",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Detailed Pokemon Companion Bottom Sheet Content
 */
@Composable
private fun StitchPokemonDetailSheet(
    pokemon: Pokemon,
    isPartner: Boolean,
    onSetAsPartner: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(StitchPrimaryTint.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data("file:///android_asset/${pokemon.spriteAssetPath}")
                    .crossfade(true)
                    .build(),
                contentDescription = pokemon.name,
                modifier = Modifier.size(80.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "#%03d ${pokemon.name}".format(pokemon.pokedexNumber),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = StitchDarkSlate
            )
            Text(
                text = "${pokemon.primaryType.uppercase()} TYPE COMPANION",
                style = MaterialTheme.typography.labelSmall,
                color = StitchPrimary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, StitchBorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Unlock Condition",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = StitchDarkSlate
                )
                Text(
                    text = pokemon.unlockRule.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = StitchTextSecondary
                )
            }
        }

        if (pokemon.isUnlocked) {
            Button(
                onClick = onSetAsPartner,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPartner) StitchDarkSlate else StitchPrimary
                ),
                shape = RoundedCornerShape(25.dp)
            ) {
                Text(
                    text = if (isPartner) "Current Watch Companion" else "Set as Active Watch Buddy",
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
