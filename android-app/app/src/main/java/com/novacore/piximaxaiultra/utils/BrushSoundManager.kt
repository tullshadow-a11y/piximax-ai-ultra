package com.novacore.piximaxaiultra.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool

class BrushSoundManager(private val context: Context) {
    private var soundPool: SoundPool? = null
    private var soundId: Int = 0
    private var isSoundLoaded: Boolean = false
    private var lastPlayTimeMs: Long = 0L

    init {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(audioAttributes)
                .build()

            soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0 && sampleId == soundId) {
                    isSoundLoaded = true
                }
            }

            val resId = context.resources.getIdentifier("brush_stroke", "raw", context.packageName)
            if (resId != 0) {
                soundId = soundPool?.load(context, resId, 1) ?: 0
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun playBrushStrokeSound(soundEnabled: Boolean = true) {
        if (!soundEnabled) return

        val now = System.currentTimeMillis()
        if (now - lastPlayTimeMs < 120) return // Throttle playback to prevent overlap
        lastPlayTimeMs = now

        try {
            if (isSoundLoaded && soundId != 0) {
                soundPool?.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
            } else {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.5f)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            soundPool?.release()
            soundPool = null
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
