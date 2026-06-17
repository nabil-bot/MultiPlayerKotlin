package com.example.multiplayer.ui.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.Toast

class BrowserContextMenuHandler(
    private val context: Context,
    private val onAddVideoAction: (String) -> Unit, // 🔹 Receive dependency configuration lambda driver hook
    private val onAddVideoToPlaylistAction: (String) -> Unit
) : View.OnCreateContextMenuListener {

    companion object {
        const val ID_COPY_TEXT = 1
        const val ID_COPY_LINK = 2
        const val ID_ADD_TO_LIST = 3
        const val ID_ADD_TO_PLAYLIST = 4
    }

    private var currentExtraData: String? = null
    private var currentHitType: Int = 0
    private var currentBrowserUrl: String = "" // Keep track of browser tab reference position text fallback paths

    fun register(webView: WebView) {
        webView.setOnCreateContextMenuListener(this)
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        if (v !is WebView || menu == null) return

        val hitTestResult = v.hitTestResult
        currentHitType = hitTestResult.type
        currentExtraData = hitTestResult.extra
        currentBrowserUrl = v.url ?: "" // Grab current web layout address link path string matching viewport location

        menu.setHeaderTitle("Link Options")

        menu.add(0, ID_COPY_TEXT, 0, "Copy text").apply {
            isEnabled = !currentExtraData.isNullOrBlank()
        }

        val isLink = currentHitType == WebView.HitTestResult.SRC_ANCHOR_TYPE ||
                currentHitType == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
        menu.add(0, ID_COPY_LINK, 1, "Copy link").apply {
            isEnabled = isLink && !currentExtraData.isNullOrBlank()
        }

        // 🔹 Enabled all the time: If you click directly on a embedded media thumb layout asset frame
        menu.add(0, ID_ADD_TO_LIST, 2, "Add to List")
        menu.add(0, ID_ADD_TO_PLAYLIST, 3, "Add to Playlist")

        for (i in 0 until menu.size()) {
            menu.getItem(i).setOnMenuItemClickListener { item ->
                handleMenuAction(item)
            }
        }
    }

    private fun handleMenuAction(item: MenuItem): Boolean {
        // 1. Capture the fallback base extra parameters or default address location
        var targetedVideoUrl = if (!currentExtraData.isNullOrBlank() && currentExtraData!!.startsWith("http")) {
            currentExtraData!!
        } else {
            currentBrowserUrl
        }

        // 🔹 THE FIX: If the finger grabbed a YouTube static image asset asset link, parse and reconstruct it!
        if (targetedVideoUrl.contains("ytimg.com/vi/")) {
            try {
                // Split the path by segments to safely isolate the unique 11-character Video ID string
                val parts = targetedVideoUrl.split("ytimg.com/vi/")
                if (parts.size > 1) {
                    // Grab the text chunk immediately following the marker segment boundary
                    val afterMarker = parts[1]
                    // Split by '/' in case there are subsequent path tags like /hqdefault.jpg or /maxresdefault.jpg
                    val videoId = afterMarker.split("/")[0]

                    // Reconstruct a standard browser watch link stream path destination seamlessly
                    targetedVideoUrl = "https://www.youtube.com/watch?v=$videoId"
                }
            } catch (e: Exception) {
                e.printStackTrace() // Safe fallback to original URL configuration if structure unexpected
            }
        }

        // 2. Execute target action selections with our cleaned parameter string values
        when (item.itemId) {
            ID_COPY_TEXT -> {
                copyToClipboard("Text Data", currentExtraData ?: "")
                showToast("Text copied to clipboard")
                return true
            }
            ID_COPY_LINK -> {
                copyToClipboard("URL Link", targetedVideoUrl)
                showToast("Link copied to clipboard")
                return true
            }
            ID_ADD_TO_LIST -> {
                onAddVideoAction(targetedVideoUrl)
                showToast("Video added to the list!")
                return true
            }
            ID_ADD_TO_PLAYLIST -> {
                onAddVideoToPlaylistAction(targetedVideoUrl)
                showToast("Video added to the playlist!")
                return true
            }
        }
        return false
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}