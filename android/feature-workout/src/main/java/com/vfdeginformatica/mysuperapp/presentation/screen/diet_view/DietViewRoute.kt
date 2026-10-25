package com.vfdeginformatica.mysuperapp.presentation.screen.diet_view

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vfdeginformatica.mysuperapp.presentation.components.toolbar.AppScaffold
import com.vfdeginformatica.mysuperapp.presentation.screen.diet_view.contract.DietViewEvent

@Composable
fun DietViewRoute(
    viewModel: DietViewViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    AppScaffold(
        title = "Minha dieta",
        canNavigateUp = true,
        onNavigateUp = onNavigateUp,
        snackBarHostState = snackbar,
        onResume = { viewModel.onEvent(DietViewEvent.Reload) },
    ) { padding ->
        DietViewScreen(state, { viewModel.onEvent(DietViewEvent.Reload) }, Modifier.padding(padding))
    }
}
