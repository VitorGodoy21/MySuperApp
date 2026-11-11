package com.vfdeginformatica.mysuperapp.presentation.screen.recipe_view

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vfdeginformatica.mysuperapp.presentation.common.RecipePhoto
import com.vfdeginformatica.mysuperapp.presentation.components.toolbar.AppScaffold
import com.vfdeginformatica.mysuperapp.presentation.screen.recipe_view.contract.*

@Composable
fun RecipeViewRoute(
    onNavigateUp: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: RecipeViewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onEvent(RecipeViewEvent.Reload) }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { if (it == RecipeViewEffect.Deleted) onNavigateUp() }
    }
    AppScaffold(
        title = "Receita", canNavigateUp = true, onNavigateUp = onNavigateUp,
        snackBarHostState = remember { SnackbarHostState() },
    ) { padding ->
        RecipeViewScreen(
            state, viewModel::onEvent, onEdit, Modifier.padding(padding),
            photo = { recipe, modifier ->
                recipe.imagePath?.let { RecipePhoto(recipe.userId, it, viewModel.images, modifier) }
            },
        )
    }
}
