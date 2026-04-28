package com.alex.cabogame.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.cabogame.ui.util.GreenBackground

@Composable
fun MainMenuScreen(onPlay: () -> Unit, onHowToPlay: () -> Unit) {
    GreenBackground {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight().padding(24.dp)
        ) {
            Spacer(Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "CABO",
                    style = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color.White)
                )
                Text(
                    "Memory, risk, and low score wins.",
                    style = TextStyle(fontSize = 14.sp, color = Color.White.copy(alpha = 0.82f))
                )
            }

            Spacer(Modifier.height(22.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(14.dp)
                ) {
                    Button(onClick = onPlay, modifier = Modifier.fillMaxWidth()) {
                        Text("Play")
                    }
                    OutlinedButton(
                        onClick = onHowToPlay,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("How To Play")
                    }
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}
