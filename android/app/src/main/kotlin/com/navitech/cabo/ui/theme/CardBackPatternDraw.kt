package com.navitech.cabo.ui.theme

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.sin

/**
 * Card-back artwork uses [dp] → px via [DrawScope] so strokes match iOS (point-sized) weight on
 * high-density screens; spacing is slightly wider than the old raw-pixel loop for clarity.
 */
fun DrawScope.drawCardBackPattern(pattern: CardBackPattern, accent: Color, w: Float, h: Float) {
    when (pattern) {
        CardBackPattern.SOLID -> Unit
        CardBackPattern.STRIPES -> {
            val stroke = 2.25.dp.toPx()
            val step = 11.dp.toPx()
            var x = -h
            while (x < w + h) {
                drawLine(
                    accent.copy(alpha = 0.22f),
                    start = Offset(x, h),
                    end = Offset(x + h * 1.05f, 0f),
                    strokeWidth = stroke,
                )
                x += step
            }
        }

        CardBackPattern.DOTS -> {
            val step = 10.dp.toPx()
            val dotR = 1.35.dp.toPx()
            var row = 0
            var y = step * 0.35f
            while (y < h) {
                var x = if (row % 2 == 0) step * 0.35f else step * 0.85f
                while (x < w) {
                    drawCircle(
                        accent.copy(alpha = 0.32f),
                        radius = dotR,
                        center = Offset(x + dotR, y + dotR),
                    )
                    x += step
                }
                row++
                y += step * 0.9f
            }
        }

        CardBackPattern.DIAMONDS -> {
            val step = 13.dp.toPx()
            val stroke = 1.35.dp.toPx()
            var row = 0
            var y = -step
            while (y < h + step) {
                var x = -step + if (row % 2 == 0) 0f else step * 0.5f
                while (x < w + step) {
                    val midX = x + step * 0.5f
                    val path = Path().apply {
                        moveTo(midX, y)
                        lineTo(x + step, y + step * 0.5f)
                        lineTo(midX, y + step)
                        lineTo(x, y + step * 0.5f)
                        close()
                    }
                    drawPath(
                        path,
                        color = accent.copy(alpha = 0.38f),
                        style = Stroke(width = stroke),
                    )
                    x += step
                }
                row++
                y += step * 0.55f
            }
        }

        CardBackPattern.CROSSHATCH -> {
            val strokeA = 1.35.dp.toPx()
            val strokeB = 1.15.dp.toPx()
            val step = 9.dp.toPx()
            var x = -h
            while (x < w + h) {
                drawLine(
                    accent.copy(alpha = 0.18f),
                    start = Offset(x, h),
                    end = Offset(x + h, 0f),
                    strokeWidth = strokeA,
                )
                x += step
            }
            var x2 = -h
            while (x2 < w + h) {
                drawLine(
                    accent.copy(alpha = 0.15f),
                    start = Offset(x2, 0f),
                    end = Offset(x2 + h, h),
                    strokeWidth = strokeB,
                )
                x2 += step
            }
        }

        CardBackPattern.SQUIGGLES -> {
            val m = minOf(w, h)
            val stroke = max(1f, m * 0.016f)
            val amp = m * 0.04f
            val lines = 6
            for (i in 0 until lines) {
                val t = i / max(lines - 1, 1).toFloat()
                val baseY = h * (0.12f + t * 0.76f)
                val phase = i * 0.85f
                fun yAt(xv: Float): Float =
                    baseY + amp * (
                        0.7f * sin((xv * 0.11 + phase).toDouble()).toFloat() +
                            0.35f * sin((xv * 0.23 + phase * 1.3f).toDouble()).toFloat()
                        )
                val path = Path().apply {
                    moveTo(0f, yAt(0f))
                    var xv = 2.5f
                    while (xv <= w) {
                        lineTo(xv, yAt(xv))
                        xv += 2.5f
                    }
                }
                drawPath(
                    path = path,
                    color = accent.copy(alpha = 0.36f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }

        CardBackPattern.PLAID -> {
            val strokeH = 1.5.dp.toPx()
            val strokeV = 1.25.dp.toPx()
            val stepY = 8.dp.toPx()
            val stepX = 9.dp.toPx()
            var yy = 0f
            while (yy < h) {
                drawLine(
                    accent.copy(alpha = 0.22f),
                    start = Offset(0f, yy),
                    end = Offset(w, yy),
                    strokeWidth = strokeH,
                )
                yy += stepY
            }
            var xx = 0f
            while (xx < w) {
                drawLine(
                    accent.copy(alpha = 0.16f),
                    start = Offset(xx, 0f),
                    end = Offset(xx, h),
                    strokeWidth = strokeV,
                )
                xx += stepX
            }
        }
    }
}

fun DrawScope.drawCardBackFillAndPattern(deck: CardDeckStyle, cornerPx: Float) {
    val w = size.width
    val h = size.height
    drawRoundRect(
        color = deck.backBase(),
        topLeft = Offset.Zero,
        size = Size(w, h),
        cornerRadius = CornerRadius(cornerPx, cornerPx),
    )
    val pat = deck.backPattern()
    if (pat != CardBackPattern.SOLID) {
        drawCardBackPattern(pat, deck.backAccent(), w, h)
    }
}
