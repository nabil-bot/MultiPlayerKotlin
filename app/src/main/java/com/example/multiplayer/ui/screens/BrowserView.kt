package com.example.multiplayer.ui.screens

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.viewinterop.AndroidView
import com.example.multiplayer.ui.components.BrowserBottomBar
import com.example.multiplayer.ui.components.BrowserTopBar
import com.example.multiplayer.ui.utils.NestedScrollWebViewContainer
import com.example.multiplayer.ui.utils.buildNavigationUrl
import com.example.multiplayer.ui.utils.customVisibility

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserView(
    isVisible: Boolean,
    onCloseBrowser: () -> Unit,
    onAddVideoToList: (String) -> Unit,
    onAddVideoToPlayList: (String) -> Unit
) {
    val context = LocalContext.current
    var urlInput by remember { mutableStateOf("https://www.youtube.com") }
    var isInputFocused by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }

    var browserBarsVisible by remember { mutableStateOf(true) }
    var lastScrollY by remember { mutableStateOf(0) }

    val topBarHeight by animateDpAsState(
        targetValue = if (browserBarsVisible) 44.dp else 0.dp,
        animationSpec = tween(250),
        label = "topBarHeight"
    )

    val bottomBarHeight by animateDpAsState(
        targetValue = if (browserBarsVisible) 40.dp else 0.dp,
        animationSpec = tween(250),
        label = "bottomBarHeight"
    )

    // 🔹 Pull-to-refresh loading state tracker
    var isRefreshing by remember { mutableStateOf(false) }

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
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    if (isMuted && view != null) {
                        com.example.multiplayer.ui.utils.BrowserAudioManager.applyMute(view)
                    }
                }

                override fun onLoadResource(view: WebView?, url: String?) {
                    super.onLoadResource(view, url)
                    if (isMuted && view != null) {
                        com.example.multiplayer.ui.utils.BrowserAudioManager.applyMute(view)
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    // 🔹 STOP REFRESH SPINNER: Hide the pull icon immediately when page finishes rendering
                    isRefreshing = false

                    if (!isInputFocused) {
                        url?.let { urlInput = it }
                    }
                    if (isMuted && view != null) {
                        com.example.multiplayer.ui.utils.BrowserAudioManager.applyMute(view)
                    }
                }

                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    if (!isInputFocused) {
                        url?.let { urlInput = it }
                    }
                }
            }

            setOnScrollChangeListener { _, _, scrollY, _, _ ->
                val delta = scrollY - lastScrollY

                when {
                    delta > 20 && browserBarsVisible -> {
                        browserBarsVisible = false
                    }

                    delta < -20 && !browserBarsVisible -> {
                        browserBarsVisible = true
                    }

                    scrollY <= 0 -> {
                        browserBarsVisible = true
                    }
                }

                lastScrollY = scrollY
            }

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                mediaPlaybackRequiresUserGesture = false
            }

            com.example.multiplayer.ui.utils.BrowserContextMenuHandler(context, onAddVideoToList, onAddVideoToPlayList).register(this)
            loadUrl(urlInput)
        }
    }

    BackHandler(enabled = isVisible) {
        if (memoizedBrowserWebView.canGoBack()) {
            memoizedBrowserWebView.goBack()
        } else {
            onCloseBrowser()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .customVisibility(isVisible)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(topBarHeight)
        ) {
            if (topBarHeight > 0.dp) {
                BrowserTopBar(
                    urlInput = urlInput,
                    onUrlValueChange = { urlInput = it },
                    isMuted = isMuted,
                    onMuteToggle = {
                        isMuted = !isMuted
                        if (isMuted) {
                            com.example.multiplayer.ui.utils.BrowserAudioManager.applyMute(memoizedBrowserWebView)
                        } else {
                            com.example.multiplayer.ui.utils.BrowserAudioManager.removeMute(memoizedBrowserWebView)
                        }
                    },
                    onFocusChanged = { isInputFocused = it },
                    onNavigate = {
                        keyboardController?.hide()
                        memoizedBrowserWebView.loadUrl(buildNavigationUrl(urlInput))
                    },
                    onCloseBrowser = onCloseBrowser
                )
            }
        }

        // --- BROWSER VIEWPORT FRAME WITH SWIPE TO REFRESH ---
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                memoizedBrowserWebView.reload()
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    (memoizedBrowserWebView.parent as? ViewGroup)?.removeView(memoizedBrowserWebView)

                    NestedScrollWebViewContainer(context, memoizedBrowserWebView).apply {
                        addView(memoizedBrowserWebView)
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bottomBarHeight)
        ) {
            if (bottomBarHeight > 0.dp) {
                // 🔹 FIX: Pass down the active instance layer safely here
                BrowserBottomBar(webView = memoizedBrowserWebView)
            }
        }
    }
}