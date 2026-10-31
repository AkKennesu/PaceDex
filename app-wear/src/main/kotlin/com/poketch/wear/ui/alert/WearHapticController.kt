package com.poketch.wear.ui.alert

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearHapticController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /**
     * Urgent double-pulse waveform for Over-Target Red Alert:
     * 0ms delay, 280ms pulse, 120ms pause, 380ms pulse at maximum amplitude (255).
     */
    fun playRedAlertWaveform() {
        if (vibrator == null || !vibrator.hasVibrator()) return

        val timings = longArrayOf(0L, 280L, 120L, 380L)
        val amplitudes = intArrayOf(0, 255, 0, 255)
        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
        vibrator.vibrate(effect)
    }

    /**
     * Joyful fanfare waveform for unlocking a new Pokémon:
     * rhythmic ascending fanfare pulses.
     */
    fun playUnlockFanfareHaptic() {
        if (vibrator == null || !vibrator.hasVibrator()) return

        val timings = longArrayOf(0L, 100L, 80L, 100L, 80L, 250L)
        val amplitudes = intArrayOf(0, 160, 0, 200, 0, 255)
        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
        vibrator.vibrate(effect)
    }

    /**
     * 0.25-Mile Milestone Haptic:
     * Single crisp, encouraging pulse.
     */
    fun playRunMilestoneQuarterHaptic() {
        if (vibrator == null || !vibrator.hasVibrator()) return

        val timings = longArrayOf(0L, 160L)
        val amplitudes = intArrayOf(0, 200)
        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
        vibrator.vibrate(effect)
    }

    /**
     * 0.50-Mile (Halfway) Milestone Haptic:
     * Double energetic pulse confirming halfway mark.
     */
    fun playRunMilestoneHalfHaptic() {
        if (vibrator == null || !vibrator.hasVibrator()) return

        val timings = longArrayOf(0L, 180L, 100L, 240L)
        val amplitudes = intArrayOf(0, 220, 0, 240)
        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
        vibrator.vibrate(effect)
    }

    /**
     * 0.75-Mile Milestone Haptic:
     * Triple rising crescendo pulse motivating final push to 1.00 mi.
     */
    fun playRunMilestoneThreeQuartersHaptic() {
        if (vibrator == null || !vibrator.hasVibrator()) return

        val timings = longArrayOf(0L, 120L, 80L, 160L, 80L, 260L)
        val amplitudes = intArrayOf(0, 180, 0, 220, 0, 255)
        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
        vibrator.vibrate(effect)
    }
}
