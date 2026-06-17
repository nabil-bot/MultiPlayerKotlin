package com.example.multiplayer.ui.screens

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserView(
    isVisible: Boolean,
    onCloseBrowser: () -> Unit,
    onAddVideoToList: (String) -> Unit, // 🔹 NEW hoisted parameter hook added here cleanly
    onAddVideoToPlayList: (String) -> Unit
) {
    val context = LocalContext.current
    var urlInput by remember { mutableStateOf("https://www.youtube.com") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val memoizedBrowserWebView = remember {
        object : WebView(context) {
            override fun onWindowVisibilityChanged(visibility: Int) {
                super.onWindowVisibilityChanged(View.VISIBLE)
            }
        }.apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    url?.let { urlInput = it }
                }
            }
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                mediaPlaybackRequiresUserGesture = false
            }

            // 🔹 MODULARITY: Pass down the context link event forward down to the independent file logic block
            com.example.multiplayer.ui.utils.BrowserContextMenuHandler(context, onAddVideoToList, onAddVideoToPlayList).register(this)

            loadUrl(urlInput)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .customVisibility(isVisible)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- TOP BROWSER CONTROLS BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCloseBrowser) {
                Icon(Icons.Default.Home, contentDescription = "Hide Browser")
            }

            TextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = {
                        keyboardController?.hide()
                        var formattedUrl = urlInput.trim()
                        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                            formattedUrl = "https://$formattedUrl"
                        }
                        memoizedBrowserWebView.loadUrl(formattedUrl)
                    }
                ),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            IconButton(onClick = { memoizedBrowserWebView.reload() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Reload Page")
            }
        }

        // --- BROWSER VIEWPORT FRAME ---
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    (memoizedBrowserWebView.parent as? ViewGroup)?.removeView(memoizedBrowserWebView)

                    object : ViewGroup(context) {
                        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
                            memoizedBrowserWebView.layout(0, 0, r - l, b - t)
                        }
                        override fun onWindowVisibilityChanged(visibility: Int) {
                            super.onWindowVisibilityChanged(View.VISIBLE)
                            memoizedBrowserWebView.dispatchWindowVisibilityChanged(View.VISIBLE)
                        }
                    }.apply {
                        addView(memoizedBrowserWebView)
                    }
                }
            )
        }
    }
}

private fun Modifier.customVisibility(isVisible: Boolean): Modifier {
    return this.then(
        if (isVisible) Modifier else Modifier.layout { _, _ -> layout(0, 0) {} }
    )
}