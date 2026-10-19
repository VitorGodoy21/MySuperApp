package com.vfdeginformatica.mysuperapp.presentation.screen.workout_view

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vfdeginformatica.mysuperapp.presentation.components.toolbar.AppScaffold
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_view.contract.WorkoutViewEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_view.contract.WorkoutViewEvent

@Composable
fun WorkoutViewRoute(
    viewModel: WorkoutViewViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHost: SnackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is WorkoutViewEffect.ShowToast -> snackBarHost.showSnackbar(effect.message)
                WorkoutViewEffect.NavigateUp -> onNavigateUp()
            }
        }
    }

    val canRegister = state.workout != null && !state.isBusy
    AppScaffold(
        title = state.workout?.name ?: "Treino",
        canNavigateUp = true,
        onNavigateUp = onNavigateUp,
        snackBarHostState = snackBarHost,
        floatingActionButtonOnClick = if (canRegister) {
            { viewModel.onEvent(WorkoutViewEvent.RegisterSessionClicked) }
        } else null,
        floatingActionButtonIcon = if (canRegister) Icons.Default.AddTask else null,
        floatingActionButtonLabel = if (canRegister) "Registrar treino" else null,
    ) { padding ->
        WorkoutViewScreen(
            uiState = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(padding),
        )
    }
}
