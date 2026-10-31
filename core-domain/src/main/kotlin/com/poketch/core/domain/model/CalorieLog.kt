package com.poketch.core.domain.model

data class CalorieLog(
    val logId: String,
    val dateIso: String,
    val mealCategory: MealCategory,
    val foodDescription: String,
    val caloriesKcal: Int,
    val proteinG: Int = 0,
    val carbsG: Int = 0,
    val fatG: Int = 0,
    val sourceDevice: DeviceSource,
    val timestampMillis: Long,
    val isSynced: Boolean = false
) {
    init {
        require(logId.isNotBlank()) { "logId must not be blank" }
        require(dateIso.isNotBlank()) { "dateIso must not be blank" }
        require(caloriesKcal > 0) { "caloriesKcal must be positive, got: $caloriesKcal" }
        require(proteinG >= 0) { "proteinG must be non-negative, got: $proteinG" }
        require(carbsG >= 0) { "carbsG must be non-negative, got: $carbsG" }
        require(fatG >= 0) { "fatG must be non-negative, got: $fatG" }
    }
}
