package com.poketch.wear.ui.battle

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.unit.sp
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
import kotlin.math.abs
import kotlin.math.roundToInt

enum class PokedexFilter(val label: String) {
    ALL("< ALL 151 >"),
    UNLOCKED("< UNLOCKED >"),
    LOCKED("< LOCKED >"),
    WATER("< WATER >"),
    RUN("< RUN >"),
    KCAL("< KCAL >"),
    SLEEP("< SLEEP >")
}

/**
 * Watch-Optimized GBA Pokédex Screen (`GbaWatchPokedexScreen.kt`).
 * Authentic 192x192 Virtual Pixel Canvas layout for Wear OS:
 * - Top Cyan Lens LED with pulse & rotary flash
 * - Top Category Filter Pill (< ALL 151 >, < UNLOCKED >, < LOCKED >, etc.)
 * - Left 68x68 Specimen Monitor with CRT scanline reveal, emerald hops, tap shake
 * - Right 3-Slot Rotary Wheel with GBA cursor bob, ball status icons, and mechanical slide
 * - Scrollbar Track & Thumb
 * - Bottom Dark-Teal Mission Dock with Typewriter readout & dynamic HP bar fill
 * - Tactile 1px Action Buttons: BACK & SET BUDDY / PIN TARGET with action confirmation
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

    // Intercept Back button to navigate back to Battle Home Screen
    BackHandler {
        onNavigateBack()
    }

    // Filter selection state
    var currentFilter by remember { mutableStateOf(PokedexFilter.ALL) }
    var selectedIndex by remember { mutableIntStateOf(0) }

    // Filtered list of Pokémon based on active pill
    val filteredPokemon = remember(state.allPokemon, currentFilter) {
        val list = when (currentFilter) {
            PokedexFilter.ALL -> state.allPokemon
            PokedexFilter.UNLOCKED -> state.allPokemon.filter { it.isUnlocked }
            PokedexFilter.LOCKED -> state.allPokemon.filter { !it.isUnlocked }
            PokedexFilter.WATER -> state.allPokemon.filter { it.unlockRule.category == PokemonCategory.WATER_HYDRATION }
            PokedexFilter.RUN -> state.allPokemon.filter { it.unlockRule.category == PokemonCategory.RUN_STEPS }
            PokedexFilter.KCAL -> state.allPokemon.filter { it.unlockRule.category == PokemonCategory.CALORIE_DEFICIT }
            PokedexFilter.SLEEP -> state.allPokemon.filter { it.unlockRule.category == PokemonCategory.SLEEP_STREAK }
        }
        if (list.isEmpty()) state.allPokemon else list
    }

    // Clamp selected index when filter changes
    LaunchedEffect(filteredPokemon.size) {
        if (filteredPokemon.isNotEmpty()) {
            selectedIndex = selectedIndex.coerceIn(0, filteredPokemon.lastIndex)
        }
    }

    // Currently focused Pokémon
    val selectedPokemon: Pokemon? = filteredPokemon.getOrNull(selectedIndex)

    // Cyan LED flash trigger (spikes to 1.0f on rotary tick or filter change)
    var ledFlashTrigger by remember { mutableIntStateOf(0) }
    val ledFlashAlpha by animateFloatAsState(
        targetValue = if (ledFlashTrigger > 0) 1.0f else 0.45f,
        animationSpec = tween(150, easing = LinearEasing),
        label = "led_flash"
    )
    LaunchedEffect(ledFlashTrigger) {
        if (ledFlashTrigger > 0) {
            delay(120)
            ledFlashTrigger = 0
        }
    }

    // Subtle continuous breathing pulse for Cyan LED
    val infiniteTransition = rememberInfiniteTransition(label = "pokedex_fx")
    val ledBreathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "led_breathing"
    )

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

    // Rotary & Drag delta accumulators
    var rotaryAccumulator by remember { mutableFloatStateOf(0f) }
    var lastRotaryTimestamp by remember { mutableLongStateOf(0L) }
    val stepThreshold = 26f

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
            selectedIndex = newIndex
            ledFlashTrigger++
            triggerTickHaptic()
        }
    }

    // Auto-focus physical crown on launch
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Specimen CRT Scanline & 2-Hop Animation
    val scanlineY = remember { Animatable(0f) }
    val hopY = remember { Animatable(0f) }
    var tapShakeOffset by remember { mutableFloatStateOf(0f) }
    var isMonitorSparkling by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPokemon?.pokedexNumber) {
        if (selectedPokemon != null) {
            // 1. Scanline sweep 0 -> 64 over 140ms
            scanlineY.snapTo(0f)
            scanlineY.animateTo(64f, animationSpec = tween(140, easing = LinearEasing))

            // 2. Emerald 2-Hop entry if unlocked (0 -> -4 -> 0 -> -2 -> 0)
            if (selectedPokemon.isUnlocked) {
                hopY.snapTo(0f)
                hopY.animateTo(-4f, animationSpec = tween(60, easing = FastOutSlowInEasing))
                hopY.animateTo(0f, animationSpec = tween(50, easing = FastOutSlowInEasing))
                hopY.animateTo(-2f, animationSpec = tween(40, easing = FastOutSlowInEasing))
                hopY.animateTo(0f, animationSpec = tween(40, easing = FastOutSlowInEasing))
            } else {
                hopY.snapTo(0f)
            }
        }
    }

    // Typewriter effect for mission description text
    var displayedMissionText by remember { mutableStateOf("") }
    var actionConfirmationMsg by remember { mutableStateOf<String?>(null) }
    var isGoldFlashing by remember { mutableStateOf(false) }

    val fullMissionText = remember(selectedPokemon?.pokedexNumber, selectedPokemon?.unlockRule?.description) {
        selectedPokemon?.let {
            if (it.isUnlocked) "UNLOCKED! READY TO BATTLE AS BUDDY."
            else "MISSION: ${it.unlockRule.description}"
        } ?: ""
    }

    LaunchedEffect(fullMissionText) {
        displayedMissionText = ""
        for (i in 1..fullMissionText.length) {
            displayedMissionText = fullMissionText.substring(0, i)
            delay(12)
        }
    }

    // Smooth progress bar animated fill
    val targetProgressRatio = selectedPokemon?.progressRatio?.toFloat() ?: 0f
    val animatedProgressRatio by animateFloatAsState(
        targetValue = targetProgressRatio,
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "progress_fill"
    )

    // Gold flash transition on action confirmation
    val goldFlashAlpha by animateFloatAsState(
        targetValue = if (isGoldFlashing) 1.0f else 0f,
        animationSpec = tween(180),
        label = "gold_flash"
    )

    // Button press states (1px tactile drop)
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

        // 1. BASE POKÉDEX SHELL (192x192 at (0,0))
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

        // 2. TOP CYAN LED LENS (center = (50, 20), radius = 5)
        val activeLedAlpha = maxOf(ledBreathingAlpha, ledFlashAlpha)
        Canvas(
            modifier = Modifier.gbaVirtualBounds(43, 13, 14, 14, scale)
        ) {
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val glowColor = if (isGoldFlashing) Color(0xFFFFD700) else Color(0xFF00E5FF)
            drawCircle(
                color = glowColor.copy(alpha = activeLedAlpha * 0.45f),
                radius = size.width * 0.48f,
                center = centerOffset
            )
            drawCircle(
                color = if (isGoldFlashing) Color(0xFFFFF9C4) else Color(0xFFE0F7FA),
                radius = size.width * 0.24f,
                center = centerOffset
            )
        }

        // 3. TOP CATEGORY FILTER PILL ((x = 62, y = 12, w = 78, h = 17))
        Box(
            modifier = Modifier
                .gbaVirtualBounds(62, 12, 78, 17, scale)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val filters = PokedexFilter.values()
                    val nextOrdinal = (currentFilter.ordinal + 1) % filters.size
                    currentFilter = filters[nextOrdinal]
                    selectedIndex = 0
                    ledFlashTrigger++
                },
            contentAlignment = Alignment.Center
        ) {
            GbaPixelImage(
                resId = R.drawable.gba_pokedex_filter_pill_78x17,
                contentDescription = "Category Filter",
                modifier = Modifier.fillMaxSize()
            )
            GbaText(
                text = currentFilter.label,
                scale = scale,
                color = Color(0xFF263238),
                fontSizeVirtualPx = 8f,
                scaleX = 1.05f,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // 4. LEFT 68x68 SPECIMEN MONITOR ((x = 16, y = 40, w = 68, h = 68))
        Box(
            modifier = Modifier
                .gbaVirtualBounds(16, 40, 68, 68, scale)
                .clip(RoundedCornerShape((6 * scale).dp))
                .clickable {
                    // Tap-to-cry shake & sparkle animation
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    coroutineScope.launch {
                        isMonitorSparkling = true
                        for (i in 0..2) {
                            tapShakeOffset = -2f
                            delay(35)
                            tapShakeOffset = 2f
                            delay(35)
                        }
                        tapShakeOffset = 0f
                        delay(120)
                        isMonitorSparkling = false
                    }
                }
        ) {
            // Monitor Frame
            GbaPixelImage(
                resId = R.drawable.gba_pokedex_specimen_frame_68x68,
                contentDescription = "Specimen Frame",
                modifier = Modifier.fillMaxSize()
            )

            // Sparkle Gold Border overlay when tapped
            if (isMonitorSparkling || isGoldFlashing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border((2 * scale).dp, Color(0xFFFFD54F), RoundedCornerShape((4 * scale).dp))
                )
            }

            // 64x64 Sprite container at (x = 2, y = 2) relative to frame
            val currentPoke = selectedPokemon
            if (currentPoke != null) {
                val spriteUri = Gen3PokemonSpriteResolver.getFrontSpriteUri(currentPoke.pokedexNumber)
                val isLocked = !currentPoke.isUnlocked

                Box(
                    modifier = Modifier
                        .size((64 * scale).dp)
                        .align(Alignment.Center)
                        .clipToBounds()
                        .offset(
                            x = (tapShakeOffset * scale).dp,
                            y = (hopY.value * scale).dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Sprite Image
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(spriteUri)
                            .crossfade(false)
                            .error(R.drawable.gba_ball_healthy)
                            .fallback(R.drawable.gba_ball_healthy)
                            .build(),
                        contentDescription = currentPoke.name,
                        filterQuality = FilterQuality.None,
                        contentScale = ContentScale.Fit,
                        colorFilter = if (isLocked) {
                            ColorFilter.tint(Color(0xFF1E2822), BlendMode.SrcIn)
                        } else null,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(if (isLocked) silhouettePulse else 1.0f)
                    )

                    // CRT Scanline sweep bar (top -> bottom reveal)
                    if (scanlineY.value < 64f) {
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

                    // Locked '?' badge in top right
                    if (isLocked) {
                        GbaText(
                            text = "?",
                            scale = scale,
                            color = Color(0xFFFFCA28),
                            fontSizeVirtualPx = 11f,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-4 * scale).dp, y = (2 * scale).dp)
                        )
                    }
                }
            }
        }

        // 5. RIGHT 3-SLOT ROTARY WHEEL (x = 88..168, y = 40..106)
        // Attach touch drag gestures on wheel area
        Box(
            modifier = Modifier
                .gbaVirtualBounds(88, 38, 80, 72, scale)
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

            // Slot Top: index - 1 (seamless wrap)
            if (count > 1) {
                val prevIndex = (selectedIndex - 1 + count) % count
                val prevPoke = filteredPokemon[prevIndex]
                val prevBallRes = if (prevPoke.isUnlocked) R.drawable.gba_ball_healthy else R.drawable.gba_ball_empty
                val prevName = if (prevPoke.isUnlocked) prevPoke.name.uppercase() else "???"

                Box(
                    modifier = Modifier
                        .gbaVirtualBounds(2, 2, 76, 19, scale)
                        .clickable { stepSelection(-1) }
                ) {
                    GbaPixelImage(
                        resId = R.drawable.gba_pokedex_row_unselected_76x19,
                        contentDescription = "Prev Row",
                        modifier = Modifier.fillMaxSize()
                    )
                    GbaPixelImage(
                        resId = prevBallRes,
                        contentDescription = "Ball",
                        modifier = Modifier.gbaVirtualBounds(6, 6, 7, 7, scale)
                    )
                    GbaText(
                        text = "#%03d %s".format(prevPoke.pokedexNumber, prevName),
                        scale = scale,
                        color = Color(0xFF546E7A),
                        fontSizeVirtualPx = 8.5f,
                        scaleX = 1.05f,
                        modifier = Modifier.gbaVirtualBounds(16, 5, 58, 12, scale)
                    )
                }
            }

            // Slot Center: index (Active Selected Slot)
            if (selectedPokemon != null) {
                val centerBallRes = if (selectedPokemon.isUnlocked) R.drawable.gba_ball_healthy else R.drawable.gba_ball_empty
                val centerName = if (selectedPokemon.isUnlocked) selectedPokemon.name.uppercase() else "???"

                Box(
                    modifier = Modifier.gbaVirtualBounds(0, 23, 80, 24, scale)
                ) {
                    GbaPixelImage(
                        resId = R.drawable.gba_pokedex_row_selected_80x24,
                        contentDescription = "Selected Row",
                        modifier = Modifier.fillMaxSize()
                    )

                    // GBA Cursor Black (▶) with 2-frame bob
                    GbaPixelImage(
                        resId = R.drawable.gba_cursor_black,
                        contentDescription = "Cursor",
                        modifier = Modifier
                            .offset(
                                x = ((4 + cursorBobPx) * scale).dp,
                                y = (7 * scale).dp
                            )
                            .size((6 * scale).dp, (10 * scale).dp)
                    )

                    // Status Pokeball Icon
                    GbaPixelImage(
                        resId = centerBallRes,
                        contentDescription = "Active Ball",
                        modifier = Modifier.gbaVirtualBounds(12, 8, 7, 7, scale)
                    )

                    // #ID + Name (Bold GBA typography, full name displayed)
                    GbaText(
                        text = "#%03d %s".format(selectedPokemon.pokedexNumber, centerName),
                        scale = scale,
                        color = Color(0xFF1B241C),
                        fontSizeVirtualPx = 9.5f,
                        scaleX = 1.08f,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.gbaVirtualBounds(21, 6, 57, 14, scale)
                    )
                }
            }

            // Slot Bottom: index + 1 (seamless wrap)
            if (count > 1) {
                val nextIndex = (selectedIndex + 1) % count
                val nextPoke = filteredPokemon[nextIndex]
                val nextBallRes = if (nextPoke.isUnlocked) R.drawable.gba_ball_healthy else R.drawable.gba_ball_empty
                val nextName = if (nextPoke.isUnlocked) nextPoke.name.uppercase() else "???"

                Box(
                    modifier = Modifier
                        .gbaVirtualBounds(2, 49, 76, 19, scale)
                        .clickable { stepSelection(1) }
                ) {
                    GbaPixelImage(
                        resId = R.drawable.gba_pokedex_row_unselected_76x19,
                        contentDescription = "Next Row",
                        modifier = Modifier.fillMaxSize()
                    )
                    GbaPixelImage(
                        resId = nextBallRes,
                        contentDescription = "Ball",
                        modifier = Modifier.gbaVirtualBounds(6, 6, 7, 7, scale)
                    )
                    GbaText(
                        text = "#%03d %s".format(nextPoke.pokedexNumber, nextName),
                        scale = scale,
                        color = Color(0xFF546E7A),
                        fontSizeVirtualPx = 8.5f,
                        scaleX = 1.05f,
                        modifier = Modifier.gbaVirtualBounds(16, 5, 58, 12, scale)
                    )
                }
            }
        }

        // 6. SCROLLBAR THUMB (Track: x = 171, y = 42, w = 5, h = 64)
        val scrollbarFraction = if (filteredPokemon.size > 1) {
            selectedIndex.toFloat() / (filteredPokemon.size - 1).toFloat()
        } else 0f
        val thumbTopY = (44f + (50f * scrollbarFraction)).roundToInt().coerceIn(44, 94)
        Box(
            modifier = Modifier
                .gbaVirtualBounds(172, thumbTopY, 3, 10, scale)
                .background(Color(0xFF81C784), RoundedCornerShape((1 * scale).dp))
        )

        // 7. BOTTOM DARK-TEAL MISSION & PROGRESS DOCK ((x = 22, y = 111, w = 148, h = 36))
        Box(
            modifier = Modifier.gbaVirtualBounds(22, 111, 148, 36, scale)
        ) {
            GbaPixelImage(
                resId = R.drawable.gba_pokedex_mission_box_148x36,
                contentDescription = "Mission Box",
                modifier = Modifier.fillMaxSize()
            )

            // Flash overlay if action confirmed
            if (isGoldFlashing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border((1.5f * scale).dp, Color(0xFFFFD54F), RoundedCornerShape((3 * scale).dp))
                )
            }

            // Mission description or confirmation text
            val messageToRender = actionConfirmationMsg ?: displayedMissionText
            GbaText(
                text = messageToRender,
                scale = scale,
                color = if (actionConfirmationMsg != null) Color(0xFFFFD54F) else Color(0xFFECEFF1),
                fontSizeVirtualPx = 8.5f,
                letterSpacingVirtualPx = 0.2f,
                maxLines = 2,
                softWrap = true,
                modifier = Modifier.gbaVirtualBounds(6, 4, 136, 18, scale)
            )

            // Progress fraction: "${currentProgress}/${requiredProgress}"
            if (selectedPokemon != null && actionConfirmationMsg == null) {
                val currentProg = selectedPokemon.currentProgress
                val targetProg = selectedPokemon.unlockRule.targetThreshold
                val progressText = if (selectedPokemon.isUnlocked) "100%" else "$currentProg/$targetProg"

                GbaText(
                    text = progressText,
                    scale = scale,
                    color = Color(0xFFFFCA28),
                    fontSizeVirtualPx = 8f,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.gbaVirtualBounds(6, 23, 60, 10, scale)
                )

                // Dynamic 48x3 Progress Bar at (x = 97, y = 138) in screen space -> (x = 75, y = 27) in box
                val barFillWidth = (48f * animatedProgressRatio).roundToInt().coerceIn(0, 48)
                if (barFillWidth > 0) {
                    val barRes = if (selectedPokemon.isUnlocked) R.drawable.gba_hp_green else R.drawable.gba_hp_yellow
                    GbaPixelImage(
                        resId = barRes,
                        contentDescription = "Mission Bar Fill",
                        modifier = Modifier.gbaVirtualBounds(75, 27, barFillWidth, 3, scale)
                    )
                }
            }
        }

        // 8. BOTTOM THUMB ACTION BUTTONS (y = 150..170)
        // Left Button: BACK ((x = 34, y = 150, w = 56, h = 20))
        val backY = if (isBackPressed) 151 else 150
        Box(
            modifier = Modifier
                .gbaVirtualBounds(34, backY, 56, 20, scale)
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
                resId = R.drawable.gba_pokedex_btn_back_56x20,
                contentDescription = "Back Button",
                modifier = Modifier.fillMaxSize()
            )
            GbaText(
                text = "BACK",
                scale = scale,
                color = Color(0xFF263238),
                fontSizeVirtualPx = 8.5f,
                fontWeight = FontWeight.ExtraBold,
                scaleX = 1.1f,
                textAlign = TextAlign.Center
            )
        }

        // Right Button: SET BUDDY (unlocked) / PIN TARGET (locked) ((x = 96, y = 150, w = 62, h = 20))
        if (selectedPokemon != null) {
            val isUnlocked = selectedPokemon.isUnlocked
            val actionBtnRes = if (isUnlocked) R.drawable.gba_pokedex_btn_buddy_62x20 else R.drawable.gba_pokedex_btn_pin_62x20
            val actionLabel = if (isUnlocked) "SET BUDDY" else "PIN TARGET"
            val actionY = if (isActionPressed) 151 else 150

            Box(
                modifier = Modifier
                    .gbaVirtualBounds(96, actionY, 62, 20, scale)
                    .clickable(
                        interactionSource = actionInteractionSource,
                        indication = null
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        coroutineScope.launch {
                            isGoldFlashing = true
                            ledFlashTrigger += 2
                            val pokeName = selectedPokemon.name.uppercase()

                            if (isUnlocked) {
                                viewModel.setActiveBuddy(selectedPokemon.pokedexNumber)
                                actionConfirmationMsg = "$pokeName SENT OUT AS BUDDY!"
                            } else {
                                viewModel.pinWildTarget(selectedPokemon.pokedexNumber)
                                actionConfirmationMsg = "$pokeName PINNED AS TARGET!"
                            }

                            delay(700)
                            isGoldFlashing = false
                            delay(300)
                            onNavigateBack()
                        }
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
                    fontSizeVirtualPx = 8f,
                    fontWeight = FontWeight.ExtraBold,
                    scaleX = 1.08f,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
