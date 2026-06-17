package com.example.multiplayer.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MusicNote // New professional audio icon

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Multi : Screen("multi", "Multi", Icons.Default.List)          // Now uses the list icon
    object Video : Screen("video", "Video", Icons.Default.PlayArrow)     // Keeps the play icon
    object Audio : Screen("audio", "Audio", Icons.Default.MusicNote)     // Now uses the clean music note icon
}