package com.handleliste.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.sin

/** The same looping clock as Holmstead's Plus button: 0 → 1 every 2.8 s. */
@Composable
fun rememberSparkleClock(): State<Float> =
    rememberInfiniteTransition(label = "sparkle")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart), label = "t")

/** Gentle breathing scale, 1.00 – 1.04. */
fun pulseOf(t: Float): Float = 1f + 0.04f * sin(t * 2f * PI.toFloat())

/** A white band of light sweeping left → right across the content. */
fun Modifier.shimmer(t: () -> Float, alpha: Float = 0.55f): Modifier = drawWithContent {
    drawContent()
    val x = -0.5f + 2f * ((t() * 1.4f) % 1.4f)
    val w = size.width
    drawRect(
        Brush.linearGradient(
            0f to Color.Transparent, 0.5f to Color.White.copy(alpha = alpha), 1f to Color.Transparent,
            start = Offset((x - 0.15f) * w, 0f), end = Offset((x + 0.15f) * w, size.height),
        )
    )
}

private val SPOTS = listOf(
    floatArrayOf(0.05f, 0.1f, 0f), floatArrayOf(0.92f, 0f, 0.3f), floatArrayOf(0.98f, 0.85f, 0.55f),
    floatArrayOf(0.12f, 0.95f, 0.75f), floatArrayOf(0.55f, -0.1f, 0.15f),
)

/** Little four-point stars twinkling around the edges (drawn outside the bounds). */
fun Modifier.sparkles(t: () -> Float, color: Color = Color(0xFFFFE082)): Modifier = drawWithContent {
    drawContent()
    for (s in SPOTS) {
        val k = sin((((t() + s[2]) % 1f) * PI).toFloat())
        if (k > 0.05f) star(Offset(s[0] * size.width, s[1] * size.height), 6.dp2px(this) * k, color.copy(alpha = k))
    }
}

private fun Float.dp2px(d: DrawScope) = this * d.density

private fun DrawScope.star(c: Offset, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticBezierTo(c.x, c.y, c.x + r, c.y)
        quadraticBezierTo(c.x, c.y, c.x, c.y + r)
        quadraticBezierTo(c.x, c.y, c.x - r, c.y)
        quadraticBezierTo(c.x, c.y, c.x, c.y - r)
        close()
    }
    drawPath(p, color)
}

/** Holmstead's gold. */
val GoldBrush = Brush.linearGradient(listOf(Color(0xFFFFE082), Color(0xFFF2B630), Color(0xFFFFB300)))
