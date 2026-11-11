package com.vfdeginformatica.mysuperapp.presentation.screen.recipe_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.Recipe
import com.vfdeginformatica.mysuperapp.domain.model.RecipeIngredient
import com.vfdeginformatica.mysuperapp.domain.repository.RecipeImageRepository
import com.vfdeginformatica.mysuperapp.domain.repository.RecipePhotoCompressor
import com.vfdeginformatica.mysuperapp.domain.repository.RecipeRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.recipes.*
import com.vfdeginformatica.mysuperapp.presentation.screen.recipe_detail.contract.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RecipeRepository,
    private val getRecipe: GetRecipeUseCase,
    private val saveRecipe: SaveRecipeUseCase,
    private val compressor: RecipePhotoCompressor,
    val images: RecipeImageRepository,
) : ViewModel() {
    private val existingId: String? = savedStateHandle.get<String>("recipeId")?.takeIf { it.isNotBlank() }
    private val owner = repository.currentUserId()
    private val newId = UUID.randomUUID().toString()
    private val state = MutableStateFlow(RecipeDetailUiState())
    val uiState = state.asStateFlow()
    private val events = Channel<RecipeDetailEffect>(Channel.BUFFERED)
    val effect = events.receiveAsFlow()
    private var loadJob: Job? = null
    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observeUserId().collect { uid ->
                if (uid == null || uid != owner) {
                    loadJob?.cancel()
                    saveJob?.cancel()
                    state.value = RecipeDetailUiState(isLoading = false, accountChanged = true, error = "Sua conta mudou. Abra a receita novamente")
                } else if (state.value.original == null && !state.value.accountChanged) load()
            }
        }
    }

    fun onEvent(event: RecipeDetailEvent) {
        when (event) {
            RecipeDetailEvent.Back -> {
                if (state.value.isSaving) return
                if (state.value.isDirty) state.update { it.copy(confirmLeave = true) }
                else viewModelScope.launch { events.send(RecipeDetailEffect.NavigateUp) }
            }
            RecipeDetailEvent.Discard -> viewModelScope.launch { events.send(RecipeDetailEffect.NavigateUp) }
            RecipeDetailEvent.Stay -> state.update { it.copy(confirmLeave = false) }
            RecipeDetailEvent.Reload -> if (!state.value.accountChanged && !state.value.isSaving && state.value.original == null) load()
            RecipeDetailEvent.Save -> save()
            is RecipeDetailEvent.DraftChanged -> if (state.value.canEdit) state.update {
                it.copy(draft = event.recipe.copy(id = it.draft.id, userId = it.draft.userId, imagePath = it.draft.imagePath), error = null)
            }
            is RecipeDetailEvent.PhotoSelected -> if (state.value.canEdit) state.update {
                it.copy(photoUri = event.uri, removePhoto = false, error = null)
            }
            RecipeDetailEvent.RemovePhoto -> if (state.value.canEdit) state.update {
                it.copy(photoUri = null, removePhoto = it.draft.imagePath != null, error = null)
            }
        }
    }

    private fun load() {
        val uid = owner ?: return
        loadJob?.cancel()
        if (existingId == null) {
            val recipe = Recipe(id = newId, userId = uid, ingredients = listOf(RecipeIngredient()), steps = listOf(""))
            state.value = RecipeDetailUiState(draft = recipe, original = recipe, isLoading = false)
            return
        }
        loadJob = viewModelScope.launch {
            getRecipe(uid, existingId).collect { result ->
                if (repository.currentUserId() != uid || state.value.accountChanged) return@collect
                state.update {
                    when (result) {
                        is Resource.Loading -> it.copy(isLoading = true, error = null)
                        is Resource.Success -> it.copy(draft = result.data!!, original = result.data, isLoading = false)
                        is Resource.Error -> it.copy(isLoading = false, error = result.message)
                    }
                }
            }
        }
    }

    private fun save() {
        val snapshot = state.value
        if (!snapshot.canEdit || repository.currentUserId() != owner) return
        snapshot.draft.normalized().validationError()?.let { message ->
            state.update { it.copy(error = message) }
            return
        }
        state.update { it.copy(isSaving = true, error = null) }
        saveJob = viewModelScope.launch {
            try {
                val photo = when {
                    snapshot.photoUri != null -> RecipePhotoChange.Replace(compressor.compress(snapshot.photoUri))
                    snapshot.removePhoto -> RecipePhotoChange.Remove
                    else -> RecipePhotoChange.Keep
                }
                if (repository.currentUserId() != owner || state.value.accountChanged) return@launch
                saveRecipe(snapshot.draft, photo).collect { result ->
                    if (repository.currentUserId() != owner || state.value.accountChanged) return@collect
                    when (result) {
                        is Resource.Loading -> Unit
                        is Resource.Success -> {
                            val saved = result.data!!
                            state.update { it.copy(draft = saved, original = saved, isSaving = false, photoUri = null, removePhoto = false) }
                            events.send(RecipeDetailEffect.Saved(saved.id))
                        }
                        is Resource.Error -> state.update { it.copy(isSaving = false, error = result.message) }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (repository.currentUserId() == owner && !state.value.accountChanged) {
                    state.update { it.copy(isSaving = false, error = e.message ?: "Não foi possível salvar a receita") }
                }
            }
        }
    }
}
