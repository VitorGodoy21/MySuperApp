package com.vfdeginformatica.mysuperapp.presentation.screen.notifications.contract

sealed interface NotificationsEvent {
    data class OnToggleAll(val enabled: Boolean) : NotificationsEvent
    data class OnToggleAccess(val enabled: Boolean) : NotificationsEvent
    data class OnToggleMuralComments(val enabled: Boolean) : NotificationsEvent
}
