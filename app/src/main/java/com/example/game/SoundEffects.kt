package com.example.game

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

class SoundEffects(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val scope = CoroutineScope(Dispatchers.Default)
    var isSoundEnabled = true
    var isVibrationEnabled = true

    fun playCrash() {
        if (isVibrationEnabled) {
            triggerCrashVibration()
        }
        if (isSoundEnabled) {
            scope.launch {
                synthesizeNoise(durationMs = 300, startFreq = 220f, endFreq = 60f)
            }
        }
    }

    fun playEngineRev() {
        if (isSoundEnabled) {
            scope.launch {
                synthesizeTone(durationMs = 120, freq = 340f)
            }
        }
    }

    private fun triggerCrashVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(
                    longArrayOf(0, 80, 50, 150),
                    intArrayOf(0, 255, 0, 200),
                    -1
                )
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(250L)
            }
        } catch (_: Exception) {
            // Ignore if vibration fails
        }
    }

    private fun synthesizeTone(durationMs: Int, freq: Float) {
        try {
            val sampleRate = 22050
            val numSamples = (durationMs * sampleRate) / 1000
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val time = i.toDouble() / sampleRate
                val angle = 2.0 * Math.PI * freq * time
                val envelope = 1.0 - (i.toDouble() / numSamples)
                buffer[i] = (sin(angle) * 15000.0 * envelope).toInt().toShort()
            }

            playBuffer(buffer, sampleRate)
        } catch (_: Exception) {
            // AudioTrack failure fallback
        }
    }

    private fun synthesizeNoise(durationMs: Int, startFreq: Float, endFreq: Float) {
        try {
            val sampleRate = 22050
            val numSamples = (durationMs * sampleRate) / 1000
            val buffer = ShortArray(numSamples)

            var phase = 0.0
            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val currentFreq = startFreq + (endFreq - startFreq) * progress
                phase += 2.0 * Math.PI * currentFreq / sampleRate
                val envelope = 1.0 - progress
                val noise = (Math.random() * 2.0 - 1.0) * 0.4
                val wave = sin(phase) * 0.6 + noise
                buffer[i] = (wave * 20000.0 * envelope).toInt().coerceIn(-32768, 32767).toShort()
            }

            playBuffer(buffer, sampleRate)
        } catch (_: Exception) {
            // AudioTrack failure fallback
        }
    }

    private fun playBuffer(buffer: ShortArray, sampleRate: Int) {
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            Thread.sleep((buffer.size * 1000L / sampleRate) + 50)
            audioTrack.release()
        } catch (_: Exception) {
            // Ignore
        }
    }
}
