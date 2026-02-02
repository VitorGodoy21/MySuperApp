package com.vfdeginformatica.mysuperapp.presentation.screen.home.contract

data class HomeUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)