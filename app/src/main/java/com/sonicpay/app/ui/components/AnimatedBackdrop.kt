package com.sonicpay.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.sonicpay.app.ui.theme.AccentMint
import com.sonicpay.app.ui.theme.AccentMintDeep
import com.sonicpay.app.ui.theme.InkBase

@Composable
fun AnimatedBackdrop(modifier: Modifier = Modifier) {
    val drift = rememberInfiniteTransition(label = "backdrop")
    val t by drift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(26000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "t"
    )

    Box(modifier.fillMaxSize().background(InkBase)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(AccentMint.copy(alpha = 0.07f), Color.Transparent),
                        center = Offset(0.12f + 0.10f * t, 0.08f),
                        radius = 1100f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(AccentMintDeep.copy(alpha = 0.09f), Color.Transparent),
                        center = Offset(0.92f - 0.08f * t, 0.94f),
                        radius = 1250f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF123B2E).copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(0.5f, 0.45f + 0.06f * t),
                        radius = 1500f
                    )
                )
        )
    }
}
