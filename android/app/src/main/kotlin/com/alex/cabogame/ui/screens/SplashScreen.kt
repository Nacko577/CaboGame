package com.alex.cabogame.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SplashBgTop = Color(0xFF0C1814)
private val SplashBgBottom = Color(0xFF050A08)
private val SplashGlow = Color(0xFF1E8F64)
private val Wordmark = Color(0xFFF0E5CC)
private val LoaderTrack = Color.White.copy(alpha = 0.12f)

@Composable
fun SplashScreen() {
    val infinite = rememberInfiniteTransition(label = "splash")
    val breathe by infinite.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )
    val sweep by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SplashBgTop, SplashBgBottom)
                )
            )
    ) {
        // Soft center glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SplashGlow.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(0.5f, 0.42f),
                        radius = 900f
                    )
                )
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            decorativeCard(
                Modifier
                    .size(width = 56.dp, height = 78.dp)
                    .offset(x = (-12).dp, y = maxHeight * 0.12f)
                    .graphicsLayer { rotationZ = -14f },
                alpha = 0.05f
            )
            decorativeCard(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(width = 52.dp, height = 72.dp)
                    .offset(x = (-18).dp, y = maxHeight * 0.08f)
                    .graphicsLayer { rotationZ = 18f },
                alpha = 0.045f
            )
            decorativeCard(
                Modifier
                    .align(Alignment.BottomStart)
                    .size(width = 48.dp, height = 66.dp)
                    .offset(x = 14.dp, y = (-100).dp)
                    .graphicsLayer { rotationZ = 22f },
                alpha = 0.035f
            )
            decorativeCard(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(width = 50.dp, height = 70.dp)
                    .offset(x = (-12).dp, y = (-88).dp)
                    .graphicsLayer { rotationZ = -11f },
                alpha = 0.04f
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.graphicsLayer {
                    scaleX = breathe
                    scaleY = breathe
                }
            ) {
                // Same overlapping “logo cards” language as the main menu.
                Box(
                    modifier = Modifier
                        .height(72.dp)
                        .width(96.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 46.dp, height = 64.dp)
                            .offset(x = 8.dp, y = 4.dp)
                            .graphicsLayer { rotationZ = -10f }
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF34C995),
                                        Color(0xFF249A72)
                                    )
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 46.dp, height = 64.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-8).dp, y = 4.dp)
                            .graphicsLayer { rotationZ = 10f }
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFF5E2C2),
                                        Color(0xFFD4B896)
                                    )
                                )
                            )
                    )
                }

                Text(
                    text = "CABO",
                    style = TextStyle(
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        color = Wordmark,
                        letterSpacing = 6.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            SplashArcLoader(
                rotation = sweep,
                modifier = Modifier.size(44.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Shuffling…",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    color = Color.White.copy(alpha = 0.42f),
                    letterSpacing = 1.sp
                )
            )
        }
    }
}

@Composable
private fun decorativeCard(modifier: Modifier, alpha: Float) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = alpha))
    )
}

@Composable
private fun SplashArcLoader(rotation: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 3.dp.toPx()
        val inset = stroke * 0.75f
        val diameter = size.minDimension - inset * 2
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)

        drawArc(
            color = LoaderTrack,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = androidx.compose.ui.geometry.Size(diameter, diameter),
            style = Stroke(width = stroke * 0.85f, cap = StrokeCap.Round)
        )

        rotate(rotation) {
            drawArc(
                color = Color.White.copy(alpha = 0.92f),
                startAngle = 0f,
                sweepAngle = 88f,
                useCenter = false,
                topLeft = topLeft,
                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }
}
