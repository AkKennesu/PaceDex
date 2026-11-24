package com.poketch.core.domain.model

import kotlin.math.roundToInt

data class RecoveryQuest(
    val dateIso: String,
    val surplusKcal: Int,
    val targetOffsetSteps: Int,       // Calculated as (surplusKcal / 0.045).roundToInt().coerceIn(800, 8000)
    val startingStepsSnapshot: Int,   // User's step count when the overage happened
    val currentOffsetSteps: Int = 0,
    val isCompleted: Boolean = false
) {
    val progressFraction: Float
        get() = if (targetOffsetSteps <= 0) 1f else (currentOffsetSteps.toFloat() / targetOffsetSteps).coerceIn(0f, 1f)

    val remainingSteps: Int
        get() = maxOf(0, targetOffsetSteps - currentOffsetSteps)

    companion object {
        fun calculateTargetOffsetSteps(surplusKcal: Int): Int {
            return (surplusKcal / 0.045).roundToInt().coerceIn(800, 8000)
        }

        fun create(
            dateIso: String,
            surplusKcal: Int,
            startingStepsSnapshot: Int,
            currentTotalSteps: Int = startingStepsSnapshot
        ): RecoveryQuest {
            val target = calculateTargetOffsetSteps(surplusKcal)
            val offset = maxOf(0, currentTotalSteps - startingStepsSnapshot)
            return RecoveryQuest(
                dateIso = dateIso,
                surplusKcal = surplusKcal,
                targetOffsetSteps = target,
                startingStepsSnapshot = startingStepsSnapshot,
                currentOffsetSteps = offset,
                isCompleted = offset >= target
            )
        }
    }
}
