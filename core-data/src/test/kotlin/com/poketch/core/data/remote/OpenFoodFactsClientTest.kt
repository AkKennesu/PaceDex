package com.poketch.core.data.remote

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.poketch.core.data.local.dao.SavedFoodDao
import com.poketch.core.data.local.db.PoketchDatabase
import com.poketch.core.data.local.entity.SavedFoodEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class OpenFoodFactsClientTest {

    private lateinit var db: PoketchDatabase
    private lateinit var savedFoodDao: SavedFoodDao
    private lateinit var client: OpenFoodFactsClient

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PoketchDatabase::class.java
        ).allowMainThreadQueries().build()

        savedFoodDao = db.savedFoodDao()
        client = OpenFoodFactsClient(savedFoodDao)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun lookupBarcode_returnsOfflineCachedProductImmediately() = runBlocking {
        val barcode = "4800016644801"
        val cached = SavedFoodEntity(
            foodId = barcode,
            name = "SkyFlakes Crackers",
            brand = "Monde Nissin",
            caloriesPerServing = 130,
            proteinG = 3,
            carbsG = 20,
            fatG = 5,
            servingLabel = "3 crackers (28g)",
            isFavorite = true,
            useCount = 5,
            lastUsedMillis = 1000L
        )
        savedFoodDao.upsert(cached)

        val result = client.lookupBarcode(barcode)
        assertThat(result.isSuccess).isTrue()
        val food = result.getOrNull()
        assertThat(food).isNotNull()
        assertThat(food?.name).isEqualTo("SkyFlakes Crackers")
        assertThat(food?.caloriesPerServing).isEqualTo(130)
        assertThat(food?.proteinG).isEqualTo(3)
        assertThat(food?.isFavorite).isTrue()
    }

    @Test
    fun parseOpenFoodFactsResponse_prefersServingNutriments() {
        val barcode = "0888849000018"
        val json = """
            {
              "status": 1,
              "status_verbose": "product found",
              "product": {
                "product_name": "Quest Nutrition Protein Bar",
                "brands": "Quest",
                "serving_size": "60g (1 bar)",
                "nutriments": {
                  "energy-kcal_serving": 200,
                  "energy-kcal_100g": 333,
                  "proteins_serving": 21.4,
                  "proteins_100g": 35.6,
                  "carbohydrates_serving": 22.0,
                  "carbohydrates_100g": 36.6,
                  "fat_serving": 7.0,
                  "fat_100g": 11.6
                }
              }
            }
        """.trimIndent()

        val parsed = client.parseOpenFoodFactsResponse(barcode, json)
        assertThat(parsed.foodId).isEqualTo(barcode)
        assertThat(parsed.name).isEqualTo("Quest Nutrition Protein Bar")
        assertThat(parsed.brand).isEqualTo("Quest")
        assertThat(parsed.servingLabel).isEqualTo("60g (1 bar)")
        assertThat(parsed.caloriesPerServing).isEqualTo(200)
        assertThat(parsed.proteinG).isEqualTo(21)
        assertThat(parsed.carbsG).isEqualTo(22)
        assertThat(parsed.fatG).isEqualTo(7)
    }

    @Test
    fun parseOpenFoodFactsResponse_fallsBackTo100gNutriments() {
        val barcode = "1234567890123"
        val json = """
            {
              "status": 1,
              "status_verbose": "product found",
              "product": {
                "product_name": "Generic Greek Yogurt",
                "brands": "",
                "serving_size": "",
                "nutriments": {
                  "energy-kcal_100g": 85,
                  "proteins_100g": 10.3,
                  "carbohydrates_100g": 3.6,
                  "fat_100g": 0.2
                }
              }
            }
        """.trimIndent()

        val parsed = client.parseOpenFoodFactsResponse(barcode, json)
        assertThat(parsed.foodId).isEqualTo(barcode)
        assertThat(parsed.name).isEqualTo("Generic Greek Yogurt")
        assertThat(parsed.brand).isNull()
        assertThat(parsed.servingLabel).isEqualTo("1 serving")
        assertThat(parsed.caloriesPerServing).isEqualTo(85)
        assertThat(parsed.proteinG).isEqualTo(10)
        assertThat(parsed.carbsG).isEqualTo(4)
        assertThat(parsed.fatG).isEqualTo(0)
    }

    @Test
    fun parseOpenFoodFactsResponse_throwsWhenStatusNotOne() {
        val barcode = "0000000000000"
        val json = """
            {
              "status": 0,
              "status_verbose": "product not found"
            }
        """.trimIndent()

        val result = runCatching {
            client.parseOpenFoodFactsResponse(barcode, json)
        }
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(NoSuchElementException::class.java)
    }

    @Test
    fun lookupBarcode_failsWhenBarcodeIsBlank() = runBlocking {
        val result = client.lookupBarcode("   ")
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
    }
}
