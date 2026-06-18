package com.example.multiplayer.ui.components

import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple // 🔹 Import the modern Material 3 ripple engine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserBottomBar(webView: WebView) {
    val context = LocalContext.current

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
            // 🔹 BUTTON 1: Add in Circle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .combinedClickable(
                        onClick = { /* Handle add in circle click here */ },
                        onLongClick = {
                            Toast.makeText(context, "Add in Circle", Toast.LENGTH_SHORT).show()
                        },
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 18.dp) // 🔹 FIX: Switched to clean M3 ripple()
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = "Add in Circle",
                    modifier = Modifier.size(20.dp)
                )
            }

            // 🔹 BUTTON 2: Add to List
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .combinedClickable(
                        onClick = { /* Handle add to list click here */ },
                        onLongClick = {
                            Toast.makeText(context, "Add to List", Toast.LENGTH_SHORT).show()
                        },
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 18.dp) // 🔹 FIX: Switched to clean M3 ripple()
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Queue,
                    contentDescription = "Add to List",
                    modifier = Modifier.size(20.dp)
                )
            }

            // 🔹 BUTTON 3: Add Entire Playlist
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .combinedClickable(
                        onClick = { /* Handle add entire playlist click here */ },
                        onLongClick = {
                            Toast.makeText(context, "Add Entire Playlist", Toast.LENGTH_SHORT).show()
                        },
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 18.dp) // 🔹 FIX: Switched to clean M3 ripple()
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlaylistAdd,
                    contentDescription = "Add Entire Playlist",
                    modifier = Modifier.size(20.dp)
                )
            }

            // 🔹 BUTTON 4: Forward Navigation
            IconButton(
                modifier = Modifier.size(36.dp),
                onClick = { if (webView.canGoForward()) webView.goForward() }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Forward",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Button 5: Bookmarks
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

            // Button 6: Menu / Options
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