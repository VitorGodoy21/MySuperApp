package com.vfdeginformatica.mysuperapp.presentation.screen.home.contract

import com.vfdeginformatica.mysuperapp.domain.model.HomeMenuItem

data class HomeUiState(
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val items: List<HomeMenuItem> = emptyList()
)