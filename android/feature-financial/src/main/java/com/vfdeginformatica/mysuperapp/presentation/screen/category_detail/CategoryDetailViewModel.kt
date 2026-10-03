package com.vfdeginformatica.mysuperapp.presentation.screen.category_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.mapper.toModel
import com.vfdeginformatica.mysuperapp.domain.model.TransactionCategory
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction_category.AddTransactionCategoryUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction_category.DeleteTransactionCategoryUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction_category.GetTransactionsCategoriesUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction_category.UpdateTransactionCategoryUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.category_detail.contract.CategoryDetailEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.category_detail.contract.CategoryDetailEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.category_detail.contract.CategoryDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTransactionsCategoriesUseCase: GetTransactionsCategoriesUseCase,
    private val addTransactionCategoryUseCase: AddTransactionCategoryUseCase,
    private val updateTransactionCategoryUseCase: UpdateTransactionCategoryUseCase,
    private val deleteTransactionCategoryUseCase: DeleteTransactionCategoryUseCase,
) : ViewModel() {

    companion object {
        const val CATEGORY_ID_ARG = "categoryId"
        const val MAX_TITLE_LENGTH = 40
        const val MAX_DESCRIPTION_LENGTH = 120
    }

    private val categoryId: String? =
        savedStateHandle.get<String>(CATEGORY_ID_ARG)?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(CategoryDetailUiState(categoryId = categoryId))
    val uiState: StateFlow<CategoryDetailUiState> = _uiState

    private val _effect = MutableSharedFlow<CategoryDetailEffect>()
    val effect: SharedFlow<CategoryDetailEffect> = _effect

    init {
        if (categoryId != null) loadCategory(categoryId)
    }

    fun onEvent(event: CategoryDetailEvent) {
        when (event) {
            is CategoryDetailEvent.TitleChanged -> updateForm {
                it.copy(title = event.title.take(MAX_TITLE_LENGTH))
            }

            is CategoryDetailEvent.DescriptionChanged -> updateForm {
                it.copy(description = event.description.take(MAX_DESCRIPTION_LENGTH))
            }

            is CategoryDetailEvent.TypeChanged -> updateForm { it.copy(transactionType = event.type) }

            CategoryDetailEvent.SaveClicked -> save()

            CategoryDetailEvent.DeleteClicked -> _uiState.update {
                it.copy(showDeleteConfirmation = true)
            }

            CategoryDetailEvent.DeleteDismissed -> _uiState.update {
                it.copy(showDeleteConfirmation = false)
            }

            CategoryDetailEvent.DeleteConfirmed -> {
                _uiState.update { it.copy(showDeleteConfirmation = false) }
                delete()
            }
        }
    }

    private fun loadCategory(id: String) {
        viewModelScope.launch {
            getTransactionsCategoriesUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }

                    is Resource.Success -> {
                        val category = result.data.orEmpty().find { it.id == id }?.toModel()
                        if (category == null) {
                            _uiState.update { it.copy(isLoading = false) }
                            _effect.emit(CategoryDetailEffect.ShowToast("Categoria não encontrada"))
                        } else {
                            updateForm {
                                it.copy(
                                    isLoading = false,
                                    title = category.title,
                                    description = category.description,
                                    transactionType = category.type,
                                )
                            }
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _effect.emit(
                            CategoryDetailEffect.ShowToast(
                                result.message ?: "Erro ao carregar categoria"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun updateForm(transform: (CategoryDetailUiState) -> CategoryDetailUiState) {
        _uiState.update { old ->
            val new = transform(old)
            new.copy(isSubmitEnabled = new.title.isNotBlank())
        }
    }

    private fun save() {
        val state = _uiState.value
        if (!state.isSubmitEnabled || state.isBusy) return

        val category = TransactionCategory(
            id = state.categoryId.orEmpty(),
            title = state.title.trim(),
            description = state.description.trim(),
            type = state.transactionType,
        )

        viewModelScope.launch {
            val flow = if (state.isEditing) {
                updateTransactionCategoryUseCase(category)
            } else {
                addTransactionCategoryUseCase(category)
            }
            flow.collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isSaving = true) }

                    is Resource.Success -> {
                        _uiState.update { it.copy(isSaving = false) }
                        _effect.emit(
                            CategoryDetailEffect.ShowToast(
                                if (state.isEditing) "Categoria atualizada!" else "Categoria criada!"
                            )
                        )
                        _effect.emit(CategoryDetailEffect.NavigateUp)
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isSaving = false) }
                        _effect.emit(
                            CategoryDetailEffect.ShowToast(
                                result.message ?: "Erro ao salvar categoria"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun delete() {
        val id = _uiState.value.categoryId ?: return
        if (_uiState.value.isBusy) return

        viewModelScope.launch {
            deleteTransactionCategoryUseCase(id).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isDeleting = true) }

                    is Resource.Success -> {
                        _uiState.update { it.copy(isDeleting = false) }
                        _effect.emit(CategoryDetailEffect.ShowToast("Categoria excluída!"))
                        _effect.emit(CategoryDetailEffect.NavigateUp)
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isDeleting = false) }
                        _effect.emit(
                            CategoryDetailEffect.ShowToast(
                                result.message ?: "Erro ao excluir categoria"
                            )
                        )
                    }
                }
            }
        }
    }
}
