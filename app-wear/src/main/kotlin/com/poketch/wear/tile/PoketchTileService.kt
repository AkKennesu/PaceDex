package com.poketch.wear.tile

import android.content.Context
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.CircularProgressIndicator
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.ProgressIndicatorColors
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import com.poketch.core.domain.repository.CalorieRepository
import com.poketch.core.domain.repository.HabitRepository
import com.poketch.core.domain.repository.PokemonRepository
import com.poketch.core.domain.repository.UserRepository
import com.poketch.wear.R
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class PoketchTileService : TileService() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var calorieRepository: CalorieRepository

    @Inject
    lateinit var habitRepository: HabitRepository

    @Inject
    lateinit var pokemonRepository: PokemonRepository

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TileEntryPoint {
        fun userRepository(): UserRepository
        fun calorieRepository(): CalorieRepository
        fun habitRepository(): HabitRepository
        fun pokemonRepository(): PokemonRepository
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)

    private fun ensureDependenciesInjected() {
        if (!::userRepository.isInitialized) {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                TileEntryPoint::class.java
            )
            userRepository = entryPoint.userRepository()
            calorieRepository = entryPoint.calorieRepository()
            habitRepository = entryPoint.habitRepository()
            pokemonRepository = entryPoint.pokemonRepository()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        return CallbackToFutureAdapter.getFuture { completer ->
            serviceScope.launch {
                try {
                    ensureDependenciesInjected()
                    val tile = buildTile(requestParams.deviceConfiguration)
                    completer.set(tile)
                } catch (e: Exception) {
                    completer.setException(e)
                }
            }
            "poketch_tile_request"
        }
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        return CallbackToFutureAdapter.getFuture { completer ->
            serviceScope.launch {
                try {
                    ensureDependenciesInjected()
                    val resources = buildResources(requestParams.version)
                    completer.set(resources)
                } catch (e: Exception) {
                    completer.setException(e)
                }
            }
            "poketch_tile_resources"
        }
    }

    private suspend fun buildTile(deviceParameters: DeviceParameters): TileBuilders.Tile {
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val profile = userRepository.getUserProfile()
        val targetCeiling = profile?.targetCalorieCeiling ?: 1900
        val consumedKcal = calorieRepository.observeTotalCaloriesByDate(todayIso).firstOrNull() ?: 0
        val todayWaterMl = habitRepository.observeDailyWaterMl(todayIso).firstOrNull() ?: 0.0

        val isOverTarget = consumedKcal > targetCeiling
        val progress = if (targetCeiling > 0) {
            (consumedKcal.toFloat() / targetCeiling.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        val arcColor = if (isOverTarget) 0xFFB71C1C.toInt() else 0xFF00E676.toInt()
        val arcTrackColor = 0x33FFFFFF

        // Outer circular progress indicator
        val progressIndicator = CircularProgressIndicator.Builder()
            .setProgress(progress)
            .setCircularProgressIndicatorColors(
                ProgressIndicatorColors(argb(arcColor), argb(arcTrackColor))
            )
            .build()

        // Click targets
        val launchWater = ActionBuilders.AndroidActivity.Builder()
            .setPackageName(packageName)
            .setClassName("com.poketch.wear.WearMainActivity")
            .addKeyToExtraMapping("destination_route", ActionBuilders.stringExtra("quick_habit_actions"))
            .build()
        val waterClickable = ModifiersBuilders.Clickable.Builder()
            .setId("click_quick_water")
            .setOnClick(ActionBuilders.LaunchAction.Builder().setAndroidActivity(launchWater).build())
            .build()

        val launchCalorie = ActionBuilders.AndroidActivity.Builder()
            .setPackageName(packageName)
            .setClassName("com.poketch.wear.WearMainActivity")
            .addKeyToExtraMapping("destination_route", ActionBuilders.stringExtra("rotary_calorie_logger"))
            .build()
        val calorieClickable = ModifiersBuilders.Clickable.Builder()
            .setId("click_log_calorie")
            .setOnClick(ActionBuilders.LaunchAction.Builder().setAndroidActivity(launchCalorie).build())
            .build()

        // Center content
        val calorieTextColor = if (isOverTarget) 0xFFFF5252.toInt() else 0xFFFFFFFF.toInt()
        val calorieLabel = if (isOverTarget) {
            "+${consumedKcal - targetCeiling} OVER"
        } else {
            "$consumedKcal / $targetCeiling kcal"
        }

        val centerColumn = LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(
                LayoutElementBuilders.Image.Builder()
                    .setResourceId(ID_BUDDY_ICON)
                    .setWidth(dp(28f))
                    .setHeight(dp(28f))
                    .build()
            )
            .addContent(
                LayoutElementBuilders.Spacer.Builder().setHeight(dp(2f)).build()
            )
            .addContent(
                Text.Builder(this, calorieLabel)
                    .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                    .setColor(argb(calorieTextColor))
                    .build()
            )
            .addContent(
                LayoutElementBuilders.Spacer.Builder().setHeight(dp(1f)).build()
            )
            .addContent(
                Text.Builder(this, "${todayWaterMl.toInt()} ml 💧")
                    .setTypography(Typography.TYPOGRAPHY_CAPTION2)
                    .setColor(argb(0xFF81D4FA.toInt()))
                    .build()
            )
            .addContent(
                LayoutElementBuilders.Spacer.Builder().setHeight(dp(4f)).build()
            )
            .addContent(
                LayoutElementBuilders.Row.Builder()
                    .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                    .addContent(
                        CompactChip.Builder(this, "+Water", waterClickable, deviceParameters)
                            .build()
                    )
                    .addContent(
                        LayoutElementBuilders.Spacer.Builder().setWidth(dp(4f)).build()
                    )
                    .addContent(
                        CompactChip.Builder(this, "+Log", calorieClickable, deviceParameters)
                            .build()
                    )
                    .build()
            )
            .build()

        val rootBox = LayoutElementBuilders.Box.Builder()
            .setWidth(dp(deviceParameters.screenWidthDp.toFloat()))
            .setHeight(dp(deviceParameters.screenHeightDp.toFloat()))
            .addContent(progressIndicator)
            .addContent(centerColumn)
            .build()

        val layout = LayoutElementBuilders.Layout.Builder()
            .setRoot(rootBox)
            .build()

        val timelineEntry = TimelineBuilders.TimelineEntry.Builder()
            .setLayout(layout)
            .build()

        val timeline = TimelineBuilders.Timeline.Builder()
            .addTimelineEntry(timelineEntry)
            .build()

        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(timeline)
            .setFreshnessIntervalMillis(60_000L) // Refresh every minute
            .build()
    }

    private suspend fun buildResources(version: String): ResourceBuilders.Resources {
        val profile = userRepository.getUserProfile()
        val activeBuddyId = profile?.activeBuddyPokemonId ?: 1
        val buddy = pokemonRepository.observePokemonById(activeBuddyId).firstOrNull()
        val assetPath = buddy?.spriteAssetPath ?: "sprites/001.png"

        val imageBytes = runCatching {
            assets.open(assetPath).use { it.readBytes() }
        }.getOrNull()

        val resourcesBuilder = ResourceBuilders.Resources.Builder()
            .setVersion(version)

        if (imageBytes != null && imageBytes.isNotEmpty()) {
            resourcesBuilder.addIdToImageMapping(
                ID_BUDDY_ICON,
                ResourceBuilders.ImageResource.Builder()
                    .setInlineResource(
                        ResourceBuilders.InlineImageResource.Builder()
                            .setData(imageBytes)
                            .build()
                    )
                    .build()
            )
        } else {
            resourcesBuilder.addIdToImageMapping(
                ID_BUDDY_ICON,
                ResourceBuilders.ImageResource.Builder()
                    .setAndroidResourceByResId(
                        ResourceBuilders.AndroidImageResourceByResId.Builder()
                            .setResourceId(R.drawable.splash_icon)
                            .build()
                    )
                    .build()
            )
        }

        return resourcesBuilder.build()
    }

    companion object {
        const val ID_BUDDY_ICON = "buddy_icon"
        const val RESOURCES_VERSION = "1"

        fun requestUpdate(context: Context) {
            runCatching {
                getUpdater(context).requestUpdate(PoketchTileService::class.java)
            }
        }
    }
}
