package com.poketch.core.data.di

import com.poketch.core.data.repository.FirebaseAuthRepository
import com.poketch.core.data.repository.OfflineFirstCalorieRepository
import com.poketch.core.data.repository.OfflineFirstHabitRepository
import com.poketch.core.data.repository.OfflineFirstPokemonRepository
import com.poketch.core.data.repository.OfflineFirstUserRepository
import com.poketch.core.data.repository.OfflineWeatherRepository
import com.poketch.core.domain.repository.AuthRepository
import com.poketch.core.domain.repository.CalorieRepository
import com.poketch.core.domain.repository.HabitRepository
import com.poketch.core.domain.repository.PokemonRepository
import com.poketch.core.domain.repository.UserRepository
import com.poketch.core.domain.repository.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        repository: FirebaseAuthRepository
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        repository: OfflineFirstUserRepository
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindCalorieRepository(
        repository: OfflineFirstCalorieRepository
    ): CalorieRepository

    @Binds
    @Singleton
    abstract fun bindHabitRepository(
        repository: OfflineFirstHabitRepository
    ): HabitRepository

    @Binds
    @Singleton
    abstract fun bindPokemonRepository(
        repository: OfflineFirstPokemonRepository
    ): PokemonRepository

    @Binds
    @Singleton
    abstract fun bindWeatherRepository(
        repository: OfflineWeatherRepository
    ): WeatherRepository

    @Binds
    @Singleton
    abstract fun bindSavedFoodRepository(
        repository: com.poketch.core.data.repository.OfflineFirstSavedFoodRepository
    ): com.poketch.core.domain.repository.SavedFoodRepository
}
