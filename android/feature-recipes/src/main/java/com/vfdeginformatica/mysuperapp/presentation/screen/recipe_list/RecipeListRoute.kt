package com.vfdeginformatica.mysuperapp.presentation.screen.recipe_list

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vfdeginformatica.mysuperapp.presentation.common.RecipePhoto
import com.vfdeginformatica.mysuperapp.presentation.components.toolbar.AppScaffold
import com.vfdeginformatica.mysuperapp.presentation.screen.recipe_list.contract.RecipeListEvent

@Composable
fun RecipeListRoute(
    onNavigateUp: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    viewModel: RecipeListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onEvent(RecipeListEvent.Reload) }
    AppScaffold(
        title = "Receitas culinárias",
        canNavigateUp = true,
        onNavigateUp = onNavigateUp,
        floatingActionButtonOnClick = onAdd,
        floatingActionButtonIcon = Icons.Default.Add,
        floatingActionButtonLabel = "Adicionar receita",
        snackBarHostState = remember { SnackbarHostState() },
    ) { padding ->
        RecipeListScreen(
            state, viewModel::onEvent, onOpen, Modifier.padding(padding),
            photo = { recipe, modifier ->
                recipe.imagePath?.let { RecipePhoto(recipe.userId, it, viewModel.images, modifier) }
            },
        )
    }
}
