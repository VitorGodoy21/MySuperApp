package com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_detail

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
import com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_detail.contract.BodyMeasurementDetailEffect

@Composable
fun BodyMeasurementDetailRoute(
    viewModel: BodyMeasurementDetailViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHost: SnackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is BodyMeasurementDetailEffect.ShowToast -> snackBarHost.showSnackbar(effect.message)
                BodyMeasurementDetailEffect.NavigateUp -> onNavigateUp()
            }
        }
    }

    AppScaffold(
        title = if (state.isEditing) "Editar pesagem" else "Nova pesagem",
        canNavigateUp = true,
        onNavigateUp = onNavigateUp,
        snackBarHostState = snackBarHost
    ) { padding ->
        BodyMeasurementDetailScreen(
            uiState = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(padding)
        )
    }
}
