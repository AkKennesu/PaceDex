package com.poketch.wear.complication

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.google.common.truth.Truth.assertThat
import com.poketch.core.domain.model.ActivityLevel
import com.poketch.core.domain.model.BiologicalSex
import com.poketch.core.domain.model.DeviceSource
import com.poketch.core.domain.model.DeficitTier
import com.poketch.core.domain.model.HabitLog
import com.poketch.core.domain.model.HabitType
import com.poketch.core.domain.model.UserProfile
import com.poketch.core.domain.repository.HabitRepository
import com.poketch.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
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
class PoketchWaterComplicationServiceTest {

    private lateinit var context: Context
    private lateinit var fakeUserRepository: FakeUserRepository
    private lateinit var fakeHabitRepository: FakeHabitRepository
    private lateinit var service: PoketchWaterComplicationService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        fakeUserRepository = FakeUserRepository()
        fakeHabitRepository = FakeHabitRepository()

        service = PoketchWaterComplicationService().apply {
            val method = android.content.ContextWrapper::class.java.getDeclaredMethod(
                "attachBaseContext",
                Context::class.java
            )
            method.isAccessible = true
            method.invoke(this, context)

            this.userRepository = fakeUserRepository
            this.habitRepository = fakeHabitRepository
        }
    }

    @Test
    fun getPreviewData_returnsValidShortTextAndRangedValue() {
        val shortText = service.getPreviewData(ComplicationType.SHORT_TEXT) as? ShortTextComplicationData
        assertThat(shortText).isNotNull()
        val textValue = shortText?.text?.getTextAt(context.resources, Instant.now())?.toString()
        assertThat(textValue).isEqualTo("1500 ml")

        val rangedValue = service.getPreviewData(ComplicationType.RANGED_VALUE) as? RangedValueComplicationData
        assertThat(rangedValue).isNotNull()
        assertThat(rangedValue?.value).isEqualTo(1500f)
        assertThat(rangedValue?.max).isEqualTo(2500f)
    }

    @Test
    fun onComplicationRequest_returnsProgressAndGoal(): Unit = runBlocking {
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
            dailyWaterGoalMl = 2400,
            activeBuddyPokemonId = 1,
            updatedAtEpochMillis = System.currentTimeMillis()
        )
        fakeHabitRepository.waterMl = 1250.0

        val shortTextRequest = ComplicationRequest(1, ComplicationType.SHORT_TEXT, true)
        val shortData = service.onComplicationRequest(shortTextRequest) as? ShortTextComplicationData

        assertThat(shortData).isNotNull()
        val shortTextValue = shortData?.text?.getTextAt(context.resources, Instant.now())?.toString()
        val shortTitleValue = shortData?.title?.getTextAt(context.resources, Instant.now())?.toString()

        assertThat(shortTextValue).isEqualTo("1250 ml")
        assertThat(shortTitleValue).isEqualTo("WTR")

        val rangedRequest = ComplicationRequest(2, ComplicationType.RANGED_VALUE, true)
        val rangedData = service.onComplicationRequest(rangedRequest) as? RangedValueComplicationData

        assertThat(rangedData).isNotNull()
        val rangedTextValue = rangedData?.text?.getTextAt(context.resources, Instant.now())?.toString()
        val rangedTitleValue = rangedData?.title?.getTextAt(context.resources, Instant.now())?.toString()

        assertThat(rangedTextValue).isEqualTo("1250")
        assertThat(rangedTitleValue).isEqualTo("WTR")
        assertThat(rangedData?.value).isEqualTo(1250f)
        assertThat(rangedData?.max).isEqualTo(2400f)
    }

    @Test
    fun onComplicationRequest_whenWaterExceedsGoal_doesNotCrashAndClampsValue(): Unit = runBlocking {
        fakeUserRepository.profile = UserProfile(
            id = 1,
            ageYears = 25,
            sex = BiologicalSex.MALE,
            heightCm = 175.0,
            weightKg = 70.0,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            deficitTier = DeficitTier.STANDARD,
            bmrKcal = 1600,
            tdeeKcal = 2200,
            targetCalorieCeiling = 1700,
            dailyWaterGoalMl = 2000,
            activeBuddyPokemonId = 1,
            updatedAtEpochMillis = System.currentTimeMillis()
        )
        fakeHabitRepository.waterMl = 3500.0

        val rangedRequest = ComplicationRequest(2, ComplicationType.RANGED_VALUE, true)
        val rangedData = service.onComplicationRequest(rangedRequest) as? RangedValueComplicationData

        assertThat(rangedData).isNotNull()
        assertThat(rangedData?.value).isEqualTo(2000f)
        assertThat(rangedData?.max).isEqualTo(2000f)
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

    private class FakeHabitRepository : HabitRepository {
        var waterMl: Double = 0.0

        override fun observeHabitsByDate(dateIso: String): Flow<List<HabitLog>> = flowOf(emptyList())
        override fun observeDailyWaterMl(dateIso: String): Flow<Double> = flowOf(waterMl)
        override fun observeDailyRunMiles(dateIso: String): Flow<Double> = flowOf(0.0)
        override fun observeDailySteps(dateIso: String): Flow<Double> = flowOf(0.0)
        override fun observeDailySleepHours(dateIso: String): Flow<Double> = flowOf(0.0)
        override suspend fun getLifetimeHabitTotal(habitType: HabitType): Double = 0.0
        override suspend fun logHabit(
            dateIso: String,
            habitType: HabitType,
            numericValue: Double,
            durationSeconds: Int,
            sourceDevice: DeviceSource
        ): HabitLog = HabitLog(
            logId = "test-log",
            dateIso = dateIso,
            habitType = habitType,
            numericValue = numericValue,
            durationSeconds = durationSeconds,
            sourceDevice = sourceDevice,
            timestampMillis = System.currentTimeMillis()
        )
        override suspend fun upsertHabitLog(log: HabitLog) {}
        override suspend fun upsertHabitLogs(logs: List<HabitLog>) {}
        override suspend fun getUnsyncedHabits(): List<HabitLog> = emptyList()
        override suspend fun markHabitsAsSynced(logIds: List<String>) {}
    }
}
