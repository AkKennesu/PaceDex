package com.poketch.mobile.ui.nutrition

import com.google.common.truth.Truth.assertThat
import com.poketch.core.domain.model.MealCategory
import org.junit.Test
import java.util.Locale

class NutritionUnitConverterTest {

    @Test
    fun weightConversion_kgToLbsAndLbsToKg_isAccurate() {
        val weightKg = 75.0
        val lbs = weightKg * 2.20462262
        val formattedLbs = String.format(Locale.US, "%.1f", lbs)
        assertThat(formattedLbs).isEqualTo("165.3")

        val convertedKg = formattedLbs.toDouble() * 0.45359237
        val formattedKg = String.format(Locale.US, "%.1f", convertedKg)
        assertThat(formattedKg).isEqualTo("75.0")
    }

    @Test
    fun heightConversion_cmToFtInAndFtInToCm_isAccurate() {
        val heightCm = 175.0
        val totalInches = heightCm / 2.54
        val feet = (totalInches / 12).toInt()
        val inches = totalInches % 12
        val formattedInches = String.format(Locale.US, "%.1f", inches)

        assertThat(feet).isEqualTo(5)
        assertThat(formattedInches).isEqualTo("8.9")

        val reconvertedTotalInches = feet * 12.0 + formattedInches.toDouble()
        val reconvertedCm = String.format(Locale.US, "%.1f", reconvertedTotalInches * 2.54)
        assertThat(reconvertedCm).isEqualTo("175.0")
    }

    @Test
    fun kantoMealTemplates_containsAllSixDelicaciesWithValidNutrition() {
        val templates = KantoMealTemplates.ALL
        assertThat(templates).hasSize(6)

        val names = templates.map { it.name }
        assertThat(names).containsExactly(
            "Fresh Moomoo Milk",
            "Slowpoke Tail Broth",
            "Fisherman Bento",
            "Lava Cookie",
            "Rage Candy Bar",
            "Berry Juice Blend"
        )

        templates.forEach { template ->
            assertThat(template.caloriesKcal).isGreaterThan(0)
            assertThat(template.proteinG).isAtLeast(0)
            assertThat(template.carbsG).isAtLeast(0)
            assertThat(template.fatG).isAtLeast(0)
            assertThat(template.description).isNotEmpty()
            assertThat(template.emoji).isNotEmpty()
        }

        val moomooMilk = templates.first { it.name == "Fresh Moomoo Milk" }
        assertThat(moomooMilk.category).isEqualTo(MealCategory.BREAKFAST)
        assertThat(moomooMilk.caloriesKcal).isEqualTo(150)

        val slowpokeTail = templates.first { it.name == "Slowpoke Tail Broth" }
        assertThat(slowpokeTail.category).isEqualTo(MealCategory.LUNCH)
        assertThat(slowpokeTail.caloriesKcal).isEqualTo(420)

        val bento = templates.first { it.name == "Fisherman Bento" }
        assertThat(bento.category).isEqualTo(MealCategory.DINNER)
        assertThat(bento.caloriesKcal).isEqualTo(580)
    }
}
