package com.stockpulse.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Identidad STOCKPULSE (seccion 7 del prompt):
 * Azul oscuro / negro / blanco, verde = ganancias, rojo = perdidas.
 */

// ── Identidad fija (ganancias/perdidas, ambos temas) ──
val GreenGain = Color(0xFF15B86A)
val RedLoss = Color(0xFFE04545)

// ── Tema oscuro (por defecto, estilo fintech) ──
val DarkBackground = Color(0xFF0B1220)        // casi negro con matiz azul
val DarkSurface = Color(0xFF121A2B)           // tarjetas
val DarkSurfaceVariant = Color(0xFF1A2438)
val DarkOutline = Color(0xFF2A3650)
val DarkTextPrimary = Color(0xFFF2F5FA)
val DarkTextSecondary = Color(0xFF9AA7BD)
val BluePrimaryDark = Color(0xFF2D6CDF)

// ── Tema claro ──
val LightBackground = Color(0xFFF7F9FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE8EDF5)
val LightOutline = Color(0xFFC6CFDE)
val DarkTextPrimaryOnLight = Color(0xFF10182A)
val DarkTextSecondaryOnLight = Color(0xFF5B6B85)
val BluePrimaryLight = Color(0xFF0E2A55)
