package com.vfdeginformatica.mysuperapp.presentation.screen.category_list

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vfdeginformatica.mysuperapp.presentation.components.toolbar.AppScaffold
import com.vfdeginformatica.mysuperapp.presentation.screen.category_list.contract.CategoryListEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.category_list.contract.CategoryListEvent

@Composable
fun CategoryListRoute(
    viewModel: CategoryListViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
    onNavigateToCategoryDetail: (String) -> Unit,
    onNavigateToNewCategory: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHost: SnackbarHostState = remember { SnackbarHostState() }

    // Recarrega ao voltar de cadastro/edição de categoria
    LaunchedEffect(Unit) {
        viewModel.onEvent(CategoryListEvent.Reload)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CategoryListEffect.NavigateToCategoryDetail ->
                    onNavigateToCategoryDetail(effect.categoryId)

                CategoryListEffect.NavigateToNewCategory -> onNavigateToNewCategory()
                is CategoryListEffect.ShowToast -> snackBarHost.showSnackbar(effect.message)
            }
        }
    }

    AppScaffold(
        title = "Categorias",
        canNavigateUp = true,
        onNavigateUp = onNavigateUp,
        snackBarHostState = snackBarHost
    ) { padding ->
        CategoryListScreen(
            uiState = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(padding)
        )
    }
}
