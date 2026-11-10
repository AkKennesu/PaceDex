package com.poketch.core.data.repository

object WeatherCodeMapper {
    /**
     * Maps WMO weather code to a compact 3/4-character GBA HUD label.
     */
    fun mapWmoToConditionShort(wmoCode: Int, isDaytime: Boolean = true): String {
        return when (wmoCode) {
            0, 1 -> if (isDaytime) "SUN" else "CLR"
            2, 3 -> "CLDY"
            45, 48 -> "FOG"
            in 51..67, in 80..82 -> "RAIN"
            in 71..77, 85, 86 -> "SNOW"
            in 95..99 -> "STRM"
            else -> if (isDaytime) "SUN" else "CLR"
        }
    }

    /**
     * Maps WMO weather code to a descriptive condition label (e.g. "SUNNY", "RAINY", "CLOUDY").
     */
    fun mapWmoToConditionText(wmoCode: Int, isDaytime: Boolean = true): String {
        return when (wmoCode) {
            0, 1 -> if (isDaytime) "SUNNY" else "CLEAR"
            2, 3 -> "CLOUDY"
            45, 48 -> "FOGGY"
            in 51..67, in 80..82 -> "RAINY"
            in 71..77, 85, 86 -> "SNOWY"
            in 95..99 -> "STORMY"
            else -> if (isDaytime) "SUNNY" else "CLEAR"
        }
    }

    /**
     * Maps 3/4-character GBA code (or fallback) to descriptive condition label.
     */
    fun mapConditionShortToText(conditionShort: String, isDaytime: Boolean = true): String {
        return when (conditionShort.uppercase()) {
            "SUN" -> if (isDaytime) "SUNNY" else "CLEAR"
            "CLR" -> "CLEAR"
            "CLDY" -> "CLOUDY"
            "FOG" -> "FOGGY"
            "RAIN" -> "RAINY"
            "SNOW" -> "SNOWY"
            "STRM" -> "STORMY"
            else -> conditionShort.uppercase()
        }
    }
}
