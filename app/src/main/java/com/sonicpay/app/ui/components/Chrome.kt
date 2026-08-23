package com.sonicpay.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sonicpay.app.ui.theme.AccentMint
import com.sonicpay.app.ui.theme.GlassBorder
import com.sonicpay.app.ui.theme.GlassFillBottom
import com.sonicpay.app.ui.theme.TextPrimary
import com.sonicpay.app.ui.theme.TextSecondary

@Composable
fun TopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Box(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

@Composable
fun GlassChip(
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) AccentMint.copy(alpha = 0.18f) else GlassFillBottom)
            .border(
                1.dp,
                if (selected) AccentMint.copy(alpha = 0.55f) else GlassBorder,
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) AccentMint else TextSecondary
        )
    }
}
