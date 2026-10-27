package com.vfdeginformatica.mysuperapp.presentation.screen.home.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Per-topic accent used by the Home cards so each row has its own identity. */
data class HomeTopicColors(
    /** Icon circle background and emphasized values. */
    val accent: Color,
    /** Text/icon drawn on top of [accent]. */
    val onAccent: Color,
    /** Soft card background. */
    val container: Color,
    /** Primary text on [container]. */
    val onContainer: Color,
)

object HomeColors {
    @Composable
    fun finance(): HomeTopicColors = if (isSystemInDarkTheme()) {
        HomeTopicColors(
            accent = Color(0xFF4DB6AC),
            onAccent = Color(0xFF00332E),
            container = Color(0xFF17403B),
            onContainer = Color(0xFFD9F5F1),
        )
    } else {
        HomeTopicColors(
            accent = Color(0xFF00796B),
            onAccent = Color.White,
            container = Color(0xFFE0F2F1),
            onContainer = Color(0xFF00322C),
        )
    }

    @Composable
    fun smartTags(): HomeTopicColors = if (isSystemInDarkTheme()) {
        HomeTopicColors(
            accent = Color(0xFF64B5F6),
            onAccent = Color(0xFF00284A),
            container = Color(0xFF1B3550),
            onContainer = Color(0xFFDCEBFF),
        )
    } else {
        HomeTopicColors(
            accent = Color(0xFF1565C0),
            onAccent = Color.White,
            container = Color(0xFFE3F2FD),
            onContainer = Color(0xFF0B2545),
        )
    }

    @Composable
    fun workout(): HomeTopicColors = if (isSystemInDarkTheme()) {
        HomeTopicColors(
            accent = Color(0xFFFFB74D),
            onAccent = Color(0xFF4A2600),
            container = Color(0xFF4A3421),
            onContainer = Color(0xFFFFE9D1),
        )
    } else {
        HomeTopicColors(
            accent = Color(0xFFEF6C00),
            onAccent = Color.White,
            container = Color(0xFFFFF3E0),
            onContainer = Color(0xFF4A2600),
        )
    }
}
