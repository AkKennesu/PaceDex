package com.poketch.core.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.poketch.core.data.local.dao.CalorieDao
import com.poketch.core.data.local.dao.HabitDao
import com.poketch.core.data.local.dao.HourlyWeatherCacheDao
import com.poketch.core.data.local.dao.PokemonDao
import com.poketch.core.data.local.dao.UserDao
import com.poketch.core.data.local.entity.CalorieLogEntity
import com.poketch.core.data.local.entity.HabitLogEntity
import com.poketch.core.data.local.entity.HourlyWeatherCacheEntity
import com.poketch.core.data.local.entity.PokemonEntity
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
        HourlyWeatherCacheEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class PoketchDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun calorieDao(): CalorieDao
    abstract fun habitDao(): HabitDao
    abstract fun pokemonDao(): PokemonDao
    abstract fun hourlyWeatherCacheDao(): HourlyWeatherCacheDao

    suspend fun ensureSeeded(context: Context?) {
        val count = pokemonDao().getUnlockedCount()
        val starters = pokemonDao().getPokemonByCategory("STARTER")
        if (starters.isEmpty() && count == 0) {
            val seedList = PokemonSeedParser.loadPokemonSeed(context)
            pokemonDao().upsertAllPokemon(seedList)
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
            ).fallbackToDestructiveMigration()
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
