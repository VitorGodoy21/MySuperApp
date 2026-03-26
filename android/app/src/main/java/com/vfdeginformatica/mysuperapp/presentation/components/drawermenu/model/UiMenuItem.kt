package com.vfdeginformatica.mysuperapp.presentation.components.drawermenu.model

import androidx.compose.ui.graphics.vector.ImageVector

data class UiMenuItem(
    val route: String,
    val label: String,
    val icon: ImageVector? = null
)