package com.vfdeginformatica.mysuperapp.presentation.screen.home.contract

sealed interface HomeEvent {
    data class OnMenuItemNavigate(val route: String) : HomeEvent
}