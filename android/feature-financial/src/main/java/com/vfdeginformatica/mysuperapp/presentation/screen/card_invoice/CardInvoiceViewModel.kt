package com.vfdeginformatica.mysuperapp.presentation.screen.card_invoice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.RecurrenceEditScope
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.card.GetCardsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.DeleteRecurringOccurrenceUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.DeleteTransactionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.GetInvoiceProjectionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.UpdateRecurringOccurrenceUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.UpdateTransactionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction_category.GetTransactionsCategoriesUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.card_invoice.contract.CardInvoiceEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.card_invoice.contract.CardInvoiceEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.card_invoice.contract.CardInvoiceUiState
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.PendingRecurringAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class CardInvoiceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getInvoiceProjectionUseCase: GetInvoiceProjectionUseCase,
    private val getCardsUseCase: GetCardsUseCase,
    private val getTransactionsCategoriesUseCase: GetTransactionsCategoriesUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val updateRecurringOccurrenceUseCase: UpdateRecurringOccurrenceUseCase,
    private val deleteRecurringOccurrenceUseCase: DeleteRecurringOccurrenceUseCase
) : ViewModel() {

    companion object {
        const val ARG_CARD_ID = "cardId"
        const val ARG_INVOICE_MONTH = "invoiceMonth"
    }

    private val cardId: String = savedStateHandle.get<String>(ARG_CARD_ID).orEmpty()
    private val invoiceMonth: YearMonth = savedStateHandle.get<String>(ARG_INVOICE_MONTH)
        ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
        ?: YearMonth.now()

    private val _uiState = MutableStateFlow(
        CardInvoiceUiState(cardId = cardId, invoiceMonth = invoiceMonth)
    )
    val uiState: StateFlow<CardInvoiceUiState> = _uiState

    private val _effect = MutableSharedFlow<CardInvoiceEffect>()
    val effect: SharedFlow<CardInvoiceEffect> = _effect

    private var loadInvoiceJob: Job? = null

    init {
        loadSources()
        loadInvoice()
    }

    fun onEvent(event: CardInvoiceEvent) {
        when (event) {
            CardInvoiceEvent.OnRefresh -> loadInvoice()

            is CardInvoiceEvent.OnTransactionClicked -> {
                _uiState.update { it.copy(editingTransaction = event.transaction) }
            }

            CardInvoiceEvent.OnEditDismiss -> {
                _uiState.update { it.copy(editingTransaction = null, pendingRecurringAction = null) }
            }

            is CardInvoiceEvent.OnEditConfirm -> {
                if (_uiState.value.editingTransaction?.isRecurring == true) {
                    _uiState.update {
                        it.copy(
                            pendingRecurringAction =
                                PendingRecurringAction.Update(event.title, event.amount)
                        )
                    }
                } else {
                    updateTransaction(event.title, event.amount)
                }
            }

            CardInvoiceEvent.OnDeleteTransaction -> {
                if (_uiState.value.editingTransaction?.isRecurring == true) {
                    _uiState.update { it.copy(pendingRecurringAction = PendingRecurringAction.Delete) }
                } else {
                    deleteTransaction()
                }
            }

            is CardInvoiceEvent.OnRecurringScopeConfirm -> applyRecurringAction(event.scope)

            CardInvoiceEvent.OnRecurringScopeDismiss -> {
                _uiState.update { it.copy(pendingRecurringAction = null) }
            }
        }
    }

    private fun loadInvoice() {
        loadInvoiceJob?.cancel()
        loadInvoiceJob = viewModelScope.launch {
            getInvoiceProjectionUseCase(invoiceMonth).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    }

                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                invoiceTransactions = result.data?.invoiceTransactions.orEmpty(),
                                recurringTransactions = result.data?.recurringTransactions.orEmpty()
                            )
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.message ?: "Erro ao carregar fatura"
                            )
                        }
                    }
                }
            }
        }
    }

    private fun loadSources() {
        viewModelScope.launch {
            getCardsUseCase().collect { result ->
                if (result is Resource.Success) {
                    _uiState.update { state ->
                        state.copy(card = result.data.orEmpty().find { it.id == cardId })
                    }
                }
            }
        }
        viewModelScope.launch {
            getTransactionsCategoriesUseCase().collect { result ->
                if (result is Resource.Success) {
                    _uiState.update { it.copy(availableCategories = result.data.orEmpty()) }
                }
            }
        }
    }

    private fun updateTransaction(title: String, amount: Double) {
        val transaction = _uiState.value.editingTransaction ?: return
        viewModelScope.launch {
            updateTransactionUseCase(transaction.id, title, amount).collect { result ->
                handleWriteResult(result, "Transação atualizada!", "Erro ao atualizar transação")
            }
        }
    }

    private fun deleteTransaction() {
        val transaction = _uiState.value.editingTransaction ?: return
        viewModelScope.launch {
            deleteTransactionUseCase(transaction.id).collect { result ->
                handleWriteResult(result, "Transação excluída!", "Erro ao excluir transação")
            }
        }
    }

    private fun applyRecurringAction(scope: RecurrenceEditScope) {
        val state = _uiState.value
        val transaction = state.editingTransaction ?: return
        val action = state.pendingRecurringAction ?: return
        val month = transaction.occurrenceMonth ?: invoiceMonth

        val (flow, successMessage) = when (action) {
            is PendingRecurringAction.Update ->
                updateRecurringOccurrenceUseCase(
                    transaction = transaction,
                    month = month,
                    title = action.title,
                    amount = action.amount,
                    scope = scope
                ) to "Recorrência atualizada!"

            PendingRecurringAction.Delete ->
                deleteRecurringOccurrenceUseCase(
                    transaction = transaction,
                    month = month,
                    scope = scope
                ) to "Recorrência excluída!"
        }

        viewModelScope.launch {
            flow.collect { result ->
                handleWriteResult(result, successMessage, "Erro ao atualizar recorrência")
            }
        }
    }

    private suspend fun handleWriteResult(
        result: Resource<Unit>,
        successMessage: String,
        errorFallback: String
    ) {
        when (result) {
            is Resource.Loading -> _uiState.update { it.copy(isSavingEdit = true) }

            is Resource.Success -> {
                _uiState.update {
                    it.copy(
                        isSavingEdit = false,
                        editingTransaction = null,
                        pendingRecurringAction = null
                    )
                }
                _effect.emit(CardInvoiceEffect.ShowToast(successMessage))
                loadInvoice()
            }

            is Resource.Error -> {
                _uiState.update { it.copy(isSavingEdit = false, pendingRecurringAction = null) }
                _effect.emit(CardInvoiceEffect.ShowToast(result.message ?: errorFallback))
            }
        }
    }
}
