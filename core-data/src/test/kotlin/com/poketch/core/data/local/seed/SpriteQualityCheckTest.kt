package com.poketch.core.data.local.seed

import android.graphics.BitmapFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpriteQualityCheckTest {

    data class SpriteBounds(
        val num: Int,
        val width: Int,
        val height: Int,
        val minX: Int,
        val maxX: Int,
        val minY: Int,
        val maxY: Int,
    ) {
        val contentWidth: Int get() = if (minX <= maxX) maxX - minX + 1 else 0
        val contentHeight: Int get() = if (minY <= maxY) maxY - minY + 1 else 0
        val bottomPadding: Int get() = if (maxY >= 0) height - 1 - maxY else 0
        val topPadding: Int get() = minY
        val leftPadding: Int get() = minX
        val rightPadding: Int get() = if (maxX >= 0) width - 1 - maxX else 0
    }

    private fun analyzeSprite(num: Int, file: File): SpriteBounds {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            ?: throw IllegalArgumentException("Cannot decode ${file.name}")
        val w = bitmap.width
        val h = bitmap.height
        var minX = w
        var maxX = -1
        var minY = h
        var maxY = -1

        for (y in 0 until h) {
            for (x in 0 until w) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = (pixel ushr 24) and 0xFF
                if (alpha > 10) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        return SpriteBounds(num, w, h, minX, maxX, minY, maxY)
    }

    private val flyingNumbers = setOf(
        12, 15, 16, 17, 18, 21, 22, 41, 42, 49,
        81, 82, 92, 93, 109, 110, 142, 144, 145, 146, 149, 151
    )

    @Test
    fun generateMetricsTableCode() {
        val spritesDir = File("src/main/assets/sprites")
        if (!spritesDir.exists()) return

        val backSprites = (1..151).associateWith { num ->
            val id = when {
                num < 10 -> "00$num"
                num < 100 -> "0$num"
                else -> "$num"
            }
            analyzeSprite(num, File(spritesDir, "pokemon_back_$id.png"))
        }

        val frontSprites = (1..151).associateWith { num ->
            val id = when {
                num < 10 -> "00$num"
                num < 100 -> "0$num"
                else -> "$num"
            }
            analyzeSprite(num, File(spritesDir, "pokemon_$id.png"))
        }

        val targetDir = File("../core-domain/src/main/kotlin/com/poketch/core/domain/model")
        val targetFile = File(targetDir, "PokemonSpriteMetricsTable.kt")

        val sb = StringBuilder()
        sb.append("package com.poketch.core.domain.model\n\n")
        sb.append("/**\n")
        sb.append(" * Pixel-perfect GBA FireRed sprite metrics for all 151 Kanto Pokémon.\n")
        sb.append(" * Enables precise ground alignment, anti-floating padding elimination,\n")
        sb.append(" * anti-oversize scaling, and authentic battle arena positioning.\n")
        sb.append(" */\n")
        sb.append("data class PokemonSpriteMetrics(\n")
        sb.append("    val backTopPad: Int,\n")
        sb.append("    val backBottomPad: Int,\n")
        sb.append("    val backLeftPad: Int,\n")
        sb.append("    val backRightPad: Int,\n")
        sb.append("    val frontTopPad: Int,\n")
        sb.append("    val frontBottomPad: Int,\n")
        sb.append("    val frontLeftPad: Int,\n")
        sb.append("    val frontRightPad: Int,\n")
        sb.append("    val isFlying: Boolean = false,\n")
        sb.append(") {\n")
        sb.append("    val backContentHeight: Int get() = 64 - backTopPad - backBottomPad\n")
        sb.append("    val backContentWidth: Int get() = 64 - backLeftPad - backRightPad\n")
        sb.append("    val frontContentHeight: Int get() = 64 - frontTopPad - frontBottomPad\n")
        sb.append("    val frontContentWidth: Int get() = 64 - frontLeftPad - frontRightPad\n\n")
        sb.append("    /** Scale factor for Buddy (back sprite) to prevent oversize/clipping issues */\n")
        sb.append("    val buddyScaleFactor: Float\n")
        sb.append("        get() = when {\n")
        sb.append("            backContentHeight >= 61 -> 0.82f\n")
        sb.append("            backContentHeight >= 56 -> 0.90f\n")
        sb.append("            else -> 1.0f\n")
        sb.append("        }\n\n")
        sb.append("    /** Scale factor for Wild Target (front sprite) */\n")
        sb.append("    val wildScaleFactor: Float\n")
        sb.append("        get() = when {\n")
        sb.append("            frontContentHeight >= 61 -> 0.80f\n")
        sb.append("            frontContentHeight >= 56 -> 0.88f\n")
        sb.append("            else -> 1.0f\n")
        sb.append("        }\n")
        sb.append("}\n\n")

        sb.append("object PokemonSpriteMetricsTable {\n")
        sb.append("    private val METRICS: Map<Int, PokemonSpriteMetrics> = mapOf(\n")

        for (num in 1..151) {
            val back = backSprites[num]!!
            val front = frontSprites[num]!!
            val isFly = flyingNumbers.contains(num)
            sb.append("        $num to PokemonSpriteMetrics(\n")
            sb.append("            backTopPad = ${back.topPadding},\n")
            sb.append("            backBottomPad = ${back.bottomPadding},\n")
            sb.append("            backLeftPad = ${back.leftPadding},\n")
            sb.append("            backRightPad = ${back.rightPadding},\n")
            sb.append("            frontTopPad = ${front.topPadding},\n")
            sb.append("            frontBottomPad = ${front.bottomPadding},\n")
            sb.append("            frontLeftPad = ${front.leftPadding},\n")
            sb.append("            frontRightPad = ${front.rightPadding},\n")
            if (isFly) {
                sb.append("            isFlying = true,\n")
            }
            sb.append("        ),\n")
        }

        sb.append("    )\n\n")
        sb.append("    private val DEFAULT = PokemonSpriteMetrics(\n")
        sb.append("        backTopPad = 10,\n")
        sb.append("        backBottomPad = 10,\n")
        sb.append("        backLeftPad = 8,\n")
        sb.append("        backRightPad = 8,\n")
        sb.append("        frontTopPad = 10,\n")
        sb.append("        frontBottomPad = 10,\n")
        sb.append("        frontLeftPad = 8,\n")
        sb.append("        frontRightPad = 8,\n")
        sb.append("    )\n\n")
        sb.append("    fun get(pokedexNumber: Int): PokemonSpriteMetrics {\n")
        sb.append("        return METRICS[pokedexNumber] ?: DEFAULT\n")
        sb.append("    }\n")
        sb.append("}\n")

        targetFile.writeText(sb.toString())
        println("Generated ${targetFile.absolutePath} with 151 entries")
    }
}
