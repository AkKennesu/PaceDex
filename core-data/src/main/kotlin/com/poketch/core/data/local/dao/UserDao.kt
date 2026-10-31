package com.poketch.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.poketch.core.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun observeUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileOnce(): UserProfileEntity?

    @Upsert
    suspend fun upsertUserProfile(profile: UserProfileEntity)

    @Query("UPDATE user_profile SET activeBuddyPokemonId = :pokemonId, updatedAtEpochMillis = :timestamp WHERE id = 1")
    suspend fun updateActiveBuddy(pokemonId: Int, timestamp: Long)
}
