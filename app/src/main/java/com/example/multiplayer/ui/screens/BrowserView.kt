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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp

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

    // 🔹 THE SAFEGUARD TRACKER: Tracks whether the user is actively typing in the address bar
    var isInputFocused by remember { mutableStateOf(false) }

    var isMuted by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current

    // 🔹 FIX STEP 1: Move function here and pass WebView as a parameter to avoid initialization ordering bugs
    fun applyMuteState(webView: WebView?) {
        webView?.evaluateJavascript(
            """
            (function() {
                document.querySelectorAll('video').forEach(function(v) {
                    v.muted = ${if (isMuted) "true" else "false"};
                });
            })();
            """.trimIndent(),
            null
        )
    }

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
                    // 🔹 Apply tip here: Only update text from web if user isn't actively typing
                    if (!isInputFocused) {
                        url?.let { urlInput = it }
                    }
                    // 🔹 FIX STEP 2: Pass 'view' (which is the current WebView) safely into the function
                    applyMuteState(view as? WebView)
                }

                override fun doUpdateVisitedHistory(
                    view: WebView?,
                    url: String?,
                    isReload: Boolean
                ) {
                    super.doUpdateVisitedHistory(view, url, isReload)

                    // 🔹 Apply tip here: Prevents auto-navigation changes from hijacking your typing input
                    if (!isInputFocused) {
                        url?.let {
                            urlInput = it
                        }
                    }
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
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                modifier = Modifier.size(40.dp),
                onClick = {
                    isMuted = !isMuted
                    // 🔹 FIX STEP 3: Pass our memoized WebView instance handle cleanly
                    applyMuteState(memoizedBrowserWebView)
                }
            ) {
                Icon(
                    imageVector =
                        if (isMuted)
                            Icons.Default.VolumeOff
                        else
                            Icons.Default.VolumeUp,
                    contentDescription = "Mute Browser"
                )
            }

            TextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .onFocusChanged { focusState ->
                        isInputFocused = focusState.isFocused
                    },
                singleLine = true,

                placeholder = {
                    Text("Search or enter URL")
                },

                trailingIcon = {
                    if (urlInput.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                urlInput = ""
                            }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear"
                            )
                        }
                    }
                },

                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Go
                ),

                keyboardActions = KeyboardActions(
                    onGo = {
                        keyboardController?.hide()
                        memoizedBrowserWebView.loadUrl(
                            buildNavigationUrl(urlInput)
                        )
                    }
                ),

                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            IconButton(
                modifier = Modifier.size(40.dp),
                onClick = onCloseBrowser
            ) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "Hide Browser"
                )
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

private fun buildNavigationUrl(input: String): String {
    val text = input.trim()

    return if (
        text.contains(".") &&
        !text.contains(" ")
    ) {
        if (
            text.startsWith("http://") ||
            text.startsWith("https://")
        ) {
            text
        } else {
            "https://$text"
        }
    } else {
        "https://www.google.com/search?q=$text"
    }
}