package com.poketch.wear.ui.battle

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.poketch.wear.R

/**
 * 192x192 Virtual Pixel Canvas for authentic GBA FireRed/LeafGreen Battle UI on Wear OS.
 * Measures available screen dimensions and scales child coordinates proportionally with
 * nearest-neighbor pixel alignment.
 */
@Composable
fun GbaWatchCanvas(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(scale: Float) -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val sizeDp = minOf(this.maxWidth, this.maxHeight)
        val scale = sizeDp.value / 192f
        Box(
            modifier = Modifier.size(sizeDp)
        ) {
            content(scale)
        }
    }
}

/**
 * Helper to position and dimension any element in exact 192x192 GBA virtual pixel space.
 */
fun Modifier.gbaVirtualBounds(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    scale: Float,
): Modifier = this
    .offset(x = (x * scale).dp, y = (y * scale).dp)
    .size(width = (width * scale).dp, height = (height * scale).dp)

/**
 * Nearest-neighbor pixel-perfect Image rendering for GBA drawable assets.
 */
@Composable
fun GbaPixelImage(
    @DrawableRes resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alpha: Float = 1.0f,
    colorFilter: ColorFilter? = null,
    contentScale: ContentScale = ContentScale.FillBounds,
) {
    Image(
        bitmap = ImageBitmap.imageResource(id = resId),
        contentDescription = contentDescription,
        filterQuality = FilterQuality.None,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter,
        modifier = modifier
    )
}

val PokemonDsFontFamily = FontFamily(
    Font(R.font.pokemon_ds_font, FontWeight.Normal),
    Font(R.font.pokemon_ds_font, FontWeight.Bold)
)

/**
 * Crisp GBA/DS typography using the authentic Pokémon DS pixel font with exact scaling.
 */
@Composable
fun GbaText(
    text: String,
    scale: Float,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF383838),
    fontSizeVirtualPx: Float = 8f,
    fontWeight: FontWeight = FontWeight.Normal,
    letterSpacingVirtualPx: Float = 0f,
    textAlign: TextAlign = TextAlign.Start,
    fontFamily: FontFamily = PokemonDsFontFamily,
    maxLines: Int = 1,
    softWrap: Boolean = false,
) {
    Text(
        text = text,
        color = color,
        fontSize = (fontSizeVirtualPx * scale).sp,
        lineHeight = ((fontSizeVirtualPx + 2f) * scale).sp,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = (letterSpacingVirtualPx * scale).sp,
        textAlign = textAlign,
        maxLines = maxLines,
        softWrap = softWrap,
        modifier = modifier
    )
}
