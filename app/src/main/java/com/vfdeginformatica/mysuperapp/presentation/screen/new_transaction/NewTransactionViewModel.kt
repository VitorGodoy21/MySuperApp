package com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.CardDto
import com.vfdeginformatica.mysuperapp.data.remote.dto.CategoryTransactionDto
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.card.GetCardsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction_category.GetTransactionsCategoriesUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.NewTransactionEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.NewTransactionEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.NewTransactionUiState
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class NewTransactionViewModel @Inject constructor(
    private val getCardsUseCase: GetCardsUseCase,
    private val getTransactionsCategoriesUseCase: GetTransactionsCategoriesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewTransactionUiState())
    val uiState: StateFlow<NewTransactionUiState> = _uiState

    private val _effect = MutableSharedFlow<NewTransactionEffect>()
    val effect: SharedFlow<NewTransactionEffect> = _effect

    init {
        loadCards()
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            getTransactionsCategoriesUseCase().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        updateForm(
                            availableCategories = result.data,
                            isLoadingCategories = false
                        )
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoadingCategories = false,
                                errorMessage = result.message
                            )
                        }
                        _effect.emit(
                            NewTransactionEffect.ShowToast(
                                result.message ?: "Erro ao cadastrar"
                            )
                        )
                    }

                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoadingCategories = true) }
                    }
                }
            }
        }
    }

    private fun loadCards() {
        viewModelScope.launch {
            getCardsUseCase().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        updateForm(
                            availableCards = result.data,
                            isLoadingCards = false
                        )
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoadingCards = false,
                                errorMessage = result.message
                            )
                        }
                        _effect.emit(
                            NewTransactionEffect.ShowToast(
                                result.message ?: "Erro ao cadastrar"
                            )
                        )
                    }

                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoadingCards = true) }
                    }

                }
            }
        }
    }

    fun onEvent(event: NewTransactionEvent) {
        when (event) {
            is NewTransactionEvent.AmountChanged -> updateForm(amount = event.amount)
            is NewTransactionEvent.CardIdChanged -> updateForm(cardId = event.cardId)
            is NewTransactionEvent.CategoryToggled -> {
                val currentCategories = _uiState.value.selectedCategories
                val categoryToToggle =
                    _uiState.value.availableCategories?.find { it.id == event.category }

                categoryToToggle?.let { category ->
                    val newCategories = if (currentCategories.any { it.id == category.id }) {
                        currentCategories.filter { it.id != category.id }
                    } else {
                        currentCategories + category
                    }
                    updateForm(selectedCategories = newCategories)
                }
            }

            is NewTransactionEvent.DateChanged -> updateForm(date = event.date)
            is NewTransactionEvent.DescriptionChanged -> updateForm(description = event.description)
            is NewTransactionEvent.InvoiceMonthChanged -> updateForm(invoiceMonth = event.month)
            is NewTransactionEvent.PaymentMethodChanged -> updateForm(paymentMethod = event.method)
            is NewTransactionEvent.RecurringChanged -> updateForm(recurring = event.isRecurring)
            NewTransactionEvent.SaveClicked -> submit()
            is NewTransactionEvent.TitleChanged -> updateForm(title = event.title)
            is NewTransactionEvent.TypeChanged -> updateForm(type = event.type)
        }
    }

    private fun updateForm(
        amount: String? = null,
        cardId: String? = null,
        selectedCategories: List<CategoryTransactionDto>? = null,
        date: LocalDate? = null,
        description: String? = null,
        invoiceMonth: String? = null,
        paymentMethod: String? = null,
        recurring: Boolean? = null,
        title: String? = null,
        type: TransactionType? = null,
        availableCards: List<CardDto>? = null,
        availableCategories: List<CategoryTransactionDto>? = null,
        isLoadingCards: Boolean? = null,
        isLoadingCategories: Boolean? = null
    ) {
        _uiState.update { old ->
            val new = old.copy(
                amount = amount ?: old.amount,
                cardId = cardId ?: old.cardId,
                selectedCategories = selectedCategories ?: old.selectedCategories,
                date = date ?: old.date,
                description = description ?: old.description,
                invoiceMonth = invoiceMonth ?: old.invoiceMonth,
                paymentMethod = paymentMethod ?: old.paymentMethod,
                isRecurring = recurring ?: old.isRecurring,
                title = title ?: old.title,
                transactionType = type ?: old.transactionType,
                availableCards = availableCards ?: old.availableCards,
                isLoadingCards = isLoadingCards ?: old.isLoadingCards,
                availableCategories = availableCategories ?: old.availableCategories,
                isLoadingCategories = isLoadingCategories ?: old.isLoadingCategories
            )

            new.copy()
        }
    }

    private fun submit() {

    }

}
