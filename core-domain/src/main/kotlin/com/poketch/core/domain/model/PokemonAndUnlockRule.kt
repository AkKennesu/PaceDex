package com.poketch.core.domain.model

enum class PokemonCategory {
    STARTER,
    WATER_HYDRATION,
    RUN_STEPS,
    CALORIE_DEFICIT,
    SLEEP_STREAK,
    LEGENDARY
}

data class UnlockRule(
    val ruleCode: String,
    val category: PokemonCategory,
    val description: String,
    val targetThreshold: Int
) {
    init {
        require(ruleCode.isNotBlank()) { "ruleCode must not be blank" }
        require(description.isNotBlank()) { "description must not be blank" }
        require(targetThreshold > 0) { "targetThreshold must be positive, got: $targetThreshold" }
    }
}

data class Pokemon(
    val pokedexNumber: Int,
    val name: String,
    val primaryType: String,
    val secondaryType: String? = null,
    val unlockRule: UnlockRule,
    val currentProgress: Int = 0,
    val isUnlocked: Boolean = false,
    val unlockedAtMillis: Long? = null,
    val spriteAssetPath: String
) {
    init {
        require(pokedexNumber in 1..151) { "pokedexNumber must be between 1 and 151 (Classic Gen-I Kanto), got: $pokedexNumber" }
        require(name.isNotBlank()) { "name must not be blank" }
        require(primaryType.isNotBlank()) { "primaryType must not be blank" }
        require(currentProgress >= 0) { "currentProgress must be non-negative, got: $currentProgress" }
        require(spriteAssetPath.isNotBlank()) { "spriteAssetPath must not be blank" }
    }

    val progressRatio: Double
        get() = (currentProgress.toDouble() / unlockRule.targetThreshold).coerceIn(0.0, 1.0)
}
