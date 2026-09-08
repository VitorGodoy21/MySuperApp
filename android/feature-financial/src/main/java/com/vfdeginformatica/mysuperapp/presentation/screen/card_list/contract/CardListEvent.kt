package com.vfdeginformatica.mysuperapp.presentation.screen.card_list.contract

sealed interface CardListEvent {
    data object Reload : CardListEvent
    data class CardClicked(val cardId: String) : CardListEvent
    data object NewCardClicked : CardListEvent
}
