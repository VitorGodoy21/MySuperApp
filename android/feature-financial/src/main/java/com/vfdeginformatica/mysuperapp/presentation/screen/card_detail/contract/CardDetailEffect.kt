package com.vfdeginformatica.mysuperapp.presentation.screen.card_detail.contract

sealed interface CardDetailEffect {
    data class ShowToast(val message: String) : CardDetailEffect
    data object CardSaved : CardDetailEffect
}
