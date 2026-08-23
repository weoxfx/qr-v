package com.sonicpay.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.ui.unit.dp

enum class OrbState { IDLE, ACTIVE, DONE }

/**
 * The central interactive orb: a layered sphere with an inner core, a rim
 * highlight and a breathing animation while engaged. Pressing scales with a
 * spring.
 */
@Composable
fun SonicOrb(
    icon: ImageVector,
    state: OrbState,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pressScale"
    )
    val breath = rememberInfiniteTransition(label = "breath")
    val breathe by breath.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val glowAlpha = when (state) {
        OrbState.IDLE -> 0.30f
        OrbState.ACTIVE -> 0.80f
        OrbState.DONE -> 0.90f
    }
    val baseColor = tint.copy(alpha = 0.92f)
    val scale = if (state == OrbState.IDLE) pressScale else pressScale * breathe

    Box(modifier = modifier.size(132.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(128.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(tint.copy(alpha = glowAlpha * 0.45f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(112.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            baseColor.copy(alpha = 0.95f),
                            baseColor.copy(alpha = 0.55f),
                            baseColor.copy(alpha = 0.30f)
                        )
                    )
                )
                .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.30f), Color.Transparent)
                        )
                    )
                    .align(Alignment.TopCenter)
            )
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.96f),
                modifier = Modifier.size(42.dp)
            )
        }
    }
}
