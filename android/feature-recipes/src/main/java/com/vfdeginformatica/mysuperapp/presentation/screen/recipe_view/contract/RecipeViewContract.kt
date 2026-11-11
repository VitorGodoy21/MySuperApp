package com.vfdeginformatica.mysuperapp.presentation.screen.recipe_view.contract

import com.vfdeginformatica.mysuperapp.domain.model.Recipe

data class RecipeViewUiState(
    val recipe: Recipe? = null,
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val error: String? = null,
    val confirmDelete: Boolean = false,
    val accountChanged: Boolean = false,
)

sealed interface RecipeViewEvent {
    data object Reload : RecipeViewEvent
    data object ToggleFavorite : RecipeViewEvent
    data object AskDelete : RecipeViewEvent
    data object DismissDelete : RecipeViewEvent
    data object ConfirmDelete : RecipeViewEvent
}

sealed interface RecipeViewEffect {
    data object Deleted : RecipeViewEffect
}
