package com.vfdeginformatica.mysuperapp.presentation.screen.financial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.card.GetCardsUseCase
import com.vfdeginformatica.mysuperapp.domain.model.RecurrenceEditScope
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.DeleteRecurringOccurrenceUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.DeleteTransactionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.GetInvoiceProjectionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.GetTransactionsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.UpdateRecurringOccurrenceUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.UpdateTransactionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction_category.GetTransactionsCategoriesUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FilterGroupBy
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialUiState
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.PaymentMethodFilter
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.PendingRecurringAction
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.TransactionTypeFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FinancialViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getCardsUseCase: GetCardsUseCase,
    private val getTransactionsCategoriesUseCase: GetTransactionsCategoriesUseCase,
    private val getInvoiceProjectionUseCase: GetInvoiceProjectionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val updateRecurringOccurrenceUseCase: UpdateRecurringOccurrenceUseCase,
    private val deleteRecurringOccurrenceUseCase: DeleteRecurringOccurrenceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinancialUiState())
    val uiState: StateFlow<FinancialUiState> = _uiState

    private val _effect = MutableSharedFlow<FinancialEffect>()
    val effect: SharedFlow<FinancialEffect> = _effect

    private var loadTransactionsJob: Job? = null
    private var loadProjectionJob: Job? = null
    private var loadPaidInvoiceJob: Job? = null

    init {
        loadFilterSources()
        reloadAll()
    }

    fun onEvent(event: FinancialEvent) {
        when (event) {
            FinancialEvent.OnPreviousMonth -> {
                _uiState.update { it.copy(selectedMonth = it.selectedMonth.minusMonths(1)) }
                reloadAll()
            }

            FinancialEvent.OnNextMonth -> {
                _uiState.update { it.copy(selectedMonth = it.selectedMonth.plusMonths(1)) }
                reloadAll()
            }

            FinancialEvent.OnRefresh -> reloadAll()

            is FinancialEvent.OnTabChanged -> {
                _uiState.update { it.copy(selectedTab = event.tab) }
            }

            FinancialEvent.OnToggleMonthList -> {
                _uiState.update { it.copy(isMonthListExpanded = !it.isMonthListExpanded) }
            }

            is FinancialEvent.OnCardInvoiceClicked -> {
                viewModelScope.launch {
                    _effect.emit(
                        FinancialEffect.NavigateToCardInvoice(
                            cardId = event.cardId,
                            invoiceMonth = _uiState.value.selectedMonthKey
                        )
                    )
                }
            }

            is FinancialEvent.OnTransactionClicked -> {
                _uiState.update { it.copy(editingTransaction = event.transaction) }
            }

            FinancialEvent.OnEditDismiss -> {
                _uiState.update { it.copy(editingTransaction = null, pendingRecurringAction = null) }
            }

            is FinancialEvent.OnEditConfirm -> {
                // Recorrente: pede o escopo antes de aplicar; demais: atualiza direto
                if (_uiState.value.editingTransaction?.isRecurring == true) {
                    _uiState.update {
                        it.copy(
                            pendingRecurringAction =
                                PendingRecurringAction.Update(
                                    event.title, event.amount, event.categoryIds
                                )
                        )
                    }
                } else {
                    updateTransaction(event.title, event.amount, event.categoryIds)
                }
            }

            FinancialEvent.OnDeleteTransaction -> {
                if (_uiState.value.editingTransaction?.isRecurring == true) {
                    _uiState.update { it.copy(pendingRecurringAction = PendingRecurringAction.Delete) }
                } else {
                    deleteTransaction()
                }
            }

            is FinancialEvent.OnRecurringScopeConfirm -> applyRecurringAction(event.scope)

            FinancialEvent.OnRecurringScopeDismiss -> {
                _uiState.update { it.copy(pendingRecurringAction = null) }
            }

            is FinancialEvent.OnTypeFilterChanged -> {
                _uiState.update { state ->
                    val next = state.copy(typeFilter = event.filter)
                    // Mantém apenas categorias compatíveis com o novo tipo
                    val validIds = next.categoriesForFilter.mapNotNull { it.id }.toSet()
                    next.copy(categoryFilterIds = next.categoryFilterIds.intersect(validIds))
                }
            }

            is FinancialEvent.OnPaymentMethodFilterChanged -> {
                _uiState.update { it.copy(paymentMethodFilter = event.filter) }
            }

            is FinancialEvent.OnCategoryFilterToggled -> {
                _uiState.update {
                    val ids = if (event.categoryId in it.categoryFilterIds) {
                        it.categoryFilterIds - event.categoryId
                    } else {
                        it.categoryFilterIds + event.categoryId
                    }
                    it.copy(categoryFilterIds = ids)
                }
            }

            FinancialEvent.OnCategoryFilterCleared -> {
                _uiState.update { it.copy(categoryFilterIds = emptySet()) }
            }

            is FinancialEvent.OnCardFilterChanged -> {
                _uiState.update { it.copy(cardFilterId = event.cardId) }
            }

            FinancialEvent.OnRecurringOnlyToggled -> {
                _uiState.update { it.copy(recurringOnly = !it.recurringOnly) }
            }

            FinancialEvent.OnInstallmentsOnlyToggled -> {
                _uiState.update { it.copy(installmentsOnly = !it.installmentsOnly) }
            }

            is FinancialEvent.OnGroupByChanged -> {
                _uiState.update { it.copy(groupBy = event.groupBy) }
            }

            FinancialEvent.OnClearFilters -> {
                _uiState.update {
                    it.copy(
                        typeFilter = TransactionTypeFilter.ALL,
                        paymentMethodFilter = PaymentMethodFilter.ALL,
                        categoryFilterIds = emptySet(),
                        cardFilterId = null,
                        recurringOnly = false,
                        installmentsOnly = false,
                        groupBy = FilterGroupBy.NONE
                    )
                }
            }
        }
    }

    private fun loadTransactions() {
        loadTransactionsJob?.cancel()
        loadTransactionsJob = viewModelScope.launch {
            getTransactionsUseCase(_uiState.value.selectedMonth).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    }

                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                transactions = result.data.orEmpty(),
                                errorMessage = null
                            )
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.message
                            )
                        }
                        _effect.emit(
                            FinancialEffect.ShowToast(
                                result.message ?: "Erro ao carregar transações"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun loadProjection() {
        loadProjectionJob?.cancel()
        loadProjectionJob = viewModelScope.launch {
            getInvoiceProjectionUseCase(_uiState.value.selectedMonth).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoadingProjection = true) }
                    }

                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoadingProjection = false,
                                invoiceTransactions = result.data?.invoiceTransactions.orEmpty(),
                                recurringTransactions = result.data?.recurringTransactions.orEmpty()
                            )
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoadingProjection = false) }
                        _effect.emit(
                            FinancialEffect.ShowToast(
                                result.message ?: "Erro ao carregar projeção"
                            )
                        )
                    }
                }
            }
        }
    }

    /** Fatura fechada no mês anterior: é o que sai da conta no mês selecionado. */
    private fun loadPaidInvoice() {
        loadPaidInvoiceJob?.cancel()
        loadPaidInvoiceJob = viewModelScope.launch {
            val paidMonth = _uiState.value.selectedMonth.minusMonths(1)
            getInvoiceProjectionUseCase(paidMonth).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoadingPaidInvoice = true) }
                    }

                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoadingPaidInvoice = false,
                                paidInvoiceTransactions = result.data?.invoiceTransactions.orEmpty(),
                                paidInvoiceRecurringTransactions = result.data?.recurringTransactions.orEmpty()
                            )
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoadingPaidInvoice = false) }
                        _effect.emit(
                            FinancialEffect.ShowToast(
                                result.message ?: "Erro ao carregar fatura anterior"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun updateTransaction(title: String, amount: Double, categoryIds: List<String>) {
        val transaction = _uiState.value.editingTransaction ?: return
        viewModelScope.launch {
            updateTransactionUseCase(transaction, title, amount, categoryIds).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isSavingEdit = true) }

                    is Resource.Success -> {
                        _uiState.update { it.copy(isSavingEdit = false, editingTransaction = null) }
                        _effect.emit(FinancialEffect.ShowToast("Transação atualizada!"))
                        reloadAll()
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isSavingEdit = false) }
                        _effect.emit(
                            FinancialEffect.ShowToast(
                                result.message ?: "Erro ao atualizar transação"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun deleteTransaction() {
        val transaction = _uiState.value.editingTransaction ?: return
        viewModelScope.launch {
            deleteTransactionUseCase(transaction.id).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isSavingEdit = true) }

                    is Resource.Success -> {
                        _uiState.update { it.copy(isSavingEdit = false, editingTransaction = null) }
                        _effect.emit(FinancialEffect.ShowToast("Transação excluída!"))
                        reloadAll()
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isSavingEdit = false) }
                        _effect.emit(
                            FinancialEffect.ShowToast(
                                result.message ?: "Erro ao excluir transação"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun applyRecurringAction(scope: RecurrenceEditScope) {
        val state = _uiState.value
        val transaction = state.editingTransaction ?: return
        val action = state.pendingRecurringAction ?: return
        // Mês da ocorrência clicada; fallback para o mês selecionado
        val month = transaction.occurrenceMonth ?: state.selectedMonth

        val (flow, successMessage) = when (action) {
            is PendingRecurringAction.Update ->
                updateRecurringOccurrenceUseCase(
                    transaction = transaction,
                    month = month,
                    title = action.title,
                    amount = action.amount,
                    categoryIds = action.categoryIds,
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
                        _effect.emit(FinancialEffect.ShowToast(successMessage))
                        reloadAll()
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isSavingEdit = false, pendingRecurringAction = null) }
                        _effect.emit(
                            FinancialEffect.ShowToast(
                                result.message ?: "Erro ao atualizar recorrência"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun reloadAll() {
        loadTransactions()
        loadProjection()
        loadPaidInvoice()
    }

    private fun loadFilterSources() {
        viewModelScope.launch {
            getCardsUseCase().collect { result ->
                if (result is Resource.Success) {
                    _uiState.update { it.copy(availableCards = result.data.orEmpty()) }
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
}