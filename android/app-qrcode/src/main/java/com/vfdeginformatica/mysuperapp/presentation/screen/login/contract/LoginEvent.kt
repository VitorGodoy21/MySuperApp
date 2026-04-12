package com.vfdeginformatica.mysuperapp.presentation.screen.login.contract

sealed interface LoginEvent {
    data class EmailChanged(val value: String) : LoginEvent
    data class PasswordChanged(val value: String) : LoginEvent
    data object SubmitClicked : LoginEvent
}