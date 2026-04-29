package com.alex.cabogame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.cabogame.ui.theme.*

@Composable
fun MainMenuScreen(onPlay: () -> Unit, onHowToPlay: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF141F1A),
                        Color(0xFF0A1410)
                    )
                )
            )
    ) {
        // Decorative card shapes in background
        Box(modifier = Modifier.fillMaxSize()) {
            // Top left card
            Surface(
                modifier = Modifier
                    .offset(x = (-10).dp, y = 100.dp)
                    .rotate(-15f)
                    .size(width = 50.dp, height = 70.dp),
                shape = RoundedCornerShape(6.dp),
                color = Color.White.copy(alpha = 0.03f)
            ) {}
            
            // Top right card
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = 80.dp)
                    .rotate(20f)
                    .size(width = 50.dp, height = 70.dp),
                shape = RoundedCornerShape(6.dp),
                color = Color.White.copy(alpha = 0.03f)
            ) {}
            
            // Bottom left card
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 20.dp, y = (-120).dp)
                    .rotate(25f)
                    .size(width = 40.dp, height = 56.dp),
                shape = RoundedCornerShape(5.dp),
                color = Color.White.copy(alpha = 0.02f)
            ) {}
            
            // Bottom right card
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-30).dp, y = (-100).dp)
                    .rotate(-10f)
                    .size(width = 40.dp, height = 56.dp),
                shape = RoundedCornerShape(5.dp),
                color = Color.White.copy(alpha = 0.02f)
            ) {}
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Spacer(Modifier.weight(1f))
            
            // Logo section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card icon above title
                Box {
                    Surface(
                        modifier = Modifier
                            .offset(x = (-10).dp)
                            .rotate(-8f)
                            .size(width = 36.dp, height = 50.dp),
                        shape = RoundedCornerShape(5.dp),
                        color = AccentGreen
                    ) {}
                    
                    Surface(
                        modifier = Modifier
                            .offset(x = 10.dp)
                            .rotate(8f)
                            .size(width = 36.dp, height = 50.dp),
                        shape = RoundedCornerShape(5.dp),
                        color = AccentGold
                    ) {}
                }
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    "CABO",
                    style = TextStyle(
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF2E6CC),
                                Color(0xFFD9C7A6)
                            )
                        )
                    )
                )
                
                Text(
                    "Memory, risk, and low score wins.",
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                )
            }
            
            Spacer(Modifier.height(80.dp))
            
            // Action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Play button
                Button(
                    onClick = onPlay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = AccentGreen.copy(alpha = 0.4f),
                            spotColor = AccentGreen.copy(alpha = 0.4f)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(AccentGreen, AccentGreenDark)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFF0F1A14),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "Play",
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F1A14)
                                )
                            )
                        }
                    }
                }
                
                // How To Play button
                OutlinedButton(
                    onClick = onHowToPlay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = 0.08f),
                        contentColor = Color.White.copy(alpha = 0.85f)
                    ),
                    border = null
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "?",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            "How To Play",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
            
            Spacer(Modifier.weight(1f))
            
            // Version text
            Text(
                "v1.0",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.25f)
                )
            )
            
            Spacer(Modifier.height(20.dp))
        }
    }
}
