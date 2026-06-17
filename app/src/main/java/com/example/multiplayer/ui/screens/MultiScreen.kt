package com.example.multiplayer.ui.screens

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

@Composable
fun MultiScreen() {
    var filePathCallbackState by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var isBrowserVisible by remember { mutableStateOf(false) }

    // 🔹 THE SECRET WEAPON: Keep a direct reference handle to the main app's native WebView instance
    var mainAppWebViewRef by remember { mutableStateOf<WebView?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            filePathCallbackState?.onReceiveValue(uris.toTypedArray())
        } else {
            filePathCallbackState?.onReceiveValue(null)
        }
        filePathCallbackState = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- LAYER 1: BASE EXPANDED MAIN APP WEBVIEW ---
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                object : WebView(ctx) {
                    override fun onWindowVisibilityChanged(visibility: Int) {
                        super.onWindowVisibilityChanged(View.VISIBLE)
                    }
                }.apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    webViewClient = WebViewClient()
                    webChromeClient = object : WebChromeClient() {
                        override fun onShowFileChooser(
                            webView: WebView?,
                            filePathCallback: ValueCallback<Array<Uri>>?,
                            fileChooserParams: FileChooserParams?
                        ): Boolean {
                            filePathCallbackState?.onReceiveValue(null)
                            filePathCallbackState = filePathCallback
                            filePickerLauncher.launch(arrayOf("*/*"))
                            return true
                        }
                    }
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        mediaPlaybackRequiresUserGesture = false
                        allowFileAccess = true
                    }

                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun postMessage(message: String) {
                            try {
                                val json = JSONObject(message)
                                val type = json.optString("type")

                                if (type == "VIDEO_BROWSE") {
                                    isBrowserVisible = true
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }, "ReactNativeWebView")

                    // Cache our instantiated component reference securely inside our state holder pointer
                    mainAppWebViewRef = this
                }
            },
            update = { webView ->
                if (webView.url != "https://nabil-bot.github.io/NmsMultiPlayer/") {
                    webView.loadUrl("https://nabil-bot.github.io/NmsMultiPlayer/")
                }
            }
        )

        // --- LAYER 2: OVERLAY BROWSER PANEL ---
        // Pass the callback mechanism down cleanly to the modular layout frame
        BrowserView(
            isVisible = isBrowserVisible,
            onCloseBrowser = { isBrowserVisible = false },
            onAddVideoToList = { targetUrl ->
                mainAppWebViewRef?.post {
                    val script = "multiPlayerInstance.addYoutubeVideo('$targetUrl')"
                    mainAppWebViewRef?.evaluateJavascript(script, null)
                }
            },
            onAddVideoToPlayList = { targetUrl ->
                mainAppWebViewRef?.post {
                    val script = "multiPlayerInstance.addToYtPlaylist('$targetUrl')"
                    mainAppWebViewRef?.evaluateJavascript(script, null)
                }
            }

        )
    }
}