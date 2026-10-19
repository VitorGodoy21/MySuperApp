package com.vfdeginformatica.mysuperapp.presentation.screen.exercise_list

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
import com.vfdeginformatica.mysuperapp.presentation.screen.exercise_list.contract.ExerciseListEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.exercise_list.contract.ExerciseListEvent

@Composable
fun ExerciseListRoute(
    viewModel: ExerciseListViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
    onNavigateToDetail: (String?) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHost: SnackbarHostState = remember { SnackbarHostState() }

    // Recarrega ao voltar de cadastro/edição de exercício
    LaunchedEffect(Unit) {
        viewModel.onEvent(ExerciseListEvent.Reload)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ExerciseListEffect.NavigateToDetail -> onNavigateToDetail(effect.exerciseId)
                is ExerciseListEffect.ShowToast -> snackBarHost.showSnackbar(effect.message)
            }
        }
    }

    AppScaffold(
        title = "Exercícios",
        canNavigateUp = true,
        onNavigateUp = onNavigateUp,
        snackBarHostState = snackBarHost,
        floatingActionButtonOnClick = { viewModel.onEvent(ExerciseListEvent.NewClicked) },
        floatingActionButtonIcon = Icons.Default.Add,
        floatingActionButtonLabel = "Novo exercício",
    ) { padding ->
        ExerciseListScreen(
            uiState = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(padding)
        )
    }
}
