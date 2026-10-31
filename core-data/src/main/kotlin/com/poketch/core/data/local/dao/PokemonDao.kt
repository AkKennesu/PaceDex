package com.poketch.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.poketch.core.data.local.entity.PokemonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PokemonDao {

    @Query("SELECT * FROM pokedex_151 ORDER BY pokedexNumber ASC")
    fun observeAllPokemon(): Flow<List<PokemonEntity>>

    @Query("SELECT * FROM pokedex_151 WHERE pokedexNumber = :pokedexNumber LIMIT 1")
    fun observePokemonById(pokedexNumber: Int): Flow<PokemonEntity?>

    @Query("SELECT * FROM pokedex_151 WHERE categoryTag = :category ORDER BY pokedexNumber ASC")
    suspend fun getPokemonByCategory(category: String): List<PokemonEntity>

    @Query("SELECT * FROM pokedex_151 WHERE unlockRuleCode = :ruleCode")
    suspend fun getPokemonByRuleCode(ruleCode: String): List<PokemonEntity>

    @Query("SELECT COUNT(*) FROM pokedex_151 WHERE isUnlocked = 1")
    suspend fun getUnlockedCount(): Int

    @Upsert
    suspend fun upsertAllPokemon(pokemon: List<PokemonEntity>)

    @Upsert
    suspend fun upsertPokemon(pokemon: PokemonEntity)
}
