package com.vfdeginformatica.mysuperapp.presentation.screen.category_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.mapper.toModel
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction_category.GetTransactionsCategoriesUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.category_list.contract.CategoryListEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.category_list.contract.CategoryListEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.category_list.contract.CategoryListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryListViewModel @Inject constructor(
    private val getTransactionsCategoriesUseCase: GetTransactionsCategoriesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryListUiState())
    val uiState: StateFlow<CategoryListUiState> = _uiState

    private val _effect = MutableSharedFlow<CategoryListEffect>()
    val effect: SharedFlow<CategoryListEffect> = _effect

    init {
        loadCategories()
    }

    fun onEvent(event: CategoryListEvent) {
        when (event) {
            CategoryListEvent.Reload -> loadCategories()

            is CategoryListEvent.CategoryClicked -> viewModelScope.launch {
                _effect.emit(CategoryListEffect.NavigateToCategoryDetail(event.categoryId))
            }

            CategoryListEvent.NewCategoryClicked -> viewModelScope.launch {
                _effect.emit(CategoryListEffect.NavigateToNewCategory)
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            getTransactionsCategoriesUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }

                    is Resource.Success -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            categories = result.data.orEmpty()
                                .map { dto -> dto.toModel() }
                                .sortedBy { category -> category.title.lowercase() },
                            errorMessage = null
                        )
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = result.message)
                        }
                        _effect.emit(
                            CategoryListEffect.ShowToast(
                                result.message ?: "Erro ao carregar categorias"
                            )
                        )
                    }
                }
            }
        }
    }
}
