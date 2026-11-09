package com.poketch.wear.watchface

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.BatteryManager
import android.text.format.DateFormat
import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.TapEvent
import androidx.wear.watchface.TapType
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.poketch.core.domain.model.UserProfile
import com.poketch.wear.R
import com.poketch.wear.WearMainActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

class PoketchCanvasRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    currentUserStyleRepository: CurrentUserStyleRepository
) : Renderer.CanvasRenderer(
    surfaceHolder = surfaceHolder,
    currentUserStyleRepository = currentUserStyleRepository,
    watchState = watchState,
    canvasType = CanvasType.HARDWARE,
    interactiveDrawModeUpdateDelayMillis = 1000L,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = false
), WatchFace.TapListener {

    private val rendererScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Data / State
    private var userProfile: UserProfile? = null
    private var activeBuddyBitmap: Bitmap? = null
    private var lastLoadedPokemonId: Int? = null

    private var todayCalories: Int = 0
    private var todaySteps: Int = 0
    private var todayWaterMl: Int = 0
    private var batteryPercent: Int = 100
    private var heartRateBpm: Int = 72

    // Bitmaps
    private val bgInteractiveBitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(context.resources, R.drawable.wf_bg_round_bezel)
    }
    private val bgAmbientBitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(context.resources, R.drawable.wf_bg_ambient_wireframe)
    }
    private val ballActiveBitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(context.resources, R.drawable.wf_icon_ball_active)
    }
    private val ballEmptyBitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(context.resources, R.drawable.wf_icon_ball_empty)
    }
    private val heartBitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(context.resources, R.drawable.wf_icon_heart)
    }
    private val cursorBitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(context.resources, R.drawable.wf_icon_cursor)
    }
    private val defaultPikachuBitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(context.resources, R.drawable.wf_icon_pikachu)
    }

    // Paints
    private val pixelArtPaint = Paint().apply {
        isFilterBitmap = false
        isAntiAlias = false
    }

    private val bitmapDrawPaint = Paint().apply {
        isFilterBitmap = true
        isAntiAlias = true
    }

    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val boldTypeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    private val whiteDatePaint = Paint().apply {
        typeface = boldTypeface
        textSize = 20f
        color = Color.WHITE
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
    }

    private val whiteClockPaint = Paint().apply {
        typeface = boldTypeface
        textSize = 52f
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val cyanAmPmPaint = Paint().apply {
        typeface = boldTypeface
        textSize = 19f
        color = 0xFFA0D0F8.toInt()
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
    }

    private val lcdDarkTextPaint = Paint().apply {
        typeface = boldTypeface
        textSize = 23f
        color = 0xFF1A281A.toInt()
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val lcdDarkStartTextPaint = Paint().apply {
        typeface = boldTypeface
        textSize = 21f
        color = 0xFF1A281A.toInt()
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
    }

    private val ambientDatePaint = Paint().apply {
        typeface = boldTypeface
        textSize = 20f
        color = 0xFF489060.toInt()
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
    }

    private val ambientBatteryPaint = Paint().apply {
        typeface = boldTypeface
        textSize = 19f
        color = 0xFF489060.toInt()
        textAlign = Paint.Align.RIGHT
        isAntiAlias = true
    }

    private val ambientClockPaint = Paint().apply {
        typeface = boldTypeface
        textSize = 52f
        color = 0xFF58D878.toInt()
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val ambientAmPmPaint = Paint().apply {
        typeface = boldTypeface
        textSize = 19f
        color = 0xFF58D878.toInt()
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
    }

    private val ambientStepsPaint = Paint().apply {
        typeface = boldTypeface
        textSize = 24f
        color = 0xFF58D878.toInt()
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val ambientLabelPaint = Paint().apply {
        typeface = boldTypeface
        textSize = 20f
        color = 0xFF489060.toInt()
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                batteryPercent = (level * 100 / scale.toFloat()).toInt()
                postInvalidate()
            }
        }
    }

    override suspend fun init() {
        super.init()

        // Fetch repositories via EntryPoint
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            PoketchWatchFaceEntryPoint::class.java
        )
        val userRepository = entryPoint.userRepository()
        val calorieRepository = entryPoint.calorieRepository()
        val habitRepository = entryPoint.habitRepository()

        // Query initial battery status
        val initialBattery = context.registerReceiver(
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = initialBattery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = initialBattery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level >= 0 && scale > 0) {
            batteryPercent = (level * 100 / scale.toFloat()).toInt()
        }

        // Start observing UserProfile for active Buddy updates
        rendererScope.launch {
            userRepository.observeUserProfile().collectLatest { profile ->
                userProfile = profile
                if (profile != null) {
                    loadBuddySprite(profile.activeBuddyPokemonId)
                } else {
                    loadBuddySprite(1)
                }
                postInvalidate()
            }
        }

        // Observe calories, steps, and hydration for today
        rendererScope.launch {
            val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            launch {
                calorieRepository.observeTotalCaloriesByDate(todayIso).collectLatest { kcal ->
                    todayCalories = kcal
                    postInvalidate()
                }
            }
            launch {
                habitRepository.observeDailySteps(todayIso).collectLatest { steps ->
                    todaySteps = steps.toInt()
                    postInvalidate()
                }
            }
            launch {
                habitRepository.observeDailyWaterMl(todayIso).collectLatest { water ->
                    todayWaterMl = water.toInt()
                    postInvalidate()
                }
            }
        }
    }

    private fun loadBuddySprite(pokemonId: Int) {
        if (pokemonId == lastLoadedPokemonId && activeBuddyBitmap != null) return
        lastLoadedPokemonId = pokemonId

        val padded = String.format(Locale.US, "%03d", pokemonId)
        val path = "sprites/pokemon_$padded.png"
        val original = runCatching {
            context.assets.open(path).use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()

        activeBuddyBitmap = if (original != null) {
            // Nearest-neighbor 98x98 scaling to preserve sharp pixel art
            Bitmap.createScaledBitmap(original, 98, 98, false)
        } else {
            defaultPikachuBitmap
        }
    }

    override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {
        val isAmbient = renderParameters.drawMode == DrawMode.AMBIENT

        // Scale virtual 450x450 layout to actual canvas dimensions
        val scaleX = bounds.width().toFloat() / 450f
        val scaleY = bounds.height().toFloat() / 450f

        canvas.save()
        canvas.scale(scaleX, scaleY)

        val is24Hour = DateFormat.is24HourFormat(context)
        val rawHour = zonedDateTime.hour
        val minute = zonedDateTime.minute
        val displayHour = if (is24Hour) rawHour else if (rawHour % 12 == 0) 12 else rawHour % 12
        val timeText = String.format(Locale.US, "%d:%02d", displayHour, minute)
        val amPmText = if (is24Hour) "" else if (rawHour >= 12) "PM" else "AM"

        val dayOfWeek = zonedDateTime.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US).uppercase()
        val month = zonedDateTime.month.getDisplayName(TextStyle.SHORT, Locale.US).uppercase()
        val day = zonedDateTime.dayOfMonth
        val dateText = "$dayOfWeek, $month $day"

        val dstRect450 = Rect(0, 0, 450, 450)

        if (isAmbient) {
            // 1. Ambient Background
            canvas.drawBitmap(bgAmbientBitmap, null, dstRect450, bitmapDrawPaint)

            // 2. Top Box: Date & Battery
            canvas.drawText(dateText, 105f, 78f, ambientDatePaint)
            canvas.drawText("$batteryPercent%", 333f, 78f, ambientBatteryPaint)

            // 3. Digital Clock & AM/PM
            canvas.drawText(timeText, 193f, 132f, ambientClockPaint)
            if (amPmText.isNotEmpty()) {
                canvas.drawText(amPmText, 280f, 124f, ambientAmPmPaint)
            }

            // 4. Middle Right: Steps Display
            canvas.drawText("$todaySteps STEPS", 295f, 222f, ambientStepsPaint)

            // 5. Bottom Box: Ambient AOD Label
            canvas.drawText("★ POKéTCH AOD ★", 225f, 358f, ambientLabelPaint)
        } else {
            // 1. Interactive Bezel Background
            canvas.drawBitmap(bgInteractiveBitmap, null, dstRect450, bitmapDrawPaint)

            // 2. Top Clock Box: Date & Day
            canvas.drawText(dateText, 105f, 78f, whiteDatePaint)

            // 3. Top Clock Box: Battery Bar (Dynamic Green Fill)
            fillPaint.color = 0xFF58D878.toInt()
            val batteryWidth = (batteryPercent * 0.46f).coerceIn(3f, 46f)
            canvas.drawRect(RectF(284f, 66f, 284f + batteryWidth, 74f), fillPaint)

            // 4. Top Clock Box: Digital Clock & AM/PM
            canvas.drawText(timeText, 193f, 132f, whiteClockPaint)
            if (amPmText.isNotEmpty()) {
                canvas.drawText(amPmText, 280f, 124f, cyanAmPmPaint)
            }

            // 5. Middle Left: Active Buddy Pokémon Sprite on Battle Grass Platform
            val spriteBitmap = activeBuddyBitmap ?: defaultPikachuBitmap
            val spriteDst = Rect(63, 169, 63 + 98, 169 + 98)
            canvas.drawBitmap(spriteBitmap, null, spriteDst, pixelArtPaint)

            // 6. Middle Right: Calorie Bar (Green)
            val targetKcal = userProfile?.targetCalorieCeiling ?: 2000
            val calWidth = ((todayCalories.toFloat() / targetKcal.coerceAtLeast(1)) * 108f).coerceIn(6f, 108f)
            fillPaint.color = 0xFF58D878.toInt()
            canvas.drawRect(RectF(265f, 183f, 265f + calWidth, 191f), fillPaint)

            // 7. Middle Right: Step Bar (Yellow)
            val stepGoal = 10000
            val stepWidth = ((todaySteps.toFloat() / stepGoal.coerceAtLeast(1)) * 108f).coerceIn(6f, 108f)
            fillPaint.color = 0xFFF8D030.toInt()
            canvas.drawRect(RectF(265f, 206f, 265f + stepWidth, 214f), fillPaint)

            // 8. Middle Right: Numeric Steps Text
            canvas.drawText("$todaySteps STEPS", 295f, 248f, lcdDarkTextPaint)

            // 9. Middle Right: Water Bar (Blue)
            val hydrationGoal = userProfile?.dailyWaterGoalMl ?: 2500
            val waterWidth = ((todayWaterMl.toFloat() / hydrationGoal.coerceAtLeast(1)) * 118f).coerceIn(8f, 118f)
            fillPaint.color = 0xFF3898F8.toInt()
            canvas.drawRect(RectF(258f, 260f, 258f + waterWidth, 268f), fillPaint)

            // 10. Bottom Box: 4 Pokéballs
            canvas.drawBitmap(ballActiveBitmap, null, Rect(112, 322, 112 + 18, 322 + 18), bitmapDrawPaint)
            canvas.drawBitmap(ballActiveBitmap, null, Rect(134, 322, 134 + 18, 322 + 18), bitmapDrawPaint)
            canvas.drawBitmap(ballActiveBitmap, null, Rect(155, 322, 155 + 18, 322 + 18), bitmapDrawPaint)
            canvas.drawBitmap(ballEmptyBitmap, null, Rect(176, 322, 176 + 18, 322 + 18), bitmapDrawPaint)

            // 11. Bottom Box: Heart Icon + Heart Rate (BPM)
            canvas.drawBitmap(heartBitmap, null, Rect(208, 321, 208 + 22, 321 + 20), bitmapDrawPaint)
            val hrText = "${if (heartRateBpm > 0) heartRateBpm else 72} BPM"
            canvas.drawText(hrText, 236f, 338f, lcdDarkStartTextPaint)

            // 12. Bottom Box: Cursor + OPEN POKéTCH Action Prompt
            canvas.drawBitmap(cursorBitmap, null, Rect(120, 347, 120 + 16, 347 + 24), bitmapDrawPaint)
            canvas.drawText("OPEN POKéTCH", 142f, 368f, lcdDarkStartTextPaint)
        }

        canvas.restore()
    }

    override fun renderHighlightLayer(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {
        // No-op highlight layer for default interactive rendering
    }

    override fun onTapEvent(
        tapType: Int,
        tapEvent: TapEvent,
        complicationSlot: ComplicationSlot?
    ) {
        if (tapType == TapType.UP) {
            // Launch WearMainActivity directly into Pokétch
            val intent = Intent(context, WearMainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        rendererScope.cancel()
        runCatching { context.unregisterReceiver(batteryReceiver) }
    }
}
