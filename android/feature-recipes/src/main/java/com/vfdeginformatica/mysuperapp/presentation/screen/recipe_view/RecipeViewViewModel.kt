package com.vfdeginformatica.mysuperapp.presentation.screen.recipe_view

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.repository.RecipeImageRepository
import com.vfdeginformatica.mysuperapp.domain.repository.RecipeRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.recipes.*
import com.vfdeginformatica.mysuperapp.presentation.screen.recipe_view.contract.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecipeViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RecipeRepository,
    private val getRecipe: GetRecipeUseCase,
    private val deleteRecipe: DeleteRecipeUseCase,
    private val favoriteRecipe: FavoriteRecipeUseCase,
    val images: RecipeImageRepository,
) : ViewModel() {
    private val id: String = checkNotNull(savedStateHandle["recipeId"])
    private val owner = repository.currentUserId()
    private val state = MutableStateFlow(RecipeViewUiState())
    val uiState = state.asStateFlow()
    private val events = Channel<RecipeViewEffect>(Channel.BUFFERED)
    val effect = events.receiveAsFlow()
    private var loadJob: Job? = null
    private var actionJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observeUserId().collect { uid ->
                if (uid == null || uid != owner) {
                    loadJob?.cancel()
                    actionJob?.cancel()
                    state.value = RecipeViewUiState(isLoading = false, accountChanged = true, error = "Sua conta mudou. Volte à lista")
                } else if (!state.value.accountChanged) reload()
            }
        }
    }

    fun onEvent(event: RecipeViewEvent) {
        if (state.value.accountChanged) return
        when (event) {
            RecipeViewEvent.Reload -> if (!state.value.isBusy) reload()
            RecipeViewEvent.AskDelete -> if (!state.value.isBusy) state.update { it.copy(confirmDelete = true) }
            RecipeViewEvent.DismissDelete -> state.update { it.copy(confirmDelete = false) }
            RecipeViewEvent.ConfirmDelete -> if (state.value.confirmDelete) perform(delete = true)
            RecipeViewEvent.ToggleFavorite -> perform(delete = false)
        }
    }

    private fun reload() {
        val uid = owner ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            getRecipe(uid, id).collect { result ->
                if (repository.currentUserId() != uid || state.value.accountChanged) return@collect
                state.update {
                    when (result) {
                        is Resource.Loading -> it.copy(isLoading = true, error = null)
                        is Resource.Success -> it.copy(recipe = result.data, isLoading = false, error = null)
                        is Resource.Error -> it.copy(recipe = null, isLoading = false, error = result.message)
                    }
                }
            }
        }
    }

    private fun perform(delete: Boolean) {
        val recipe = state.value.recipe ?: return
        if (state.value.isBusy || state.value.isLoading || repository.currentUserId() != owner) return
        state.update { it.copy(isBusy = true, confirmDelete = false, error = null) }
        actionJob = viewModelScope.launch {
            val operation = if (delete) deleteRecipe(recipe) else favoriteRecipe(recipe)
            operation.collect { result ->
                if (repository.currentUserId() != owner || state.value.accountChanged) return@collect
                when (result) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        state.update {
                            it.copy(isBusy = false, recipe = if (delete) null else recipe.copy(isFavorite = !recipe.isFavorite))
                        }
                        if (delete) events.send(RecipeViewEffect.Deleted)
                    }
                    is Resource.Error -> state.update { it.copy(isBusy = false, error = result.message) }
                }
            }
        }
    }
}
