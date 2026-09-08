package com.vfdeginformatica.mysuperapp.presentation.screen.card_list.contract

import com.vfdeginformatica.mysuperapp.data.remote.dto.CardDto

data class CardListUiState(
    val isLoading: Boolean = false,
    val cards: List<CardDto> = emptyList(),
    val errorMessage: String? = null,
)
