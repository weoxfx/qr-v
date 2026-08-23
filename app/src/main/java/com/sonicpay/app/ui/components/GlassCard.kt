package com.sonicpay.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sonicpay.app.ui.theme.GlassBorder
import com.sonicpay.app.ui.theme.GlassFillBottom
import com.sonicpay.app.ui.theme.GlassFillTop
import com.sonicpay.app.ui.theme.GlassHighlight

/**
 * Layered glass panel: vertical translucent fill, hairline border that is
 * brighter along the top edge (light source), and an optional slow specular
 * sheen drifting across the surface.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 26,
    sheen: Boolean = false,
    content: @Composable Modifier.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    val sheenTransition = rememberInfiniteTransition(label = "sheen")
    val sweep by sheenTransition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(listOf(GlassFillTop, GlassFillBottom))
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(GlassHighlight, GlassBorder.copy(alpha = 0.35f))
                ),
                shape = shape
            )
    ) {
        if (sheen) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            0.0f to Color.Transparent,
                            0.5f to Color.White.copy(alpha = 0.05f),
                            1.0f to Color.Transparent,
                            start = Offset(sweep * 900f - 300f, 0f),
                            end = Offset(sweep * 900f, 1400f)
                        )
                    )
            )
        }
        Modifier.fillMaxSize().content()
    }
}
