package com.alex.cabogame.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.cabogame.ui.theme.SplashGreenDark
import com.alex.cabogame.ui.theme.SplashGreenDeep
import com.alex.cabogame.ui.util.GreenBackground

@Composable
fun SplashScreen() {
    GreenBackground(topColor = SplashGreenDark, bottomColor = SplashGreenDeep) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "♣",
                style = TextStyle(fontSize = 64.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.9f))
            )
            Text(
                text = "CABO",
                style = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Black, color = Color.White)
            )
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
        }
    }
}
