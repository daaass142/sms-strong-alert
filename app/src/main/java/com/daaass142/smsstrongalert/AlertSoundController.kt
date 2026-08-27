package com.daaass142.smsstrongalert

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object AlertSoundController {
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var previousAlarmVolume: Int? = null

    fun start(context: Context, durationMs: Long) {
        stop()

        val appContext = context.applicationContext
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        audioManager = appContext.getSystemService(AudioManager::class.java)
        val manager = audioManager

        if (manager != null) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener { }
                .build()
            manager.requestAudioFocus(audioFocusRequest!!)

            if (Prefs.forceMaxVolume(appContext)) {
                previousAlarmVolume = manager.getStreamVolume(AudioManager.STREAM_ALARM)
                val max = manager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                manager.setStreamVolume(AudioManager.STREAM_ALARM, max, 0)
            }
        }

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        runCatching {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(attributes)
                setDataSource(appContext, soundUri)
                isLooping = true
                setVolume(1f, 1f)
                prepare()
                start()
            }
        }

        vibrator = if (Build.VERSION.SDK_INT >= 31) {
            appContext.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Vibrator::class.java)
        }

        vibrator?.vibrate(
            VibrationEffect.createWaveform(
                longArrayOf(0, 850, 300, 850, 300),
                0,
            ),
        )

        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ stop() }, durationMs.coerceIn(15_000L, 300_000L))
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)

        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null

        runCatching { vibrator?.cancel() }
        vibrator = null

        val manager = audioManager
        val request = audioFocusRequest
        if (manager != null && request != null) {
            runCatching { manager.abandonAudioFocusRequest(request) }
        }

        val previous = previousAlarmVolume
        if (manager != null && previous != null) {
            runCatching { manager.setStreamVolume(AudioManager.STREAM_ALARM, previous, 0) }
        }

        audioManager = null
        audioFocusRequest = null
        previousAlarmVolume = null
    }
}
