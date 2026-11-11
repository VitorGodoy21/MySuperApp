package com.vfdeginformatica.mysuperapp.presentation.screen.recipe_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.repository.RecipeImageRepository
import com.vfdeginformatica.mysuperapp.domain.repository.RecipeRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.recipes.GetRecipesUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.recipe_list.contract.RecipeListEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.recipe_list.contract.RecipeListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecipeListViewModel @Inject constructor(
    private val repository: RecipeRepository,
    private val getRecipes: GetRecipesUseCase,
    val images: RecipeImageRepository,
) : ViewModel() {
    private val state = MutableStateFlow(RecipeListUiState())
    val uiState = state.asStateFlow()
    private var loadJob: Job? = null
    private var uid: String? = null

    init {
        viewModelScope.launch {
            repository.observeUserId().collect {
                loadJob?.cancel()
                uid = it
                state.value = RecipeListUiState()
                reload()
            }
        }
    }

    fun onEvent(event: RecipeListEvent) {
        when (event) {
            RecipeListEvent.Reload -> reload()
            is RecipeListEvent.QueryChanged -> state.update { it.copy(query = event.value) }
            RecipeListEvent.FavoritesToggled -> state.update { it.copy(favoritesOnly = !it.favoritesOnly) }
        }
    }

    private fun reload() {
        val owner = uid ?: run {
            state.value = RecipeListUiState(isLoading = false, error = "Entre na sua conta para ver as receitas")
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            getRecipes(owner).collect { result ->
                if (repository.currentUserId() != owner) return@collect
                state.update {
                    when (result) {
                        is Resource.Loading -> it.copy(isLoading = true, error = null)
                        is Resource.Success -> it.copy(isLoading = false, recipes = result.data.orEmpty(), error = null)
                        is Resource.Error -> it.copy(isLoading = false, error = result.message)
                    }
                }
            }
        }
    }
}
