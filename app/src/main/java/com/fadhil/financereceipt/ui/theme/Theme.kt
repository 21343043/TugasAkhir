package com.fadhil.financereceipt.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color


private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF64748B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8EFF6),
    onPrimaryContainer = Color(0xFF172B46),

    secondary = Color(0xFF64748B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EFF6),
    onSecondaryContainer = Color(0xFF172B46),

    tertiary = Color(0xFF64748B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8EFF6),
    onTertiaryContainer = Color(0xFF172B46),

    background = Color(0xFFF7F9FB),
    onBackground = Color(0xFF172B46),

    surface = Color(0xFFF7F9FB),
    onSurface = Color(0xFF172B46),

    surfaceVariant = Color(0xFFE8EFF6),
    onSurfaceVariant = Color(0xFF475569),

    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F9FB),
    surfaceContainer = Color(0xFFEFF3F7),
    surfaceContainerHigh = Color(0xFFE8EFF6),
    surfaceContainerHighest = Color(0xFFE1E8F0),

    inverseSurface = Color(0xFFE8EFF6),
    inverseOnSurface = Color(0xFF172B46),
    inversePrimary = Color(0xFF64748B),

    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFD8E0E8),
    surfaceTint = Color(0xFF64748B)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFCBD5E1),
    onPrimary = Color(0xFF172B46),
    primaryContainer = Color(0xFF334155),
    onPrimaryContainer = Color(0xFFE8EFF6),

    secondary = Color(0xFFCBD5E1),
    onSecondary = Color(0xFF172B46),
    secondaryContainer = Color(0xFF334155),
    onSecondaryContainer = Color(0xFFE8EFF6),

    tertiary = Color(0xFFCBD5E1),
    onTertiary = Color(0xFF172B46),
    tertiaryContainer = Color(0xFF334155),
    onTertiaryContainer = Color(0xFFE8EFF6),

    background = Color(0xFF111827),
    onBackground = Color(0xFFE8EFF6),

    surface = Color(0xFF111827),
    onSurface = Color(0xFFE8EFF6),

    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),

    surfaceContainerLowest = Color(0xFF0B1220),
    surfaceContainerLow = Color(0xFF111827),
    surfaceContainer = Color(0xFF1E293B),
    surfaceContainerHigh = Color(0xFF334155),
    surfaceContainerHighest = Color(0xFF475569),

    inverseSurface = Color(0xFFE8EFF6),
    inverseOnSurface = Color(0xFF172B46),
    inversePrimary = Color(0xFF64748B),

    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF475569),
    surfaceTint = Color(0xFFCBD5E1)
)


@Composable
fun TugasAkhirTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}