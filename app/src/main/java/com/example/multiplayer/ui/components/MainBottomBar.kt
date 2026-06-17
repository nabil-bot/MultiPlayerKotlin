package com.example.multiplayer.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.multiplayer.ui.navigation.Screen

@Composable
fun MainBottomBar(
    currentRoute: String,
    onTabSelected: (String) -> Unit
) {
    val items = listOf(Screen.Multi, Screen.Video, Screen.Audio)

    NavigationBar(
        modifier = Modifier.height(100.dp) // Keeps your exact custom 100.dp aesthetic height profile intact
    ) {
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = null,
                selected = currentRoute == screen.route,
                alwaysShowLabel = true,
                onClick = {
                    if (currentRoute != screen.route) {
                        onTabSelected(screen.route)
                    }
                }
            )
        }
    }
}