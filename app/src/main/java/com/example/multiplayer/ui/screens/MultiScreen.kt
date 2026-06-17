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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.multiplayer.ui.utils.MediaSessionManager
import org.json.JSONObject

@Composable
fun MultiScreen() {
    val context = LocalContext.current
    var filePathCallbackState by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var isBrowserVisible by remember { mutableStateOf(false) }

    // 🔹 THE SECRET WEAPON: Keep a direct reference handle to the main app's native WebView instance
    var mainAppWebViewRef by remember { mutableStateOf<WebView?>(null) }

    // 🔹 MODULAR HOOK: Initialize Media Session Controller Context tracking once on first presentation view load
    LaunchedEffect(Unit) {
        // We pass a dynamic lazy-evaluation provider closure lambda down into the Manager layer.
        // This ensures it always grabs the latest initialized reference instance pointer target of mainAppWebViewRef,
        // even if the underlying layout forces a dynamic layout recomposition recalculation pass.
        MediaSessionManager.initialize(context) { mainAppWebViewRef }
    }

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
                        // Keeps background audio streams alive unconditionally
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
                    mainAppWebViewRef = this
                }
            },
            update = { webView ->
                if (webView.url != "https://nabil-bot.github.io/NmsMultiPlayer/") {
                    webView.loadUrl("https://nabil-bot.github.io/NmsMultiPlayer/")
                }
            }
        )

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