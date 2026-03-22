package com.vfdeginformatica.mysuperapp.presentation.screen.home.contract

import androidx.fragment.app.FragmentActivity

sealed interface HomeEvent {
    data class OnMenuItemNavigate(
        val route: String,
        val passwordRequired: Boolean,
        val activity: FragmentActivity? = null
    ) : HomeEvent
}