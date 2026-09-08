package com.vfdeginformatica.mysuperapp.presentation.screen.new_card.contract

sealed interface NewCardEffect {
    data class ShowToast(val message: String) : NewCardEffect
    data object CardSaved : NewCardEffect
}
