package com.vfdeginformatica.mysuperapp.presentation.screen.settings.contract

sealed interface SettingsEffect {
    data class Navigate(val route: String) : SettingsEffect
}
