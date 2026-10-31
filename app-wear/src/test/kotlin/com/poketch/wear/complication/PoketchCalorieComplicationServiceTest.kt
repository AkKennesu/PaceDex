package com.poketch.wear.complication

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.google.common.truth.Truth.assertThat
import com.poketch.core.domain.calculator.CalorieBudgetState
import com.poketch.core.domain.model.ActivityLevel
import com.poketch.core.domain.model.BiologicalSex
import com.poketch.core.domain.model.CalorieLog
import com.poketch.core.domain.model.DailyNutritionSummary
import com.poketch.core.domain.model.DeficitTier
import com.poketch.core.domain.model.UserProfile
import com.poketch.core.domain.repository.CalorieRepository
import com.poketch.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class PoketchCalorieComplicationServiceTest {

    private lateinit var context: Context
    private lateinit var fakeUserRepository: FakeUserRepository
    private lateinit var fakeCalorieRepository: FakeCalorieRepository
    private lateinit var service: PoketchCalorieComplicationService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        fakeUserRepository = FakeUserRepository()
        fakeCalorieRepository = FakeCalorieRepository()

        service = PoketchCalorieComplicationService().apply {
            val method = android.content.ContextWrapper::class.java.getDeclaredMethod(
                "attachBaseContext",
                Context::class.java
            )
            method.isAccessible = true
            method.invoke(this, context)

            this.userRepository = fakeUserRepository
            this.calorieRepository = fakeCalorieRepository
        }
    }

    @Test
    fun getPreviewData_returnsValidShortTextAndRangedValue() {
        val shortText = service.getPreviewData(ComplicationType.SHORT_TEXT) as? ShortTextComplicationData
        assertThat(shortText).isNotNull()
        val textValue = shortText?.text?.getTextAt(context.resources, Instant.now())?.toString()
        assertThat(textValue).isEqualTo("450 kcal")

        val rangedValue = service.getPreviewData(ComplicationType.RANGED_VALUE) as? RangedValueComplicationData
        assertThat(rangedValue).isNotNull()
        assertThat(rangedValue?.value).isEqualTo(1450f)
        assertThat(rangedValue?.max).isEqualTo(1900f)
    }

    @Test
    fun onComplicationRequest_normalBudget_displaysRemainingCalories(): Unit = runBlocking {
        fakeUserRepository.profile = UserProfile(
            id = 1,
            ageYears = 26,
            sex = BiologicalSex.MALE,
            heightCm = 175.0,
            weightKg = 72.0,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            deficitTier = DeficitTier.STANDARD,
            bmrKcal = 1650,
            tdeeKcal = 2300,
            targetCalorieCeiling = 1800,
            dailyWaterGoalMl = 2500,
            activeBuddyPokemonId = 1,
            updatedAtEpochMillis = System.currentTimeMillis()
        )
        fakeCalorieRepository.totalCalories = 1200

        val shortTextRequest = ComplicationRequest(1, ComplicationType.SHORT_TEXT, true)
        val data = service.onComplicationRequest(shortTextRequest) as? ShortTextComplicationData

        assertThat(data).isNotNull()
        val textValue = data?.text?.getTextAt(context.resources, Instant.now())?.toString()
        val titleValue = data?.title?.getTextAt(context.resources, Instant.now())?.toString()

        // 1800 - 1200 = 600 remaining
        assertThat(textValue).isEqualTo("600 kcal")
        assertThat(titleValue).isEqualTo("REMAIN")
    }

    @Test
    fun onComplicationRequest_overTarget_displaysOverWarning(): Unit = runBlocking {
        fakeUserRepository.profile = UserProfile(
            id = 1,
            ageYears = 26,
            sex = BiologicalSex.MALE,
            heightCm = 175.0,
            weightKg = 72.0,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            deficitTier = DeficitTier.STANDARD,
            bmrKcal = 1650,
            tdeeKcal = 2300,
            targetCalorieCeiling = 1800,
            dailyWaterGoalMl = 2500,
            activeBuddyPokemonId = 1,
            updatedAtEpochMillis = System.currentTimeMillis()
        )
        fakeCalorieRepository.totalCalories = 2100

        val shortTextRequest = ComplicationRequest(2, ComplicationType.SHORT_TEXT, true)
        val shortData = service.onComplicationRequest(shortTextRequest) as? ShortTextComplicationData

        assertThat(shortData).isNotNull()
        val shortTextValue = shortData?.text?.getTextAt(context.resources, Instant.now())?.toString()
        val shortTitleValue = shortData?.title?.getTextAt(context.resources, Instant.now())?.toString()

        // 2100 - 1800 = 300 over
        assertThat(shortTextValue).isEqualTo("+300 OVER")
        assertThat(shortTitleValue).isEqualTo("ALERT")

        val rangedRequest = ComplicationRequest(3, ComplicationType.RANGED_VALUE, true)
        val rangedData = service.onComplicationRequest(rangedRequest) as? RangedValueComplicationData

        assertThat(rangedData).isNotNull()
        val rangedTextValue = rangedData?.text?.getTextAt(context.resources, Instant.now())?.toString()
        val rangedTitleValue = rangedData?.title?.getTextAt(context.resources, Instant.now())?.toString()

        assertThat(rangedTextValue).isEqualTo("+300")
        assertThat(rangedTitleValue).isEqualTo("OVER")
        assertThat(rangedData?.value).isEqualTo(2100f)
        assertThat(rangedData?.max).isEqualTo(1800f)
    }

    private class FakeUserRepository : UserRepository {
        var profile: UserProfile? = null

        override fun observeUserProfile(): Flow<UserProfile?> = flowOf(profile)
        override suspend fun getUserProfile(): UserProfile? = profile
        override suspend fun saveUserProfile(
            ageYears: Int,
            sex: BiologicalSex,
            heightCm: Double,
            weightKg: Double,
            activityLevel: ActivityLevel,
            deficitTier: DeficitTier,
            dailyWaterGoalMl: Int,
            activeBuddyPokemonId: Int
        ) {
            profile = UserProfile(
                id = 1,
                ageYears = ageYears,
                sex = sex,
                heightCm = heightCm,
                weightKg = weightKg,
                activityLevel = activityLevel,
                deficitTier = deficitTier,
                bmrKcal = 1600,
                tdeeKcal = 2200,
                targetCalorieCeiling = 1700,
                dailyWaterGoalMl = dailyWaterGoalMl,
                activeBuddyPokemonId = activeBuddyPokemonId,
                updatedAtEpochMillis = System.currentTimeMillis()
            )
        }
        override suspend fun upsertUserProfile(profile: UserProfile) { this.profile = profile }
        override suspend fun updateActiveBuddy(pokemonId: Int) {
            profile = profile?.copy(activeBuddyPokemonId = pokemonId)
        }
    }

    private class FakeCalorieRepository : CalorieRepository {
        var totalCalories = 0

        override fun observeLogsByDate(dateIso: String): Flow<List<CalorieLog>> = flowOf(emptyList())
        override fun observeTotalCaloriesByDate(dateIso: String): Flow<Int> = flowOf(totalCalories)
        override fun observeRecentDailyNutrition(days: Int): Flow<List<DailyNutritionSummary>> = flowOf(emptyList())
        override suspend fun getRecentDailyNutrition(days: Int): List<DailyNutritionSummary> = emptyList()
        override fun observeDailyBudgetState(dateIso: String): Flow<CalorieBudgetState> =
            flowOf(CalorieBudgetState.OnTrack(totalCalories, 2000, 2000 - totalCalories, 0.5))

        override suspend fun upsertCalorieLog(log: CalorieLog) {}
        override suspend fun upsertCalorieLogs(logs: List<CalorieLog>) {}
        override suspend fun deleteCalorieLog(logId: String) {}
        override suspend fun getUnsyncedLogs(): List<CalorieLog> = emptyList()
        override suspend fun markLogsAsSynced(logIds: List<String>) {}
    }
}
