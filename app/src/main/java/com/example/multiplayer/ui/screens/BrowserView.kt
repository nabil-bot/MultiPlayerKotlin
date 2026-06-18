package com.example.multiplayer.ui.screens

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

import androidx.core.view.NestedScrollingParent3
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.NestedScrollingChildHelper
import android.view.MotionEvent

import androidx.activity.compose.BackHandler


import androidx.core.view.NestedScrollingChild3
import androidx.core.view.ViewCompat
import kotlin.math.abs

import android.view.VelocityTracker


private class NestedScrollWebViewContainer(
    context: android.content.Context,
    private val webView: WebView
) : ViewGroup(context), NestedScrollingChild3 {

    private val childHelper = NestedScrollingChildHelper(this)
    private var lastY = 0f
    private val touchSlop = android.view.ViewConfiguration.get(context).scaledPagingTouchSlop
    private var isDragging = false
    private var isPulling = false
    private val parentOffset = IntArray(2)
    private val consumedArr = IntArray(2)

    private var velocityTracker: VelocityTracker? = null


    private var startX = 0f
    private var startY = 0f
    private val pullEngageThreshold = touchSlop * 6   // require a bigger, deliberate pull

    init {
        isNestedScrollingEnabled = true
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        webView.layout(0, 0, r - l, b - t)
    }


    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastY = ev.y
                startX = ev.x
                startY = ev.y
                isDragging = false
                isPulling = false

                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain()
                velocityTracker?.addMovement(ev)

                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH)
            }

            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(ev)

                val dy = (lastY - ev.y).toInt()
                lastY = ev.y

                if (!isDragging && abs(dy) > touchSlop) {
                    isDragging = true
                }

                val totalDy = ev.y - startY   // positive = moved down since gesture start
                val totalDx = abs(ev.x - startX)

                // Only engage pull-to-refresh for a deliberate, mostly-vertical, sustained
                // downward drag while the page is pinned at the top. This filters out most
                // accidental triggers from scrolling/dragging elements inside the page
                // (carousels, inner lists), which tend to be smaller or more horizontal.
                if (!isPulling && isDragging && webView.scrollY == 0 &&
                    totalDy > pullEngageThreshold && totalDx < pullEngageThreshold
                ) {
                    isPulling = true
                }

                if (isPulling) {
                    dispatchNestedPreScroll(0, dy, consumedArr, parentOffset, ViewCompat.TYPE_TOUCH)
                    val unconsumedY = dy - consumedArr[1]
                    if (unconsumedY != 0) {
                        dispatchNestedScroll(0, 0, 0, unconsumedY, parentOffset, ViewCompat.TYPE_TOUCH)
                    }
                    consumedArr[0] = 0
                    consumedArr[1] = 0
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    velocityTracker?.addMovement(ev)
                    velocityTracker?.computeCurrentVelocity(1000)
                    // Vertical finger velocity; nested scroll convention here matches
                    // our dy sign (negative = was pulling down, positive = pushing up).
                    val velocityY = -(velocityTracker?.yVelocity ?: 0f)

                    dispatchNestedPreFling(0f, velocityY)
                    dispatchNestedFling(0f, velocityY, false)
                }
                isDragging = false
                isPulling = false
                stopNestedScroll(ViewCompat.TYPE_TOUCH)

                velocityTracker?.recycle()
                velocityTracker = null
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean = false
    override fun onTouchEvent(event: MotionEvent?): Boolean = false

    override fun setNestedScrollingEnabled(enabled: Boolean) { childHelper.isNestedScrollingEnabled = enabled }
    override fun isNestedScrollingEnabled() = childHelper.isNestedScrollingEnabled
    override fun startNestedScroll(axes: Int) = childHelper.startNestedScroll(axes)
    override fun startNestedScroll(axes: Int, type: Int) = childHelper.startNestedScroll(axes, type)
    override fun stopNestedScroll() = childHelper.stopNestedScroll()
    override fun stopNestedScroll(type: Int) = childHelper.stopNestedScroll(type)
    override fun hasNestedScrollingParent() = childHelper.hasNestedScrollingParent()
    override fun hasNestedScrollingParent(type: Int) = childHelper.hasNestedScrollingParent(type)

    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int,
        offsetInWindow: IntArray?, type: Int
    ) = childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type)

    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int,
        offsetInWindow: IntArray?, type: Int, consumed: IntArray
    ) = childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type, consumed)

    override fun dispatchNestedPreScroll(
        dx: Int, dy: Int, consumed: IntArray?, offsetInWindow: IntArray?, type: Int
    ) = childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)

    override fun dispatchNestedFling(velocityX: Float, velocityY: Float, consumed: Boolean) =
        childHelper.dispatchNestedFling(velocityX, velocityY, consumed)

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float) =
        childHelper.dispatchNestedPreFling(velocityX, velocityY)
}

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

        BrowserBottomBar()
    }
}

// 🌐 MODULAR COMPONENT: SLIM HIGH-CONTROL TOP BAR
@Composable
fun BrowserTopBar(
    urlInput: String,
    onUrlValueChange: (String) -> Unit,
    isMuted: Boolean,
    onMuteToggle: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onNavigate: () -> Unit,
    onCloseBrowser: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = Modifier.size(36.dp),
                onClick = onMuteToggle
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = "Mute Browser",
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .padding(horizontal = 4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(17.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(17.dp)
                    )
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BasicTextField(
                        value = urlInput,
                        onValueChange = onUrlValueChange,
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focusState -> onFocusChanged(focusState.isFocused) },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { onNavigate() })
                    )

                    if (urlInput.isNotEmpty()) {
                        IconButton(
                            onClick = { onUrlValueChange("") },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Input",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (urlInput.isEmpty()) {
                    Text(
                        text = "Search or enter URL",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }

            IconButton(
                modifier = Modifier.size(36.dp),
                onClick = onCloseBrowser
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Hide Browser",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// 🗺️ MODULAR COMPONENT: NAVIGATIONAL NAVIGATION BOTTOM BAR
@Composable
fun BrowserBottomBar() {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = Modifier.size(36.dp),
                onClick = { /* Reserved for webView.goBack() */ }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                modifier = Modifier.size(36.dp),
                onClick = { /* Reserved for webView.goForward() */ }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Forward",
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                modifier = Modifier.size(36.dp),
                onClick = { /* Reserved for Bookmark collection action */ }
            ) {
                Icon(
                    imageVector = Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmarks",
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                modifier = Modifier.size(36.dp),
                onClick = { /* Reserved for history or advanced parameters menu */ }
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    modifier = Modifier.size(20.dp)
                )
            }
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

    return if (text.contains(".") && !text.contains(" ")) {
        if (text.startsWith("http://") || text.startsWith("https://")) {
            text
        } else {
            "https://$text"
        }
    } else {
        "https://www.google.com/search?q=$text"
    }
}