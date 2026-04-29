package com.alex.cabogame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainMenuScreen(onPlay: () -> Unit, onHowToPlay: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1713),
                        Color(0xFF08100D)
                    )
                )
            )
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // Decorative background cards.
            Box(
                modifier = Modifier
                    .size(width = 60.dp, height = 84.dp)
                    .offset(x = (-20).dp, y = (maxHeight * 0.15f))
                    .graphicsLayer(rotationZ = -15f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.03f))
            )
            Box(
                modifier = Modifier
                    .size(width = 60.dp, height = 84.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-20).dp, y = (maxHeight * 0.10f))
                    .graphicsLayer(rotationZ = 20f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.03f))
            )
            Box(
                modifier = Modifier
                    .size(width = 50.dp, height = 70.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = 10.dp, y = (-120).dp)
                    .graphicsLayer(rotationZ = 25f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.02f))
            )
            Box(
                modifier = Modifier
                    .size(width = 50.dp, height = 70.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-16).dp, y = (-90).dp)
                    .graphicsLayer(rotationZ = -10f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.02f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Overlapping logo cards.
                    Box(
                        modifier = Modifier
                            .height(70.dp)
                            .width(92.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 62.dp)
                                .offset(x = 10.dp, y = 2.dp)
                                .graphicsLayer(rotationZ = -8f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF2EC08C))
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 62.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = (-8).dp, y = 2.dp)
                                .graphicsLayer(rotationZ = 8f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF2D7A6))
                        )
                    }

                    Text(
                        text = "CABO",
                        style = TextStyle(
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            color = Color(0xFFF0E5CC)
                        ),
                        letterSpacing = 4.sp
                    )
                }

                Spacer(modifier = Modifier.height(80.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Button(
                        onClick = onPlay,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFF0B1A14)
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF3CC696),
                                            Color(0xFF28AD81)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Subtle top-left highlight to mimic the iOS shine.
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.34f),
                                                Color.White.copy(alpha = 0.08f),
                                                Color.Transparent
                                            ),
                                            start = Offset(0f, 0f),
                                            end = Offset(640f, 280f)
                                        )
                                    )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Play",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onHowToPlay,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.85f)),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.15f))
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    ) {
                        Icon(Icons.Filled.HelpOutline, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "How To Play",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    "v1.0",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}
