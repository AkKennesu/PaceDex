package com.poketch.wear.complication

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.google.common.truth.Truth.assertThat
import com.poketch.core.domain.model.ActivityLevel
import com.poketch.core.domain.model.BiologicalSex
import com.poketch.core.domain.model.DeficitTier
import com.poketch.core.domain.model.Pokemon
import com.poketch.core.domain.model.PokemonCategory
import com.poketch.core.domain.model.UnlockRule
import com.poketch.core.domain.model.UserProfile
import com.poketch.core.domain.repository.PokemonRepository
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
class PoketchBuddyPokemonComplicationServiceTest {

    private lateinit var context: Context
    private lateinit var fakeUserRepository: FakeUserRepository
    private lateinit var fakePokemonRepository: FakePokemonRepository
    private lateinit var service: PoketchBuddyPokemonComplicationService

    private val samplePikachu = Pokemon(
        pokedexNumber = 25,
        name = "Pikachu",
        primaryType = "ELECTRIC",
        unlockRule = UnlockRule("STEP_10K", PokemonCategory.RUN_STEPS, "Walk 10,000 steps", 10000),
        spriteAssetPath = "sprites/pokemon_025.png"
    )

    private val sampleBulbasaur = Pokemon(
        pokedexNumber = 1,
        name = "Bulbasaur",
        primaryType = "GRASS",
        unlockRule = UnlockRule("STARTER", PokemonCategory.STARTER, "Starter", 1),
        spriteAssetPath = "sprites/pokemon_001.png"
    )

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        fakeUserRepository = FakeUserRepository()
        fakePokemonRepository = FakePokemonRepository(listOf(samplePikachu, sampleBulbasaur))

        service = PoketchBuddyPokemonComplicationService().apply {
            val method = android.content.ContextWrapper::class.java.getDeclaredMethod(
                "attachBaseContext",
                Context::class.java
            )
            method.isAccessible = true
            method.invoke(this, context)

            this.userRepository = fakeUserRepository
            this.pokemonRepository = fakePokemonRepository
        }
    }

    @Test
    fun getPreviewData_returnsValidSmallImage() {
        val smallImagePreview = service.getPreviewData(ComplicationType.SMALL_IMAGE) as? SmallImageComplicationData
        assertThat(smallImagePreview).isNotNull()
        assertThat(smallImagePreview?.smallImage).isNotNull()
    }

    @Test
    fun onComplicationRequest_returnsActiveBuddyPokemon() = runBlocking {
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
            dailyWaterGoalMl = 2500,
            activeBuddyPokemonId = 1, // Bulbasaur
            updatedAtEpochMillis = System.currentTimeMillis()
        )

        val request = ComplicationRequest(2, ComplicationType.SMALL_IMAGE, true)
        val data = service.onComplicationRequest(request) as? SmallImageComplicationData

        assertThat(data).isNotNull()
        assertThat(data?.smallImage).isNotNull()
        val desc = data?.contentDescription?.getTextAt(context.resources, Instant.now())?.toString()
        assertThat(desc).isEqualTo("Bulbasaur")
        assertThat(data?.tapAction).isNotNull()
    }

    @Test
    fun onComplicationRequest_updatesWhenActiveBuddyChanges() = runBlocking {
        // Change buddy to Pikachu (25)
        fakeUserRepository.updateActiveBuddy(25)

        val request = ComplicationRequest(2, ComplicationType.SMALL_IMAGE, true)
        val data = service.onComplicationRequest(request) as? SmallImageComplicationData

        assertThat(data).isNotNull()
        val desc = data?.contentDescription?.getTextAt(context.resources, Instant.now())?.toString()
        assertThat(desc).isEqualTo("Pikachu")
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
            profile = (profile ?: UserProfile(
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
                dailyWaterGoalMl = 2500,
                activeBuddyPokemonId = pokemonId,
                updatedAtEpochMillis = System.currentTimeMillis()
            )).copy(activeBuddyPokemonId = pokemonId)
        }
    }

    private class FakePokemonRepository(
        private val pokemonList: List<Pokemon>
    ) : PokemonRepository {
        override fun observeAllPokemon(): Flow<List<Pokemon>> = flowOf(pokemonList)
        override fun observePokemonById(pokedexNumber: Int): Flow<Pokemon?> =
            flowOf(pokemonList.find { it.pokedexNumber == pokedexNumber })
        override suspend fun getPokemonByCategory(category: PokemonCategory): List<Pokemon> = emptyList()
        override suspend fun getPokemonByRuleCode(ruleCode: String): List<Pokemon> = emptyList()
        override suspend fun getUnlockedCount(): Int = pokemonList.count { it.isUnlocked }
        override suspend fun updateProgress(pokedexNumber: Int, newProgress: Int): Pokemon? = null
        override suspend fun unlockPokemon(pokedexNumber: Int, timestampMillis: Long): Pokemon? = null
        override suspend fun upsertPokemon(pokemon: Pokemon) {}
        override suspend fun upsertAllPokemon(pokemonList: List<Pokemon>) {}
    }
}
