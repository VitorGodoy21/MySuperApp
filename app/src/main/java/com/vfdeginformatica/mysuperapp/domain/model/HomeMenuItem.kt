package com.vfdeginformatica.mysuperapp.domain.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class HomeMenuItem(
    val title: String,
    val icon: ImageVector,
    val backgroundColor: Color,
    val route: String,
    val content: @Composable () -> Unit
)
