package com.poketch.core.domain.engine

import com.google.common.truth.Truth.assertThat
import com.poketch.core.domain.model.KantoEvolutionChains
import com.poketch.core.domain.model.Pokemon
import com.poketch.core.domain.model.PokemonCategory
import com.poketch.core.domain.model.UnlockRule
import org.junit.Test

class KantoEvolutionChainsTest {

    private fun makePokemon(
        id: Int,
        name: String,
        isUnlocked: Boolean = false,
        progress: Int = 0,
        threshold: Int = 1
    ): Pokemon {
        return Pokemon(
            pokedexNumber = id,
            name = name,
            primaryType = "Normal",
            unlockRule = UnlockRule("RULE_$id", PokemonCategory.STARTER, "Desc", threshold),
            currentProgress = progress,
            isUnlocked = isUnlocked,
            spriteAssetPath = "sprites/pokemon_$id.png"
        )
    }

    @Test
    fun `starter evolution chains are mapped correctly across all 3 stages`() {
        // Bulbasaur line
        assertThat(KantoEvolutionChains.getPreEvolutionId(1)).isNull()
        assertThat(KantoEvolutionChains.getPreEvolutionId(2)).isEqualTo(1)
        assertThat(KantoEvolutionChains.getPreEvolutionId(3)).isEqualTo(2)
        assertThat(KantoEvolutionChains.getEvolutionStage(1)).isEqualTo(1)
        assertThat(KantoEvolutionChains.getEvolutionStage(2)).isEqualTo(2)
        assertThat(KantoEvolutionChains.getEvolutionStage(3)).isEqualTo(3)

        // Charmander line
        assertThat(KantoEvolutionChains.getPreEvolutionId(4)).isNull()
        assertThat(KantoEvolutionChains.getPreEvolutionId(5)).isEqualTo(4)
        assertThat(KantoEvolutionChains.getPreEvolutionId(6)).isEqualTo(5)

        // Squirtle line
        assertThat(KantoEvolutionChains.getPreEvolutionId(7)).isNull()
        assertThat(KantoEvolutionChains.getPreEvolutionId(8)).isEqualTo(7)
        assertThat(KantoEvolutionChains.getPreEvolutionId(9)).isEqualTo(8)
    }

    @Test
    fun `hydration and psychic evolution lines map correctly`() {
        // Magikarp -> Gyarados
        assertThat(KantoEvolutionChains.getPreEvolutionId(130)).isEqualTo(129)
        assertThat(KantoEvolutionChains.isEvolution(130)).isTrue()
        assertThat(KantoEvolutionChains.isEvolution(129)).isFalse()

        // Abra -> Kadabra -> Alakazam
        assertThat(KantoEvolutionChains.getPreEvolutionId(64)).isEqualTo(63)
        assertThat(KantoEvolutionChains.getPreEvolutionId(65)).isEqualTo(64)
        assertThat(KantoEvolutionChains.getEvolutionStage(65)).isEqualTo(3)

        // Gastly -> Haunter -> Gengar
        assertThat(KantoEvolutionChains.getPreEvolutionId(93)).isEqualTo(92)
        assertThat(KantoEvolutionChains.getPreEvolutionId(94)).isEqualTo(93)

        // Clefairy -> Clefable
        assertThat(KantoEvolutionChains.getPreEvolutionId(36)).isEqualTo(35)

        // Jigglypuff -> Wigglytuff
        assertThat(KantoEvolutionChains.getPreEvolutionId(40)).isEqualTo(39)
    }

    @Test
    fun `engine marks unlocked evolutions with isEvolution and preEvolution`() {
        val catalog = listOf(
            makePokemon(1, "Bulbasaur", isUnlocked = true, progress = 1, threshold = 1),
            makePokemon(2, "Ivysaur", isUnlocked = false, progress = 0, threshold = 7),
            makePokemon(3, "Venusaur", isUnlocked = false, progress = 0, threshold = 21)
        )

        val context = UnlockTriggerContext(
            starterChoiceId = 1,
            currentLoggingStreakDays = 7
        )

        val result = PokemonUnlockEngine.evaluate(catalog, context, nowMillis = 1000L)

        val ivysaurUnlock = result.newlyUnlockedEvents.find { it.pokemon.pokedexNumber == 2 }
        assertThat(ivysaurUnlock).isNotNull()
        assertThat(ivysaurUnlock!!.isEvolution).isTrue()
        assertThat(ivysaurUnlock.preEvolution?.pokedexNumber).isEqualTo(1)
        assertThat(ivysaurUnlock.preEvolution?.name).isEqualTo("Bulbasaur")
    }

    @Test
    fun `sleep sessions unlock sleep and recovery pokemon including Jigglypuff Clefairy and Gastly line`() {
        val catalog = listOf(
            makePokemon(35, "Clefairy", threshold = 5),
            makePokemon(39, "Jigglypuff", threshold = 7),
            makePokemon(92, "Gastly", threshold = 2),
            makePokemon(93, "Haunter", threshold = 5),
            makePokemon(94, "Gengar", threshold = 14)
        )

        // 14 days of sleep logged
        val context = UnlockTriggerContext(
            hasSleepSessionToday = true,
            todaySleepHours = 8.5,
            sleepStreakDays = 14
        )

        val result = PokemonUnlockEngine.evaluate(catalog, context, nowMillis = 2000L)

        val unlockedIds = result.newlyUnlockedEvents.map { it.pokemon.pokedexNumber }
        assertThat(unlockedIds).contains(35) // Clefairy
        assertThat(unlockedIds).contains(39) // Jigglypuff
        assertThat(unlockedIds).contains(92) // Gastly
        assertThat(unlockedIds).contains(93) // Haunter
        assertThat(unlockedIds).contains(94) // Gengar

        // Gengar should be an evolution from Haunter
        val gengarUnlock = result.newlyUnlockedEvents.find { it.pokemon.pokedexNumber == 94 }
        assertThat(gengarUnlock?.isEvolution).isTrue()
        assertThat(gengarUnlock?.preEvolution?.pokedexNumber).isEqualTo(93)
    }
}
