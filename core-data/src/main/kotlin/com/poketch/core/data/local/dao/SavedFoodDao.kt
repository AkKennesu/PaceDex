package com.poketch.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.poketch.core.data.local.entity.SavedFoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedFoodDao {

    @Query("SELECT * FROM saved_foods ORDER BY isFavorite DESC, lastUsedMillis DESC LIMIT :limit")
    fun observeFavoritesAndRecents(limit: Int = 12): Flow<List<SavedFoodEntity>>

    @Query("SELECT * FROM saved_foods WHERE foodId = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): SavedFoodEntity?

    @Upsert
    suspend fun upsert(food: SavedFoodEntity)

    @Query("UPDATE saved_foods SET isFavorite = :isFavorite WHERE foodId = :foodId")
    suspend fun toggleFavorite(foodId: String, isFavorite: Boolean)

    @Query("UPDATE saved_foods SET useCount = useCount + 1, lastUsedMillis = :nowMillis WHERE foodId = :foodId")
    suspend fun incrementUsage(foodId: String, nowMillis: Long)

    // Supporting queries & helpers for database seeding and repository
    @Query("SELECT * FROM saved_foods WHERE isFavorite = 1 ORDER BY useCount DESC, lastUsedMillis DESC")
    fun observeFavorites(): Flow<List<SavedFoodEntity>>

    @Query("SELECT * FROM saved_foods ORDER BY lastUsedMillis DESC LIMIT :limit")
    fun observeRecentFoods(limit: Int = 10): Flow<List<SavedFoodEntity>>

    @Query("SELECT * FROM saved_foods ORDER BY lastUsedMillis DESC")
    fun observeAllSavedFoods(): Flow<List<SavedFoodEntity>>

    @Query("SELECT * FROM saved_foods WHERE name LIKE '%' || :query || '%' OR (brand IS NOT NULL AND brand LIKE '%' || :query || '%') ORDER BY lastUsedMillis DESC")
    fun searchSavedFoods(query: String): Flow<List<SavedFoodEntity>>

    @Query("SELECT * FROM saved_foods WHERE foodId = :foodId LIMIT 1")
    suspend fun getFoodById(foodId: String): SavedFoodEntity?

    @Query("SELECT COUNT(*) FROM saved_foods")
    suspend fun getCount(): Int

    @Upsert
    suspend fun upsertSavedFood(food: SavedFoodEntity)

    @Upsert
    suspend fun upsertAllSavedFoods(foods: List<SavedFoodEntity>)

    @Query("UPDATE saved_foods SET useCount = useCount + 1, lastUsedMillis = :timestampMillis WHERE foodId = :foodId")
    suspend fun recordFoodUsage(foodId: String, timestampMillis: Long = System.currentTimeMillis())

    @Query("DELETE FROM saved_foods WHERE foodId = :foodId")
    suspend fun deleteSavedFood(foodId: String)
}
