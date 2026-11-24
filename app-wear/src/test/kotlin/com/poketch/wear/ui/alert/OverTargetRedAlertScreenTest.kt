package com.poketch.wear.ui.alert

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class OverTargetRedAlertScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun redAlertScreen_rendersAccurately_andHandlesActionClicks() {
        var startRunClicked = false
        var acknowledgeClicked = false

        val testState = OverTargetRedAlertUiState(
            consumedKcal = 2100,
            targetCalorieCeiling = 1800,
            surplusKcal = 300,
            activeBuddy = null,
            isLoading = false
        )

        composeTestRule.setContent {
            OverTargetRedAlertContent(
                state = testState,
                onStartRun = { startRunClicked = true },
                onAcknowledge = { acknowledgeClicked = true }
            )
        }

        // 1. Red Alert Banner & Headers
        composeTestRule.onNodeWithText("🚨 RED ALERT 🚨").assertIsDisplayed()
        composeTestRule.onNodeWithText("OVER TARGET BY +300 KCAL").assertIsDisplayed()
        composeTestRule.onNodeWithText("2100 / 1800 kcal").assertIsDisplayed()

        // 2. Action: Start Recovery Walk
        val startWalkButton = composeTestRule.onNodeWithText("Start Recovery Walk (6,667 stp)")
        startWalkButton.assertIsDisplayed()
        startWalkButton.performClick()
        assertThat(startRunClicked).isTrue()

        // 3. Action: Acknowledge
        val acknowledgeButton = composeTestRule.onNodeWithText("Acknowledge")
        acknowledgeButton.assertIsDisplayed()
        acknowledgeButton.performClick()
        assertThat(acknowledgeClicked).isTrue()
    }

    @Test
    fun redAlertScreen_rendersRecoveryQuest_whenQuestActive() {
        val quest = com.poketch.core.domain.model.RecoveryQuest(
            dateIso = "2026-11-24",
            surplusKcal = 300,
            targetOffsetSteps = 6667,
            startingStepsSnapshot = 1000,
            currentOffsetSteps = 2500,
            isCompleted = false
        )
        val testState = OverTargetRedAlertUiState(
            consumedKcal = 2100,
            targetCalorieCeiling = 1800,
            surplusKcal = 300,
            activeBuddy = null,
            recoveryQuest = quest,
            isLoading = false
        )

        composeTestRule.setContent {
            OverTargetRedAlertContent(
                state = testState,
                onStartRun = {},
                onAcknowledge = {}
            )
        }

        composeTestRule.onNodeWithText("⚡ RECOVERY QUEST ⚡").assertIsDisplayed()
        composeTestRule.onNodeWithText("2500 / 6667 Steps").assertIsDisplayed()
        composeTestRule.onNodeWithText("4167 steps remaining to balance").assertIsDisplayed()
    }

    @Test
    fun redAlertScreen_rendersCelebrationAndClearsAlert_whenQuestCompleted() {
        var dismissedClicked = false
        val quest = com.poketch.core.domain.model.RecoveryQuest(
            dateIso = "2026-11-24",
            surplusKcal = 300,
            targetOffsetSteps = 6667,
            startingStepsSnapshot = 1000,
            currentOffsetSteps = 6667,
            isCompleted = true
        )
        val testState = OverTargetRedAlertUiState(
            consumedKcal = 2100,
            targetCalorieCeiling = 1800,
            surplusKcal = 300,
            activeBuddy = null,
            recoveryQuest = quest,
            isLoading = false
        )

        composeTestRule.setContent {
            OverTargetRedAlertContent(
                state = testState,
                onStartRun = {},
                onAcknowledge = { dismissedClicked = true }
            )
        }

        composeTestRule.onNodeWithText("🎉 DEFICIT RESTORED 🎉").assertIsDisplayed()
        composeTestRule.onNodeWithText("RECOVERY QUEST CLEARED!").assertIsDisplayed()
        composeTestRule.onNodeWithText("🎉 QUEST COMPLETE!").assertIsDisplayed()

        val dismissButton = composeTestRule.onNodeWithText("Dismiss (Deficit Restored)")
        dismissButton.assertIsDisplayed()
        dismissButton.performClick()
        assertThat(dismissedClicked).isTrue()
    }

    @Test
    fun redAlertScreen_rendersStartRecoveryWalkButton_withTargetSteps() {
        var startWalkClicked = false
        val quest = com.poketch.core.domain.model.RecoveryQuest(
            dateIso = "2026-11-24",
            surplusKcal = 110,
            targetOffsetSteps = 2400,
            startingStepsSnapshot = 1000,
            currentOffsetSteps = 950,
            isCompleted = false
        )
        val testState = OverTargetRedAlertUiState(
            consumedKcal = 1910,
            targetCalorieCeiling = 1800,
            surplusKcal = 110,
            activeBuddy = null,
            recoveryQuest = quest,
            isLoading = false
        )

        composeTestRule.setContent {
            OverTargetRedAlertContent(
                state = testState,
                onStartRun = { startWalkClicked = true },
                onAcknowledge = {}
            )
        }

        val startWalkButton = composeTestRule.onNodeWithText("Start Recovery Walk (2,400 stp)")
        startWalkButton.assertIsDisplayed()
        startWalkButton.performClick()
        assertThat(startWalkClicked).isTrue()
    }
}
