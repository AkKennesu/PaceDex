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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextGeometricTransform
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
 * Supports both raster bitmaps (PNGs with FilterQuality.None) and vector drawables.
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val isVector = androidx.compose.runtime.remember(resId) {
        try {
            val typedValue = android.util.TypedValue()
            context.resources.getValue(resId, typedValue, true)
            typedValue.string?.endsWith(".xml") == true
        } catch (_: Exception) {
            false
        }
    }

    if (isVector) {
        Image(
            painter = androidx.compose.ui.res.painterResource(id = resId),
            contentDescription = contentDescription,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter,
            modifier = modifier
        )
    } else {
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
}

val PokemonDsFontFamily = FontFamily(
    Font(R.font.pokemon_ds_font, FontWeight.Normal),
    Font(R.font.pokemon_ds_font, FontWeight.Bold)
)

/**
 * Crisp GBA/DS typography using the authentic Pokémon DS pixel font with exact scaling.
 * Supports horizontal scaleX expansion, letter spacing, and PlatformTextStyle(includeFontPadding = false).
 */
@Composable
fun GbaText(
    text: String,
    scale: Float,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF383838),
    fontSizeVirtualPx: Float = 10f,
    lineHeightVirtualPx: Float = fontSizeVirtualPx,
    fontWeight: FontWeight = FontWeight.Bold,
    letterSpacingVirtualPx: Float = 0.4f,
    scaleX: Float = 1.15f,
    textAlign: TextAlign = TextAlign.Start,
    fontFamily: FontFamily = PokemonDsFontFamily,
    includeFontPadding: Boolean = false,
    maxLines: Int = 1,
    softWrap: Boolean = false,
) {
    Text(
        text = text,
        color = color,
        fontSize = (fontSizeVirtualPx * scale).sp,
        lineHeight = (lineHeightVirtualPx * scale).sp,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = (letterSpacingVirtualPx * scale).sp,
        textAlign = textAlign,
        style = TextStyle(
            textGeometricTransform = TextGeometricTransform(scaleX = scaleX),
            platformStyle = PlatformTextStyle(includeFontPadding = includeFontPadding)
        ),
        maxLines = maxLines,
        softWrap = softWrap,
        modifier = modifier
    )
}
