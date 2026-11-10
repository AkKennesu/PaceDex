package com.poketch.wear.watchface

import com.poketch.core.domain.repository.CalorieRepository
import com.poketch.core.domain.repository.HabitRepository
import com.poketch.core.domain.repository.PokemonRepository
import com.poketch.core.domain.repository.UserRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PoketchWatchFaceEntryPoint {
    fun userRepository(): UserRepository
    fun calorieRepository(): CalorieRepository
    fun habitRepository(): HabitRepository
    fun pokemonRepository(): PokemonRepository
    fun weatherRepository(): com.poketch.core.domain.repository.WeatherRepository
}
