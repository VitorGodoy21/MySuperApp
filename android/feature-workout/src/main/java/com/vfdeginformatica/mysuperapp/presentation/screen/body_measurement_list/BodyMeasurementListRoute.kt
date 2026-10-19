package com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_list

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
import com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_list.contract.BodyMeasurementListEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_list.contract.BodyMeasurementListEvent

@Composable
fun BodyMeasurementListRoute(
    viewModel: BodyMeasurementListViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
    onNavigateToDetail: (String?) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHost: SnackbarHostState = remember { SnackbarHostState() }

    // Recarrega ao voltar da tela de cadastro/edição
    LaunchedEffect(Unit) {
        viewModel.onEvent(BodyMeasurementListEvent.Reload)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is BodyMeasurementListEffect.NavigateToDetail -> onNavigateToDetail(effect.id)
                is BodyMeasurementListEffect.ShowToast -> snackBarHost.showSnackbar(effect.message)
            }
        }
    }

    AppScaffold(
        title = "Dados físicos",
        canNavigateUp = true,
        onNavigateUp = onNavigateUp,
        snackBarHostState = snackBarHost,
        floatingActionButtonOnClick = { viewModel.onEvent(BodyMeasurementListEvent.NewClicked) },
        floatingActionButtonIcon = Icons.Default.Add,
        floatingActionButtonLabel = "Nova pesagem",
    ) { padding ->
        BodyMeasurementListScreen(
            uiState = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(padding)
        )
    }
}
