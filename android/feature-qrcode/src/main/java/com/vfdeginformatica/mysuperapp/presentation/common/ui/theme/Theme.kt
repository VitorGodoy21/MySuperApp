package com.vfdeginformatica.mysuperapp.presentation.common.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// ── Esquema de cores: modo escuro roxo ────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary = PurpleDarkPrimary,
    onPrimary = PurpleDarkOnPrimary,
    primaryContainer = PurpleDarkPrimaryContainer,
    onPrimaryContainer = PurpleDarkOnPrimaryContainer,
    secondary = PurpleDarkSecondary,
    onSecondary = PurpleDarkOnSecondary,
    tertiary = PurpleDarkTertiary,
    onTertiary = PurpleDarkOnTertiary,
    error = PurpleDarkError,
    onError = PurpleDarkOnError,
    background = PurpleDarkBackground,
    onBackground = PurpleDarkOnBackground,
    surface = PurpleDarkSurface,
    onSurface = PurpleDarkOnSurface,
    surfaceVariant = PurpleDarkSurfaceVariant,
    onSurfaceVariant = PurpleDarkOnSurfaceVariant,
    outline = PurpleDarkOutline,
    inverseSurface = PurpleDarkInverseSurface,
    inverseOnSurface = PurpleDarkInverseOnSurface,
    inversePrimary = PurpleDarkInversePrimary,
)

// ── Esquema de cores: modo claro roxo ─────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary = PurpleLightPrimary,
    onPrimary = PurpleLightOnPrimary,
    primaryContainer = PurpleLightPrimaryContainer,
    onPrimaryContainer = PurpleLightOnPrimaryContainer,
    secondary = PurpleLightSecondary,
    onSecondary = PurpleLightOnSecondary,
    tertiary = PurpleLightTertiary,
    onTertiary = PurpleLightOnTertiary,
    error = PurpleLightError,
    onError = PurpleLightOnError,
    background = PurpleLightBackground,
    onBackground = PurpleLightOnBackground,
    surface = PurpleLightSurface,
    onSurface = PurpleLightOnSurface,
    surfaceVariant = PurpleLightSurfaceVariant,
    onSurfaceVariant = PurpleLightOnSurfaceVariant,
    outline = PurpleLightOutline,
    inverseSurface = PurpleLightInverseSurface,
    inverseOnSurface = PurpleLightInverseOnSurface,
    inversePrimary = PurpleLightInversePrimary,
)

@Composable
fun MySuperAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Dynamic color desabilitado: garante que o roxo da marca seja sempre aplicado
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}