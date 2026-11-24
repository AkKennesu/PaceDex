package com.poketch.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.poketch.core.data.local.entity.RecoveryQuestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecoveryQuestDao {
    @Query("SELECT * FROM recovery_quests WHERE dateIso = :dateIso LIMIT 1")
    fun observeQuestByDate(dateIso: String): Flow<RecoveryQuestEntity?>

    @Query("SELECT * FROM recovery_quests WHERE dateIso = :dateIso LIMIT 1")
    suspend fun getQuestByDate(dateIso: String): RecoveryQuestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuest(quest: RecoveryQuestEntity)

    @Query("UPDATE recovery_quests SET currentOffsetSteps = :currentOffsetSteps, isCompleted = :isCompleted, updatedAtMillis = :updatedAtMillis WHERE dateIso = :dateIso")
    suspend fun updateProgress(
        dateIso: String,
        currentOffsetSteps: Int,
        isCompleted: Boolean,
        updatedAtMillis: Long = System.currentTimeMillis(),
    )

    @Query("DELETE FROM recovery_quests WHERE dateIso = :dateIso")
    suspend fun deleteQuest(dateIso: String)

    @Query("SELECT COUNT(*) FROM recovery_quests")
    suspend fun getCount(): Int
}
