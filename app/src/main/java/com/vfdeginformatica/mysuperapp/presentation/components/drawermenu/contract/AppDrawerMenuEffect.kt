package com.vfdeginformatica.mysuperapp.presentation.components.drawermenu.contract

interface AppDrawerMenuEffect {
    data class Navigate(val route: String) : AppDrawerMenuEffect
    data object NavigateToLogin : AppDrawerMenuEffect
    data object NavigateToHome : AppDrawerMenuEffect
    data class ShowToastMessage(val msg: String) : AppDrawerMenuEffect
    data class ShowSnackBarMessage(val msg: String) : AppDrawerMenuEffect
}