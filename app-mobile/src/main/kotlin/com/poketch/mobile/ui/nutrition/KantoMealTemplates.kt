package com.poketch.mobile.ui.nutrition

import com.poketch.core.domain.model.MealCategory

data class KantoMealTemplate(
    val name: String,
    val category: MealCategory,
    val caloriesKcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val description: String,
    val emoji: String
)

object KantoMealTemplates {
    val ALL: List<KantoMealTemplate> = listOf(
        KantoMealTemplate(
            name = "Fresh Moomoo Milk",
            category = MealCategory.BREAKFAST,
            caloriesKcal = 150,
            proteinG = 10,
            carbsG = 12,
            fatG = 7,
            description = "Rich, creamy milk full of wholesome calcium & protein.",
            emoji = "🥛"
        ),
        KantoMealTemplate(
            name = "Slowpoke Tail Broth",
            category = MealCategory.LUNCH,
            caloriesKcal = 420,
            proteinG = 42,
            carbsG = 12,
            fatG = 14,
            description = "Nutritious, high-protein delicacy slow-simmered for recovery.",
            emoji = "🍲"
        ),
        KantoMealTemplate(
            name = "Fisherman Bento",
            category = MealCategory.DINNER,
            caloriesKcal = 580,
            proteinG = 46,
            carbsG = 65,
            fatG = 15,
            description = "Fresh Vermilion Port catch with whole-grain sticky rice.",
            emoji = "🍱"
        ),
        KantoMealTemplate(
            name = "Lava Cookie",
            category = MealCategory.SNACK,
            caloriesKcal = 220,
            proteinG = 4,
            carbsG = 32,
            fatG = 9,
            description = "Crunchy Lavaridge specialty that cures all status ailments.",
            emoji = "🍪"
        ),
        KantoMealTemplate(
            name = "Rage Candy Bar",
            category = MealCategory.SNACK,
            caloriesKcal = 310,
            proteinG = 6,
            carbsG = 45,
            fatG = 12,
            description = "Iconic Lake of Rage treat offering a quick energy boost.",
            emoji = "🍫"
        ),
        KantoMealTemplate(
            name = "Berry Juice Blend",
            category = MealCategory.SNACK,
            caloriesKcal = 95,
            proteinG = 1,
            carbsG = 23,
            fatG = 0,
            description = "100% natural fermented berry blend for fast hydration.",
            emoji = "🧃"
        )
    )
}
