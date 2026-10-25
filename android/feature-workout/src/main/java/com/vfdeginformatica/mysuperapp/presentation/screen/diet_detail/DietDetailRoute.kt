package com.vfdeginformatica.mysuperapp.presentation.screen.diet_detail

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
import com.vfdeginformatica.mysuperapp.presentation.screen.diet_detail.contract.DietDetailEffect

@Composable
fun DietDetailRoute(
    viewModel: DietDetailViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                DietDetailEffect.NavigateUp -> onNavigateUp()
                is DietDetailEffect.ShowMessage -> snackbar.showSnackbar(effect.message)
            }
        }
    }
    AppScaffold(
        title = if (state.exists) "Editar dieta" else "Nova dieta",
        canNavigateUp = true,
        onNavigateUp = onNavigateUp,
        snackBarHostState = snackbar,
    ) { padding ->
        DietDetailScreen(state, viewModel::onEvent, Modifier.padding(padding))
    }
}
