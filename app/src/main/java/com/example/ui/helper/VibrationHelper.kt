package com.example.ui.helper

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * مساعد ردود الفعل اللمسية والاهتزاز (Haptic & Vibration Feedback Helper)
 *
 * المسؤوليات المعمارية:
 * 1. دعم التوافقية العكسية لأنظمة Android القديمة والحديثة (Vibrator vs VibratorManager).
 * 2. عزل استدعاءات عتاد الاهتزاز عن واجهة المستخدم لضمان تجربة سلسة وتفادي انهيار التطبيق حال غياب العتاد أو رفض الإذن.
 */
object VibrationHelper {
    private val SUCCESS_PATTERN = longArrayOf(0, 40, 80, 80)
    private val DELETE_PATTERN = longArrayOf(0, 100, 60, 100)

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun vibrate(context: Context, milliseconds: Long = 100) {
        try {
            val vibrator = getVibrator(context)
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(milliseconds)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun vibratePattern(context: Context, pattern: LongArray, repeat: Int = -1) {
        try {
            val vibrator = getVibrator(context)
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, repeat))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, repeat)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Custom beautiful patterns for a world-class premium experience
    fun triggerSuccessVibration(context: Context) {
        // Double tap pattern: wait 0, vibrate 40, wait 80, vibrate 80
        vibratePattern(context, SUCCESS_PATTERN)
    }

    fun triggerDeleteVibration(context: Context) {
        // Warning pattern: wait 0, vibrate 100, wait 60, vibrate 100
        vibratePattern(context, DELETE_PATTERN)
    }

    fun triggerClickVibration(context: Context) {
        // Quick subtle tick
        vibrate(context, 25)
    }
}

