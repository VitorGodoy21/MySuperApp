package com.vfdeginformatica.mysuperapp.presentation.screen.recipe_detail.contract

import com.vfdeginformatica.mysuperapp.domain.model.Recipe

data class RecipeDetailUiState(
    val draft: Recipe = Recipe(),
    val original: Recipe? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val photoUri: String? = null,
    val removePhoto: Boolean = false,
    val accountChanged: Boolean = false,
    val confirmLeave: Boolean = false,
) {
    val isDirty get() = original != null && (draft != original || photoUri != null || removePhoto)
    val canEdit get() = !isLoading && !isSaving && !accountChanged && original != null
}

sealed interface RecipeDetailEvent {
    data class DraftChanged(val recipe: Recipe) : RecipeDetailEvent
    data class PhotoSelected(val uri: String) : RecipeDetailEvent
    data object RemovePhoto : RecipeDetailEvent
    data object Save : RecipeDetailEvent
    data object Reload : RecipeDetailEvent
    data object Back : RecipeDetailEvent
    data object Discard : RecipeDetailEvent
    data object Stay : RecipeDetailEvent
}

sealed interface RecipeDetailEffect {
    data class Saved(val id: String) : RecipeDetailEffect
    data object NavigateUp : RecipeDetailEffect
}
