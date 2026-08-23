package com.sonicpay.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sonicpay.app.ui.theme.AccentBlue
import com.sonicpay.app.ui.theme.AccentCyan
import com.sonicpay.app.ui.theme.AccentViolet
import com.sonicpay.app.ui.theme.BgBottom
import com.sonicpay.app.ui.theme.BgTop
import com.sonicpay.app.ui.theme.GlassBorder
import com.sonicpay.app.ui.theme.GlassWhite

/**
 * A frosted "liquid glass" card: soft translucent fill, a subtle light border,
 * and a faint diagonal sheen — reads as glassmorphic without requiring
 * real-time blur (keeps it compatible across all Android versions).
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 28,
    content: @Composable Modifier.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(GlassWhite, Color(0x0DFFFFFF)),
                    start = Offset(0f, 0f),
                    end = Offset(400f, 400f)
                )
            )
            .border(1.dp, GlassBorder, shape)
    ) {
        Modifier.fillMaxSize().content()
    }
}

/** Slowly drifting gradient backdrop used behind every screen. */
@Composable
fun AnimatedBackdrop(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "backdrop")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(BgTop, BgBottom, BgTop),
                    start = Offset(shift * 300f, 0f),
                    end = Offset(1000f - shift * 300f, 1200f)
                )
            )
    ) {
        // Soft glow blobs to fake depth without a real blur renderer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(AccentBlue.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(120f + shift * 60f, 220f),
                        radius = 700f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(AccentViolet.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(900f - shift * 80f, 1400f),
                        radius = 800f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(AccentCyan.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(500f, 900f - shift * 100f),
                        radius = 600f
                    )
                )
        )
    }
}
