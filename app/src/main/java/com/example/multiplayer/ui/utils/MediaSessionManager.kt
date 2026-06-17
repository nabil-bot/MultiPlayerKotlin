package com.example.multiplayer.ui.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.webkit.WebView



object MediaSessionManager {
    private var mediaSession: MediaSessionCompat? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { }

    fun initialize(context: Context, webViewProvider: () -> WebView?) {
        if (mediaSession != null) return

        // 🔹 FIX 1: Explicitly pass the Class type inside getSystemService to satisfy older compiler configurations
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        requestAudioFocus()

        mediaSession = MediaSessionCompat(context, "NmsMultiPlayerSession").apply {
            isActive = true

            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )

            val stateBuilder = PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_PLAY_PAUSE or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
            setPlaybackState(stateBuilder.build())

            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    sendSignalToWeb(webViewProvider(), "PLAY")
                }

                override fun onPause() {
                    sendSignalToWeb(webViewProvider(), "PAUSE")
                }

                override fun onSkipToNext() {
                    sendSignalToWeb(webViewProvider(), "NEXT")
                }

                override fun onSkipToPrevious() {
                    sendSignalToWeb(webViewProvider(), "PREVIOUS")
                }
            })
        }
    }

    private fun sendSignalToWeb(webView: WebView?, signal: String) {
        webView?.post {
            // 🔹 FIX 2: Rewrote the string to completely separate Kotlin variables from JS code blocks
            val script = "if(typeof multiPlayerInstance !== 'undefined') { multiPlayerInstance.handleMediaSignal(\"$signal\"); }"
            webView.evaluateJavascript(script, null)
        }
    }

    private fun requestAudioFocus() {
        audioManager?.let { am ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
                am.requestAudioFocus(focusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
        }
    }

    fun release() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
            audioManager?.abandonAudioFocusRequest(focusRequest!!)
        }
        mediaSession?.apply {
            isActive = false
            release()
        }
        mediaSession = null
    }
}