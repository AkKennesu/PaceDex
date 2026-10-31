package com.poketch.core.data.repository

import com.poketch.core.data.local.dao.PokemonDao
import com.poketch.core.data.local.entity.toDomain
import com.poketch.core.data.local.entity.toEntity
import com.poketch.core.domain.model.Pokemon
import com.poketch.core.domain.model.PokemonCategory
import com.poketch.core.domain.repository.PokemonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstPokemonRepository @Inject constructor(
    private val pokemonDao: PokemonDao
) : PokemonRepository {

    override fun observeAllPokemon(): Flow<List<Pokemon>> {
        return pokemonDao.observeAllPokemon().map { list -> list.map { it.toDomain() } }
    }

    override fun observePokemonById(pokedexNumber: Int): Flow<Pokemon?> {
        return pokemonDao.observePokemonById(pokedexNumber).map { it?.toDomain() }
    }

    override suspend fun getPokemonByCategory(category: PokemonCategory): List<Pokemon> {
        return pokemonDao.getPokemonByCategory(category.name).map { it.toDomain() }
    }

    override suspend fun getPokemonByRuleCode(ruleCode: String): List<Pokemon> {
        return pokemonDao.getPokemonByRuleCode(ruleCode).map { it.toDomain() }
    }

    override suspend fun getUnlockedCount(): Int {
        return pokemonDao.getUnlockedCount()
    }

    override suspend fun updateProgress(pokedexNumber: Int, newProgress: Int): Pokemon? {
        val existing = pokemonDao.observePokemonById(pokedexNumber).firstOrNull()?.toDomain() ?: return null
        val updatedProgress = maxOf(existing.currentProgress, newProgress)
        val isNowUnlocked = existing.isUnlocked || (updatedProgress >= existing.unlockRule.targetThreshold)
        val unlockTime = if (isNowUnlocked && existing.unlockedAtMillis == null) {
            System.currentTimeMillis()
        } else {
            existing.unlockedAtMillis
        }

        val updatedPokemon = existing.copy(
            currentProgress = updatedProgress,
            isUnlocked = isNowUnlocked,
            unlockedAtMillis = unlockTime
        )

        pokemonDao.upsertPokemon(updatedPokemon.toEntity())
        return updatedPokemon
    }

    override suspend fun unlockPokemon(pokedexNumber: Int, timestampMillis: Long): Pokemon? {
        val existing = pokemonDao.observePokemonById(pokedexNumber).firstOrNull()?.toDomain() ?: return null
        val updated = existing.copy(
            currentProgress = maxOf(existing.currentProgress, existing.unlockRule.targetThreshold),
            isUnlocked = true,
            unlockedAtMillis = existing.unlockedAtMillis ?: timestampMillis
        )
        pokemonDao.upsertPokemon(updated.toEntity())
        return updated
    }

    override suspend fun upsertPokemon(pokemon: Pokemon) {
        pokemonDao.upsertPokemon(pokemon.toEntity())
    }

    override suspend fun upsertAllPokemon(pokemonList: List<Pokemon>) {
        pokemonDao.upsertAllPokemon(pokemonList.map { it.toEntity() })
    }
}
