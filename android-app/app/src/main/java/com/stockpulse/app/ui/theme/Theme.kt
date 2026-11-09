package com.stockpulse.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Tema STOCKPULSE: oscuro por defecto (identidad fintech) y claro,
 * siguiendo la preferencia del sistema (seccion 7: modo oscuro y claro).
 */

private val DarkColors = darkColorScheme(
    primary = BluePrimaryDark,
    onPrimary = DarkTextPrimary,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = DarkTextPrimary,
    secondary = GreenGain,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkTextPrimary,
    error = RedLoss,
    onError = DarkTextPrimary,
    errorContainer = DarkSurfaceVariant,
    onErrorContainer = DarkTextPrimary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
)

private val LightColors = lightColorScheme(
    primary = BluePrimaryLight,
    onPrimary = LightSurface,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = DarkTextPrimaryOnLight,
    secondary = GreenGain,
    onSecondary = LightSurface,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = DarkTextPrimaryOnLight,
    error = RedLoss,
    onError = LightSurface,
    errorContainer = LightSurfaceVariant,
    onErrorContainer = DarkTextPrimaryOnLight,
    background = LightBackground,
    onBackground = DarkTextPrimaryOnLight,
    surface = LightSurface,
    onSurface = DarkTextPrimaryOnLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = DarkTextSecondaryOnLight,
    outline = LightOutline,
)

@Composable
fun StockPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
