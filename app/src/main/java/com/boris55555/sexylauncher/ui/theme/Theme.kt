package com.boris55555.sexylauncher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

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
    selectedFontName: String = "Sans Serif",
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    val fontFamily = when (selectedFontName) {
        "Serif" -> FontFamily.Serif
        "Monospace" -> FontFamily.Monospace
        "Sans Serif Condensed" -> FontFamily.SansSerif // We can use regular sans-serif as fallback or keep condensed if you like
        "Sans Serif Medium" -> FontFamily.SansSerif
        "Sans Serif Black" -> FontFamily.SansSerif
        else -> FontFamily.SansSerif
    }

    val baseTypography = com.boris55555.sexylauncher.ui.theme.Typography
    
    val typography = Typography(
        bodyLarge = baseTypography.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = baseTypography.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = baseTypography.bodySmall.copy(fontFamily = fontFamily),
        headlineLarge = baseTypography.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = baseTypography.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = baseTypography.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = baseTypography.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = baseTypography.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = baseTypography.titleSmall.copy(fontFamily = fontFamily),
        labelLarge = baseTypography.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = baseTypography.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = baseTypography.labelSmall.copy(fontFamily = fontFamily),
        displayLarge = baseTypography.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = baseTypography.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = baseTypography.displaySmall.copy(fontFamily = fontFamily),
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
