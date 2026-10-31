package com.poketch.core.data.local.seed

import org.junit.Test
import java.io.File

class SeedExporterTest {

    @Test
    fun exportSeedJsonToAssets() {
        val list = PokemonSeedData.ALL_151
        val sb = StringBuilder()
        sb.append("[\n")
        list.forEachIndexed { index, p ->
            sb.append("  {\n")
            sb.append("    \"pokedexNumber\": ${p.pokedexNumber},\n")
            sb.append("    \"name\": \"${p.name}\",\n")
            sb.append("    \"primaryType\": \"${p.primaryType}\",\n")
            if (p.secondaryType != null) {
                sb.append("    \"secondaryType\": \"${p.secondaryType}\",\n")
            } else {
                sb.append("    \"secondaryType\": null,\n")
            }
            sb.append("    \"categoryTag\": \"${p.categoryTag}\",\n")
            sb.append("    \"unlockRuleCode\": \"${p.unlockRuleCode}\",\n")
            sb.append("    \"unlockDescription\": \"${p.unlockDescription}\",\n")
            sb.append("    \"requiredProgress\": ${p.requiredProgress},\n")
            sb.append("    \"spriteAssetPath\": \"${p.spriteAssetPath}\"\n")
            if (index < list.size - 1) {
                sb.append("  },\n")
            } else {
                sb.append("  }\n")
            }
        }
        sb.append("]\n")

        val targetDir = File("src/main/assets")
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        val targetFile = File(targetDir, "pokemon_151_seed.json")
        targetFile.writeText(sb.toString())
        println("Exported ${list.size} pokemon to ${targetFile.absolutePath}")
    }
}
