package com.poketch.wear.complication

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
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

    private fun findNonTransparentBounds(bitmap: Bitmap): Rect? {
        val width = bitmap.width
        val height = bitmap.height
        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                val alpha = (pixels[rowOffset + x] ushr 24) and 0xFF
                if (alpha > 20) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        return if (maxX >= minX && maxY >= minY) {
            Rect(minX, minY, maxX + 1, maxY + 1)
        } else {
            null
        }
    }

    private fun loadBuddyIcon(pokedexNumber: Int): Icon? {
        val padded = Gen3PokemonSpriteResolver.formatPokedexId(pokedexNumber)
        val path = "sprites/pokemon_$padded.png"
        val originalBitmap = runCatching {
            assets.open(path).use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()

        if (originalBitmap != null) {
            val bounds = findNonTransparentBounds(originalBitmap)
            val cropped = if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
                Bitmap.createBitmap(originalBitmap, bounds.left, bounds.top, bounds.width(), bounds.height())
            } else {
                originalBitmap
            }

            val targetSlotSize = 140
            val maxDrawSize = 132f
            val scale = minOf(maxDrawSize / cropped.width, maxDrawSize / cropped.height)
            val scaledW = (cropped.width * scale).toInt().coerceAtLeast(1)
            val scaledH = (cropped.height * scale).toInt().coerceAtLeast(1)

            val scaled = Bitmap.createScaledBitmap(cropped, scaledW, scaledH, false)

            val outputBitmap = Bitmap.createBitmap(targetSlotSize, targetSlotSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(outputBitmap)
            val paint = Paint().apply {
                isFilterBitmap = false
                isAntiAlias = false
            }

            // Horizontally centered, bottom aligned resting on grass pedestal
            val destX = (targetSlotSize - scaledW) / 2f
            val destY = (targetSlotSize - scaledH - 2f).coerceAtLeast(0f)
            canvas.drawBitmap(scaled, destX, destY, paint)

            return Icon.createWithBitmap(outputBitmap)
        } else {
            return runCatching {
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
