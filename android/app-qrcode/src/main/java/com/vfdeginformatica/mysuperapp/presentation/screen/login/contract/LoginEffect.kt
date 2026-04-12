package com.vfdeginformatica.mysuperapp.presentation.screen.login.contract

sealed interface LoginEffect {
    data object NavigateToHome : LoginEffect
    data class ShowToast(val message: String) : LoginEffect
}