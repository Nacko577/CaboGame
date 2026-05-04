package com.navitech.cabo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4CAF7D),
    onPrimary = Color.White,
    secondary = Color(0xFF81C784),
    background = GreenDeep,
    surface = GreenDark,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun CaboGameTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
