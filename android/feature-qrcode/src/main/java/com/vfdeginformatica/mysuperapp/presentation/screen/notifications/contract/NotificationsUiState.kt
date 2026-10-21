package com.vfdeginformatica.mysuperapp.presentation.screen.notifications.contract

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val enabledAll: Boolean = true,
    val enabledAccess: Boolean = true,
    val enabledMuralComments: Boolean = true,
    val errorMessage: String = "",
    val successMessage: String = ""
)
