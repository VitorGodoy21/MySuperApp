package com.vfdeginformatica.mysuperapp.presentation.screen.category_expenses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.mapper.toModel
import com.vfdeginformatica.mysuperapp.domain.model.Transaction
import com.vfdeginformatica.mysuperapp.domain.model.TransactionCategory
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.GetTransactionsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction_category.GetTransactionsCategoriesUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.category_expenses.contract.CategoryExpenseReport
import com.vfdeginformatica.mysuperapp.presentation.screen.category_expenses.contract.CategoryExpensesEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.category_expenses.contract.CategoryExpensesUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class CategoryExpensesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getTransactionsCategoriesUseCase: GetTransactionsCategoriesUseCase,
) : ViewModel() {

    companion object {
        const val ARG_MONTH = "month"
    }

    private val month = savedStateHandle.get<String>(ARG_MONTH)
        ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
        ?: YearMonth.now()

    private val _uiState = MutableStateFlow(CategoryExpensesUiState(month = month))
    val uiState: StateFlow<CategoryExpensesUiState> = _uiState

    private val _effect = MutableSharedFlow<CategoryExpensesEffect>()
    val effect: SharedFlow<CategoryExpensesEffect> = _effect

    private var transactions: List<Transaction> = emptyList()
    private var categories: List<TransactionCategory> = emptyList()

    init {
        loadTransactions()
        loadCategories()
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            getTransactionsUseCase(month).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update {
                        it.copy(isLoadingTransactions = true)
                    }

                    is Resource.Success -> {
                        transactions = result.data.orEmpty()
                        _uiState.update {
                            it.copy(
                                isLoadingTransactions = false,
                                report = CategoryExpenseReport.from(transactions, categories)
                            )
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoadingTransactions = false) }
                        _effect.emit(
                            CategoryExpensesEffect.ShowToast(
                                result.message ?: "Erro ao carregar despesas do mês"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            getTransactionsCategoriesUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update {
                        it.copy(isLoadingCategories = true)
                    }

                    is Resource.Success -> {
                        categories = result.data.orEmpty().map { it.toModel() }
                        _uiState.update {
                            it.copy(
                                isLoadingCategories = false,
                                report = CategoryExpenseReport.from(transactions, categories)
                            )
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoadingCategories = false) }
                        _effect.emit(
                            CategoryExpensesEffect.ShowToast(
                                result.message ?: "Erro ao carregar categorias"
                            )
                        )
                    }
                }
            }
        }
    }
}
