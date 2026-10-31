package com.poketch.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.poketch.core.domain.model.Pokemon
import com.poketch.core.domain.model.PokemonCategory
import com.poketch.core.domain.model.UnlockRule

@Entity(
    tableName = "pokedex_151",
    indices = [
        Index(value = ["categoryTag"]),
        Index(value = ["unlockRuleCode"])
    ]
)
data class PokemonEntity(
    @PrimaryKey val pokedexNumber: Int, // 1 to 151
    val name: String,
    val primaryType: String,
    val secondaryType: String?,
    val categoryTag: String, // "STARTER" | "WATER_HYDRATION" | "RUN_STEPS" | "CALORIE_DEFICIT" | "SLEEP_STREAK" | "LEGENDARY"
    val unlockRuleCode: String,
    val unlockDescription: String,
    val requiredProgress: Int,
    val currentProgress: Int = 0,
    val isUnlocked: Boolean = false,
    val unlockedAtMillis: Long? = null,
    val spriteAssetPath: String
)

fun PokemonEntity.toDomain(): Pokemon {
    return Pokemon(
        pokedexNumber = pokedexNumber,
        name = name,
        primaryType = primaryType,
        secondaryType = secondaryType,
        unlockRule = UnlockRule(
            ruleCode = unlockRuleCode,
            category = PokemonCategory.valueOf(categoryTag),
            description = unlockDescription,
            targetThreshold = requiredProgress
        ),
        currentProgress = currentProgress,
        isUnlocked = isUnlocked,
        unlockedAtMillis = unlockedAtMillis,
        spriteAssetPath = spriteAssetPath
    )
}

fun Pokemon.toEntity(): PokemonEntity {
    return PokemonEntity(
        pokedexNumber = pokedexNumber,
        name = name,
        primaryType = primaryType,
        secondaryType = secondaryType,
        categoryTag = unlockRule.category.name,
        unlockRuleCode = unlockRule.ruleCode,
        unlockDescription = unlockRule.description,
        requiredProgress = unlockRule.targetThreshold,
        currentProgress = currentProgress,
        isUnlocked = isUnlocked,
        unlockedAtMillis = unlockedAtMillis,
        spriteAssetPath = spriteAssetPath
    )
}
