package com.poketch.core.domain.engine

import com.poketch.core.domain.model.Pokemon

sealed interface UnlockEvent {
    data class NewPokemonUnlocked(
        val pokemon: Pokemon,
        val triggerReason: String,
        val timestampMillis: Long
    ) : UnlockEvent

    data class ProgressAdvanced(
        val pokedexNumber: Int,
        val previousProgress: Int,
        val newProgress: Int,
        val targetThreshold: Int
    ) : UnlockEvent
}

data class UnlockEvaluationResult(
    val updatedCatalog: List<Pokemon>,
    val newlyUnlockedEvents: List<UnlockEvent.NewPokemonUnlocked>,
    val progressEvents: List<UnlockEvent.ProgressAdvanced>
)
