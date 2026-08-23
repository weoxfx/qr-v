package com.sonicpay.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Concentric rings that pulse outward — used both for the merchant's
 * "broadcasting" state and the customer's "listening" state so the two
 * roles feel visually connected.
 */
@Composable
fun PulseRings(
    modifier: Modifier = Modifier,
    color: Color,
    active: Boolean,
    ringCount: Int = 3,
    maxRadiusDp: Int = 140,
    center: @Composable () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Box(
        modifier = modifier.size(maxRadiusDp.dp * 2),
        contentAlignment = Alignment.Center
    ) {
        if (active) {
            Canvas(modifier = Modifier.size(maxRadiusDp.dp * 2)) {
                val maxR = maxRadiusDp.dp.toPx()
                for (i in 0 until ringCount) {
                    val offset = i.toFloat() / ringCount
                    var p = (progress + offset) % 1f
                    val radius = p * maxR
                    val alpha = (1f - p) * 0.55f
                    drawCircle(
                        color = color.copy(alpha = alpha),
                        radius = radius,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
        }
        center()
    }
}
