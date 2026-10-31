package com.poketch.core.domain.calculator

sealed interface CalorieBudgetState {
    val consumedKcal: Int
    val targetCeilingKcal: Int
    val consumptionRatio: Double

    data class OnTrack(
        override val consumedKcal: Int,
        override val targetCeilingKcal: Int,
        val remainingKcal: Int,
        override val consumptionRatio: Double
    ) : CalorieBudgetState

    data class ApproachingCeiling(
        override val consumedKcal: Int,
        override val targetCeilingKcal: Int,
        val remainingKcal: Int,
        override val consumptionRatio: Double
    ) : CalorieBudgetState

    data class OverTargetWarning(
        override val consumedKcal: Int,
        override val targetCeilingKcal: Int,
        val surplusKcal: Int,
        val suggestedOffsetSteps: Int,
        val suggestedOffsetRunMiles: Double,
        override val consumptionRatio: Double
    ) : CalorieBudgetState
}
