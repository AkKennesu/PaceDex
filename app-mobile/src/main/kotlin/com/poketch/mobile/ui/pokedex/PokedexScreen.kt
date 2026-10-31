package com.poketch.mobile.ui.pokedex

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.poketch.core.domain.model.Pokemon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokedexScreen(
    modifier: Modifier = Modifier,
    viewModel: PokedexViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Completion Header
        PokedexHeader(
            unlockedCount = state.unlockedCount,
            totalCount = state.totalCount
        )

        // 2. Category Filter Chips
        PokedexFilterChips(
            selectedFilter = state.selectedFilter,
            onFilterSelected = viewModel::onFilterSelected
        )

        // 3. 3-Column LazyVerticalGrid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = state.filteredPokemon,
                key = { it.pokedexNumber }
            ) { pokemon ->
                PokemonGridItem(
                    pokemon = pokemon,
                    isActiveBuddy = pokemon.pokedexNumber == state.activeBuddyId,
                    onClick = { viewModel.onPokemonClicked(pokemon) }
                )
            }
        }
    }

    // 4. Pokémon Detail Bottom Sheet
    state.selectedPokemon?.let { pokemon ->
        ModalBottomSheet(
            onDismissRequest = viewModel::onDismissBottomSheet,
            sheetState = sheetState
        ) {
            PokemonDetailSheetContent(
                pokemon = pokemon,
                isActiveBuddy = pokemon.pokedexNumber == state.activeBuddyId,
                onSetAsBuddy = {
                    viewModel.setActiveBuddy(pokemon.pokedexNumber)
                }
            )
        }
    }
}

@Composable
private fun PokedexHeader(
    unlockedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    val progressRatio = if (totalCount > 0) unlockedCount.toFloat() / totalCount else 0f
    val percent = (progressRatio * 100).toInt()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "151 Kanto Pokédex",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$unlockedCount / $totalCount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            LinearProgressIndicator(
                progress = { progressRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = Color(0xFF2E7D32),
                trackColor = MaterialTheme.colorScheme.surface,
                strokeCap = StrokeCap.Round
            )

            Text(
                text = "Kanto Completion: $percent% • Unlock all 151 through fitness discipline",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PokedexFilterChips(
    selectedFilter: PokedexFilter,
    onFilterSelected: (PokedexFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PokedexFilter.entries.forEach { filter ->
            val isSelected = selectedFilter == filter
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label) }
            )
        }
    }
}

@Composable
private fun PokemonGridItem(
    pokemon: Pokemon,
    isActiveBuddy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imageUri = "file:///android_asset/${pokemon.spriteAssetPath}"
    val isUnlocked = pokemon.isUnlocked

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.78f)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnlocked) 2.dp else 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top row: ID badge and Buddy star
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#%03d".format(pokemon.pokedexNumber),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                    )

                    if (isActiveBuddy) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Active Buddy",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Sprite
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (isUnlocked) Color(0xFFF1F8E9) else Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = pokemon.name,
                        filterQuality = FilterQuality.None,
                        colorFilter = if (isUnlocked) null else ColorFilter.tint(Color.Black.copy(alpha = 0.85f)),
                        modifier = Modifier.size(52.dp)
                    )
                }

                // Bottom: Name & Info
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isUnlocked) pokemon.name else "???",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else Color.Gray
                    )

                    if (isUnlocked) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = getPokemonTypeColor(pokemon.primaryType),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = pokemon.primaryType,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    } else {
                        // Mini progress bar for locked pokemon
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = { pokemon.progressRatio.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.LightGray
                            )
                            Text(
                                text = "${pokemon.currentProgress}/${pokemon.unlockRule.targetThreshold}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PokemonDetailSheetContent(
    pokemon: Pokemon,
    isActiveBuddy: Boolean,
    onSetAsBuddy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imageUri = "file:///android_asset/${pokemon.spriteAssetPath}"
    val isUnlocked = pokemon.isUnlocked

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sprite
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(if (isUnlocked) Color(0xFFE8F5E9) else Color(0xFFEEEEEE)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUri)
                    .crossfade(true)
                    .build(),
                contentDescription = pokemon.name,
                filterQuality = FilterQuality.None,
                colorFilter = if (isUnlocked) null else ColorFilter.tint(Color.Black.copy(alpha = 0.85f)),
                modifier = Modifier.size(100.dp)
            )
        }

        // Title and #ID
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isUnlocked) pokemon.name.uppercase() else "???",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = "#%03d (Classic Gen-I Kanto)".format(pokemon.pokedexNumber),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Type Pills
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = getPokemonTypeColor(pokemon.primaryType)
            ) {
                Text(
                    text = pokemon.primaryType,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            pokemon.secondaryType?.let { sec ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = getPokemonTypeColor(sec)
                ) {
                    Text(
                        text = sec,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Criteria & Progress Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Unlock Criteria (${pokemon.unlockRule.category.name.replace('_', ' ')})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = pokemon.unlockRule.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                // Progress Bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Milestone Progress",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${pokemon.currentProgress} / ${pokemon.unlockRule.targetThreshold} (${(pokemon.progressRatio * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { pokemon.progressRatio.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isUnlocked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                        strokeCap = StrokeCap.Round
                    )
                }

                // Unlock Date / Status
                val unlockedMillis = pokemon.unlockedAtMillis
                Text(
                    text = if (isUnlocked && (unlockedMillis != null)) {
                        val formattedDate = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(unlockedMillis))
                        "✓ Unlocked on $formattedDate"
                    } else if (isUnlocked) {
                        "✓ Unlocked in Kanto"
                    } else {
                        "🔒 Currently Locked"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isUnlocked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                )
            }
        }

        // Action Button: "Set as Watch Buddy"
        Button(
            onClick = onSetAsBuddy,
            enabled = isUnlocked && !isActiveBuddy,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActiveBuddy) Color(0xFF388E3C) else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            if (isActiveBuddy) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Active Watch Buddy ⭐",
                    fontWeight = FontWeight.Bold
                )
            } else if (isUnlocked) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Set as Watch Buddy",
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "Unlock to Set as Buddy",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun getPokemonTypeColor(type: String): Color {
    return when (type.uppercase()) {
        "NORMAL" -> Color(0xFFA8A878)
        "FIRE" -> Color(0xFFF08030)
        "WATER" -> Color(0xFF6890F0)
        "GRASS" -> Color(0xFF78C850)
        "ELECTRIC" -> Color(0xFFF8D030)
        "ICE" -> Color(0xFF98D8D8)
        "FIGHTING" -> Color(0xFFC03028)
        "POISON" -> Color(0xFFA040A0)
        "GROUND" -> Color(0xFFE0C068)
        "FLYING" -> Color(0xFFA890F0)
        "PSYCHIC" -> Color(0xFFF85888)
        "BUG" -> Color(0xFFA8B820)
        "ROCK" -> Color(0xFFB8A038)
        "GHOST" -> Color(0xFF705898)
        "DRAGON" -> Color(0xFF7038F8)
        "STEEL" -> Color(0xFFB8B8D0)
        "FAIRY" -> Color(0xFFEE99AC)
        else -> Color(0xFF8A8A8A)
    }
}
