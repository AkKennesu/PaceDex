package com.poketch.core.data.repository

import com.poketch.core.data.local.dao.SavedFoodDao
import com.poketch.core.data.local.entity.SavedFoodEntity
import com.poketch.core.data.remote.OpenFoodFactsClient
import com.poketch.core.domain.model.SavedFood
import com.poketch.core.domain.repository.SavedFoodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstSavedFoodRepository @Inject constructor(
    private val savedFoodDao: SavedFoodDao,
    private val openFoodFactsClient: OpenFoodFactsClient
) : SavedFoodRepository {

    override fun observeFavoritesAndRecents(limit: Int): Flow<List<SavedFood>> {
        return savedFoodDao.observeFavoritesAndRecents(limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeFavorites(): Flow<List<SavedFood>> {
        return savedFoodDao.observeFavorites().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeRecentFoods(limit: Int): Flow<List<SavedFood>> {
        return savedFoodDao.observeRecentFoods(limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun searchSavedFoods(query: String): Flow<List<SavedFood>> {
        return savedFoodDao.searchSavedFoods(query).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getFoodById(foodId: String): SavedFood? {
        val cached = savedFoodDao.getFoodById(foodId)?.toDomain()
        if (cached != null) return cached
        return openFoodFactsClient.lookupBarcode(foodId).getOrNull()?.toDomain()
    }

    override suspend fun getByBarcode(barcode: String): SavedFood? {
        return openFoodFactsClient.lookupBarcode(barcode).getOrNull()?.toDomain()
            ?: savedFoodDao.getByBarcode(barcode)?.toDomain()
    }

    override suspend fun saveFood(food: SavedFood) {
        savedFoodDao.upsert(food.toEntity())
    }

    override suspend fun toggleFavorite(foodId: String, isFavorite: Boolean) {
        savedFoodDao.toggleFavorite(foodId, isFavorite)
    }

    override suspend fun recordFoodUsage(foodId: String) {
        savedFoodDao.incrementUsage(foodId, System.currentTimeMillis())
    }

    override suspend fun deleteSavedFood(foodId: String) {
        savedFoodDao.deleteSavedFood(foodId)
    }

    private fun SavedFoodEntity.toDomain(): SavedFood = SavedFood(
        foodId = foodId,
        name = name,
        brand = brand,
        caloriesPerServing = caloriesPerServing,
        proteinG = proteinG,
        carbsG = carbsG,
        fatG = fatG,
        servingLabel = servingLabel,
        isFavorite = isFavorite,
        useCount = useCount,
        lastUsedMillis = lastUsedMillis
    )

    private fun SavedFood.toEntity(): SavedFoodEntity = SavedFoodEntity(
        foodId = foodId,
        name = name,
        brand = brand,
        caloriesPerServing = caloriesPerServing,
        proteinG = proteinG,
        carbsG = carbsG,
        fatG = fatG,
        servingLabel = servingLabel,
        isFavorite = isFavorite,
        useCount = useCount,
        lastUsedMillis = lastUsedMillis
    )
}
