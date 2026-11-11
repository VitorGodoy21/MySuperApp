package com.vfdeginformatica.mysuperapp.presentation.screen.recipe_list.contract

import com.vfdeginformatica.mysuperapp.domain.model.Recipe
import com.vfdeginformatica.mysuperapp.domain.model.filterRecipes

data class RecipeListUiState(
    val recipes: List<Recipe> = emptyList(),
    val query: String = "",
    val favoritesOnly: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val visibleRecipes get() = recipes.filterRecipes(query, favoritesOnly)
}

sealed interface RecipeListEvent {
    data object Reload : RecipeListEvent
    data class QueryChanged(val value: String) : RecipeListEvent
    data object FavoritesToggled : RecipeListEvent
}
