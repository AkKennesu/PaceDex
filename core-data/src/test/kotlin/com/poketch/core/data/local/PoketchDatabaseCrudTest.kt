package com.poketch.core.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.poketch.core.data.local.dao.CalorieDao
import com.poketch.core.data.local.dao.HabitDao
import com.poketch.core.data.local.dao.PokemonDao
import com.poketch.core.data.local.dao.UserDao
import com.poketch.core.data.local.db.PoketchDatabase
import com.poketch.core.data.local.entity.CalorieLogEntity
import com.poketch.core.data.local.entity.HabitLogEntity
import com.poketch.core.data.local.entity.SavedFoodEntity
import com.poketch.core.data.local.seed.PokemonSeedData
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class PoketchDatabaseCrudTest {

    private lateinit var db: PoketchDatabase
    private lateinit var userDao: UserDao
    private lateinit var calorieDao: CalorieDao
    private lateinit var habitDao: HabitDao
    private lateinit var pokemonDao: PokemonDao

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PoketchDatabase::class.java
        ).allowMainThreadQueries().build()

        userDao = db.userDao()
        calorieDao = db.calorieDao()
        habitDao = db.habitDao()
        pokemonDao = db.pokemonDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun pokemonSeed_all151EntriesSeededAndQueryableByCategoryAndRule() = runBlocking {
        // Seed database
        pokemonDao.upsertAllPokemon(PokemonSeedData.ALL_151)

        val all = pokemonDao.observeAllPokemon().first()
        assertThat(all).hasSize(151)

        // Verify #001 Bulbasaur and #151 Mew
        val bulbasaur = pokemonDao.observePokemonById(1).first()
        assertThat(bulbasaur).isNotNull()
        assertThat(bulbasaur?.name).isEqualTo("Bulbasaur")
        assertThat(bulbasaur?.categoryTag).isEqualTo("STARTER")

        val mew = pokemonDao.observePokemonById(151).first()
        assertThat(mew).isNotNull()
        assertThat(mew?.name).isEqualTo("Mew")
        assertThat(mew?.categoryTag).isEqualTo("LEGENDARY")

        // Query by category
        val starters = pokemonDao.getPokemonByCategory("STARTER")
        assertThat(starters).hasSize(10) // 1..9 + #133 Eevee

        val waterHydration = pokemonDao.getPokemonByCategory("WATER_HYDRATION")
        assertThat(waterHydration.size).isAtLeast(20)

        // Query by rule
        val ponyta = pokemonDao.getPokemonByRuleCode("RUN_1_MILE_ONCE")
        assertThat(ponyta).hasSize(1)
        assertThat(ponyta[0].name).isEqualTo("Ponyta")
    }

    @Test
    fun calorieDao_idempotentUpsert_updatesInPlaceWithoutDuplication() = runBlocking {
        val testDate = "2026-10-30"
        val logId = "test-log-uuid-1"

        val initialEntry = CalorieLogEntity(
            logId = logId,
            dateIso = testDate,
            mealCategory = "LUNCH",
            foodDescription = "Salad",
            caloriesKcal = 350,
            proteinG = 20,
            carbsG = 15,
            fatG = 10,
            sourceDevice = "MOBILE",
            timestampMillis = 1000L,
            isSynced = false
        )

        calorieDao.upsertCalorieLog(initialEntry)

        var total = calorieDao.getTotalCaloriesByDateOnce(testDate)
        assertThat(total).isEqualTo(350)

        // Upsert same UUID with updated calories (350 -> 450)
        val updatedEntry = initialEntry.copy(caloriesKcal = 450)
        calorieDao.upsertCalorieLog(updatedEntry)

        // Total should be 450, NOT 800 (no duplicate row)
        total = calorieDao.getTotalCaloriesByDateOnce(testDate)
        assertThat(total).isEqualTo(450)

        val logs = calorieDao.observeLogsByDate(testDate).first()
        assertThat(logs).hasSize(1)
        assertThat(logs[0].caloriesKcal).isEqualTo(450)
    }

    @Test
    fun habitDao_idempotentUpsert_updatesInPlaceWithoutDuplication() = runBlocking {
        val testDate = "2026-10-30"
        val habitId = "habit-water-uuid-1"

        val initialHabit = HabitLogEntity(
            logId = habitId,
            dateIso = testDate,
            habitType = "WATER_ML",
            numericValue = 250.0,
            durationSeconds = 0,
            sourceDevice = "WEAR",
            timestampMillis = 1000L,
            isSynced = false
        )

        habitDao.upsertHabitLog(initialHabit)

        var dailyWater = habitDao.observeDailyHabitTotal(testDate, "WATER_ML").first()
        assertThat(dailyWater).isEqualTo(250.0)

        // Re-inserting same UUID with new value (e.g., corrected from 250 to 500)
        val updatedHabit = initialHabit.copy(numericValue = 500.0)
        habitDao.upsertHabitLog(updatedHabit)

        // Daily sum must be 500, not 750
        dailyWater = habitDao.observeDailyHabitTotal(testDate, "WATER_ML").first()
        assertThat(dailyWater).isEqualTo(500.0)

        val lifetimeWater = habitDao.getLifetimeHabitTotal("WATER_ML")
        assertThat(lifetimeWater).isEqualTo(500.0)
    }

    @Test
    fun observeTotalCaloriesByDate_emitsExactSumsAfterInsertAndDelete() = runBlocking {
        val dateIso = "2026-10-30"

        val log1 = CalorieLogEntity(
            logId = "log-1",
            dateIso = dateIso,
            mealCategory = "BREAKFAST",
            foodDescription = "Oatmeal",
            caloriesKcal = 300,
            sourceDevice = "MOBILE",
            timestampMillis = 1000L
        )

        val log2 = CalorieLogEntity(
            logId = "log-2",
            dateIso = dateIso,
            mealCategory = "LUNCH",
            foodDescription = "Chicken Rice",
            caloriesKcal = 650,
            sourceDevice = "WEAR",
            timestampMillis = 2000L
        )

        calorieDao.upsertCalorieLog(log1)
        assertThat(calorieDao.observeTotalCaloriesByDate(dateIso).first()).isEqualTo(300)

        calorieDao.upsertCalorieLog(log2)
        assertThat(calorieDao.observeTotalCaloriesByDate(dateIso).first()).isEqualTo(950)

        // Delete log1
        calorieDao.deleteCalorieLog(log1.logId)
        assertThat(calorieDao.observeTotalCaloriesByDate(dateIso).first()).isEqualTo(650)
    }

    @Test
    fun migration_2_3_createsSavedFoodsTablePreservingSchema() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(ApplicationProvider.getApplicationContext())
                .name("test_migration.db")
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) {}
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        val writableDb = helper.writableDatabase
        PoketchDatabase.MIGRATION_2_3.migrate(writableDb)

        val cursor = writableDb.query("SELECT * FROM saved_foods")
        assertThat(cursor).isNotNull()
        assertThat(cursor.columnCount).isEqualTo(11)
        cursor.close()
        writableDb.close()
    }

    @Test
    fun savedFoodDao_upsertAndObserveFavoritesAndRecents() = runBlocking {
        val savedFoodDao = db.savedFoodDao()
        val food1 = SavedFoodEntity(
            foodId = "012345678901",
            name = "Protein Bar",
            brand = "Quest",
            caloriesPerServing = 200,
            proteinG = 20,
            carbsG = 22,
            fatG = 7,
            isFavorite = true,
            useCount = 1,
            lastUsedMillis = 1000L
        )
        val food2 = SavedFoodEntity(
            foodId = "fav_apple",
            name = "Honeycrisp Apple",
            caloriesPerServing = 95,
            proteinG = 0,
            carbsG = 25,
            fatG = 0,
            isFavorite = false,
            useCount = 3,
            lastUsedMillis = 2000L
        )

        savedFoodDao.upsert(food1)
        savedFoodDao.upsert(food2)

        val list = savedFoodDao.observeFavoritesAndRecents(10).first()
        assertThat(list).hasSize(2)
        // food1 is favorite, so it should be first
        assertThat(list[0].foodId).isEqualTo("012345678901")
        assertThat(list[0].isFavorite).isTrue()

        // Lookup by barcode
        val barcodeLookup = savedFoodDao.getByBarcode("012345678901")
        assertThat(barcodeLookup).isNotNull()
        assertThat(barcodeLookup?.name).isEqualTo("Protein Bar")

        // Increment usage
        savedFoodDao.incrementUsage("012345678901", 3000L)
        val updated = savedFoodDao.getByBarcode("012345678901")
        assertThat(updated?.useCount).isEqualTo(2)
        assertThat(updated?.lastUsedMillis).isEqualTo(3000L)

        // Toggle favorite
        savedFoodDao.toggleFavorite("012345678901", false)
        val unfavorited = savedFoodDao.getByBarcode("012345678901")
        assertThat(unfavorited?.isFavorite).isFalse()
    }
}
