package com.vfdeginformatica.mysuperapp.presentation.screen.financial

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vfdeginformatica.mysuperapp.presentation.components.toolbar.AppScaffold
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialEffect

@Composable
fun FinancialRoute(
    viewModel: FinancialViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
    onNewTransaction: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHost: SnackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val fabExpanded by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 10 }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is FinancialEffect.ShowToast -> snackBarHost.showSnackbar(effect.message)
            }
        }
    }

    AppScaffold(
        title = "Financeiro",
        canNavigateUp = true,
        onNavigateUp = onNavigateUp,
        snackBarHostState = snackBarHost,
        floatingActionButtonExpanded = fabExpanded,
        floatingActionButtonIcon = Icons.Default.Add,
        floatingActionButtonLabel = "Criar Nova transação",
        floatingActionButtonOnClick = {
            onNewTransaction.invoke()
        }
    ) { padding ->
        FinancialScreen(
            uiState = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(padding)
        )
    }
}