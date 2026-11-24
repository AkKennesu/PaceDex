package com.poketch.mobile.ui.evolution

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.poketch.core.domain.engine.UnlockEvent
import kotlinx.coroutines.delay

@Composable
fun EvolutionCeremonyDialog(
    event: UnlockEvent.NewPokemonUnlocked,
    onSetAsActiveBuddy: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preEvolution = event.preEvolution
    val evolvedPokemon = event.pokemon

    // Phase: 1 = Morphing/Anticipation, 2 = Transformation Complete
    var stage by remember { mutableStateOf(1) }

    LaunchedEffect(Unit) {
        delay(1800)
        stage = 2
    }

    val infiniteTransition = rememberInfiniteTransition(label = "EvolutionSparkle")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val finalScale by animateFloatAsState(
        targetValue = if (stage == 2) 1f else 0.9f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "FinalScale"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.fillMaxWidth(),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (stage == 1) {
                    Text(
                        text = "✨ WHAT?! ✨",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFB300),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${preEvolution?.name ?: "Pokémon"} is evolving!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "🎉 EVOLUTION COMPLETE! 🎉",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF4CAF50),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Your ${preEvolution?.name ?: "Pokémon"} evolved into ${evolvedPokemon.name}!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Morphing / Transformation Glow Area
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = if (stage == 1) {
                                    listOf(Color(0xFFFFD54F).copy(alpha = 0.5f), Color(0xFF1E293B).copy(alpha = 0.3f))
                                } else {
                                    listOf(Color(0xFF81C784).copy(alpha = 0.6f), Color(0xFF1B5E20).copy(alpha = 0.2f))
                                }
                            )
                        )
                        .border(
                            width = 3.dp,
                            color = if (stage == 1) Color(0xFFFFB300) else Color(0xFF4CAF50),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val activePokemon = if (stage == 1 && preEvolution != null) preEvolution else evolvedPokemon
                    val spriteUri = "file:///android_asset/${activePokemon.spriteAssetPath}"

                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(spriteUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = activePokemon.name,
                        filterQuality = FilterQuality.None,
                        modifier = Modifier
                            .size(120.dp)
                            .graphicsLayer {
                                val s = if (stage == 1) pulseScale else finalScale
                                scaleX = s
                                scaleY = s
                            }
                    )
                }

                // Type Badges & Pokedex ID
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = getTypeColor(evolvedPokemon.primaryType),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = evolvedPokemon.primaryType.uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    evolvedPokemon.secondaryType?.let { secType ->
                        Surface(
                            color = getTypeColor(secType),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = secType.uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "#%03d".format(evolvedPokemon.pokedexNumber),
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Trigger Reason Info
                Text(
                    text = event.triggerReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Prompt Section
                AnimatedVisibility(visible = stage == 2, enter = fadeIn()) {
                    Text(
                        text = "Would you like to set ${evolvedPokemon.name} as your new Active Watch Buddy?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button 1: Set Evolved Form as Active Watch Buddy
                Button(
                    onClick = {
                        onSetAsActiveBuddy(evolvedPokemon.pokedexNumber)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Set ${evolvedPokemon.name} as Active Watch Buddy",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Button 2: Keep Current Buddy
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Keep Current Buddy", fontSize = 13.sp)
                }
            }
        },
        dismissButton = null
    )
}

private fun getTypeColor(type: String): Color {
    return when (type.lowercase()) {
        "grass" -> Color(0xFF4CAF50)
        "fire" -> Color(0xFFFF5722)
        "water" -> Color(0xFF2196F3)
        "electric" -> Color(0xFFFFC107)
        "poison" -> Color(0xFF9C27B0)
        "bug" -> Color(0xFF8BC34A)
        "flying" -> Color(0xFF03A9F4)
        "normal" -> Color(0xFF9E9E9E)
        "ground" -> Color(0xFF795548)
        "fairy" -> Color(0xFFE91E63)
        "psychic" -> Color(0xFFE040FB)
        "rock" -> Color(0xFF607D8B)
        "ghost" -> Color(0xFF673AB7)
        "dragon" -> Color(0xFF3F51B5)
        "ice" -> Color(0xFF00BCD4)
        else -> Color(0xFF607D8B)
    }
}
