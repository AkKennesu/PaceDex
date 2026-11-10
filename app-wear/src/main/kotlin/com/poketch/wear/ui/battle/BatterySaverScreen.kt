package com.poketch.wear.ui.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.poketch.core.domain.model.Gen3PokemonSpriteResolver
import com.poketch.wear.R
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Centered Gen 3 Dark Battery Saver / OLED Ambient Mode Screen.
 * Implemented on a 192x192 virtual pixel canvas scaled with nearest-neighbor filtering.
 * Features:
 * - Hidden default Wear OS TimeText (hero clock in top GBA deck).
 * - Full dark power-saving base with #040806 OLED nocturnal scanlines.
 * - Bold & Prominent Gen 3 Gold Hero Clock (19.5sp) with smaller AM/PM indicator (11sp) and bold drop shadow.
 * - Centered resting Buddy front sprite (36x36) at (78, 58) on the nocturnal grass pod with Zzz pixel icon at (114, 58).
 * - Centered dark status card at (28, 102, 136, 42) with bold 8.5px readable calorie and hydration text + progress bars.
 * - Centered bottom "▶ RESUME BATTLE" command button at (44, 148, 104, 20) with direct tap trigger.
 */
@Composable
fun BatterySaverScreen(
    onResumeBattle: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GbaWatchBattleViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Live clock formatted with 12h time digits and smaller AM/PM indicator
    val timeDigits by produceState(initialValue = "") {
        val sdf = SimpleDateFormat("h:mm", Locale.ENGLISH)
        while (true) {
            value = sdf.format(Date())
            delay(1000)
        }
    }
    val amPmIndicator by produceState(initialValue = "") {
        val sdf = SimpleDateFormat("a", Locale.ENGLISH)
        while (true) {
            value = sdf.format(Date()).uppercase()
            delay(1000)
        }
    }
    val displayDigits = timeDigits.ifEmpty { "10:58" }
    val displayAmPm = amPmIndicator.ifEmpty { "AM" }

    // Calorie stats calculations
    val dailyCeilingKcal = state.targetCalorieCeiling.coerceAtLeast(1)
    val consumedKcal = state.consumedKcal
    val remainingKcal = (dailyCeilingKcal - consumedKcal).coerceAtLeast(0)

    // Hydration stats calculations
    val waterGoalMl = state.dailyWaterGoalMl.coerceAtLeast(1)
    val waterMl = state.todayWaterMl

    // Active resting Buddy sprite
    val buddy = state.activeBuddy
    val buddyPokedexNum = buddy?.pokedexNumber ?: 1
    val buddyUri = Gen3PokemonSpriteResolver.getFrontSpriteUri(buddyPokedexNum)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        GbaWatchCanvas { scale ->
            // 1. BASE IMAGE: 192x192 GBA Gen 3 dark power-saving base
            GbaPixelImage(
                resId = R.drawable.gba_battery_saver_gen3_dark_base,
                contentDescription = "GBA Battery Saver Dark Base",
                modifier = Modifier.gbaVirtualBounds(0, 0, 192, 192, scale)
            )

            // 2. CENTERED GEN 3 CLOCK: Bold hero digits (19.5sp) with smaller AM/PM indicator (11sp)
            // Occupying the y = 30..51 lower well of gba_saver_clock_deck.png
            Box(
                modifier = Modifier.gbaVirtualBounds(48, 31, 96, 20, scale),
                contentAlignment = Alignment.Center
            ) {
                // Drop shadow layer
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.offset(x = (1.2f * scale).dp, y = (1.2f * scale).dp)
                ) {
                    Text(
                        text = displayDigits,
                        color = Color(0xFF080C12),
                        fontSize = (19.5f * scale).sp,
                        fontFamily = PokemonDsFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            shadow = Shadow(
                                color = Color(0xFF080C12),
                                offset = Offset(1.0f * scale, 0.2f * scale),
                                blurRadius = 0f
                            )
                        )
                    )
                    Spacer(modifier = Modifier.width((3f * scale).dp))
                    Text(
                        text = displayAmPm,
                        color = Color(0xFF080C12),
                        fontSize = (11f * scale).sp,
                        fontFamily = PokemonDsFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            shadow = Shadow(
                                color = Color(0xFF080C12),
                                offset = Offset(0.8f * scale, 0.2f * scale),
                                blurRadius = 0f
                            )
                        ),
                        modifier = Modifier.padding(bottom = (1.5f * scale).dp)
                    )
                }

                // Foreground gold layer
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = displayDigits,
                        color = Color(0xFFF8D848),
                        fontSize = (19.5f * scale).sp,
                        fontFamily = PokemonDsFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            shadow = Shadow(
                                color = Color(0xFFF8D848),
                                offset = Offset(1.0f * scale, 0.2f * scale),
                                blurRadius = 0f
                            )
                        )
                    )
                    Spacer(modifier = Modifier.width((3f * scale).dp))
                    Text(
                        text = displayAmPm,
                        color = Color(0xFFF8D848),
                        fontSize = (11f * scale).sp,
                        fontFamily = PokemonDsFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            shadow = Shadow(
                                color = Color(0xFFF8D848),
                                offset = Offset(0.8f * scale, 0.2f * scale),
                                blurRadius = 0f
                            )
                        ),
                        modifier = Modifier.padding(bottom = (1.5f * scale).dp)
                    )
                }
            }

            // 3. CENTERED RESTING BUDDY (36x36 at (78, 58)) on the nocturnal grass pod
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(buddyUri)
                    .crossfade(false)
                    .error(R.drawable.gba_saver_bulbasaur)
                    .fallback(R.drawable.gba_saver_bulbasaur)
                    .build(),
                contentDescription = buddy?.name ?: "Resting Buddy",
                filterQuality = FilterQuality.None,
                contentScale = ContentScale.Fit,
                modifier = Modifier.gbaVirtualBounds(78, 58, 36, 36, scale)
            )

            // 4. ZZZ PIXEL ICON: (17x12 at (114, 58))
            GbaPixelImage(
                resId = R.drawable.gba_saver_zzz_pixel,
                contentDescription = "Zzz Sleeping Icon",
                modifier = Modifier.gbaVirtualBounds(114, 58, 17, 12, scale)
            )

            // 5. CENTERED DARK STATUS CARD: (x = 28, y = 102, w = 136, h = 42)
            // 5a. Calorie text: "$remainingKcal KCAL LEFT" centered above the CAL bar with bold 8.5px simulated stroke
            Box(
                modifier = Modifier.gbaVirtualBounds(28, 104, 136, 11, scale),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$remainingKcal KCAL LEFT",
                    color = Color(0xFF58E878),
                    fontSize = (8.5f * scale).sp,
                    fontFamily = PokemonDsFontFamily,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        shadow = Shadow(
                            color = Color(0xFF58E878),
                            offset = Offset(1.0f * scale, 0.2f * scale),
                            blurRadius = 0f
                        )
                    )
                )
            }

            // 5b. Calorie Bar: gba_hp_green stretched to width at (x = 62, y = 116, h = 3)
            val hpBarWidth = ((remainingKcal.toFloat() / dailyCeilingKcal.toFloat()).coerceIn(0f, 1f) * 86f).roundToInt()
            if (hpBarWidth > 0) {
                GbaPixelImage(
                    resId = R.drawable.gba_hp_green,
                    contentDescription = "Calorie Bar Fill",
                    modifier = Modifier.gbaVirtualBounds(62, 116, hpBarWidth, 3, scale)
                )
            }

            // 5c. Water text: "$waterMl / $waterGoalMl ML" centered above the WTR bar with bold 8.5px simulated stroke
            Box(
                modifier = Modifier.gbaVirtualBounds(28, 124, 136, 11, scale),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$waterMl / $waterGoalMl ML",
                    color = Color(0xFF48C8F8),
                    fontSize = (8.5f * scale).sp,
                    fontFamily = PokemonDsFontFamily,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        shadow = Shadow(
                            color = Color(0xFF48C8F8),
                            offset = Offset(1.0f * scale, 0.2f * scale),
                            blurRadius = 0f
                        )
                    )
                )
            }

            // 5d. Water Bar: gba_exp_cyan stretched to width at (x = 62, y = 136, h = 3)
            val waterBarWidth = ((waterMl.toFloat() / waterGoalMl.toFloat()).coerceIn(0f, 1f) * 86f).roundToInt()
            if (waterBarWidth > 0) {
                GbaPixelImage(
                    resId = R.drawable.gba_exp_cyan,
                    contentDescription = "Water Bar Fill",
                    modifier = Modifier.gbaVirtualBounds(62, 136, waterBarWidth, 3, scale)
                )
            }

            // 6. CENTERED BOTTOM "▶ RESUME BATTLE" COMMAND BUTTON: (x = 44, y = 148, w = 104, h = 20)
            // Tapping inside Rect(44, 148, 148, 168) invokes onResumeBattle()
            Box(
                modifier = Modifier
                    .gbaVirtualBounds(44, 148, 104, 20, scale)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onResumeBattle
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▶ RESUME BATTLE",
                    color = Color(0xFFF8F8F8),
                    fontSize = (7f * scale).sp,
                    fontFamily = PokemonDsFontFamily,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        shadow = Shadow(
                            color = Color(0xFFF8F8F8),
                            offset = Offset(0.8f * scale, 0.2f * scale),
                            blurRadius = 0f
                        )
                    )
                )
            }
        }
    }
}
