package com.poketch.core.domain.model

data class EvolutionStageInfo(
    val stage: Int, // 1 for Basic, 2 for Stage 1, 3 for Stage 2
    val totalStages: Int, // 2 or 3 (or 1 for non-evolving)
    val preEvolutionPokedexNumber: Int?,
    val evolutionFamilyName: String
)

object KantoEvolutionChains {

    /**
     * Complete mapping of all 151 Classic Kanto Gen-1 evolutions:
     * maps each evolved Pokemon's Pokedex number to its direct pre-evolution's Pokedex number.
     */
    val PRE_EVOLUTION_MAP: Map<Int, Int> = mapOf(
        // Starters (#001 - #009)
        2 to 1,   // Ivysaur <- Bulbasaur
        3 to 2,   // Venusaur <- Ivysaur
        5 to 4,   // Charmeleon <- Charmander
        6 to 5,   // Charizard <- Charmeleon
        8 to 7,   // Wartortle <- Squirtle
        9 to 8,   // Blastoise <- Wartortle

        // Early Forest Bugs & Birds (#010 - #028)
        11 to 10, // Metapod <- Caterpie
        12 to 11, // Butterfree <- Metapod
        14 to 13, // Kakuna <- Weedle
        15 to 14, // Beedrill <- Kakuna
        17 to 16, // Pidgeotto <- Pidgey
        18 to 17, // Pidgeot <- Pidgeotto
        20 to 19, // Raticate <- Rattata
        22 to 21, // Fearow <- Spearow
        24 to 23, // Arbok <- Ekans
        26 to 25, // Raichu <- Pikachu
        28 to 27, // Sandslash <- Sandshrew

        // Poison, Fairy, Fire & Bat lines (#029 - #045)
        30 to 29, // Nidorina <- Nidoran♀
        31 to 30, // Nidoqueen <- Nidorina
        33 to 32, // Nidorino <- Nidoran♂
        34 to 33, // Nidoking <- Nidorino
        36 to 35, // Clefable <- Clefairy
        38 to 37, // Ninetales <- Vulpix
        40 to 39, // Wigglytuff <- Jigglypuff
        42 to 41, // Golbat <- Zubat
        44 to 43, // Gloom <- Oddish
        45 to 44, // Vileplume <- Gloom

        // Bugs, Moles, Cats, Water & Fighting (#046 - #068)
        47 to 46, // Parasect <- Paras
        49 to 48, // Venomoth <- Venonat
        51 to 50, // Dugtrio <- Diglett
        53 to 52, // Persian <- Meowth
        55 to 54, // Golduck <- Psyduck
        57 to 56, // Primeape <- Mankey
        59 to 58, // Arcanine <- Growlithe
        61 to 60, // Poliwhirl <- Poliwag
        62 to 61, // Poliwrath <- Poliwhirl
        64 to 63, // Kadabra <- Abra
        65 to 64, // Alakazam <- Kadabra
        67 to 66, // Machoke <- Machop
        68 to 67, // Machamp <- Machoke

        // Grass, Jellyfish, Rock, Horse & Psychic lines (#069 - #094)
        70 to 69, // Weepinbell <- Bellsprout
        71 to 70, // Victreebel <- Weepinbell
        73 to 72, // Tentacruel <- Tentacool
        75 to 74, // Graveler <- Geodude
        76 to 75, // Golem <- Graveler
        78 to 77, // Rapidash <- Ponyta
        80 to 79, // Slowbro <- Slowpoke
        82 to 81, // Magneton <- Magnemite
        85 to 84, // Dodrio <- Doduo
        87 to 86, // Dewgong <- Seel
        89 to 88, // Muk <- Grimer
        91 to 90, // Cloyster <- Shellder
        93 to 92, // Haunter <- Gastly
        94 to 93, // Gengar <- Haunter

        // Sleep, Crabs, Electric & Bone lines (#095 - #112)
        97 to 96,   // Hypno <- Drowzee
        99 to 98,   // Kingler <- Krabby
        101 to 100, // Electrode <- Voltorb
        103 to 102, // Exeggutor <- Exeggcute
        105 to 104, // Marowak <- Cubone
        110 to 109, // Weezing <- Koffing
        112 to 111, // Rhydon <- Rhyhorn

        // Water, Starfish & Gyarados (#113 - #130)
        117 to 116, // Seadra <- Horsea
        119 to 118, // Seaking <- Goldeen
        121 to 120, // Starmie <- Staryu
        130 to 129, // Gyarados <- Magikarp

        // Eevee branched evolutions (#133 -> #134, #135, #136)
        134 to 133, // Vaporeon <- Eevee
        135 to 133, // Jolteon <- Eevee
        136 to 133, // Flareon <- Eevee

        // Fossils & Dragons (#138 - #149)
        139 to 138, // Omastar <- Omanyte
        141 to 140, // Kabutops <- Kabuto
        148 to 147, // Dragonair <- Dratini
        149 to 148  // Dragonite <- Dragonair
    )

    fun getPreEvolutionId(pokedexNumber: Int): Int? = PRE_EVOLUTION_MAP[pokedexNumber]

    fun isEvolution(pokedexNumber: Int): Boolean = PRE_EVOLUTION_MAP.containsKey(pokedexNumber)

    fun getEvolutionStage(pokedexNumber: Int): Int {
        val pre1 = PRE_EVOLUTION_MAP[pokedexNumber] ?: return 1
        val pre2 = PRE_EVOLUTION_MAP[pre1] ?: return 2
        return 3
    }
}
