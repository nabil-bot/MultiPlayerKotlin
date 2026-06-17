package com.example.multiplayer.ui.utils

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.webkit.WebView
import androidx.core.content.ContextCompat
import com.example.multiplayer.AudioKeepAliveService

object BackgroundPlayManager {

    /**
     * Initializes all low-level system settings required to keep media playback
     * running smoothly when the app enters the background or the screen locks.
     */
    fun initialize(context: Context) {
        // 1. Configure the global Chromium engine adjustments immediately at boot
        WebView.setWebContentsDebuggingEnabled(true)
        WebView(context.applicationContext).apply {
            settings.mediaPlaybackRequiresUserGesture = false
            setWillNotDraw(false)
        }

        // 2. Configure non-blocking audio focus logic
        setupMultiAudioPlayback(context)

        // 3. Start the keep-alive background foreground service permanently
        val intent = Intent(context, AudioKeepAliveService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    private fun setupMultiAudioPlayback(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val focusChangeListener = AudioManager.OnAudioFocusChangeListener { }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                .build()

            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()

            // Note: Requested passively per working configurations
        }
    }
}