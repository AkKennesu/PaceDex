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
}
