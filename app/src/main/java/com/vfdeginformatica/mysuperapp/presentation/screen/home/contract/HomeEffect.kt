package com.vfdeginformatica.mysuperapp.presentation.screen.home.contract

sealed interface HomeEffect {
    data class ShowToast(val message: String) : HomeEffect
}