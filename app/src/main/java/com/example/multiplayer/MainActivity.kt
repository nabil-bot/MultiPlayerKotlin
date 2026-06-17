package com.example.multiplayer

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.core.view.WindowCompat
import com.example.multiplayer.ui.components.MainBottomBar
import com.example.multiplayer.ui.screens.AudioScreen
import com.example.multiplayer.ui.screens.MultiScreen
import com.example.multiplayer.ui.screens.VideoScreen
import com.example.multiplayer.ui.theme.MultiPlayerTheme
import com.example.multiplayer.ui.utils.BackgroundPlayManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔹 Background play features remain safely intact and modularized
        BackgroundPlayManager.initialize(this)

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        setContent {
            MultiPlayerTheme {
                // 🔹 FIX STEP 1: Dynamically listen to the system dark theme state hook
                val isDarkTheme = isSystemInDarkTheme()

                val view = androidx.compose.ui.platform.LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val currentWindow = (view.context as android.app.Activity).window

                        WindowCompat.setDecorFitsSystemWindows(currentWindow, true)

                        // 🔹 FIX STEP 2: Invert the flag dynamically based on the active theme
                        // Light theme -> true (Black icons)
                        // Dark theme  -> false (White icons)
                        val windowInsetsController = WindowCompat.getInsetsController(currentWindow, view)
                        windowInsetsController.isAppearanceLightStatusBars = !isDarkTheme
                    }
                }

                // Single source of truth driving the persistent views
                var currentTabRoute by remember { mutableStateOf("multi") }

                Scaffold(
                    bottomBar = {
                        MainBottomBar(
                            currentRoute = currentTabRoute,
                            onTabSelected = { selectedRoute -> currentTabRoute = selectedRoute }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Tab 1: Multi Tab
                        Box(modifier = Modifier.fillMaxSize().customVisibility(currentTabRoute == "multi")) {
                            MultiScreen()
                        }

                        // Tab 2: Video Tab
                        Box(modifier = Modifier.fillMaxSize().customVisibility(currentTabRoute == "video")) {
                            VideoScreen()
                        }

                        // Tab 3: Audio Tab
                        Box(modifier = Modifier.fillMaxSize().customVisibility(currentTabRoute == "audio")) {
                            AudioScreen()
                        }
                    }
                }
            }
        }
    }

    // High performance UI layout extension method to hide components without unloading state
    private fun Modifier.customVisibility(isVisible: Boolean): Modifier {
        return this.then(
            if (isVisible) Modifier else Modifier.layout { _, _ -> layout(0, 0) {} }
        )
    }
}