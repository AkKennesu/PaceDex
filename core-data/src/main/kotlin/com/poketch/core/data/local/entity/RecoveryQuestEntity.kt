package com.poketch.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.poketch.core.domain.model.RecoveryQuest

@Entity(tableName = "recovery_quests")
data class RecoveryQuestEntity(
    @PrimaryKey val dateIso: String,
    val surplusKcal: Int,
    val targetOffsetSteps: Int,
    val startingStepsSnapshot: Int,
    val currentOffsetSteps: Int = 0,
    val isCompleted: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis(),
) {
    fun toDomain(): RecoveryQuest = RecoveryQuest(
        dateIso = dateIso,
        surplusKcal = surplusKcal,
        targetOffsetSteps = targetOffsetSteps,
        startingStepsSnapshot = startingStepsSnapshot,
        currentOffsetSteps = currentOffsetSteps,
        isCompleted = isCompleted,
    )

    companion object {
        fun fromDomain(quest: RecoveryQuest, updatedAtMillis: Long = System.currentTimeMillis()): RecoveryQuestEntity =
            RecoveryQuestEntity(
                dateIso = quest.dateIso,
                surplusKcal = quest.surplusKcal,
                targetOffsetSteps = quest.targetOffsetSteps,
                startingStepsSnapshot = quest.startingStepsSnapshot,
                currentOffsetSteps = quest.currentOffsetSteps,
                isCompleted = quest.isCompleted,
                updatedAtMillis = updatedAtMillis,
            )
    }
}
