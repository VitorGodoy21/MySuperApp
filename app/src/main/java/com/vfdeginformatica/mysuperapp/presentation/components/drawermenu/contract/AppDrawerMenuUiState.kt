package com.vfdeginformatica.mysuperapp.presentation.components.drawermenu.contract

import com.vfdeginformatica.mysuperapp.presentation.components.drawermenu.model.UiMenuItem

data class AppDrawerMenuUiState(
    val isOpen: Boolean = true,
    val userName: String? = null,
    val userEmail: String? = null,
    val userTypeName: String? = null,
    val avatarUrl: String? = null,
    val items: List<UiMenuItem> = emptyList(),
    val selectedRoute: String? = null,
    val closeDrawer: Boolean = false
)
