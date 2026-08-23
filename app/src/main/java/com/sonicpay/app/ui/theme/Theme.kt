package com.sonicpay.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SonicPayColorScheme = darkColorScheme(
    primary = AccentBlue,
    secondary = AccentViolet,
    tertiary = AccentCyan,
    background = BgTop,
    surface = BgBottom,
    onPrimary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

@Composable
fun SonicPayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SonicPayColorScheme,
        typography = SonicPayTypography,
        content = content
    )
}
