package com.poketch.core.domain.repository

import com.poketch.core.domain.model.Pokemon
import com.poketch.core.domain.model.PokemonCategory
import kotlinx.coroutines.flow.Flow

interface PokemonRepository {
    fun observeAllPokemon(): Flow<List<Pokemon>>
    fun observePokemonById(pokedexNumber: Int): Flow<Pokemon?>
    suspend fun getPokemonByCategory(category: PokemonCategory): List<Pokemon>
    suspend fun getPokemonByRuleCode(ruleCode: String): List<Pokemon>
    suspend fun getUnlockedCount(): Int
    suspend fun updateProgress(pokedexNumber: Int, newProgress: Int): Pokemon?
    suspend fun unlockPokemon(pokedexNumber: Int, timestampMillis: Long): Pokemon?
    suspend fun upsertPokemon(pokemon: Pokemon)
    suspend fun upsertAllPokemon(pokemonList: List<Pokemon>)
}
