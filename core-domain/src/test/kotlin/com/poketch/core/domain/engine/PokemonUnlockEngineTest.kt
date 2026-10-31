package com.poketch.core.domain.engine

import com.google.common.truth.Truth.assertThat
import com.poketch.core.domain.model.Pokemon
import com.poketch.core.domain.model.PokemonCategory
import com.poketch.core.domain.model.UnlockRule
import org.junit.Test

class PokemonUnlockEngineTest {

    private fun createStandardCatalog(): List<Pokemon> {
        val thresholds = mapOf(
            1 to 1, 2 to 7, 3 to 21,
            4 to 1, 5 to 7, 6 to 21,
            7 to 1, 8 to 7, 9 to 21,
            16 to 5000, 17 to 10000, 18 to 15000,
            54 to 1, 55 to 5,
            60 to 4, 61 to 7, 62 to 1,
            63 to 7, 64 to 14, 65 to 30,
            73 to 25000, 77 to 1, 78 to 5,
            86 to 2000, 87 to 14, 91 to 21,
            95 to 20000, 99 to 3000,
            104 to 1, 106 to 10,
            113 to 3, 117 to 50000, 118 to 1, 119 to 12,
            121 to 7, 125 to 3, 128 to 2,
            129 to 7, 130 to 30, 131 to 100000,
            133 to 1, 134 to 25, 135 to 15,
            143 to 3, 144 to 30, 145 to 50, 146 to 30,
            149 to 26, 150 to 149, 151 to 150
        )
        val names = mapOf(
            1 to "Bulbasaur", 2 to "Ivysaur", 3 to "Venusaur",
            4 to "Charmander", 5 to "Charmeleon", 6 to "Charizard",
            7 to "Squirtle", 8 to "Wartortle", 9 to "Blastoise",
            16 to "Pidgey", 17 to "Pidgeotto", 18 to "Pidgeot",
            54 to "Psyduck", 55 to "Golduck",
            63 to "Abra", 64 to "Kadabra", 65 to "Alakazam",
            77 to "Ponyta", 78 to "Rapidash",
            106 to "Hitmonlee",
            113 to "Chansey",
            129 to "Magikarp", 130 to "Gyarados", 131 to "Lapras",
            133 to "Eevee",
            143 to "Snorlax", 144 to "Articuno", 145 to "Zapdos", 146 to "Moltres",
            149 to "Dragonite", 150 to "Mewtwo", 151 to "Mew"
        )
        return (1..151).map { id ->
            val cat = when (id) {
                in 1..9, 133 -> PokemonCategory.STARTER
                in listOf(54, 55, 60, 61, 62, 72, 73, 86, 87, 90, 91, 98, 99, 116, 117, 118, 119, 120, 121, 129, 130, 131, 134) -> PokemonCategory.WATER_HYDRATION
                in listOf(16, 17, 18, 19, 20, 21, 22, 25, 26, 50, 51, 52, 53, 56, 57, 58, 59, 66, 67, 68, 77, 78, 81, 82, 84, 85, 95, 100, 101, 106, 107, 111, 112, 115, 123, 125, 126, 127, 128, 135, 136, 147, 148) -> PokemonCategory.RUN_STEPS
                in listOf(27, 28, 32, 33, 34, 37, 38, 43, 44, 45, 46, 47, 63, 64, 65, 71, 74, 75, 76, 83, 88, 89, 102, 103, 104, 105, 108, 113, 114, 122, 132, 137, 143) -> PokemonCategory.CALORIE_DEFICIT
                in listOf(10, 11, 12, 13, 14, 15, 23, 24, 29, 30, 31, 35, 36, 39, 40, 41, 42, 48, 49, 79, 80, 92, 93, 94, 96, 97, 109, 110, 124) -> PokemonCategory.SLEEP_STREAK
                else -> PokemonCategory.LEGENDARY
            }
            Pokemon(
                pokedexNumber = id,
                name = names[id] ?: "Pokemon-$id",
                primaryType = "Normal",
                secondaryType = null,
                unlockRule = UnlockRule(
                    ruleCode = "RULE_$id",
                    category = cat,
                    description = "Unlock Pokemon $id",
                    targetThreshold = thresholds[id] ?: 10
                ),
                currentProgress = 0,
                isUnlocked = false,
                unlockedAtMillis = null,
                spriteAssetPath = "sprites/pokemon_${id.toString().padStart(3, '0')}.png"
            )
        }
    }

    @Test
    fun onboardingStarterSelection_unlocksSelectedStarterAndLeavesOthersLocked() {
        val catalog = createStandardCatalog()
        val nowMillis = 1730000000000L

        // Day 1: Choose Bulbasaur (#001)
        val contextDay1 = UnlockTriggerContext(
            starterChoiceId = 1,
            currentLoggingStreakDays = 1
        )
        val resultDay1 = PokemonUnlockEngine.evaluate(catalog, contextDay1, nowMillis)
        val catalogDay1 = resultDay1.updatedCatalog.associateBy { it.pokedexNumber }

        // Bulbasaur (#001) is unlocked
        assertThat(catalogDay1[1]?.isUnlocked).isTrue()
        assertThat(catalogDay1[1]?.unlockedAtMillis).isEqualTo(nowMillis)
        // Charmander (#004) and Squirtle (#007) remain locked
        assertThat(catalogDay1[4]?.isUnlocked).isFalse()
        assertThat(catalogDay1[7]?.isUnlocked).isFalse()
        // Evolutions (#002, #003) remain locked
        assertThat(catalogDay1[2]?.isUnlocked).isFalse()
        assertThat(catalogDay1[3]?.isUnlocked).isFalse()

        // Day 7: Advance streak to 7 days
        val contextDay7 = UnlockTriggerContext(
            starterChoiceId = 1,
            currentLoggingStreakDays = 7
        )
        val resultDay7 = PokemonUnlockEngine.evaluate(resultDay1.updatedCatalog, contextDay7, nowMillis + 60000L)
        val catalogDay7 = resultDay7.updatedCatalog.associateBy { it.pokedexNumber }

        // Ivysaur (#002) is now unlocked
        assertThat(catalogDay7[2]?.isUnlocked).isTrue()
        assertThat(resultDay7.newlyUnlockedEvents.map { it.pokemon.pokedexNumber }).contains(2)
        // Venusaur (#003) is still locked, with progress advanced to 7 / 21
        assertThat(catalogDay7[3]?.isUnlocked).isFalse()
        assertThat(catalogDay7[3]?.currentProgress).isEqualTo(7)

        // Day 21: Advance streak to 21 days
        val contextDay21 = UnlockTriggerContext(
            starterChoiceId = 1,
            currentLoggingStreakDays = 21
        )
        val resultDay21 = PokemonUnlockEngine.evaluate(resultDay7.updatedCatalog, contextDay21, nowMillis + 120000L)
        val catalogDay21 = resultDay21.updatedCatalog.associateBy { it.pokedexNumber }

        // Venusaur (#003) is now unlocked
        assertThat(catalogDay21[3]?.isUnlocked).isTrue()
        assertThat(resultDay21.newlyUnlockedEvents.map { it.pokemon.pokedexNumber }).contains(3)
        // Charmander (#004) and Squirtle (#007) are still locked (unlocked at 30 days)
        assertThat(catalogDay21[4]?.isUnlocked).isFalse()
        assertThat(catalogDay21[7]?.isUnlocked).isFalse()
    }

    @Test
    fun hydrationMilestones_unlockPsyduckMagikarpGyaradosAndLapras() {
        val catalog = createStandardCatalog()
        val nowMillis = 1730000000000L

        // 1. Hit daily water goal -> unlocks #054 Psyduck
        val contextGoal = UnlockTriggerContext(
            todayWaterMl = 2500,
            dailyWaterGoalMl = 2500,
            waterGoalStreakDays = 1
        )
        val resultGoal = PokemonUnlockEngine.evaluate(catalog, contextGoal, nowMillis)
        val mapGoal = resultGoal.updatedCatalog.associateBy { it.pokedexNumber }
        assertThat(mapGoal[54]?.isUnlocked).isTrue()
        assertThat(mapGoal[129]?.isUnlocked).isFalse() // Magikarp still locked

        // 2. 7-day hydration streak -> unlocks #129 Magikarp
        val contextStreak7 = contextGoal.copy(waterGoalStreakDays = 7)
        val resultStreak7 = PokemonUnlockEngine.evaluate(resultGoal.updatedCatalog, contextStreak7, nowMillis + 1000L)
        val mapStreak7 = resultStreak7.updatedCatalog.associateBy { it.pokedexNumber }
        assertThat(mapStreak7[129]?.isUnlocked).isTrue()
        assertThat(mapStreak7[130]?.isUnlocked).isFalse() // Gyarados still locked

        // 3. 30-day hydration streak -> unlocks #130 Gyarados and #144 Articuno
        val contextStreak30 = contextStreak7.copy(waterGoalStreakDays = 30)
        val resultStreak30 = PokemonUnlockEngine.evaluate(resultStreak7.updatedCatalog, contextStreak30, nowMillis + 2000L)
        val mapStreak30 = resultStreak30.updatedCatalog.associateBy { it.pokedexNumber }
        assertThat(mapStreak30[130]?.isUnlocked).isTrue()
        assertThat(mapStreak30[144]?.isUnlocked).isTrue()
        assertThat(resultStreak30.newlyUnlockedEvents.map { it.pokemon.pokedexNumber }).containsAtLeast(130, 144)
        assertThat(mapStreak30[131]?.isUnlocked).isFalse() // Lapras still locked

        // 4. 100L lifetime water -> unlocks #131 Lapras
        val context100L = contextStreak30.copy(lifetimeWaterLiters = 100.0)
        val result100L = PokemonUnlockEngine.evaluate(resultStreak30.updatedCatalog, context100L, nowMillis + 3000L)
        val map100L = result100L.updatedCatalog.associateBy { it.pokedexNumber }
        assertThat(map100L[131]?.isUnlocked).isTrue()
        assertThat(result100L.newlyUnlockedEvents.map { it.pokemon.pokedexNumber }).contains(131)
    }

    @Test
    fun runningAndSteps_completingOneMileRun_unlocksPonyta077() {
        val catalog = createStandardCatalog()
        val nowMillis = 1730000000000L

        val context = UnlockTriggerContext(
            latestRunDistanceMiles = 1.02
        )
        val result = PokemonUnlockEngine.evaluate(catalog, context, nowMillis)
        val ponyta = result.updatedCatalog.first { it.pokedexNumber == 77 }

        assertThat(ponyta.isUnlocked).isTrue()
        assertThat(ponyta.unlockedAtMillis).isEqualTo(nowMillis)

        val unlockEvent = result.newlyUnlockedEvents.firstOrNull { it.pokemon.pokedexNumber == 77 }
        assertThat(unlockEvent).isNotNull()
        assertThat(unlockEvent?.triggerReason).isEqualTo("Completed first 1-mile run")
    }

    @Test
    fun calorieDiscipline_stayingUnderTargetCeiling_unlocksAbraLineAndChansey() {
        val catalog = createStandardCatalog()
        val nowMillis = 1730000000000L

        // Log 3 meals while strictly within calorie target, on day 7 of streak
        val contextGood = UnlockTriggerContext(
            todayMealsLoggedCount = 3,
            isWithinCalorieTargetToday = true,
            calorieDeficitStreakDays = 7
        )
        val resultGood = PokemonUnlockEngine.evaluate(catalog, contextGood, nowMillis)
        val mapGood = resultGood.updatedCatalog.associateBy { it.pokedexNumber }

        // #113 Chansey (3 meals) and #063 Abra (7-day streak) are unlocked
        assertThat(mapGood[113]?.isUnlocked).isTrue()
        assertThat(mapGood[63]?.isUnlocked).isTrue()

        // #064 Kadabra (requires 14 days) advanced progress to 7 but remains locked
        assertThat(mapGood[64]?.isUnlocked).isFalse()
        assertThat(mapGood[64]?.currentProgress).isEqualTo(7)

        // Exceeding target ceiling (isWithinCalorieTargetToday = false, streak reset to 0)
        val contextSurplus = UnlockTriggerContext(
            todayMealsLoggedCount = 3,
            isWithinCalorieTargetToday = false,
            calorieDeficitStreakDays = 0
        )
        val resultSurplus = PokemonUnlockEngine.evaluate(resultGood.updatedCatalog, contextSurplus, nowMillis + 1000L)
        val mapSurplus = resultSurplus.updatedCatalog.associateBy { it.pokedexNumber }

        // #064 Kadabra halted progression, remains at 7 and locked
        assertThat(mapSurplus[64]?.isUnlocked).isFalse()
        assertThat(mapSurplus[64]?.currentProgress).isEqualTo(7)
        assertThat(resultSurplus.newlyUnlockedEvents.map { it.pokemon.pokedexNumber }).doesNotContain(64)
    }

    @Test
    fun completingPokedex001To150_automaticallyUnlocks151Mew() {
        val standardCatalog = createStandardCatalog()
        val nowMillis = 1730000000000L

        // Prepare catalog where #001 to #148 are already unlocked, and #149, #150, #151 are locked
        val preparedCatalog = standardCatalog.map { p ->
            if (p.pokedexNumber in 1..148) {
                p.copy(isUnlocked = true, currentProgress = p.unlockRule.targetThreshold, unlockedAtMillis = nowMillis - 1000L)
            } else {
                p
            }
        }

        // Trigger unlocking #149 Dragonite (cumulative 26.2 marathon miles)
        val context = UnlockTriggerContext(
            cumulativeMilesRun = 26.2
        )
        val result = PokemonUnlockEngine.evaluate(preparedCatalog, context, nowMillis)
        val mapResult = result.updatedCatalog.associateBy { it.pokedexNumber }

        // #149 Dragonite unlocked
        assertThat(mapResult[149]?.isUnlocked).isTrue()
        // #150 Mewtwo unlocked (149 total Pokémon unlocked)
        assertThat(mapResult[150]?.isUnlocked).isTrue()
        // In the same pass, once #001 to #150 are unlocked, #151 Mew unlocks!
        assertThat(mapResult[151]?.isUnlocked).isTrue()
        assertThat(result.newlyUnlockedEvents.map { it.pokemon.pokedexNumber }).containsAtLeast(149, 150, 151)
    }

    @Test
    fun monotonicGuarantee_unlockedPokemonNeverRelockWhenDailyStatsReset() {
        val catalog = createStandardCatalog()
        val nowMillis = 1730000000000L

        // Day 1: Walk 15,000 steps -> unlocks #016 Pidgey (5k), #017 Pidgeotto (10k), #018 Pidgeot (15k)
        val contextDay1 = UnlockTriggerContext(todaySteps = 15000)
        val resultDay1 = PokemonUnlockEngine.evaluate(catalog, contextDay1, nowMillis)
        val mapDay1 = resultDay1.updatedCatalog.associateBy { it.pokedexNumber }

        assertThat(mapDay1[16]?.isUnlocked).isTrue()
        assertThat(mapDay1[17]?.isUnlocked).isTrue()
        assertThat(mapDay1[18]?.isUnlocked).isTrue()

        // Day 2: Next day stats reset to zero
        val contextDay2 = UnlockTriggerContext(
            todaySteps = 0,
            latestRunDistanceMiles = 0.0,
            todayWaterMl = 0
        )
        val resultDay2 = PokemonUnlockEngine.evaluate(resultDay1.updatedCatalog, contextDay2, nowMillis + 86400000L)
        val mapDay2 = resultDay2.updatedCatalog.associateBy { it.pokedexNumber }

        // Monotonic guarantee: never re-locks, progress never decreases
        assertThat(mapDay2[16]?.isUnlocked).isTrue()
        assertThat(mapDay2[17]?.isUnlocked).isTrue()
        assertThat(mapDay2[18]?.isUnlocked).isTrue()
        assertThat(mapDay2[16]?.currentProgress).isAtLeast(5000)
        assertThat(mapDay2[17]?.currentProgress).isAtLeast(10000)
        assertThat(mapDay2[18]?.currentProgress).isAtLeast(15000)

        // Does not re-emit unlock events for already unlocked Pokémon
        assertThat(resultDay2.newlyUnlockedEvents.map { it.pokemon.pokedexNumber }).doesNotContain(16)
        assertThat(resultDay2.newlyUnlockedEvents.map { it.pokemon.pokedexNumber }).doesNotContain(17)
        assertThat(resultDay2.newlyUnlockedEvents.map { it.pokemon.pokedexNumber }).doesNotContain(18)
    }
}
