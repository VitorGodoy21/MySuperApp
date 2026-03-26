package com.vfdeginformatica.mysuperapp.presentation.my_super_app

data class SessionUiState(
    val isLoggedIn: Boolean = false,
    val isLoggedOut: Boolean = false,
    val isLoading: Boolean = false,
)
