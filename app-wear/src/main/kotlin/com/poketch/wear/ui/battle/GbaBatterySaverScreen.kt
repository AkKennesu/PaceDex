package com.poketch.wear.ui.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.poketch.wear.R

/**
 * State E: GBA Battery Saver OLED Ambient Mode Screen.
 * Activated via "RUN" option in the GBA battle command menu.
 * Renders on a pure #000000 black OLED display with minimal power draw,
 * displaying low-power clock, calorie balance, hydration, and buddy sprite,
 * with a single tap action to resume full battle mode.
 */
@Composable
fun GbaBatterySaverScreen(
    onResumeBattle: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GbaWatchBattleViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val buddy = state.activeBuddy
    val buddyPokedexNum = buddy?.pokedexNumber ?: 1
    val buddyUri = com.poketch.core.domain.model.Gen3PokemonSpriteResolver.getFrontSpriteUri(buddyPokedexNum)

    val ceiling = state.targetCalorieCeiling.coerceAtLeast(1)
    val consumed = state.consumedKcal
    val isOverTarget = consumed > ceiling
    val remainingKcal = ceiling - consumed
    val surplus = consumed - ceiling

    Scaffold(
        timeText = { TimeText() },
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Buddy icon (32x32 px) with nearest-neighbor scaling
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(buddyUri)
                    .crossfade(false)
                    .error(R.drawable.gba_ball_healthy)
                    .fallback(R.drawable.gba_ball_healthy)
                    .build(),
                contentDescription = buddy?.name ?: "Buddy",
                filterQuality = FilterQuality.None,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Calorie status line
            if (isOverTarget) {
                Text(
                    text = "+$surplus KCAL OVER",
                    color = Color(0xFFFF5252),
                    fontSize = 18.sp,
                    fontFamily = PokemonDsFontFamily,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "$remainingKcal KCAL LEFT",
                    color = Color(0xFF69F0AE),
                    fontSize = 18.sp,
                    fontFamily = PokemonDsFontFamily,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Hydration status line
            Text(
                text = "${state.todayWaterMl} / ${state.dailyWaterGoalMl} ML",
                color = Color(0xFF80D8FF),
                fontSize = 15.sp,
                fontFamily = PokemonDsFontFamily,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Resume Battle Action Chip
            Chip(
                onClick = onResumeBattle,
                label = {
                    Text(
                        text = "RESUME BATTLE",
                        fontFamily = PokemonDsFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF8F8F8),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                colors = ChipDefaults.chipColors(
                    backgroundColor = Color(0xFF1E2822),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(34.dp)
                    .border(1.dp, Color(0xFF388E3C), RoundedCornerShape(17.dp))
            )
        }
    }
}
