package com.vfdeginformatica.mysuperapp.presentation.screen.card_list.contract

sealed interface CardListEffect {
    data class NavigateToCardDetail(val cardId: String) : CardListEffect
    data object NavigateToNewCard : CardListEffect
    data class ShowToast(val message: String) : CardListEffect
}
