package com.vfdeginformatica.mysuperapp.presentation.screen.settings.contract

sealed interface SettingsEvent {
    data object OnNotificationsClick : SettingsEvent
}
