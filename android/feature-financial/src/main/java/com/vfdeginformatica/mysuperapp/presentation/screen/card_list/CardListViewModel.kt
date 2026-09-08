package com.vfdeginformatica.mysuperapp.presentation.screen.card_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.card.GetCardsUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.card_list.contract.CardListEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.card_list.contract.CardListEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.card_list.contract.CardListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CardListViewModel @Inject constructor(
    private val getCardsUseCase: GetCardsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardListUiState())
    val uiState: StateFlow<CardListUiState> = _uiState

    private val _effect = MutableSharedFlow<CardListEffect>()
    val effect: SharedFlow<CardListEffect> = _effect

    init {
        loadCards()
    }

    fun onEvent(event: CardListEvent) {
        when (event) {
            CardListEvent.Reload -> loadCards()

            is CardListEvent.CardClicked -> viewModelScope.launch {
                _effect.emit(CardListEffect.NavigateToCardDetail(event.cardId))
            }

            CardListEvent.NewCardClicked -> viewModelScope.launch {
                _effect.emit(CardListEffect.NavigateToNewCard)
            }
        }
    }

    private fun loadCards() {
        viewModelScope.launch {
            getCardsUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }

                    is Resource.Success -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            cards = result.data.orEmpty().sortedBy { card -> card.bank },
                            errorMessage = null
                        )
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = result.message)
                        }
                        _effect.emit(
                            CardListEffect.ShowToast(
                                result.message ?: "Erro ao carregar cartões"
                            )
                        )
                    }
                }
            }
        }
    }
}
