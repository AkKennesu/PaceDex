package com.poketch.mobile.ui.pokedex

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.FilterChipDefaults
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
import com.poketch.mobile.ui.theme.PoketchBorder
import com.poketch.mobile.ui.theme.PoketchBorderSubtle
import com.poketch.mobile.ui.theme.PoketchCrimson
import com.poketch.mobile.ui.theme.PoketchCyan
import com.poketch.mobile.ui.theme.PoketchEmerald
import com.poketch.mobile.ui.theme.PoketchSurface
import com.poketch.mobile.ui.theme.PoketchSurfaceElevated
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
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 14.dp),
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
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
            sheetState = sheetState,
            containerColor = PoketchSurface,
            contentColor = Color.White
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PoketchSurface),
        border = BorderStroke(1.dp, PoketchBorder)
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
                Column {
                    Text(
                        text = "KANTO SPECIES INDEX",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = PoketchCrimson
                    )
                    Text(
                        text = "151 Pokédex",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PoketchEmerald.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, PoketchEmerald.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "$unlockedCount / $totalCount Unlocked",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = PoketchEmerald,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progressRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = PoketchEmerald,
                trackColor = Color(0xFF1E293B),
                strokeCap = StrokeCap.Round
            )

            Text(
                text = "Completion: $percent% • Unlock species by hitting calorie deficits, water & daily steps!",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
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
                label = { Text(filter.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PoketchCrimson,
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF131B2A),
                    labelColor = Color(0xFF94A3B8)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) PoketchCrimson else PoketchBorderSubtle
                )
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
            .aspectRatio(0.76f)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) PoketchSurface else Color(0xFF0F172A)
        ),
        border = BorderStroke(
            1.dp,
            if (isActiveBuddy) PoketchCrimson else if (isUnlocked) PoketchBorder else PoketchBorderSubtle
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
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
                        color = if (isUnlocked) Color(0xFF94A3B8) else Color(0xFF475569)
                    )

                    if (isActiveBuddy) {
                        Surface(
                            shape = CircleShape,
                            color = PoketchCrimson.copy(alpha = 0.2f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Active Buddy",
                                tint = PoketchCrimson,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(2.dp)
                            )
                        }
                    }
                }

                // Sprite on pedestal
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(
                            if (isUnlocked) Color(0xFF1E293B) else Color(0xFF0B111E)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = pokemon.name,
                        filterQuality = FilterQuality.None,
                        colorFilter = if (isUnlocked) null else ColorFilter.tint(Color.Black.copy(alpha = 0.88f)),
                        modifier = Modifier.size(50.dp)
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
                        color = if (isUnlocked) Color.White else Color(0xFF64748B)
                    )

                    if (isUnlocked) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = getPokemonTypeColor(pokemon.primaryType),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = pokemon.primaryType.uppercase(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    } else {
                        // Mini progress bar for locked pokemon
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = { pokemon.progressRatio.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = PoketchCrimson,
                                trackColor = Color(0xFF1E293B)
                            )
                            Text(
                                text = "${pokemon.currentProgress}/${pokemon.unlockRule.targetThreshold}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = Color(0xFF64748B),
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
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sprite Pedestal
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F172A))
                .border(2.dp, PoketchBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUri)
                    .crossfade(true)
                    .build(),
                contentDescription = pokemon.name,
                filterQuality = FilterQuality.None,
                colorFilter = if (isUnlocked) null else ColorFilter.tint(Color.Black.copy(alpha = 0.88f)),
                modifier = Modifier.size(100.dp)
            )
        }

        // Title and #ID
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isUnlocked) pokemon.name.uppercase() else "???",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = Color.White
            )
            Text(
                text = "#%03d • Kanto Species".format(pokemon.pokedexNumber),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8)
            )
        }

        // Type Pills
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = getPokemonTypeColor(pokemon.primaryType)
            ) {
                Text(
                    text = pokemon.primaryType.uppercase(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            pokemon.secondaryType?.let { sec ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = getPokemonTypeColor(sec)
                ) {
                    Text(
                        text = sec.uppercase(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }

        // Criteria & Progress Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, PoketchBorder)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "UNLOCK CRITERIA (${pokemon.unlockRule.category.name.replace('_', ' ')})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = PoketchCyan,
                    letterSpacing = 1.sp
                )

                Text(
                    text = pokemon.unlockRule.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
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
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "${pokemon.currentProgress} / ${pokemon.unlockRule.targetThreshold} (${(pokemon.progressRatio * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    LinearProgressIndicator(
                        progress = { pokemon.progressRatio.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isUnlocked) PoketchEmerald else PoketchCrimson,
                        trackColor = Color(0xFF1E293B),
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
                    color = if (isUnlocked) PoketchEmerald else PoketchCrimson
                )
            }
        }

        // Action Button: "Set as Watch Buddy"
        Button(
            onClick = onSetAsBuddy,
            enabled = isUnlocked && !isActiveBuddy,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActiveBuddy) PoketchEmerald else PoketchCrimson,
                disabledContainerColor = Color(0xFF1E293B)
            ),
            shape = RoundedCornerShape(14.dp),
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
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

private fun getPokemonTypeColor(type: String): Color {
    return when (type.uppercase()) {
        "NORMAL" -> Color(0xFFA8A878)
        "FIRE" -> Color(0xFFF08030)
        "WATER" -> Color(0xFF38BDF8)
        "GRASS" -> Color(0xFF4ADE80)
        "ELECTRIC" -> Color(0xFFFACC15)
        "ICE" -> Color(0xFF98D8D8)
        "FIGHTING" -> Color(0xFFE11D48)
        "POISON" -> Color(0xFFA855F7)
        "GROUND" -> Color(0xFFE0C068)
        "FLYING" -> Color(0xFFA890F0)
        "PSYCHIC" -> Color(0xFFF43F5E)
        "BUG" -> Color(0xFFA8B820)
        "ROCK" -> Color(0xFFB8A038)
        "GHOST" -> Color(0xFF818CF8)
        "DRAGON" -> Color(0xFF7038F8)
        "STEEL" -> Color(0xFF94A3B8)
        "FAIRY" -> Color(0xFFF472B6)
        else -> Color(0xFF64748B)
    }
}
