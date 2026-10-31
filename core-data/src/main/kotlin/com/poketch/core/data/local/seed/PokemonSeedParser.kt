package com.poketch.core.data.local.seed

import android.content.Context
import com.poketch.core.data.local.entity.PokemonEntity
import org.json.JSONArray
import java.io.InputStreamReader

object PokemonSeedParser {

    private const val SEED_FILE = "pokemon_151_seed.json"

    fun loadPokemonSeed(context: Context?): List<PokemonEntity> {
        if (context == null) {
            return PokemonSeedData.ALL_151
        }

        return try {
            val assetManager = context.assets
            val hasFile = assetManager.list("")?.contains(SEED_FILE) == true
            if (!hasFile) {
                return PokemonSeedData.ALL_151
            }

            assetManager.open(SEED_FILE).use { inputStream ->
                val jsonString = InputStreamReader(inputStream).readText()
                val jsonArray = JSONArray(jsonString)
                val list = mutableListOf<PokemonEntity>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.getInt("pokedexNumber")
                    val padded = id.toString().padStart(3, '0')
                    val secondaryType = if (obj.has("secondaryType") && !obj.isNull("secondaryType")) {
                        obj.getString("secondaryType")
                    } else {
                        null
                    }

                    val isStarter = id in listOf(1, 4, 7, 25)
                    list.add(
                        PokemonEntity(
                            pokedexNumber = id,
                            name = obj.getString("name"),
                            primaryType = obj.getString("primaryType"),
                            secondaryType = secondaryType,
                            categoryTag = obj.getString("categoryTag"),
                            unlockRuleCode = obj.getString("unlockRuleCode"),
                            unlockDescription = obj.getString("unlockDescription"),
                            requiredProgress = obj.getInt("requiredProgress"),
                            currentProgress = if (isStarter) 1 else 0,
                            isUnlocked = isStarter,
                            unlockedAtMillis = if (isStarter) 1700000000000L else null,
                            spriteAssetPath = obj.optString("spriteAssetPath", "sprites/pokemon_$padded.png")
                        )
                    )
                }

                if (list.size == 151) list else PokemonSeedData.ALL_151
            }
        } catch (_: Exception) {
            PokemonSeedData.ALL_151
        }
    }
}
