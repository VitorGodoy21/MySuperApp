package com.vfdeginformatica.mysuperapp.presentation.screen.financial

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType

@Composable
fun FinancialRoute(
    viewModel: FinancialViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
    onNewTransaction: (TransactionType) -> Unit,
    onCardInvoice: (cardId: String, invoiceMonth: String) -> Unit = { _, _ -> },
    onCategoryExpenses: (month: String) -> Unit = {},
    onReport: (month: String) -> Unit = {}
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
                is FinancialEffect.NavigateToCardInvoice ->
                    onCardInvoice(effect.cardId, effect.invoiceMonth)
                is FinancialEffect.NavigateToCategoryExpenses ->
                    onCategoryExpenses(effect.month)
                is FinancialEffect.NavigateToFinancialReport ->
                    onReport(effect.month)
            }
        }
    }

    // Recarrega ao voltar para esta tela (ex.: após criar nova transação)
    LaunchedEffect(Unit) {
        viewModel.onEvent(FinancialEvent.OnRefresh)
    }

    AppScaffold(
        title = "Financeiro",
        canNavigateUp = true,
        onNavigateUp = onNavigateUp,
        snackBarHostState = snackBarHost,
        toolbarActions = {
            IconButton(onClick = { viewModel.onEvent(FinancialEvent.OnReportClicked) }) {
                Icon(Icons.Default.Assessment, contentDescription = "Relatório mensal")
            }
        },
        floatingActionButtonContent = {
            TransactionFabMenu(expanded = fabExpanded, onNewTransaction = onNewTransaction)
        }
    ) { padding ->
        FinancialScreen(
            uiState = state,
            onEvent = viewModel::onEvent,
            onCategoryExpenses = { viewModel.onEvent(FinancialEvent.OnCategoryExpensesClicked) },
            modifier = Modifier.padding(padding)
        )
    }
}