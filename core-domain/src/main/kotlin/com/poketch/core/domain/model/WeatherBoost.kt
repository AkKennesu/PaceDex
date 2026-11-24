package com.poketch.core.domain.model

data class WeatherBoost(
    val boostType: String,
    val chipLabel: String,
    val iconEmoji: String,
    val boostedTypesDescription: String
) {
    companion object {
        fun from(weatherCode: Int?, isNightWindow: Boolean, partnerType: String? = null): WeatherBoost {
            return when {
                weatherCode != null && (weatherCode in 51..67 || weatherCode in 80..82 || weatherCode in 95..99) -> {
                    val typeLabel = when {
                        partnerType.equals("Electric", ignoreCase = true) -> "Electric"
                        else -> "Water"
                    }
                    WeatherBoost(
                        boostType = "WATER_ELECTRIC",
                        chipLabel = "🌧 Rain Boost Active: 2× $typeLabel Progress",
                        iconEmoji = "🌧",
                        boostedTypesDescription = "Water & Electric"
                    )
                }
                isNightWindow -> {
                    val typeLabel = when {
                        partnerType.equals("Fairy", ignoreCase = true) -> "Fairy"
                        else -> "Ghost"
                    }
                    WeatherBoost(
                        boostType = "GHOST_FAIRY",
                        chipLabel = "🌙 Night Boost Active: 2× $typeLabel Progress",
                        iconEmoji = "🌙",
                        boostedTypesDescription = "Ghost & Fairy"
                    )
                }
                else -> {
                    val typeLabel = when {
                        partnerType.equals("Fire", ignoreCase = true) -> "Fire"
                        else -> "Grass"
                    }
                    WeatherBoost(
                        boostType = "FIRE_GRASS",
                        chipLabel = "☀️ Sunny Boost Active: 2× $typeLabel Progress",
                        iconEmoji = "☀️",
                        boostedTypesDescription = "Fire & Grass"
                    )
                }
            }
        }
    }
}
