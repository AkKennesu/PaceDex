package com.poketch.core.domain.model

/**
 * Universal Gen III FireRed/LeafGreen Sprite Resolver for all 151 Kanto Pokémon.
 * Resolves local offline-first asset URIs (bundled in assets/sprites/) with fallback to
 * the official PokeAPI GitHub CDN raw endpoints:
 * https://github.com/PokeAPI/sprites
 */
object Gen3PokemonSpriteResolver {

    const val BASE_POKEAPI_GEN3_FRONT =
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/versions/generation-iii/firered-leafgreen"
    const val BASE_POKEAPI_GEN3_BACK =
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/versions/generation-iii/firered-leafgreen/back"
    const val BASE_POKEAPI_GEN3_SHINY =
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/versions/generation-iii/firered-leafgreen/shiny"

    /**
     * Formats a 1-based Pokédex number (1..151) into a 3-digit padded string (e.g. 1 -> "001", 151 -> "151").
     */
    fun formatPokedexId(pokedexNumber: Int): String {
        val clamped = pokedexNumber.coerceIn(1, 151)
        return when {
            clamped < 10 -> "00$clamped"
            clamped < 100 -> "0$clamped"
            else -> "$clamped"
        }
    }

    /**
     * Resolves the primary local asset URI for a front sprite (e.g. for Wild Target, Pokédex, or Dashboard).
     */
    fun getFrontSpriteUri(pokedexNumber: Int, isShiny: Boolean = false): String {
        val id = formatPokedexId(pokedexNumber)
        val filename = if (isShiny) "pokemon_shiny_$id.png" else "pokemon_$id.png"
        return "file:///android_asset/sprites/$filename"
    }

    /**
     * Resolves the primary local asset URI for a back sprite (authentic GBA battle stance for Player Buddy).
     */
    fun getBackSpriteUri(pokedexNumber: Int): String {
        val id = formatPokedexId(pokedexNumber)
        return "file:///android_asset/sprites/pokemon_back_$id.png"
    }

    /**
     * Official PokeAPI GitHub CDN fallback URL for front sprite.
     */
    fun getPokeApiFrontUrl(pokedexNumber: Int, isShiny: Boolean = false): String {
        val clamped = pokedexNumber.coerceIn(1, 151)
        val base = if (isShiny) BASE_POKEAPI_GEN3_SHINY else BASE_POKEAPI_GEN3_FRONT
        return "$base/$clamped.png"
    }

    /**
     * Official PokeAPI GitHub CDN fallback URL for back sprite.
     */
    fun getPokeApiBackUrl(pokedexNumber: Int): String {
        val clamped = pokedexNumber.coerceIn(1, 151)
        return "$BASE_POKEAPI_GEN3_BACK/$clamped.png"
    }
}

val Pokemon.frontSpriteUri: String
    get() = Gen3PokemonSpriteResolver.getFrontSpriteUri(pokedexNumber)

val Pokemon.backSpriteUri: String
    get() = Gen3PokemonSpriteResolver.getBackSpriteUri(pokedexNumber)

val Pokemon.shinySpriteUri: String
    get() = Gen3PokemonSpriteResolver.getFrontSpriteUri(pokedexNumber, isShiny = true)
