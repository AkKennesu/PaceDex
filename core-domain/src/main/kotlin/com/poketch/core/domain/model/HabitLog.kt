package com.poketch.core.domain.model

data class HabitLog(
    val logId: String,
    val dateIso: String,
    val habitType: HabitType,
    val numericValue: Double,
    val durationSeconds: Int = 0,
    val sourceDevice: DeviceSource,
    val timestampMillis: Long,
    val isSynced: Boolean = false
) {
    init {
        require(logId.isNotBlank()) { "logId must not be blank" }
        require(dateIso.isNotBlank()) { "dateIso must not be blank" }
        require(numericValue > 0.0) { "numericValue must be strictly positive, got: $numericValue" }
        require(durationSeconds >= 0) { "durationSeconds must be non-negative, got: $durationSeconds" }
    }
}
