package com.example.multiplayer.ui.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout

fun Modifier.customVisibility(isVisible: Boolean): Modifier {
    return this.then(
        if (isVisible) Modifier else Modifier.layout { _, _ -> layout(0, 0) {} }
    )
}

fun buildNavigationUrl(input: String): String {
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