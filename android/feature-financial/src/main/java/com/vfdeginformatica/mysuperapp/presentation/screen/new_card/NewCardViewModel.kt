package com.vfdeginformatica.mysuperapp.presentation.screen.new_card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.card.AddCardUseCase
import com.vfdeginformatica.mysuperapp.presentation.common.CurrencyVisualTransformation
import com.vfdeginformatica.mysuperapp.presentation.screen.new_card.contract.NewCardEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.new_card.contract.NewCardEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.new_card.contract.NewCardUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewCardViewModel @Inject constructor(
    private val addCardUseCase: AddCardUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewCardUiState())
    val uiState: StateFlow<NewCardUiState> = _uiState

    private val _effect = MutableSharedFlow<NewCardEffect>()
    val effect: SharedFlow<NewCardEffect> = _effect

    fun onEvent(event: NewCardEvent) {
        when (event) {
            is NewCardEvent.BankChanged -> updateForm { it.copy(bank = event.bank) }

            is NewCardEvent.MethodToggled -> updateForm { state ->
                val methods = if (state.paymentMethods.contains(event.method)) {
                    state.paymentMethods - event.method
                } else {
                    state.paymentMethods + event.method
                }
                state.copy(paymentMethods = methods)
            }

            is NewCardEvent.FinalCodeChanged -> updateForm {
                it.copy(finalCode = event.finalCode.filter { c -> c.isDigit() }.take(4))
            }

            is NewCardEvent.ClosingDayChanged -> updateForm {
                it.copy(closingDay = event.day.filter { c -> c.isDigit() }.take(2))
            }

            is NewCardEvent.InvoiceDueDateChanged -> updateForm {
                it.copy(invoiceDueDate = event.day.filter { c -> c.isDigit() }.take(2))
            }

            is NewCardEvent.InitialBalanceChanged -> updateForm {
                it.copy(
                    initialBalance = event.amount.filter { c -> c.isDigit() }
                        .trimStart('0').take(12)
                )
            }

            is NewCardEvent.CurrentInvoiceChanged -> updateForm {
                it.copy(
                    currentInvoice = event.amount.filter { c -> c.isDigit() }
                        .trimStart('0').take(12)
                )
            }

            NewCardEvent.SaveClicked -> submit()
        }
    }

    private fun updateForm(transform: (NewCardUiState) -> NewCardUiState) {
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

    private fun submit() {
        val state = _uiState.value
        if (!state.isSubmitEnabled || state.isLoading) return

        viewModelScope.launch {
            addCardUseCase(
                bank = state.bank.trim(),
                paymentMethods = state.paymentMethods.toList().sorted(),
                finalCode = if (state.hasCredit) state.finalCode else "",
                closingDay = if (state.hasCredit) state.closingDay.toIntOrNull() ?: 0 else 0,
                invoiceDueDate = if (state.hasCredit) state.invoiceDueDate.toIntOrNull() ?: 0 else 0,
                initialBalance = CurrencyVisualTransformation.centsToDouble(state.initialBalance),
                currentInvoice = if (state.hasCredit) {
                    CurrencyVisualTransformation.centsToDouble(state.currentInvoice)
                } else {
                    0.0
                }
            ).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }

                    is Resource.Success -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _effect.emit(NewCardEffect.ShowToast("Cartão cadastrado com sucesso!"))
                        _effect.emit(NewCardEffect.CardSaved)
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _effect.emit(
                            NewCardEffect.ShowToast(
                                result.message ?: "Erro ao cadastrar cartão"
                            )
                        )
                    }
                }
            }
        }
    }
}
