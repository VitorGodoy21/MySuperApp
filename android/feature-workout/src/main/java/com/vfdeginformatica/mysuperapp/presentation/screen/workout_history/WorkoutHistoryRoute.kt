package com.vfdeginformatica.mysuperapp.presentation.screen.workout_history

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
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_history.contract.WorkoutHistoryEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_history.contract.WorkoutHistoryEvent

@Composable
fun WorkoutHistoryRoute(
    viewModel: WorkoutHistoryViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
    onNavigateToWorkout: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHost: SnackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is WorkoutHistoryEffect.NavigateToWorkout -> onNavigateToWorkout(effect.workoutId)
                is WorkoutHistoryEffect.ShowToast -> snackBarHost.showSnackbar(effect.message)
            }
        }
    }

    AppScaffold(
        title = "Histórico de treinos",
        canNavigateUp = true,
        onNavigateUp = onNavigateUp,
        snackBarHostState = snackBarHost,
        onResume = { viewModel.onEvent(WorkoutHistoryEvent.Reload) },
    ) { padding ->
        WorkoutHistoryScreen(
            uiState = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(padding),
        )
    }
}
