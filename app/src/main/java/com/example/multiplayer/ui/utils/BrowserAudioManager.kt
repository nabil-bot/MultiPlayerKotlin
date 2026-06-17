package com.example.multiplayer.ui.utils

import android.webkit.WebView

object BrowserAudioManager {

    fun applyMute(webView: WebView) {
        webView.evaluateJavascript(
            """
            (function() {
                function muteMedia() {
                    document.querySelectorAll('video, audio').forEach(function(media) {
                        if (!media.muted) {
                            media.muted = true;
                        }
                    });
                }

                // Run immediately
                muteMedia();

                // Prevent duplicates; ensure only one observer runs globally
                if (!window.__multiPlayerMuteObserver) {
                    const observer = new MutationObserver(function() {
                        muteMedia();
                    });

                    observer.observe(document.documentElement, {
                        childList: true,
                        subtree: true,
                        attributes: true,
                        attributeFilter: ['src', 'muted']
                    });

                    window.__multiPlayerMuteObserver = observer;
                }
            })();
            """.trimIndent(),
            null
        )
    }

    fun removeMute(webView: WebView) {
        webView.evaluateJavascript(
            """
            (function() {
                if (window.__multiPlayerMuteObserver) {
                    window.__multiPlayerMuteObserver.disconnect();
                    window.__multiPlayerMuteObserver = null;
                }

                document.querySelectorAll('video, audio').forEach(function(media) {
                    media.muted = false;
                });
            })();
            """.trimIndent(),
            null
        )
    }
}