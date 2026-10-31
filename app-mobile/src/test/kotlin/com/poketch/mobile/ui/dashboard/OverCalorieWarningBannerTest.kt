package com.poketch.mobile.ui.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.poketch.core.domain.calculator.CalorieBudgetState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class OverCalorieWarningBannerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun overCalorieWarningBanner_rendersAccurately_whenOverTarget() {
        val warningState = CalorieBudgetState.OverTargetWarning(
            consumedKcal = 2250,
            targetCeilingKcal = 1900,
            surplusKcal = 350,
            suggestedOffsetSteps = 7000,
            suggestedOffsetRunMiles = 2.5,
            consumptionRatio = 1.18
        )

        composeTestRule.setContent {
            OverCalorieWarningCard(warning = warningState)
        }

        // Header and alert title
        composeTestRule.onNodeWithText("OVER CALORIE TARGET!").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Warning").assertIsDisplayed()

        // Surplus details
        composeTestRule.onNodeWithText("+350 kcal above target ceiling").assertIsDisplayed()

        // Suggested offsets
        composeTestRule.onNodeWithText("Suggested burn offsets:").assertIsDisplayed()
        composeTestRule.onNodeWithText("• Walk 7000 steps (~7k)").assertIsDisplayed()
        composeTestRule.onNodeWithText("• Or jog 2.5 miles").assertIsDisplayed()
    }
}
