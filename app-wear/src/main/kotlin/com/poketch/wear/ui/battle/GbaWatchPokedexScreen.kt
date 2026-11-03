package com.poketch.wear.ui.battle

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.poketch.core.domain.model.Gen3PokemonSpriteResolver
import com.poketch.core.domain.model.Pokemon
import com.poketch.core.domain.model.PokemonCategory
import com.poketch.wear.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class PokedexFilter(val label: String) {
    ALL("ALL 151"),
    UNLOCKED("UNLOCKED"),
    LOCKED("LOCKED")
}

/**
 * Generates an authentic compact mission label (e.g., "1MI RUN", "5K STP", "2.5L WTR", "7D KCAL")
 * for the gold-framed teal mission bar.
 */
internal fun getCompactMissionTag(pokemon: Pokemon): String {
    val rule = pokemon.unlockRule
    val desc = rule.description.uppercase()
    return when {
        desc.contains("RUN") || desc.contains("MILE") -> {
            val num = desc.filter { it.isDigit() }
            if (num.isNotEmpty()) "${num}MI RUN" else "RUN"
        }
        rule.category == PokemonCategory.RUN_STEPS || desc.contains("STEP") -> {
            val target = rule.targetThreshold
            if (target >= 1000) "${target / 1000}K STP" else "$target STP"
        }
        rule.category == PokemonCategory.WATER_HYDRATION || desc.contains("WATER") || desc.contains("HYDRATION") -> {
            val target = rule.targetThreshold
            val liters = target / 1000.0
            val formatted = if (liters % 1.0 == 0.0) "${liters.toInt()}L" else "%.1fL".format(Locale.US, liters)
            "$formatted WTR"
        }
        rule.category == PokemonCategory.CALORIE_DEFICIT || desc.contains("CAL") || desc.contains("DEFICIT") -> {
            val target = rule.targetThreshold
            "${target}D KCAL"
        }
        rule.category == PokemonCategory.SLEEP_STREAK || desc.contains("SLEEP") -> {
            val target = rule.targetThreshold
            "${target}D SLEEP"
        }
        rule.category == PokemonCategory.STARTER -> "STARTER"
        rule.category == PokemonCategory.LEGENDARY -> "LEGEND"
        else -> rule.category.name.take(7)
    }
}

/**
 * Authentic Generation III / Kanto Pokédex Screen (`GbaWatchPokedexScreen.kt`)
 * Built on a 192x192 Virtual Pixel Canvas layout for Wear OS.
 *
 * Exact Virtual Layout:
 * 1. Base Background (192x192 at (0, 0))
 * 2. Top Sensor Array:
 *    - Blue Glass Camera Lens Glow: center = (49, 32), radius = 4, cyan pulse + 140ms white-cyan tick flash
 *    - 3 Indicator LEDs: (134, 32) Red, (141, 32) Yellow, (148, 32) Green, sequential chase on scroll
 *    - Zone 1 Counter Box: (x = 64, y = 24, w = 64, h = 17) -> "${id}/151", tap cycles filter
 * 3. Left Metallic CRT Specimen Monitor: (x = 32, y = 50, w = 56, h = 54)
 *    - CRT Sprite Window: (x = 42, y = 55, w = 36, h = 34), 140ms scanline wipe + 2-hop entry / silhouette
 *    - Bottom Integrated Nameplate: (x = 38, y = 92, w = 46, h = 9) -> "#025"
 * 4. Zone 2 — Right 3-Slot GBA Rotary Wheel: x = 93..161, y = 50..104
 *    - Top Slot (index - 1, x = 93, y = 50, w = 68, h = 15)
 *    - Center Selected Slot (index, x = 93, y = 67, w = 68, h = 20) with cursor bob
 *    - Bottom Slot (index + 1, x = 93, y = 89, w = 68, h = 15)
 * 5. Zone 3 — Gold-Framed Teal Mission Bar: (x = 38, y = 109, w = 117, h = 20)
 *    - Left Label (x = 43, y = 114, w = 31, h = 10): "READY" / mission tag
 *    - Center Recessed Bar (x = 76, y = 116, maxW = 46, h = 5): animated 5px fill
 *    - Right Readout (x = 124, y = 114, w = 28, h = 10): "100%" / "${ratio}%"
 * 6. Zone 4 — 3D GBA Action Buttons & Footer:
 *    - Left BACK (x = 42, y = 134, w = 31, h = 26)
 *    - Right SET BUDDY / PIN TARGET (x = 78, y = 134, w = 72, h = 26)
 *    - Footer Hint (x = 36, y = 163, w = 120, h = 8): "TURN CROWN TO SCROLL"
 */
@Composable
fun GbaWatchPokedexScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GbaWatchBattleViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRound = LocalConfiguration.current.isScreenRound
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    // Intercept Back gesture / physical button
    BackHandler {
        onNavigateBack()
    }

    // Filter selection state
    var currentFilter by remember { mutableStateOf(PokedexFilter.ALL) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var filterFeedbackText by remember { mutableStateOf<String?>(null) }

    // Filtered list of Pokémon based on active mode
    val filteredPokemon = remember(state.allPokemon, currentFilter) {
        val list = when (currentFilter) {
            PokedexFilter.ALL -> state.allPokemon
            PokedexFilter.UNLOCKED -> state.allPokemon.filter { it.isUnlocked }
            PokedexFilter.LOCKED -> state.allPokemon.filter { !it.isUnlocked }
        }
        list.ifEmpty { state.allPokemon }
    }

    // Clamp / wrap index when list changes
    LaunchedEffect(filteredPokemon.size) {
        if (filteredPokemon.isNotEmpty()) {
            selectedIndex = selectedIndex.coerceIn(0, filteredPokemon.lastIndex)
        }
    }

    val selectedPokemon: Pokemon? = filteredPokemon.getOrNull(selectedIndex)

    // Rotary & Drag physics variables
    var rotaryAccumulator by remember { mutableFloatStateOf(0f) }
    var lastRotaryTimestamp by remember { mutableLongStateOf(0L) }
    var lastScrollTimestamp by remember { mutableLongStateOf(0L) }
    val stepThreshold = 28f

    // Blue Camera Lens flash trigger on tick
    var lensTickTrigger by remember { mutableIntStateOf(0) }
    val lensFlashAlpha by animateFloatAsState(
        targetValue = if (lensTickTrigger > 0) 1.0f else 0f,
        animationSpec = tween(140, easing = LinearEasing),
        label = "lens_flash"
    )
    LaunchedEffect(lensTickTrigger) {
        if (lensTickTrigger > 0) {
            delay(140)
            lensTickTrigger = 0
        }
    }

    // Continuous ambient cyan breathing pulse for camera lens
    val infiniteTransition = rememberInfiniteTransition(label = "pokedex_ambient")
    val lensPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lens_pulse"
    )

    // 3 LED sequential chase while scrolling
    val isActivelyScrolling by produceState(initialValue = false, key1 = lastScrollTimestamp) {
        if (lastScrollTimestamp > 0L) {
            value = true
            delay(400)
            value = false
        }
    }

    val ledChaseStep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "led_chase"
    )
    val activeLedIndex = if (isActivelyScrolling) ledChaseStep.toInt().coerceIn(0, 2) else -1

    // Mechanical drum slide offset & direction tracking
    val drumSlideY = remember { Animatable(0f) }
    var drumSlideDirection by remember { mutableIntStateOf(1) }

    fun triggerTickHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } catch (_: Exception) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun stepSelection(deltaSteps: Int) {
        if (filteredPokemon.isEmpty()) return
        val count = filteredPokemon.size
        val newIndex = ((selectedIndex + deltaSteps) % count + count) % count
        if (newIndex != selectedIndex) {
            drumSlideDirection = if (deltaSteps > 0) 1 else -1
            selectedIndex = newIndex
            lensTickTrigger++
            lastScrollTimestamp = System.currentTimeMillis()
            triggerTickHaptic()
            coroutineScope.launch {
                drumSlideY.snapTo(if (drumSlideDirection > 0) 4f else -4f)
                drumSlideY.animateTo(0f, animationSpec = tween(110, easing = FastOutSlowInEasing))
            }
        }
    }

    // Auto-focus physical crown on launch
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Classic GBA 2-frame Cursor Bob: 0px <-> 2px stepped every 320ms
    val cursorStep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(640, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cursor_bob"
    )
    val cursorBobPx = if (cursorStep >= 1f) 2f else 0f

    // Locked silhouette breathing alpha
    val silhouettePulse by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "silhouette_pulse"
    )

    // Specimen CRT Scanline (0..34) & 2-Hop Animation (0 -> -3 -> 0 -> -1 -> 0)
    val scanlineY = remember { Animatable(0f) }
    val hopY = remember { Animatable(0f) }
    var tapShakeOffset by remember { mutableFloatStateOf(0f) }
    var isSpecimenLedFlashing by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPokemon?.pokedexNumber) {
        if (selectedPokemon != null) {
            // 140ms top-to-bottom CRT scanline wipe (0 -> 34)
            scanlineY.snapTo(0f)
            scanlineY.animateTo(34f, animationSpec = tween(140, easing = LinearEasing))

            // GBA Emerald 2-Hop entry if unlocked
            if (selectedPokemon.isUnlocked) {
                hopY.snapTo(0f)
                hopY.animateTo(-3f, animationSpec = tween(60, easing = FastOutSlowInEasing))
                hopY.animateTo(0f, animationSpec = tween(50, easing = FastOutSlowInEasing))
                hopY.animateTo(-1f, animationSpec = tween(40, easing = FastOutSlowInEasing))
                hopY.animateTo(0f, animationSpec = tween(40, easing = FastOutSlowInEasing))
            } else {
                hopY.snapTo(0f)
            }
        }
    }

    // Smooth progress bar animated fill
    val targetRatio = if (selectedPokemon?.isUnlocked == true) 1f else selectedPokemon?.progressRatio?.toFloat()?.coerceIn(0f, 1f) ?: 0f
    val animatedProgressRatio by animateFloatAsState(
        targetValue = targetRatio,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "progress_ratio"
    )

    // Tactile button press interactions (1px physical drop)
    val backInteractionSource = remember { MutableInteractionSource() }
    val isBackPressed by backInteractionSource.collectIsPressedAsState()

    val actionInteractionSource = remember { MutableInteractionSource() }
    val isActionPressed by actionInteractionSource.collectIsPressedAsState()

    GbaWatchCanvas(
        modifier = modifier
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onRotaryScrollEvent { event ->
                val now = System.currentTimeMillis()
                val delta = event.verticalScrollPixels
                val timeDiff = now - lastRotaryTimestamp
                lastRotaryTimestamp = now

                // Fast-spin acceleration: consecutive ticks < 35ms with high delta step by 3
                val isFastSpin = timeDiff in 1..35 && abs(delta) > 55f
                val stepMultiplier = if (isFastSpin) 3 else 1

                rotaryAccumulator += delta
                if (abs(rotaryAccumulator) >= stepThreshold) {
                    val rawSteps = (rotaryAccumulator / stepThreshold).toInt()
                    rotaryAccumulator %= stepThreshold
                    stepSelection(rawSteps * stepMultiplier)
                }
                true
            }
    ) { scale ->

        // 1. BASE BACKGROUND (192x192 at (0, 0))
        val shellRes = if (isRound) {
            R.drawable.gba_pokedex_watch_round_empty_tracks
        } else {
            R.drawable.gba_pokedex_watch_square_empty_tracks
        }
        GbaPixelImage(
            resId = shellRes,
            contentDescription = "GBA Pokédex Shell",
            modifier = Modifier.gbaVirtualBounds(0, 0, 192, 192, scale)
        )

        // 2. TOP SENSOR ARRAY & ZONE 1 COUNTER BOX
        // Blue Glass Camera Lens Glow: center = (49, 32), radius = 4
        Canvas(
            modifier = Modifier.gbaVirtualBounds(45, 28, 8, 8, scale)
        ) {
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val r = 4f * scale * density
            // Ambient cyan breathing glow
            drawCircle(
                color = Color(0xFF80F8FF).copy(alpha = lensPulseAlpha),
                radius = r,
                center = centerOffset
            )
            // 140ms bright tick flash
            if (lensFlashAlpha > 0f) {
                drawCircle(
                    color = Color(0xFFFFFFFF).copy(alpha = lensFlashAlpha * 0.95f),
                    radius = r * 1.25f,
                    center = centerOffset
                )
            }
        }

        // 3 Indicator LEDs: (134, 32) Red, (141, 32) Yellow, (148, 32) Green
        Canvas(
            modifier = Modifier.gbaVirtualBounds(130, 29, 24, 6, scale)
        ) {
            val cy = 3f * scale * density
            val baseRadius = 1.5f * scale * density
            val activeRadius = 2.4f * scale * density

            // Red LED (x = 134)
            val rx = 4f * scale * density
            val isRedActive = activeLedIndex == 0
            if (isRedActive) {
                drawCircle(color = Color(0x66FF2020), radius = activeRadius * 1.5f, center = Offset(rx, cy))
            }
            drawCircle(
                color = if (isRedActive) Color(0xFFFF2525) else Color(0xFF5A1515),
                radius = if (isRedActive) activeRadius else baseRadius,
                center = Offset(rx, cy)
            )

            // Yellow LED (x = 141)
            val yx = 11f * scale * density
            val isYellowActive = activeLedIndex == 1
            if (isYellowActive) {
                drawCircle(color = Color(0x66FFFF20), radius = activeRadius * 1.5f, center = Offset(yx, cy))
            }
            drawCircle(
                color = if (isYellowActive) Color(0xFFFFDD25) else Color(0xFF5A5015),
                radius = if (isYellowActive) activeRadius else baseRadius,
                center = Offset(yx, cy)
            )

            // Green LED (x = 148)
            val gx = 18f * scale * density
            val isGreenActive = activeLedIndex == 2
            if (isGreenActive) {
                drawCircle(color = Color(0x6620FF20), radius = activeRadius * 1.5f, center = Offset(gx, cy))
            }
            drawCircle(
                color = if (isGreenActive) Color(0xFF25FF25) else Color(0xFF155A15),
                radius = if (isGreenActive) activeRadius else baseRadius,
                center = Offset(gx, cy)
            )
        }

        // Zone 1 Counter Box: (x = 64, y = 24, w = 64, h = 17)
        Box(
            modifier = Modifier
                .gbaVirtualBounds(64, 24, 64, 17, scale)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val nextFilter = when (currentFilter) {
                        PokedexFilter.ALL -> PokedexFilter.UNLOCKED
                        PokedexFilter.UNLOCKED -> PokedexFilter.LOCKED
                        PokedexFilter.LOCKED -> PokedexFilter.ALL
                    }
                    currentFilter = nextFilter
                    selectedIndex = 0
                    lensTickTrigger++
                    filterFeedbackText = nextFilter.label
                    coroutineScope.launch {
                        delay(800)
                        if (filterFeedbackText == nextFilter.label) {
                            filterFeedbackText = null
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            GbaPixelImage(
                resId = R.drawable.gba_pokedex_top_counter_pill,
                contentDescription = "Counter Box",
                modifier = Modifier.fillMaxSize()
            )
            val counterText = filterFeedbackText ?: "${selectedPokemon?.pokedexNumber?.toString()?.padStart(3, '0') ?: "001"}/151"
            val counterFontSize = if (filterFeedbackText != null) 9.8f else 11.5f
            GbaText(
                text = counterText,
                scale = scale,
                color = Color(0xFFF8F8F8),
                fontSizeVirtualPx = counterFontSize,
                lineHeightVirtualPx = counterFontSize,
                fontWeight = FontWeight.ExtraBold,
                scaleX = 1.05f,
                textAlign = TextAlign.Center
            )
        }

        // 3. LEFT KANTO METALLIC CRT SPECIMEN MONITOR: (x = 32, y = 50, w = 56, h = 54)
        Box(
            modifier = Modifier
                .gbaVirtualBounds(32, 50, 56, 54, scale)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    coroutineScope.launch {
                        isSpecimenLedFlashing = true
                        for (i in 0..2) {
                            tapShakeOffset = -2f
                            delay(35)
                            tapShakeOffset = 2f
                            delay(35)
                        }
                        tapShakeOffset = 0f
                        delay(120)
                        isSpecimenLedFlashing = false
                    }
                }
        ) {
            GbaPixelImage(
                resId = R.drawable.gba_pokedex_specimen_box,
                contentDescription = "Specimen Monitor",
                modifier = Modifier.fillMaxSize()
            )

            // Flashing Red LED dots at (56, 52) & (63, 52) -> relative to monitor box: x = 24, 31, y = 2
            if (isSpecimenLedFlashing) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cy = 2f * scale * density
                    val r = 1.6f * scale * density
                    val dot1X = 24f * scale * density
                    val dot2X = 31f * scale * density
                    drawCircle(color = Color(0xFFFF2020), radius = r, center = Offset(dot1X, cy))
                    drawCircle(color = Color(0xFFFF2020), radius = r, center = Offset(dot2X, cy))
                }
            }
        }

        // CRT Sprite Window: (x = 42, y = 55, w = 36, h = 34)
        val currentPoke = selectedPokemon
        if (currentPoke != null) {
            val spriteUri = Gen3PokemonSpriteResolver.getFrontSpriteUri(currentPoke.pokedexNumber)
            val isLocked = !currentPoke.isUnlocked

            Box(
                modifier = Modifier
                    .gbaVirtualBounds(42, 55, 36, 34, scale)
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(spriteUri)
                        .crossfade(false)
                        .error(R.drawable.gba_ball_empty)
                        .fallback(R.drawable.gba_ball_empty)
                        .build(),
                    contentDescription = currentPoke.name,
                    filterQuality = FilterQuality.None,
                    contentScale = ContentScale.Fit,
                    colorFilter = if (isLocked) {
                        ColorFilter.tint(Color(0xFF243030))
                    } else null,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(
                            x = (tapShakeOffset * scale).dp,
                            y = (hopY.value * scale).dp
                        )
                        .alpha(if (isLocked) silhouettePulse else 1.0f)
                )

                // 140ms CRT Scanline wipe (0..34)
                if (scanlineY.value < 34f) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val curY = scanlineY.value * scale * density
                        drawLine(
                            color = Color(0x99B2EBF2),
                            start = Offset(0f, curY),
                            end = Offset(size.width, curY),
                            strokeWidth = 2f * scale * density
                        )
                    }
                }
            }
        }

        // Bottom Integrated Nameplate Strip: (x = 38, y = 91, w = 46, h = 10)
        val nameplateId = "#${selectedPokemon?.pokedexNumber?.toString()?.padStart(3, '0') ?: "001"}"
        GbaText(
            text = nameplateId,
            scale = scale,
            color = Color(0xFF283030),
            fontSizeVirtualPx = 10.5f,
            lineHeightVirtualPx = 10.5f,
            fontWeight = FontWeight.ExtraBold,
            scaleX = 1.05f,
            textAlign = TextAlign.Center,
            modifier = Modifier.gbaVirtualBounds(38, 91, 46, 10, scale)
        )

        // 4. ZONE 2 — RIGHT 3-SLOT GBA ROTARY WHEEL (x = 93..161, y = 50..104)
        Box(
            modifier = Modifier
                .gbaVirtualBounds(93, 50, 68, 54, scale)
                .pointerInput(Unit) {
                    var accumulatedDrag = 0f
                    detectVerticalDragGestures(
                        onDragEnd = { accumulatedDrag = 0f },
                        onDragCancel = { accumulatedDrag = 0f }
                    ) { change, dragAmount ->
                        change.consume()
                        accumulatedDrag += dragAmount
                        val dragThreshold = 22f * density
                        if (abs(accumulatedDrag) >= dragThreshold) {
                            val steps = -(accumulatedDrag / dragThreshold).toInt()
                            accumulatedDrag %= dragThreshold
                            stepSelection(steps)
                        }
                    }
                }
        ) {
            val count = filteredPokemon.size
            val prevIndex = ((selectedIndex - 1) % count + count) % count
            val nextIndex = ((selectedIndex + 1) % count + count) % count
            val prevPokemon = filteredPokemon.getOrNull(prevIndex)
            val nextPokemon = filteredPokemon.getOrNull(nextIndex)

            // Inner sliding drum container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (drumSlideY.value * scale).dp)
            ) {
                // Top Slot (index - 1, x = 93, y = 50, w = 68, h = 15) -> local (0, 0, 68, 15)
                if (prevPokemon != null && count > 1) {
                    Box(
                        modifier = Modifier
                            .gbaVirtualBounds(0, 0, 68, 15, scale)
                            .clickable { stepSelection(-1) }
                    ) {
                        GbaPixelImage(
                            resId = R.drawable.gba_pokedex_row_unselected,
                            contentDescription = "Prev Slot",
                            modifier = Modifier.fillMaxSize()
                        )
                        if (prevPokemon.isUnlocked) {
                            val prevNameSize = when {
                                prevPokemon.name.length > 9 -> 8.2f
                                prevPokemon.name.length > 7 -> 9.0f
                                else -> 9.8f
                            }
                            GbaText(
                                text = prevPokemon.name.uppercase(),
                                scale = scale,
                                color = Color(0xFF606858),
                                fontSizeVirtualPx = prevNameSize,
                                lineHeightVirtualPx = prevNameSize,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.gbaVirtualBounds(6, 2, 58, 11, scale)
                            )
                        } else {
                            GbaPixelImage(
                                resId = R.drawable.gba_pokedex_icon_lock,
                                contentDescription = "Locked",
                                modifier = Modifier.gbaVirtualBounds(6, 3, 7, 9, scale)
                            )
                            GbaText(
                                text = "???",
                                scale = scale,
                                color = Color(0xFF606858),
                                fontSizeVirtualPx = 9.8f,
                                lineHeightVirtualPx = 9.8f,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.gbaVirtualBounds(16, 2, 48, 11, scale)
                            )
                        }
                    }
                }

                // Center Active Selected Slot (index, x = 93, y = 67, w = 68, h = 20) -> local (0, 17, 68, 20)
                if (selectedPokemon != null) {
                    Box(
                        modifier = Modifier.gbaVirtualBounds(0, 17, 68, 20, scale)
                    ) {
                        GbaPixelImage(
                            resId = R.drawable.gba_pokedex_row_selected,
                            contentDescription = "Selected Slot",
                            modifier = Modifier.fillMaxSize()
                        )

                        // GBA Cursor Black (6x10 px ▶) with 2-frame bob at x = 98 -> local x = 5 + cursorBobPx
                        GbaPixelImage(
                            resId = R.drawable.gba_cursor_black,
                            contentDescription = "Cursor",
                            modifier = Modifier
                                .offset(
                                    x = ((5 + cursorBobPx) * scale).dp,
                                    y = (5 * scale).dp
                                )
                                .size((6 * scale).dp, (10 * scale).dp)
                        )

                        if (selectedPokemon.isUnlocked) {
                            val selectedNameSize = when {
                                selectedPokemon.name.length > 9 -> 10.2f
                                selectedPokemon.name.length > 7 -> 11.2f
                                else -> 12.2f
                            }
                            GbaText(
                                text = selectedPokemon.name.uppercase(),
                                scale = scale,
                                color = Color(0xFF202828),
                                fontSizeVirtualPx = selectedNameSize,
                                lineHeightVirtualPx = selectedNameSize,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                modifier = Modifier.gbaVirtualBounds(14, 3, 52, 14, scale)
                            )
                        } else {
                            GbaPixelImage(
                                resId = R.drawable.gba_pokedex_icon_lock,
                                contentDescription = "Locked",
                                modifier = Modifier.gbaVirtualBounds(14, 5, 7, 9, scale)
                            )
                            GbaText(
                                text = "???",
                                scale = scale,
                                color = Color(0xFF202828),
                                fontSizeVirtualPx = 12.0f,
                                lineHeightVirtualPx = 12.0f,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.gbaVirtualBounds(24, 3, 42, 14, scale)
                            )
                        }
                    }
                }

                // Bottom Slot (index + 1, x = 93, y = 89, w = 68, h = 15) -> local (0, 39, 68, 15)
                if (nextPokemon != null && count > 1) {
                    Box(
                        modifier = Modifier
                            .gbaVirtualBounds(0, 39, 68, 15, scale)
                            .clickable { stepSelection(1) }
                    ) {
                        GbaPixelImage(
                            resId = R.drawable.gba_pokedex_row_unselected,
                            contentDescription = "Next Slot",
                            modifier = Modifier.fillMaxSize()
                        )
                        if (nextPokemon.isUnlocked) {
                            val nextNameSize = when {
                                nextPokemon.name.length > 9 -> 8.2f
                                nextPokemon.name.length > 7 -> 9.0f
                                else -> 9.8f
                            }
                            GbaText(
                                text = nextPokemon.name.uppercase(),
                                scale = scale,
                                color = Color(0xFF606858),
                                fontSizeVirtualPx = nextNameSize,
                                lineHeightVirtualPx = nextNameSize,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.gbaVirtualBounds(6, 2, 58, 11, scale)
                            )
                        } else {
                            GbaPixelImage(
                                resId = R.drawable.gba_pokedex_icon_lock,
                                contentDescription = "Locked",
                                modifier = Modifier.gbaVirtualBounds(6, 3, 7, 9, scale)
                            )
                            GbaText(
                                text = "???",
                                scale = scale,
                                color = Color(0xFF606858),
                                fontSizeVirtualPx = 9.8f,
                                lineHeightVirtualPx = 9.8f,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.gbaVirtualBounds(16, 2, 48, 11, scale)
                            )
                        }
                    }
                }
            }
        }

        // 5. ZONE 3 — GBA GOLD-FRAMED TEAL MISSION & PROGRESS BAR: (x = 38, y = 109, w = 117, h = 20)
        GbaPixelImage(
            resId = R.drawable.gba_pokedex_mission_bar,
            contentDescription = "Mission Bar Background",
            modifier = Modifier.gbaVirtualBounds(38, 109, 117, 20, scale)
        )

        // Left Status / Short Mission Label: (x = 42, y = 112, w = 33, h = 14)
        if (selectedPokemon != null) {
            val statusLabel = if (selectedPokemon.isUnlocked) "READY" else getCompactMissionTag(selectedPokemon)
            val statusFontSize = if (statusLabel.length > 6) 8.2f else 9.5f
            GbaText(
                text = statusLabel,
                scale = scale,
                color = Color(0xFFF8F8F8),
                fontSizeVirtualPx = statusFontSize,
                lineHeightVirtualPx = statusFontSize,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Start,
                maxLines = 1,
                modifier = Modifier.gbaVirtualBounds(42, 112, 33, 14, scale)
            )

            // Center Recessed Progress Bar Fill: (x = 76, y = 116, maxWidth = 46, height = 5)
            val barWidth = (46f * animatedProgressRatio).roundToInt().coerceIn(0, 46)
            if (barWidth > 0) {
                val barRes = if (selectedPokemon.isUnlocked) R.drawable.gba_pokedex_bar_green_5px else R.drawable.gba_pokedex_bar_yellow_5px
                GbaPixelImage(
                    resId = barRes,
                    contentDescription = "Progress Bar Fill",
                    modifier = Modifier.gbaVirtualBounds(76, 116, barWidth, 5, scale)
                )
            }

            // Right Percentage Readout: (x = 123, y = 112, w = 29, h = 14)
            val percentText = "${(targetRatio * 100).roundToInt()}%"
            GbaText(
                text = percentText,
                scale = scale,
                color = Color(0xFFF8D840),
                fontSizeVirtualPx = 9.5f,
                lineHeightVirtualPx = 9.5f,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.gbaVirtualBounds(123, 112, 29, 14, scale)
            )
        }

        // 6. ZONE 4 — 3D GBA ACTION BUTTONS & FOOTER HINT (y = 134..171)
        // Left BACK Button: (x = 42, y = 134, w = 31, h = 26)
        val backY = if (isBackPressed) 135 else 134
        Box(
            modifier = Modifier
                .gbaVirtualBounds(42, backY, 31, 26, scale)
                .clickable(
                    interactionSource = backInteractionSource,
                    indication = null
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigateBack()
                },
            contentAlignment = Alignment.Center
        ) {
            GbaPixelImage(
                resId = R.drawable.gba_pokedex_btn_back,
                contentDescription = "Back Button",
                modifier = Modifier.fillMaxSize()
            )
            GbaText(
                text = "BACK",
                scale = scale,
                color = Color(0xFF283038),
                fontSizeVirtualPx = 12.0f,
                lineHeightVirtualPx = 12.0f,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }

        // Right Contextual Action Button: (x = 78, y = 134, w = 72, h = 26)
        if (selectedPokemon != null) {
            val isUnlocked = selectedPokemon.isUnlocked
            val actionBtnRes = if (isUnlocked) R.drawable.gba_pokedex_btn_buddy else R.drawable.gba_pokedex_btn_pin
            val actionLabel = if (isUnlocked) "SET BUDDY" else "PIN TARGET"
            val actionY = if (isActionPressed) 135 else 134

            Box(
                modifier = Modifier
                    .gbaVirtualBounds(78, actionY, 72, 26, scale)
                    .clickable(
                        interactionSource = actionInteractionSource,
                        indication = null
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isUnlocked) {
                            viewModel.setActiveBuddy(selectedPokemon.pokedexNumber)
                        } else {
                            viewModel.pinWildTarget(selectedPokemon.pokedexNumber)
                        }
                        onNavigateBack()
                    },
                contentAlignment = Alignment.Center
            ) {
                GbaPixelImage(
                    resId = actionBtnRes,
                    contentDescription = actionLabel,
                    modifier = Modifier.fillMaxSize()
                )
                GbaText(
                    text = actionLabel,
                    scale = scale,
                    color = Color.White,
                    fontSizeVirtualPx = 11.8f,
                    lineHeightVirtualPx = 12.0f,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Bottom Footer Hint: (x = 26, y = 162, w = 140, h = 11)
        GbaText(
            text = "TURN CROWN TO SCROLL",
            scale = scale,
            color = Color(0xFF703028),
            fontSizeVirtualPx = 8.5f,
            lineHeightVirtualPx = 9.0f,
            fontWeight = FontWeight.ExtraBold,
            scaleX = 1.05f,
            textAlign = TextAlign.Center,
            modifier = Modifier.gbaVirtualBounds(26, 162, 140, 11, scale)
        )
    }
}
