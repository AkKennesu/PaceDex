package com.poketch.core.data.remote

import com.poketch.core.data.local.dao.SavedFoodDao
import com.poketch.core.data.local.entity.SavedFoodEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Offline-First Barcode Lookup Client for OpenFoodFacts.
 *
 * Checks local Room DB cache first (0 ms latency, 100% offline).
 * On cache miss, queries OpenFoodFacts v2 product API with a 6-second timeout.
 * Parses and caches newly discovered products in [SavedFoodDao], or returns a clear failure.
 */
@Singleton
class OpenFoodFactsClient @Inject constructor(
    private val savedFoodDao: SavedFoodDao
) {

    suspend fun lookupBarcode(barcode: String): Result<SavedFoodEntity> = withContext(Dispatchers.IO) {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Barcode cannot be blank"))
        }

        // 1. Offline Cache First: check local Room DB immediately
        val cached = savedFoodDao.getByBarcode(cleanBarcode)
        if (cached != null) {
            return@withContext Result.success(cached)
        }

        // 2. Network Fallback: query OpenFoodFacts API v2
        runCatching {
            fetchFromOpenFoodFacts(cleanBarcode)
        }.mapCatching { entity ->
            // 3. Cache the parsed SavedFoodEntity in Room DB
            savedFoodDao.upsert(entity)
            entity
        }
    }

    private fun fetchFromOpenFoodFacts(barcode: String): SavedFoodEntity {
        val url = URL("https://world.openfoodfacts.org/api/v2/product/$barcode.json?fields=product_name,brands,nutriments,serving_size")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }

        try {
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("OpenFoodFacts returned HTTP $responseCode for barcode $barcode")
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            return parseOpenFoodFactsResponse(barcode, responseBody)
        } finally {
            connection.disconnect()
        }
    }

    fun parseOpenFoodFactsResponse(barcode: String, responseJson: String): SavedFoodEntity {
        val root = JSONObject(responseJson)
        val status = root.optInt("status", -1)
        if (status != 1) {
            val verbose = root.optString("status_verbose", "product not found")
            throw NoSuchElementException("Product with barcode $barcode not found on OpenFoodFacts (status=$status, $verbose)")
        }

        val product = root.optJSONObject("product")
            ?: throw NoSuchElementException("Product data missing for barcode $barcode")

        val productName = product.optString("product_name").trim().ifEmpty {
            "Scanned Food ($barcode)"
        }
        val brand = product.optString("brands").trim().takeIf { it.isNotEmpty() }
        val servingLabel = product.optString("serving_size").trim().ifEmpty {
            "1 serving"
        }

        val nutriments = product.optJSONObject("nutriments") ?: JSONObject()
        val energyKcal = parseNutrient(nutriments, "energy-kcal_serving", "energy-kcal_100g")
        val protein = parseNutrient(nutriments, "proteins_serving", "proteins_100g")
        val carbs = parseNutrient(nutriments, "carbohydrates_serving", "carbohydrates_100g")
        val fat = parseNutrient(nutriments, "fat_serving", "fat_100g")

        return SavedFoodEntity(
            foodId = barcode,
            name = productName,
            brand = brand,
            caloriesPerServing = energyKcal,
            proteinG = protein,
            carbsG = carbs,
            fatG = fat,
            servingLabel = servingLabel,
            isFavorite = false,
            useCount = 1,
            lastUsedMillis = System.currentTimeMillis()
        )
    }

    private fun parseNutrient(nutriments: JSONObject, primaryKey: String, fallbackKey: String): Int {
        val primaryVal = nutriments.optDouble(primaryKey, Double.NaN)
        if (!primaryVal.isNaN()) {
            return primaryVal.roundToInt().coerceAtLeast(0)
        }
        val fallbackVal = nutriments.optDouble(fallbackKey, Double.NaN)
        if (!fallbackVal.isNaN()) {
            return fallbackVal.roundToInt().coerceAtLeast(0)
        }
        if (primaryKey.startsWith("energy-kcal")) {
            val rawKcal = nutriments.optDouble("energy-kcal", Double.NaN)
            if (!rawKcal.isNaN()) {
                return rawKcal.roundToInt().coerceAtLeast(0)
            }
        }
        return 0
    }

    companion object {
        const val USER_AGENT = "PaceDex-Android/1.0"
        const val TIMEOUT_MS = 6_000
    }
}
