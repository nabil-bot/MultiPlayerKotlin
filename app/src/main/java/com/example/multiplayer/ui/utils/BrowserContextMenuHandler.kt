package com.example.multiplayer.ui.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.Toast

class BrowserContextMenuHandler(private val context: Context) : View.OnCreateContextMenuListener {

    // Unique Identifier Tokens for Menu Items
    companion object {
        const val ID_COPY_TEXT = 1
        const val ID_COPY_LINK = 2
        const val ID_ADD_TO_LIST = 3
        const val ID_ADD_TO_PLAYLIST = 4
    }

    private var currentExtraData: String? = null
    private var currentHitType: Int = 0

    fun register(webView: WebView) {
        webView.setOnCreateContextMenuListener(this)
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        if (v !is WebView || menu == null) return

        val hitTestResult = v.hitTestResult
        currentHitType = hitTestResult.type
        currentExtraData = hitTestResult.extra

        // Set dark header style title based on what was long-pressed
        menu.setHeaderTitle("Link Options")

        // 1. Copy Text Option (Enabled if there is valid text or data selected)
        menu.add(0, ID_COPY_TEXT, 0, "Copy text").apply {
            isEnabled = !currentExtraData.isNullOrBlank()
        }

        // 2. Copy Link Option (Enabled specifically for hyperlinks or image links)
        val isLink = currentHitType == WebView.HitTestResult.SRC_ANCHOR_TYPE ||
                currentHitType == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
        menu.add(0, ID_COPY_LINK, 1, "Copy link").apply {
            isEnabled = isLink && !currentExtraData.isNullOrBlank()
        }

        // 3. Add to List Option (Placeholder for later)
        menu.add(0, ID_ADD_TO_LIST, 2, "Add to List")

        // 4. Add to Playlist Option (Placeholder for later)
        menu.add(0, ID_ADD_TO_PLAYLIST, 3, "Add to Playlist")

        // Bind the item click engine actions dynamically
        for (i in 0 until menu.size()) {
            menu.getItem(i).setOnMenuItemClickListener { item ->
                handleMenuAction(item)
            }
        }
    }

    private fun handleMenuAction(item: MenuItem): Boolean {
        when (item.itemId) {
            ID_COPY_TEXT -> {
                copyToClipboard("Text Data", currentExtraData ?: "")
                showToast("Text copied to clipboard")
                return true
            }
            ID_COPY_LINK -> {
                copyToClipboard("URL Link", currentExtraData ?: "")
                showToast("Link copied to clipboard")
                return true
            }
            ID_ADD_TO_LIST -> {
                showToast("Add to List coming soon!")
                return true
            }
            ID_ADD_TO_PLAYLIST -> {
                showToast("Add to Playlist coming soon!")
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