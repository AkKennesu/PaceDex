package com.poketch.wear.complication

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PhotoImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.poketch.core.domain.model.Gen3PokemonSpriteResolver
import com.poketch.core.domain.repository.PokemonRepository
import com.poketch.core.domain.repository.UserRepository
import com.poketch.wear.R
import com.poketch.wear.WearMainActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

@AndroidEntryPoint
class PoketchBuddyPokemonComplicationService : SuspendingComplicationDataSourceService() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var pokemonRepository: PokemonRepository

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BuddyComplicationEntryPoint {
        fun userRepository(): UserRepository
        fun pokemonRepository(): PokemonRepository
    }

    private fun ensureDependenciesInjected() {
        if (!::userRepository.isInitialized) {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                BuddyComplicationEntryPoint::class.java
            )
            userRepository = entryPoint.userRepository()
            pokemonRepository = entryPoint.pokemonRepository()
        }
    }

    private fun loadBuddyIcon(pokedexNumber: Int): Icon? {
        val padded = Gen3PokemonSpriteResolver.formatPokedexId(pokedexNumber)
        val path = "sprites/pokemon_$padded.png"
        val bitmap = runCatching {
            assets.open(path).use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()

        return if (bitmap != null) {
            // Nearest-neighbor 98x98 scaling to fill the complication slot sharply without blur
            val scaled = Bitmap.createScaledBitmap(bitmap, 98, 98, false)
            Icon.createWithBitmap(scaled)
        } else {
            runCatching {
                Icon.createWithResource(this, R.drawable.wf_icon_pikachu)
            }.getOrNull()
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val icon = loadBuddyIcon(25) ?: return null
        val desc = PlainComplicationText.Builder("Pikachu").build()

        return SmallImageComplicationData.Builder(
            smallImage = SmallImage.Builder(icon, SmallImageType.PHOTO).build(),
            contentDescription = desc
        ).build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        ensureDependenciesInjected()

        val profile = userRepository.getUserProfile()
        val activeBuddyId = profile?.activeBuddyPokemonId ?: 25
        val pokemon = pokemonRepository.observePokemonById(activeBuddyId).firstOrNull()
        val pokemonName = pokemon?.name ?: "Pokémon"

        val icon = loadBuddyIcon(activeBuddyId) ?: return null
        val desc = PlainComplicationText.Builder(pokemonName).build()

        val tapIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, WearMainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("destination_route", "gba_battle")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return SmallImageComplicationData.Builder(
            smallImage = SmallImage.Builder(icon, SmallImageType.PHOTO).build(),
            contentDescription = desc
        )
            .setTapAction(tapIntent)
            .build()
    }

    companion object {
        fun requestUpdate(context: Context) {
            try {
                val requester = ComplicationDataSourceUpdateRequester.create(
                    context,
                    ComponentName(context, PoketchBuddyPokemonComplicationService::class.java)
                )
                requester.requestUpdateAll()
            } catch (_: Exception) {
            }
        }
    }
}
