package com.poketch.core.domain.repository

import com.poketch.core.domain.model.SavedFood
import kotlinx.coroutines.flow.Flow

interface SavedFoodRepository {
    fun observeFavoritesAndRecents(limit: Int = 12): Flow<List<SavedFood>>
    fun observeFavorites(): Flow<List<SavedFood>>
    fun observeRecentFoods(limit: Int = 10): Flow<List<SavedFood>>
    fun searchSavedFoods(query: String): Flow<List<SavedFood>>
    suspend fun getFoodById(foodId: String): SavedFood?
    suspend fun getByBarcode(barcode: String): SavedFood? = getFoodById(barcode)
    suspend fun saveFood(food: SavedFood)
    suspend fun toggleFavorite(foodId: String, isFavorite: Boolean)
    suspend fun recordFoodUsage(foodId: String)
    suspend fun deleteSavedFood(foodId: String)
}
