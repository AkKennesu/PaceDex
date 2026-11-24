package com.poketch.core.domain.model

data class SavedFood(
    val foodId: String,          // Barcode string or UUID
    val name: String,
    val brand: String? = null,
    val caloriesPerServing: Int,
    val proteinG: Int = 0,
    val carbsG: Int = 0,
    val fatG: Int = 0,
    val servingLabel: String = "1 serving",
    val isFavorite: Boolean = false,
    val useCount: Int = 1,
    val lastUsedMillis: Long = System.currentTimeMillis()
)
