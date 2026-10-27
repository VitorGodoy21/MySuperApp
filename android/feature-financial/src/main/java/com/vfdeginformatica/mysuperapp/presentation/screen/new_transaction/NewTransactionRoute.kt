package com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction

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
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.NewTransactionEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.NewTransactionEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType

@Composable
fun NewTransactionRoute(
    viewModel: NewTransactionViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
    onNewCard: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHost: SnackbarHostState = remember { SnackbarHostState() }

    // Recarrega os cartões ao (re)entrar na tela (ex.: após cadastrar um novo cartão)
    LaunchedEffect(Unit) {
        viewModel.onEvent(NewTransactionEvent.ReloadCards)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is NewTransactionEffect.ShowToast -> snackBarHost.showSnackbar(effect.message)
                is NewTransactionEffect.TransactionSaved -> onNavigateUp()
            }
        }
    }

    AppScaffold(
        title = if (state.transactionType == TransactionType.INCOME) "Nova receita" else "Novo gasto",
        canNavigateUp = true,
        onNavigateUp = onNavigateUp,
        snackBarHostState = snackBarHost
    ) { padding ->
        NewTransactionScreen(
            uiState = state,
            onEvent = viewModel::onEvent,
            onNewCard = onNewCard,
            modifier = Modifier.padding(padding)
        )
    }
}