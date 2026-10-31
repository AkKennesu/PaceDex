package com.poketch.wear.ui.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.poketch.core.domain.model.Pokemon
import com.poketch.wear.R
import com.poketch.wear.ui.layout.rememberResponsiveColumnState

/**
 * State D: 151 Kanto Pokédex & Unlock Missions Screen.
 * Displays all 151 Kanto Pokémon with live unlock progress:
 * - Unlocked: Full color 64x64 GBA sprite, stats, and "SEND OUT AS BUDDY" action.
 * - Locked: Dark silhouette, mission description, progress bar, and "PIN AS WILD TARGET" action.
 */
@Composable
fun GbaPokedexMissionsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GbaWatchBattleViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val columnState = rememberResponsiveColumnState()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filterTabs = listOf("ALL (151)", "UNLOCKED", "LOCKED")

    val filteredList = remember(state.allPokemon, selectedFilterIndex) {
        when (selectedFilterIndex) {
            1 -> state.allPokemon.filter { it.isUnlocked }
            2 -> state.allPokemon.filter { !it.isUnlocked }
            else -> state.allPokemon
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = columnState.listState) }
    ) {
        ScalingLazyColumn(
            state = columnState.listState,
            contentPadding = columnState.contentPadding,
            autoCentering = columnState.autoCentering,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1410)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header
            item {
                Text(
                    text = "POKéDEX",
                    color = Color(0xFFFFD54F),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PokemonDsFontFamily,
                    textAlign = TextAlign.Center
                )
            }

            // Quick Filter Tabs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E2820))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    filterTabs.forEachIndexed { index, title ->
                        val isSelected = selectedFilterIndex == index
                        CompactChip(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedFilterIndex = index
                            },
                            label = {
                                Text(
                                    text = if (index == 0) "ALL" else if (index == 1) "CAUGHT" else "WILD",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                    fontFamily = PokemonDsFontFamily
                                )
                            },
                            colors = ChipDefaults.chipColors(
                                backgroundColor = if (isSelected) Color(0xFF2E7D32) else Color.Transparent,
                                contentColor = if (isSelected) Color.White else Color(0xFFB0BEC5)
                            ),
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }
            }

            // List of Pokémon Cards
            items(filteredList, key = { it.pokedexNumber }) { pokemon ->
                PokedexCard(
                    pokemon = pokemon,
                    isActiveBuddy = state.activeBuddy?.pokedexNumber == pokemon.pokedexNumber,
                    isPinnedTarget = state.pinnedTargetPokemonId == pokemon.pokedexNumber,
                    onSendOutBuddy = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setActiveBuddy(pokemon.pokedexNumber)
                    },
                    onPinTarget = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.pinWildTarget(pokemon.pokedexNumber)
                    },
                    context = context
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Back button
            item {
                CompactChip(
                    onClick = onNavigateBack,
                    label = {
                        Text(
                            text = "◀ BATTLE ARENA",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PokemonDsFontFamily
                        )
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF37474F),
                        contentColor = Color(0xFFECEFF1)
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(36.dp)
                )
            }
        }
    }
}

@Composable
private fun PokedexCard(
    pokemon: Pokemon,
    isActiveBuddy: Boolean,
    isPinnedTarget: Boolean,
    onSendOutBuddy: () -> Unit,
    onPinTarget: () -> Unit,
    context: android.content.Context,
    modifier: Modifier = Modifier,
) {
    val isLocked = !pokemon.isUnlocked
    var showShinyPreview by remember { mutableStateOf(false) }
    val spriteUri = com.poketch.core.domain.model.Gen3PokemonSpriteResolver.getFrontSpriteUri(
        pokedexNumber = pokemon.pokedexNumber,
        isShiny = showShinyPreview && !isLocked
    )

    Column(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF18221A))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sprite (Full Color Gen III if unlocked, authentic pixel silhouette if locked, tap to toggle Shiny)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F1410))
                    .clickable(enabled = !isLocked) {
                        showShinyPreview = !showShinyPreview
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(spriteUri)
                        .crossfade(false)
                        .error(R.drawable.gba_ball_healthy)
                        .fallback(R.drawable.gba_ball_healthy)
                        .build(),
                    contentDescription = pokemon.name,
                    filterQuality = FilterQuality.None,
                    contentScale = ContentScale.Fit,
                    colorFilter = if (isLocked) {
                        ColorFilter.tint(Color(0xFF2E3832), BlendMode.SrcIn)
                    } else null,
                    modifier = Modifier.size(42.dp)
                )

                if (showShinyPreview && !isLocked) {
                    Text(
                        text = "★",
                        color = Color(0xFFFFD700),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "#%03d".format(pokemon.pokedexNumber),
                        color = Color(0xFFFFD54F),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PokemonDsFontFamily
                    )
                    if (isActiveBuddy) {
                        Text(
                            text = "[BUDDY]",
                            color = Color(0xFF69F0AE),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = PokemonDsFontFamily
                        )
                    } else if (isPinnedTarget) {
                        Text(
                            text = "[TARGET]",
                            color = Color(0xFFFFAB40),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = PokemonDsFontFamily
                        )
                    }
                }
                Text(
                    text = if (isLocked) "???" else pokemon.name.uppercase(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PokemonDsFontFamily
                )
                Text(
                    text = "${pokemon.primaryType}${pokemon.secondaryType?.let { "/$it" } ?: ""}",
                    color = Color(0xFFB0BEC5),
                    fontSize = 13.sp,
                    fontFamily = PokemonDsFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (isLocked) {
            // Mission info & progress bar
            Text(
                text = "MISSION: ${pokemon.unlockRule.description}",
                color = Color(0xFFE0E0E0),
                fontSize = 13.sp,
                fontFamily = PokemonDsFontFamily,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PROGRESS: ${pokemon.currentProgress}/${pokemon.unlockRule.targetThreshold}",
                    color = Color(0xFFFFCA28),
                    fontSize = 13.sp,
                    fontFamily = PokemonDsFontFamily
                )
                Text(
                    text = "${(pokemon.progressRatio * 100).toInt()}%",
                    color = Color(0xFFFFCA28),
                    fontSize = 13.sp,
                    fontFamily = PokemonDsFontFamily
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action: PIN AS WILD TARGET
            CompactChip(
                onClick = onPinTarget,
                label = {
                    Text(
                        text = if (isPinnedTarget) "✓ TARGET PINNED" else "PIN AS TARGET",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PokemonDsFontFamily
                    )
                },
                colors = ChipDefaults.chipColors(
                    backgroundColor = if (isPinnedTarget) Color(0xFFE65100) else Color(0xFF37474F),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            )
        } else {
            // Unlocked: SEND OUT AS BUDDY
            CompactChip(
                onClick = onSendOutBuddy,
                label = {
                    Text(
                        text = if (isActiveBuddy) "★ ACTIVE BUDDY" else "SEND OUT AS BUDDY",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PokemonDsFontFamily
                    )
                },
                colors = ChipDefaults.chipColors(
                    backgroundColor = if (isActiveBuddy) Color(0xFF1B5E20) else Color(0xFF2E7D32),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            )
        }
    }
}
