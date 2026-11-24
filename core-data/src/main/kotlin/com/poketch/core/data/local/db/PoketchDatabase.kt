package com.poketch.core.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.poketch.core.data.local.dao.CalorieDao
import com.poketch.core.data.local.dao.HabitDao
import com.poketch.core.data.local.dao.HourlyWeatherCacheDao
import com.poketch.core.data.local.dao.PokemonDao
import com.poketch.core.data.local.dao.RecoveryQuestDao
import com.poketch.core.data.local.dao.SavedFoodDao
import com.poketch.core.data.local.dao.UserDao
import com.poketch.core.data.local.entity.CalorieLogEntity
import com.poketch.core.data.local.entity.HabitLogEntity
import com.poketch.core.data.local.entity.HourlyWeatherCacheEntity
import com.poketch.core.data.local.entity.PokemonEntity
import com.poketch.core.data.local.entity.RecoveryQuestEntity
import com.poketch.core.data.local.entity.SavedFoodEntity
import com.poketch.core.data.local.entity.UserProfileEntity
import com.poketch.core.data.local.seed.PokemonSeedParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfileEntity::class,
        CalorieLogEntity::class,
        HabitLogEntity::class,
        PokemonEntity::class,
        HourlyWeatherCacheEntity::class,
        SavedFoodEntity::class,
        RecoveryQuestEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class PoketchDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun calorieDao(): CalorieDao
    abstract fun habitDao(): HabitDao
    abstract fun pokemonDao(): PokemonDao
    abstract fun hourlyWeatherCacheDao(): HourlyWeatherCacheDao
    abstract fun savedFoodDao(): SavedFoodDao
    abstract fun recoveryQuestDao(): RecoveryQuestDao

    suspend fun ensureSeeded(context: Context?) {
        val count = pokemonDao().getUnlockedCount()
        val starters = pokemonDao().getPokemonByCategory("STARTER")
        if (starters.isEmpty() && count == 0) {
            val seedList = PokemonSeedParser.loadPokemonSeed(context)
            pokemonDao().upsertAllPokemon(seedList)
        }
        if (savedFoodDao().getCount() == 0) {
            val defaultFavorites = listOf(
                SavedFoodEntity(
                    foodId = "fav_oatmeal_whey",
                    name = "Oatmeal with Whey",
                    brand = "Trainer Fuel",
                    caloriesPerServing = 360,
                    proteinG = 28,
                    carbsG = 48,
                    fatG = 6,
                    servingLabel = "1 bowl",
                    isFavorite = true,
                    useCount = 5,
                    lastUsedMillis = System.currentTimeMillis() - 3600_000
                ),
                SavedFoodEntity(
                    foodId = "fav_chicken_bowl",
                    name = "Grilled Chicken Bowl",
                    brand = "Meal Prep",
                    caloriesPerServing = 520,
                    proteinG = 44,
                    carbsG = 55,
                    fatG = 12,
                    servingLabel = "1 bowl",
                    isFavorite = true,
                    useCount = 4,
                    lastUsedMillis = System.currentTimeMillis() - 7200_000
                ),
                SavedFoodEntity(
                    foodId = "fav_greek_yogurt",
                    name = "Greek Yogurt Parfait",
                    brand = "Chobani",
                    caloriesPerServing = 240,
                    proteinG = 22,
                    carbsG = 30,
                    fatG = 3,
                    servingLabel = "1 cup",
                    isFavorite = true,
                    useCount = 3,
                    lastUsedMillis = System.currentTimeMillis() - 10800_000
                ),
                SavedFoodEntity(
                    foodId = "fav_boiled_eggs",
                    name = "Boiled Eggs (2 Large)",
                    brand = "Farm Fresh",
                    caloriesPerServing = 140,
                    proteinG = 12,
                    carbsG = 1,
                    fatG = 10,
                    servingLabel = "2 eggs",
                    isFavorite = true,
                    useCount = 2,
                    lastUsedMillis = System.currentTimeMillis() - 14400_000
                ),
                SavedFoodEntity(
                    foodId = "0888849000018",
                    name = "Quest Bar Chocolate Chip",
                    brand = "Quest Nutrition",
                    caloriesPerServing = 200,
                    proteinG = 21,
                    carbsG = 22,
                    fatG = 7,
                    servingLabel = "1 bar (60g)",
                    isFavorite = true,
                    useCount = 1,
                    lastUsedMillis = System.currentTimeMillis() - 18000_000
                )
            )
            savedFoodDao().upsertAllSavedFoods(defaultFavorites)
        }
        if (userDao().getUserProfileOnce() == null) {
            val defaultProfile = UserProfileEntity(
                id = 1,
                ageYears = 25,
                sex = "MALE",
                heightCm = 175.0,
                weightKg = 70.0,
                activityLevel = "MODERATELY_ACTIVE",
                deficitTier = "STANDARD",
                bmrKcal = 1650,
                tdeeKcal = 2300,
                dailyDeficitGoalKcal = 500,
                targetCalorieCeiling = 1800,
                dailyWaterGoalMl = 2500,
                activeBuddyPokemonId = 1,
                updatedAtEpochMillis = System.currentTimeMillis()
            )
            userDao().upsertUserProfile(defaultProfile)
        }
    }

    companion object {
        const val DATABASE_NAME = "poketch.db"

        /**
         * Migration from Database version 2 to version 3.
         * Creates the `saved_foods` table for offline barcode food scanning & 1-tap favorites,
         * preserving all existing user profiles, calorie logs, habits, and pokedex progress.
         */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `saved_foods` (
                        `foodId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `brand` TEXT,
                        `caloriesPerServing` INTEGER NOT NULL,
                        `proteinG` INTEGER NOT NULL,
                        `carbsG` INTEGER NOT NULL,
                        `fatG` INTEGER NOT NULL,
                        `servingLabel` TEXT NOT NULL,
                        `isFavorite` INTEGER NOT NULL,
                        `useCount` INTEGER NOT NULL,
                        `lastUsedMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`foodId`)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Migration from Database version 3 to version 4.
         * Creates the `recovery_quests` table for persisting Playable Over-Target Recovery Quests,
         * preserving all existing user profiles, calorie logs, habits, pokedex progress, and saved foods.
         */
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recovery_quests` (
                        `dateIso` TEXT NOT NULL,
                        `surplusKcal` INTEGER NOT NULL,
                        `targetOffsetSteps` INTEGER NOT NULL,
                        `startingStepsSnapshot` INTEGER NOT NULL,
                        `currentOffsetSteps` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`dateIso`)
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var instance: PoketchDatabase? = null

        fun getInstance(
            context: Context,
            coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
        ): PoketchDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context, coroutineScope).also { instance = it }
            }
        }

        private fun buildDatabase(
            context: Context,
            coroutineScope: CoroutineScope
        ): PoketchDatabase {
            lateinit var database: PoketchDatabase
            database = Room.databaseBuilder(
                context.applicationContext,
                PoketchDatabase::class.java,
                DATABASE_NAME
            )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .addCallback(object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    coroutineScope.launch {
                        val seedList = PokemonSeedParser.loadPokemonSeed(context.applicationContext)
                        database.pokemonDao().upsertAllPokemon(seedList)
                    }
                }
            }).build()
            return database
        }
    }
}
