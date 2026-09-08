package com.vfdeginformatica.mysuperapp.presentation.screen.card_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.card.GetCardsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.card.UpdateCardUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.NewTransactionUseCase
import com.vfdeginformatica.mysuperapp.presentation.common.CurrencyVisualTransformation
import com.vfdeginformatica.mysuperapp.presentation.screen.card_detail.contract.CardDetailEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.card_detail.contract.CardDetailEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.card_detail.contract.CardDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CardDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCardsUseCase: GetCardsUseCase,
    private val updateCardUseCase: UpdateCardUseCase,
    private val newTransactionUseCase: NewTransactionUseCase,
) : ViewModel() {

    private val cardId: String = savedStateHandle.get<String>("cardId").orEmpty()

    private val _uiState = MutableStateFlow(CardDetailUiState(cardId = cardId))
    val uiState: StateFlow<CardDetailUiState> = _uiState

    private val _effect = MutableSharedFlow<CardDetailEffect>()
    val effect: SharedFlow<CardDetailEffect> = _effect

    init {
        loadCard()
    }

    fun onEvent(event: CardDetailEvent) {
        when (event) {
            is CardDetailEvent.BankChanged -> updateForm { it.copy(bank = event.bank) }

            is CardDetailEvent.MethodToggled -> updateForm { state ->
                val methods = if (state.paymentMethods.contains(event.method)) {
                    state.paymentMethods - event.method
                } else {
                    state.paymentMethods + event.method
                }
                state.copy(paymentMethods = methods)
            }

            is CardDetailEvent.FinalCodeChanged -> updateForm {
                it.copy(finalCode = event.finalCode.filter { c -> c.isDigit() }.take(4))
            }

            is CardDetailEvent.ClosingDayChanged -> updateForm {
                it.copy(closingDay = event.day.filter { c -> c.isDigit() }.take(2))
            }

            is CardDetailEvent.InvoiceDueDateChanged -> updateForm {
                it.copy(invoiceDueDate = event.day.filter { c -> c.isDigit() }.take(2))
            }

            is CardDetailEvent.BalanceToAddChanged -> _uiState.update {
                it.copy(
                    balanceToAdd = event.amount.filter { c -> c.isDigit() }
                        .trimStart('0').take(12)
                )
            }

            CardDetailEvent.SaveClicked -> save()

            CardDetailEvent.AddBalanceClicked -> addBalance()
        }
    }

    private fun loadCard() {
        viewModelScope.launch {
            getCardsUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }

                    is Resource.Success -> {
                        val card = result.data.orEmpty().find { it.id == cardId }
                        if (card == null) {
                            _uiState.update { it.copy(isLoading = false) }
                            _effect.emit(CardDetailEffect.ShowToast("Cartão não encontrado"))
                        } else {
                            updateForm {
                                it.copy(
                                    isLoading = false,
                                    bank = card.bank,
                                    paymentMethods = card.paymentMethods.toSet(),
                                    finalCode = card.finalCode,
                                    closingDay = if (card.closingDay > 0) {
                                        card.closingDay.toString()
                                    } else {
                                        ""
                                    },
                                    invoiceDueDate = if (card.invoiceDueDate > 0) {
                                        card.invoiceDueDate.toString()
                                    } else {
                                        ""
                                    },
                                )
                            }
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _effect.emit(
                            CardDetailEffect.ShowToast(
                                result.message ?: "Erro ao carregar cartão"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun updateForm(transform: (CardDetailUiState) -> CardDetailUiState) {
        _uiState.update { old ->
            val new = transform(old)
            val closingDayValid = new.closingDay.toIntOrNull() in 1..31
            val dueDateValid = new.invoiceDueDate.toIntOrNull() in 1..31
            val creditFieldsValid = !new.hasCredit ||
                    (new.finalCode.length == 4 && closingDayValid && dueDateValid)
            new.copy(
                isSubmitEnabled = new.bank.isNotBlank() &&
                        new.paymentMethods.isNotEmpty() &&
                        creditFieldsValid
            )
        }
    }

    private fun save() {
        val state = _uiState.value
        if (!state.isSubmitEnabled || state.isSaving) return

        viewModelScope.launch {
            updateCardUseCase(
                cardId = cardId,
                bank = state.bank.trim(),
                paymentMethods = state.paymentMethods.toList().sorted(),
                finalCode = if (state.hasCredit) state.finalCode else "",
                closingDay = if (state.hasCredit) state.closingDay.toIntOrNull() ?: 0 else 0,
                invoiceDueDate = if (state.hasCredit) {
                    state.invoiceDueDate.toIntOrNull() ?: 0
                } else {
                    0
                },
            ).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isSaving = true) }

                    is Resource.Success -> {
                        _uiState.update { it.copy(isSaving = false) }
                        _effect.emit(CardDetailEffect.ShowToast("Cartão atualizado com sucesso!"))
                        _effect.emit(CardDetailEffect.CardSaved)
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isSaving = false) }
                        _effect.emit(
                            CardDetailEffect.ShowToast(
                                result.message ?: "Erro ao atualizar cartão"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun addBalance() {
        val state = _uiState.value
        val amount = CurrencyVisualTransformation.centsToDouble(state.balanceToAdd)
        if (amount <= 0.0 || state.isAddingBalance) return

        viewModelScope.launch {
            newTransactionUseCase(
                title = "Ajuste de saldo — ${state.bank}",
                description = "Saldo adicionado pela tela de configuração do cartão",
                transactionType = "INCOME",
                paymentMethod = "",
                amount = amount,
                isRecurring = false,
                category = emptyList(),
                status = "pending",
                cardId = cardId,
            ).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isAddingBalance = true) }

                    is Resource.Success -> {
                        _uiState.update { it.copy(isAddingBalance = false, balanceToAdd = "") }
                        _effect.emit(
                            CardDetailEffect.ShowToast("Saldo adicionado como receita!")
                        )
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isAddingBalance = false) }
                        _effect.emit(
                            CardDetailEffect.ShowToast(
                                result.message ?: "Erro ao adicionar saldo"
                            )
                        )
                    }
                }
            }
        }
    }
}
