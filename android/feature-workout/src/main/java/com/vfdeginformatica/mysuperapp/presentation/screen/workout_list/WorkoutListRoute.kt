package com.vfdeginformatica.mysuperapp.presentation.screen.workout_list

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vfdeginformatica.mysuperapp.presentation.components.toolbar.AppScaffold
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_list.contract.WorkoutListEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_list.contract.WorkoutListEvent

@Composable
fun WorkoutListRoute(
    viewModel: WorkoutListViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
    onNavigateToDetail: (String?) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHost: SnackbarHostState = remember { SnackbarHostState() }

    // Recarrega ao voltar da criação/edição de um treino
    LaunchedEffect(Unit) {
        viewModel.onEvent(WorkoutListEvent.Reload)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is WorkoutListEffect.NavigateToDetail -> onNavigateToDetail(effect.workoutId)
                is WorkoutListEffect.ShowToast -> snackBarHost.showSnackbar(effect.message)
            }
        }
    }

    AppScaffold(
        title = "Treinos",
        canNavigateUp = true,
        onNavigateUp = onNavigateUp,
        snackBarHostState = snackBarHost,
        floatingActionButtonOnClick = { viewModel.onEvent(WorkoutListEvent.NewClicked) },
        floatingActionButtonIcon = Icons.Default.Add,
        floatingActionButtonLabel = "Novo treino",
    ) { padding ->
        WorkoutListScreen(
            uiState = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(padding),
        )
    }
}
