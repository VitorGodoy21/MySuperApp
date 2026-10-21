package com.vfdeginformatica.mysuperapp.presentation.screen.notifications.contract

sealed interface NotificationsEffect {
    data class ShowToast(val message: String) : NotificationsEffect
}
