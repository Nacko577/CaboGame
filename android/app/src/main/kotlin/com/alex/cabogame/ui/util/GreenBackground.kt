package com.alex.cabogame.ui.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.alex.cabogame.ui.theme.GreenDark
import com.alex.cabogame.ui.theme.GreenDeep

@Composable
fun GreenBackground(
    topColor: Color = GreenDark,
    bottomColor: Color = GreenDeep,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors = listOf(topColor, bottomColor))),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
