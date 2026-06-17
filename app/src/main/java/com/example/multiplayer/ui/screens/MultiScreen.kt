package com.example.multiplayer.ui.screens

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun MultiScreen() {
    // This reference tracks the pending callback link to the web browser engine
    var filePathCallbackState by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    // 🔹 NATIVE PICKER LAUNCHER: Opens the secure Android system file layout provider
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(), // Allows picking multiple media assets
    ) { uris ->
        // Pass the chosen file URIs back to the browser framework thread
        if (uris.isNotEmpty()) {
            filePathCallbackState?.onReceiveValue(uris.toTypedArray())
        } else {
            filePathCallbackState?.onReceiveValue(null) // Clear state if user cancelled
        }
        filePathCallbackState = null
    }

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

                // 🔹 THE SECRET: WebChromeClient intercepts advanced browser events like file picker requests
                webChromeClient = object : WebChromeClient() {
                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        // Cancel any pending unexpected parallel picker callbacks
                        filePathCallbackState?.onReceiveValue(null)
                        filePathCallbackState = filePathCallback

                        // Trigger our native Jetpack Compose system picker UI layer
                        // "*/*" means allow any file type. You can restrict to "video/*, audio/*" if needed.
                        filePickerLauncher.launch(arrayOf("*/*"))
                        return true // Tell the engine we are handling this manually in Kotlin
                    }
                }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    mediaPlaybackRequiresUserGesture = false
                    allowFileAccess = true // Permits loading resources from localized sandbox paths
                }
            }
        },
        update = { webView ->
            if (webView.url != "https://nabil-bot.github.io/NmsMultiPlayer/") {
                webView.loadUrl("https://nabil-bot.github.io/NmsMultiPlayer/")
            }
        }
    )
}