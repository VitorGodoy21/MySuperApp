package com.vfdeginformatica.mysuperapp.presentation.screen.card_list

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
import com.vfdeginformatica.mysuperapp.presentation.screen.card_list.contract.CardListEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.card_list.contract.CardListEvent

@Composable
fun CardListRoute(
    viewModel: CardListViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
    onNavigateToCardDetail: (String) -> Unit,
    onNavigateToNewCard: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHost: SnackbarHostState = remember { SnackbarHostState() }

    // Recarrega ao voltar de cadastro/edição de cartão
    LaunchedEffect(Unit) {
        viewModel.onEvent(CardListEvent.Reload)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CardListEffect.NavigateToCardDetail -> onNavigateToCardDetail(effect.cardId)
                CardListEffect.NavigateToNewCard -> onNavigateToNewCard()
                is CardListEffect.ShowToast -> snackBarHost.showSnackbar(effect.message)
            }
        }
    }

    AppScaffold(
        title = "Cartões",
        canNavigateUp = true,
        onNavigateUp = onNavigateUp,
        snackBarHostState = snackBarHost
    ) { padding ->
        CardListScreen(
            uiState = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(padding)
        )
    }
}
