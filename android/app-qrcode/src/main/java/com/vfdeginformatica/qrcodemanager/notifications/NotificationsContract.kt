package com.vfdeginformatica.qrcodemanager.notifications

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val enabledAll: Boolean = true,
    val enabledAccess: Boolean = true,
    val enabledMuralComments: Boolean = true,
    val errorMessage: String = "",
    val successMessage: String = ""
)

sealed interface NotificationsEvent {
    data class OnToggleAll(val enabled: Boolean) : NotificationsEvent
    data class OnToggleAccess(val enabled: Boolean) : NotificationsEvent
    data class OnToggleMuralComments(val enabled: Boolean) : NotificationsEvent
}

sealed interface NotificationsEffect {
    data class ShowToast(val message: String) : NotificationsEffect
}

