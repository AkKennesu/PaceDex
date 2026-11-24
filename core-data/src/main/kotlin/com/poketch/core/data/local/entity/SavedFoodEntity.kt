package com.poketch.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_foods")
data class SavedFoodEntity(
    @PrimaryKey val foodId: String,          // Barcode string (e.g. "4800016644801") or UUID for custom favorites
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
