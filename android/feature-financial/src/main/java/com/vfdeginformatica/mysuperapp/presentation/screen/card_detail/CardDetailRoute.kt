package com.vfdeginformatica.mysuperapp.presentation.screen.card_detail

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
import com.vfdeginformatica.mysuperapp.presentation.screen.card_detail.contract.CardDetailEffect

@Composable
fun CardDetailRoute(
    viewModel: CardDetailViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHost: SnackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CardDetailEffect.ShowToast -> snackBarHost.showSnackbar(effect.message)
                CardDetailEffect.CardSaved -> onNavigateUp()
            }
        }
    }

    AppScaffold(
        title = "Configurar Cartão",
        canNavigateUp = true,
        onNavigateUp = onNavigateUp,
        snackBarHostState = snackBarHost
    ) { padding ->
        CardDetailScreen(
            uiState = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(padding)
        )
    }
}
