package com.poketch.wear.ui.battle

import com.google.common.truth.Truth.assertThat
import com.poketch.core.domain.model.PokemonSpriteMetricsTable
import org.junit.Test
import kotlin.math.roundToInt

class PokemonSpriteQualityCheckTest {

    @Test
    fun all151BuddyPokemon_stayWithinBoundsAndNeverFloat() {
        for (pokedexNum in 1..151) {
            val metrics = PokemonSpriteMetricsTable.get(pokedexNum)
            val scale = metrics.buddyScaleFactor
            val targetBaselineY = if (metrics.isFlying) 122f else 125f

            val frameSize = (64f * scale).roundToInt()
            val visibleBottomOffset = (64f - metrics.backBottomPad.toFloat()) * scale

            val topY = (targetBaselineY - visibleBottomOffset).roundToInt()
            val leftX = 18
            val visibleBottomY = topY + visibleBottomOffset.roundToInt()
            val visibleTopY = topY + (metrics.backTopPad.toFloat() * scale).roundToInt()
            val visibleLeftX = leftX + (metrics.backLeftPad.toFloat() * scale).roundToInt()
            val visibleRightX = leftX + ((64f - metrics.backRightPad.toFloat()) * scale).roundToInt()

            // 1. Lowest pixel must anchor at target baseline (no floating!)
            assertThat(visibleBottomY).isAtLeast(targetBaselineY.toInt() - 1)
            assertThat(visibleBottomY).isAtMost(targetBaselineY.toInt() + 1)

            // 2. Visible top must not be cut off by Box 1 (y=68)
            assertThat(visibleTopY).isAtLeast(67)

            // 3. Buddy visible pixels must stay inside watch screen left edge (x >= 4)
            assertThat(visibleLeftX).isAtLeast(4)

            // 4. Buddy visible pixels must not collide with Box 2 (which starts at x=84)
            assertThat(visibleRightX).isAtMost(84)
        }
    }

    @Test
    fun all151WildPokemon_stayWithinBoundsAndStandOnPlatform() {
        for (pokedexNum in 1..151) {
            val metrics = PokemonSpriteMetricsTable.get(pokedexNum)
            val scale = metrics.wildScaleFactor
            val targetBaselineY = if (metrics.isFlying) 65f else 70f

            val frameSize = (64f * scale).roundToInt()
            val visibleBottomOffset = (64f - metrics.frontBottomPad.toFloat()) * scale

            val topY = (targetBaselineY - visibleBottomOffset).roundToInt()
            val leftX = (146f - frameSize / 2f).roundToInt()
            val visibleBottomY = topY + visibleBottomOffset.roundToInt()
            val visibleTopY = topY + (metrics.frontTopPad.toFloat() * scale).roundToInt()
            val visibleLeftX = leftX + (metrics.frontLeftPad.toFloat() * scale).roundToInt()
            val visibleRightX = leftX + ((64f - metrics.frontRightPad.toFloat()) * scale).roundToInt()

            // 1. Lowest pixel must anchor on platform surface
            assertThat(visibleBottomY).isAtLeast(targetBaselineY.toInt() - 1)
            assertThat(visibleBottomY).isAtMost(targetBaselineY.toInt() + 1)

            // 2. Visible top must stay safely below top of screen (y >= 10)
            assertThat(visibleTopY).isAtLeast(10)

            // 3. Right edge must stay within 192 circular screen (rightX <= 186)
            assertThat(visibleRightX).isAtMost(186)
        }
    }
}
