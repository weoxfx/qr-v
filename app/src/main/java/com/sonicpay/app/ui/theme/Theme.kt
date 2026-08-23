package com.sonicpay.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SonicPayColorScheme = darkColorScheme(
    primary = AccentMint,
    onPrimary = InkBase,
    secondary = AccentMintDeep,
    tertiary = WarnAmber,
    background = InkBase,
    surface = InkRaised,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = WarnAmber,
)

@Composable
fun SonicPayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SonicPayColorScheme,
        typography = SonicPayTypography,
        content = content
    )
}
