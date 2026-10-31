package com.poketch.core.data.di

import android.content.Context
import com.poketch.core.data.local.dao.CalorieDao
import com.poketch.core.data.local.dao.HabitDao
import com.poketch.core.data.local.dao.PokemonDao
import com.poketch.core.data.local.dao.UserDao
import com.poketch.core.data.local.db.PoketchDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providePoketchDatabase(
        @ApplicationContext context: Context
    ): PoketchDatabase {
        val db = PoketchDatabase.getInstance(context)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            db.ensureSeeded(context)
        }
        return db
    }

    @Provides
    fun provideUserDao(database: PoketchDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideCalorieDao(database: PoketchDatabase): CalorieDao {
        return database.calorieDao()
    }

    @Provides
    fun provideHabitDao(database: PoketchDatabase): HabitDao {
        return database.habitDao()
    }

    @Provides
    fun providePokemonDao(database: PoketchDatabase): PokemonDao {
        return database.pokemonDao()
    }
}
