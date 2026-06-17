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

    // Flag driving the visibility state of our custom browser modular layout layer
    var isBrowserVisible by remember { mutableStateOf(false) }

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

                    // 🔹 THE SECRET WEAPON: Inject JS interface mapping window.ReactNativeWebView.postMessage
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun postMessage(message: String) {
                            try {
                                val json = JSONObject(message)
                                val type = json.optString("type")

                                // Catch your exact web application custom signal block
                                if (type == "VIDEO_BROWSE") {
                                    isBrowserVisible = true
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }, "ReactNativeWebView")
                }
            },
            update = { webView ->
                if (webView.url != "https://nabil-bot.github.io/NmsMultiPlayer/") {
                    webView.loadUrl("https://nabil-bot.github.io/NmsMultiPlayer/")
                }
            }
        )

        // --- LAYER 2: OVERLAY BROWSER PANEL (Kept cached in structural stack memory layout) ---
        BrowserView(
            isVisible = isBrowserVisible,
            onCloseBrowser = { isBrowserVisible = false }
        )
    }
}