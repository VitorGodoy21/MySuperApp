package com.vfdeginformatica.mysuperapp.presentation.screen.financial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.card.GetCardsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.DeleteTransactionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.GetInvoiceProjectionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.GetTransactionsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.UpdateTransactionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction_category.GetTransactionsCategoriesUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialTab
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialUiState
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
    private val deleteTransactionUseCase: DeleteTransactionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinancialUiState())
    val uiState: StateFlow<FinancialUiState> = _uiState

    private val _effect = MutableSharedFlow<FinancialEffect>()
    val effect: SharedFlow<FinancialEffect> = _effect

    private var loadTransactionsJob: Job? = null
    private var loadProjectionJob: Job? = null

    init {
        loadFilterSources()
        loadTransactions()
    }

    fun onEvent(event: FinancialEvent) {
        when (event) {
            FinancialEvent.OnPreviousMonth -> {
                _uiState.update { it.copy(selectedMonth = it.selectedMonth.minusMonths(1)) }
                loadTransactions()
            }

            FinancialEvent.OnNextMonth -> {
                _uiState.update { it.copy(selectedMonth = it.selectedMonth.plusMonths(1)) }
                loadTransactions()
            }

            FinancialEvent.OnRefresh -> {
                loadTransactions()
                if (_uiState.value.selectedTab == FinancialTab.PROJECTION) loadProjection()
            }

            is FinancialEvent.OnTabChanged -> {
                _uiState.update { it.copy(selectedTab = event.tab) }
                if (event.tab == FinancialTab.PROJECTION) loadProjection()
            }

            FinancialEvent.OnProjectionPreviousMonth -> {
                _uiState.update { it.copy(projectionMonth = it.projectionMonth.minusMonths(1)) }
                loadProjection()
            }

            FinancialEvent.OnProjectionNextMonth -> {
                _uiState.update { it.copy(projectionMonth = it.projectionMonth.plusMonths(1)) }
                loadProjection()
            }

            is FinancialEvent.OnTransactionClicked -> {
                _uiState.update { it.copy(editingTransaction = event.transaction) }
            }

            FinancialEvent.OnEditDismiss -> {
                _uiState.update { it.copy(editingTransaction = null) }
            }

            is FinancialEvent.OnEditConfirm -> updateTransaction(event.title, event.amount)

            FinancialEvent.OnDeleteTransaction -> deleteTransaction()

            is FinancialEvent.OnTypeFilterChanged -> {
                _uiState.update { it.copy(typeFilter = event.filter) }
            }

            is FinancialEvent.OnCategoryFilterChanged -> {
                _uiState.update { it.copy(categoryFilterId = event.categoryId) }
            }

            is FinancialEvent.OnCardFilterChanged -> {
                _uiState.update { it.copy(cardFilterId = event.cardId) }
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
            getInvoiceProjectionUseCase(_uiState.value.projectionMonth).collect { result ->
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

    private fun updateTransaction(title: String, amount: Double) {
        val transaction = _uiState.value.editingTransaction ?: return
        viewModelScope.launch {
            updateTransactionUseCase(transaction.id, title, amount).collect { result ->
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

    private fun reloadAll() {
        loadTransactions()
        if (_uiState.value.selectedTab == FinancialTab.PROJECTION) loadProjection()
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