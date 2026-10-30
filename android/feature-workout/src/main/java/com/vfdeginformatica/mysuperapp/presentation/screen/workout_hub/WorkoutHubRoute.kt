package com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub

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
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub.contract.WorkoutHubEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub.contract.WorkoutHubEvent

@Composable
fun WorkoutHubRoute(
    viewModel: WorkoutHubViewModel = hiltViewModel(),
    showDiet: Boolean = true,
    onNavigateUp: () -> Unit,
    onNavigateToWorkout: (String) -> Unit,
    onNavigateToDiet: () -> Unit,
    onNavigateToBodyMeasurements: () -> Unit,
    onNavigateToHistory: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHost: SnackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                WorkoutHubEffect.NavigateToDiet -> onNavigateToDiet()
                WorkoutHubEffect.NavigateToBodyMeasurements -> onNavigateToBodyMeasurements()
                WorkoutHubEffect.NavigateToHistory -> onNavigateToHistory()
                is WorkoutHubEffect.NavigateToWorkout -> onNavigateToWorkout(effect.workoutId)
                is WorkoutHubEffect.ShowToast -> snackBarHost.showSnackbar(effect.message)
            }
        }
    }

    AppScaffold(
        title = "Academia",
        canNavigateUp = true,
        onNavigateUp = onNavigateUp,
        snackBarHostState = snackBarHost,
        onResume = { viewModel.onEvent(WorkoutHubEvent.Reload) },
    ) { padding ->
        WorkoutHubScreen(
            uiState = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(padding),
            showDiet = showDiet,
        )
    }
}
