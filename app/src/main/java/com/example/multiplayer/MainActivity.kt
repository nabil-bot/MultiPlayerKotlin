package com.example.multiplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import com.example.multiplayer.ui.components.MainBottomBar
import com.example.multiplayer.ui.screens.AudioScreen
import com.example.multiplayer.ui.screens.MultiScreen
import com.example.multiplayer.ui.screens.VideoScreen
import com.example.multiplayer.ui.theme.MultiPlayerTheme
import com.example.multiplayer.ui.utils.BackgroundPlayManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔹 MODULARIZED: Background play features are cleanly initialized here
        BackgroundPlayManager.initialize(this)

        setContent {
            MultiPlayerTheme {
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
                        // 🔹 PERSISTENT STATE LOOKUP: We retain views structurally in memory
                        // by toggling their visibility boundaries instead of dropping state.

                        // Tab 1: Multi Tab (WebView tracking state is preserved 100% safely)
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