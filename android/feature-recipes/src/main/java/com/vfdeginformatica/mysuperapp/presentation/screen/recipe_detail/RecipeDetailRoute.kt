package com.vfdeginformatica.mysuperapp.presentation.screen.recipe_detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vfdeginformatica.mysuperapp.presentation.common.RecipePhoto
import com.vfdeginformatica.mysuperapp.presentation.components.toolbar.AppScaffold
import com.vfdeginformatica.mysuperapp.presentation.screen.recipe_detail.contract.*

@Composable
fun RecipeDetailRoute(
    onNavigateUp: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: RecipeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler { viewModel.onEvent(RecipeDetailEvent.Back) }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect {
            when (it) {
                is RecipeDetailEffect.Saved -> onSaved(it.id)
                RecipeDetailEffect.NavigateUp -> onNavigateUp()
            }
        }
    }
    AppScaffold(
        title = "Salvar receita",
        canNavigateUp = true,
        onNavigateUp = { viewModel.onEvent(RecipeDetailEvent.Back) },
        snackBarHostState = remember { SnackbarHostState() },
    ) { padding ->
        RecipeDetailScreen(
            state, viewModel::onEvent, Modifier.padding(padding),
            photo = { recipe, modifier ->
                recipe.imagePath?.let { RecipePhoto(recipe.userId, it, viewModel.images, modifier) }
            },
        )
    }
}
