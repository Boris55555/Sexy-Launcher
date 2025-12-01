package com.boris55555.sexylauncher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    secondary = White,
    onSecondary = Black,
    tertiary = White,
    onTertiary = Black,
    background = Black,
    onBackground = White,
    surface = Black,
    onSurface = White,
)

private val LightColorScheme = lightColorScheme(
    primary = Black,
    onPrimary = White,
    secondary = Black,
    onSecondary = White,
    tertiary = Black,
    onTertiary = White,
    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
)

@Composable
fun SexyLauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
