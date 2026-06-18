package com.example.multiplayer.ui.utils

import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewGroup
import android.webkit.WebView
import androidx.core.view.NestedScrollingChild3
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat
import kotlin.math.abs

/**
 * Bridges a raw Android [WebView]'s touch handling into Compose's nested-scroll
 * system so that a wrapping [androidx.compose.material3.pulltorefresh.PullToRefreshBox]
 * can correctly detect pull-to-refresh gestures.
 *
 * Only engages pull-to-refresh forwarding for a deliberate, mostly-vertical,
 * sustained downward drag while the page is pinned at the top — this avoids
 * misfiring when the user scrolls/drags an element inside the web page itself
 * (e.g. a carousel or inner list).
 */
class NestedScrollWebViewContainer(
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
    private val pullEngageThreshold = touchSlop * 6 // require a bigger, deliberate pull

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

                val totalDy = ev.y - startY // positive = moved down since gesture start
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