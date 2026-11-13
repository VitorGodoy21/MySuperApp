package com.vfdeginformatica.mysuperapp.presentation.screen.financial

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.data.mapper.toModel
import com.vfdeginformatica.mysuperapp.domain.model.FinancialTotals.isInvoicePayment
import com.vfdeginformatica.mysuperapp.domain.model.PaidInvoice
import com.vfdeginformatica.mysuperapp.domain.model.RecurrenceEditScope
import com.vfdeginformatica.mysuperapp.domain.model.Transaction
import com.vfdeginformatica.mysuperapp.domain.model.hasOverrideForOccurrence
import com.vfdeginformatica.mysuperapp.presentation.common.EditTransactionDialog
import com.vfdeginformatica.mysuperapp.presentation.common.PayInvoiceDialog
import com.vfdeginformatica.mysuperapp.presentation.common.RecurrenceScopeDialog
import com.vfdeginformatica.mysuperapp.presentation.common.balanceColor
import com.vfdeginformatica.mysuperapp.presentation.common.expenseColor
import com.vfdeginformatica.mysuperapp.presentation.common.formatCurrency
import com.vfdeginformatica.mysuperapp.presentation.common.formatPercent
import com.vfdeginformatica.mysuperapp.presentation.common.incomeColor
import com.vfdeginformatica.mysuperapp.presentation.common.monthLabel
import com.vfdeginformatica.mysuperapp.presentation.common.ptBr
import com.vfdeginformatica.mysuperapp.presentation.common.shortMonthLabel
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FilterGroupBy
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialTab
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialUiState
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.OTHERS_KEY
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.PaymentMethodFilter
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.PendingRecurringAction
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.TransactionTypeFilter
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType
import java.text.SimpleDateFormat

@Composable
fun FinancialScreen(
    uiState: FinancialUiState,
    onEvent: (FinancialEvent) -> Unit,
    onCategoryExpenses: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TabSelector(uiState, onEvent)
            MonthSelector(uiState, onEvent)
            when (uiState.selectedTab) {
                FinancialTab.MONTH -> MonthContent(uiState, onEvent, onCategoryExpenses)
                FinancialTab.FILTERS -> FiltersContent(uiState, onEvent)
            }
        }

        if (uiState.isLoading || uiState.isLoadingProjection) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }

        if (!uiState.isLoading && uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .align(Alignment.Center)
            )
        }
    }

    uiState.payingInvoice?.let { invoice ->
        PayInvoiceDialog(
            invoice = invoice,
            isSaving = uiState.isPayingInvoice,
            onDismiss = { onEvent(FinancialEvent.OnPayInvoiceDismiss) },
            onConfirm = { amount, date -> onEvent(FinancialEvent.OnPayInvoiceConfirm(amount, date)) }
        )
    }

    uiState.editingTransaction?.let { transaction ->
        EditTransactionDialog(
            transaction = transaction,
            availableCategories = uiState.availableCategories.map { it.toModel() },
            isSaving = uiState.isSavingEdit,
            onDismiss = { onEvent(FinancialEvent.OnEditDismiss) },
            onConfirm = { title, amount, categoryIds ->
                onEvent(FinancialEvent.OnEditConfirm(title, amount, categoryIds))
            },
            onDelete = { onEvent(FinancialEvent.OnDeleteTransaction) }
        )
        uiState.pendingRecurringAction?.let { action ->
            RecurrenceScopeDialog(
                transaction = transaction,
                isDelete = action is PendingRecurringAction.Delete,
                isSaving = uiState.isSavingEdit,
                onScope = { onEvent(FinancialEvent.OnRecurringScopeConfirm(it)) },
                onDismiss = { onEvent(FinancialEvent.OnRecurringScopeDismiss) }
            )
        }
    }
}

@Composable
private fun TabSelector(
    uiState: FinancialUiState,
    onEvent: (FinancialEvent) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = uiState.selectedTab == FinancialTab.MONTH,
            onClick = { onEvent(FinancialEvent.OnTabChanged(FinancialTab.MONTH)) },
            label = { Text("Mês", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = uiState.selectedTab == FinancialTab.FILTERS,
            onClick = { onEvent(FinancialEvent.OnTabChanged(FinancialTab.FILTERS)) },
            label = {
                Text("Filtros", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MonthSelector(
    uiState: FinancialUiState,
    onEvent: (FinancialEvent) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = { onEvent(FinancialEvent.OnPreviousMonth) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mês anterior")
        }
        Text(text = monthLabel(uiState.selectedMonth), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { onEvent(FinancialEvent.OnNextMonth) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próximo mês")
        }
    }
}

// ---------------------------------------------------------------------------
// Aba Mês: resumo detalhado + lista recolhível
// ---------------------------------------------------------------------------

@Composable
private fun MonthContent(
    uiState: FinancialUiState,
    onEvent: (FinancialEvent) -> Unit,
    onCategoryExpenses: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item { SummaryCard(uiState, onEvent) }

        if (uiState.invoiceByCard.isNotEmpty()) {
            item { InvoiceByCardCard(uiState, onEvent) }
        }

        if (uiState.totalExpense > 0.0) {
            item { PaymentMethodBreakdownCard(uiState) }
            item { ExpenseNatureCard(uiState) }
            if (uiState.expenseByCategory.isNotEmpty()) {
                item { TopCategoriesCard(uiState, onCategoryExpenses) }
            }
            if (uiState.expenseByCard.isNotEmpty()) {
                item { ExpenseByCardCard(uiState) }
            }
        }

        item {
            HorizontalDivider()
            TextButton(
                onClick = { onEvent(FinancialEvent.OnToggleMonthList) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (uiState.isMonthListExpanded) {
                        "Ocultar transações"
                    } else {
                        "Ver transações (${uiState.transactions.size})"
                    }
                )
            }
        }

        if (uiState.isMonthListExpanded) {
            transactionItems(uiState.transactions, uiState, onEvent, "Nenhuma transação no período")
        }
    }
}

@Composable
private fun SummaryCard(
    uiState: FinancialUiState,
    onEvent: (FinancialEvent) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Saldo do período", style = MaterialTheme.typography.labelLarge)
            Text(
                text = formatCurrency(uiState.balance),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = balanceColor(uiState.balance)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Receitas", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = formatCurrency(uiState.totalIncome),
                        color = incomeColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Despesas", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = formatCurrency(uiState.cashExpense),
                        color = expenseColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(
                text = "Débito ${formatCurrency(uiState.debitExpense)} • " +
                        "Faturas ${formatCurrency(uiState.paidInvoiceTotal)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            uiState.paidInvoices.forEach { invoice ->
                PaidInvoiceRow(invoice, uiState, onEvent)
            }
            Text(
                text = "Compras no crédito do mês: ${formatCurrency(uiState.creditExpense)} " +
                        "(entram na próxima fatura)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Linha de uma fatura que sai da conta no mês: cartão, mês da fatura, vencimento e total, com
 * o botão "Pagar" enquanto estiver em aberto ou a data/valor do pagamento quando quitada.
 */
@Composable
private fun PaidInvoiceRow(
    invoice: PaidInvoice,
    uiState: FinancialUiState,
    onEvent: (FinancialEvent) -> Unit
) {
    val canOpen = invoice.cardId.isNotBlank()
    val due = invoice.invoiceDueDate.takeIf { it > 0 }?.let { " • vence dia $it" }.orEmpty()
    val paidOn = invoice.payment?.date?.toDate()?.let { SimpleDateFormat("dd/MM", ptBr).format(it) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { m -> if (canOpen) m.clickable { onEvent(FinancialEvent.OnPaidInvoiceClicked(invoice.cardId)) } else m },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${invoice.bank} • Fatura de ${shortMonthLabel(invoice.invoiceMonth)}$due",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = if (invoice.isPaid) {
                    "Paga${paidOn?.let { " em $it" }.orEmpty()} • ${formatCurrency(invoice.effectiveTotal)}"
                } else {
                    "Em aberto • ${formatCurrency(invoice.projectedTotal)}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (invoice.isPaid) incomeColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!invoice.isPaid && canOpen) {
            TextButton(
                enabled = !uiState.isLoadingPaidInvoice,
                onClick = { onEvent(FinancialEvent.OnPayInvoiceClicked(invoice.cardId)) }
            ) { Text("Pagar") }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick == null) Modifier else Modifier.clickable { onClick() })
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

@Composable
private fun AmountLine(
    label: String,
    value: Double,
    color: Color = MaterialTheme.colorScheme.onSurface,
    trailing: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        trailing?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Text(text = formatCurrency(value), color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProportionBar(fraction: Double) {
    LinearProgressIndicator(
        progress = { fraction.coerceIn(0.0, 1.0).toFloat() },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun InvoiceByCardCard(uiState: FinancialUiState, onEvent: (FinancialEvent) -> Unit) {
    SectionCard(
        title = "Fatura de ${monthLabel(uiState.invoiceMonth)}",
        subtitle = "Fatura em aberto: as compras no crédito feitas em ${monthLabel(uiState.selectedMonth)} " +
                "são somadas aqui. Inclui parcelas e recorrentes de crédito. " +
                "Toque num cartão para ver os lançamentos."
    ) {
        uiState.invoiceByCard.entries.forEach { entry ->
            val card = uiState.availableCards.find { it.id == entry.key }
            val closing = card?.takeIf { it.closingDay > 0 }?.let { "fecha dia ${it.closingDay}" }
            val due = card?.takeIf { it.invoiceDueDate > 0 }?.let { "vence dia ${it.invoiceDueDate}" }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEvent(FinancialEvent.OnCardInvoiceClicked(entry.key)) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    AmountLine(
                        label = card?.bank ?: "Cartão",
                        value = entry.value,
                        color = expenseColor
                    )
                    val info = listOfNotNull(closing, due).joinToString(" • ")
                    if (info.isNotEmpty()) {
                        Text(
                            text = info,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Ver fatura",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (uiState.invoiceByCard.size > 1) {
            HorizontalDivider()
            AmountLine(
                label = "Total das faturas",
                value = uiState.projectedInvoiceTotal,
                color = expenseColor
            )
        }
    }
}

@Composable
private fun PaymentMethodBreakdownCard(uiState: FinancialUiState) {
    val total = uiState.totalExpense
    SectionCard(title = "Despesas por forma de pagamento") {
        AmountLine(
            label = "Crédito",
            value = uiState.creditExpense,
            color = expenseColor,
            trailing = formatPercent(uiState.creditExpense / total * 100)
        )
        ProportionBar(uiState.creditExpense / total)
        AmountLine(
            label = "Débito",
            value = uiState.debitExpense,
            color = expenseColor,
            trailing = formatPercent(uiState.debitExpense / total * 100)
        )
        ProportionBar(uiState.debitExpense / total)
    }
}

@Composable
private fun ExpenseNatureCard(uiState: FinancialUiState) {
    val total = uiState.totalExpense
    SectionCard(title = "Natureza das despesas") {
        AmountLine(
            label = "Recorrentes",
            value = uiState.recurringExpense,
            color = expenseColor,
            trailing = formatPercent(uiState.recurringExpense / total * 100)
        )
        AmountLine(
            label = "Parceladas",
            value = uiState.installmentExpense,
            color = expenseColor,
            trailing = formatPercent(uiState.installmentExpense / total * 100)
        )
        AmountLine(
            label = "Avulsas",
            value = uiState.singleExpense,
            color = expenseColor,
            trailing = formatPercent(uiState.singleExpense / total * 100)
        )
    }
}

@Composable
private fun TopCategoriesCard(uiState: FinancialUiState, onClick: () -> Unit) {
    SectionCard(
        title = "Top categorias de despesa",
        subtitle = "Toque para ver todas • transações com várias categorias contam em cada uma",
        onClick = onClick
    ) {
        uiState.expenseByCategory.forEach { item ->
            val label = when (item.categoryId) {
                null -> "Sem categoria"
                OTHERS_KEY -> "Outras"
                else -> uiState.availableCategories
                    .find { it.id == item.categoryId }?.title ?: "Categoria"
            }
            AmountLine(
                label = label,
                value = item.total,
                color = expenseColor,
                trailing = formatPercent(item.percent)
            )
        }
    }
}

@Composable
private fun ExpenseByCardCard(uiState: FinancialUiState) {
    SectionCard(
        title = "Despesas por cartão",
        subtitle = "Pela data da compra (crédito e débito)"
    ) {
        uiState.expenseByCard.forEach { (cardId, total) ->
            AmountLine(
                label = uiState.availableCards.find { it.id == cardId }?.bank ?: "Cartão",
                value = total,
                color = expenseColor
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Aba Filtros
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FiltersContent(
    uiState: FinancialUiState,
    onEvent: (FinancialEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { onEvent(FinancialEvent.OnSearchQueryChanged(it)) },
                label = { Text("Pesquisar por nome ou preço") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onEvent(FinancialEvent.OnSearchQueryChanged("")) }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpar pesquisa")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item { TypeFilterChips(uiState, onEvent) }

        item {
            FilterLabel("Forma de pagamento")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentMethodFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = uiState.paymentMethodFilter == filter,
                        onClick = { onEvent(FinancialEvent.OnPaymentMethodFilterChanged(filter)) },
                        label = {
                            Text(
                                when (filter) {
                                    PaymentMethodFilter.ALL -> "Todas"
                                    PaymentMethodFilter.CREDIT -> "Só crédito"
                                    PaymentMethodFilter.DEBIT -> "Só débito"
                                }
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onEvent(FinancialEvent.OnToggleCategoryFilter) },
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            stateDescription = if (uiState.isCategoryFilterExpanded) "Expandido" else "Recolhido"
                        }
                ) {
                    Text(
                        text = if (uiState.categoryFilterIds.isEmpty()) {
                            "Categorias"
                        } else {
                            "Categorias (${uiState.categoryFilterIds.size} selecionadas)"
                        },
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start
                    )
                    Icon(
                        imageVector = if (uiState.isCategoryFilterExpanded) {
                            Icons.Default.ExpandLess
                        } else {
                            Icons.Default.ExpandMore
                        },
                        contentDescription = null
                    )
                }
                if (uiState.categoryFilterIds.isNotEmpty()) {
                    TextButton(onClick = { onEvent(FinancialEvent.OnCategoryFilterCleared) }) {
                        Text("Limpar")
                    }
                }
            }
            if (uiState.isCategoryFilterExpanded && uiState.categoriesForFilter.isEmpty()) {
                Text(
                    text = "Nenhuma categoria para o tipo selecionado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (uiState.isCategoryFilterExpanded) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.categoriesForFilter.forEach { category ->
                        val id = category.id ?: return@forEach
                        FilterChip(
                            selected = id in uiState.categoryFilterIds,
                            onClick = { onEvent(FinancialEvent.OnCategoryFilterToggled(id)) },
                            label = { Text(category.title) }
                        )
                    }
                }
            }
        }

        item { CardFilterDropdown(uiState, onEvent) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.recurringOnly,
                    onClick = { onEvent(FinancialEvent.OnRecurringOnlyToggled) },
                    label = { Text("Só recorrentes") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = uiState.installmentsOnly,
                    onClick = { onEvent(FinancialEvent.OnInstallmentsOnlyToggled) },
                    label = { Text("Só parceladas") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            FilterLabel("Agrupar por")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterGroupBy.entries.forEach { groupBy ->
                    FilterChip(
                        selected = uiState.groupBy == groupBy,
                        onClick = { onEvent(FinancialEvent.OnGroupByChanged(groupBy)) },
                        label = {
                            Text(
                                when (groupBy) {
                                    FilterGroupBy.NONE -> "Nenhum"
                                    FilterGroupBy.CATEGORY -> "Categoria"
                                    FilterGroupBy.CARD -> "Cartão"
                                }
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (uiState.hasActiveFilters || uiState.groupBy != FilterGroupBy.NONE) {
            item {
                TextButton(
                    onClick = { onEvent(FinancialEvent.OnClearFilters) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Limpar filtros")
                }
            }
        }

        item { FilteredTotalsCard(uiState) }

        if (uiState.groupBy != FilterGroupBy.NONE && uiState.groupedTotals.isNotEmpty()) {
            item { GroupedTotalsCard(uiState) }
        }

        item { HorizontalDivider() }

        transactionItems(
            uiState.filteredTransactions,
            uiState,
            onEvent,
            "Nenhuma transação para os filtros selecionados"
        )
    }
}

@Composable
private fun FilterLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun TypeFilterChips(
    uiState: FinancialUiState,
    onEvent: (FinancialEvent) -> Unit
) {
    FilterLabel("Tipo")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = uiState.typeFilter == TransactionTypeFilter.ALL,
            onClick = { onEvent(FinancialEvent.OnTypeFilterChanged(TransactionTypeFilter.ALL)) },
            label = { Text("Todas") },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = uiState.typeFilter == TransactionTypeFilter.INCOME,
            onClick = { onEvent(FinancialEvent.OnTypeFilterChanged(TransactionTypeFilter.INCOME)) },
            label = { Text("Receitas") },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = uiState.typeFilter == TransactionTypeFilter.EXPENSE,
            onClick = { onEvent(FinancialEvent.OnTypeFilterChanged(TransactionTypeFilter.EXPENSE)) },
            label = { Text("Despesas") },
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardFilterDropdown(
    uiState: FinancialUiState,
    onEvent: (FinancialEvent) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val cardLabel = uiState.availableCards.find { it.id == uiState.cardFilterId }?.bank ?: "Todos"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = cardLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Cartão") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Todos") },
                onClick = {
                    onEvent(FinancialEvent.OnCardFilterChanged(null))
                    expanded = false
                }
            )
            uiState.availableCards.forEach { card ->
                DropdownMenuItem(
                    text = { Text(card.bank) },
                    onClick = {
                        onEvent(FinancialEvent.OnCardFilterChanged(card.id))
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FilteredTotalsCard(uiState: FinancialUiState) {
    SectionCard(
        title = "Resultado",
        subtitle = "${uiState.filteredTransactions.size} transação(ões)"
    ) {
        AmountLine("Receitas", uiState.filteredIncome, incomeColor)
        AmountLine("Despesas", uiState.filteredExpense, expenseColor)
        HorizontalDivider()
        AmountLine(
            label = "Saldo",
            value = uiState.filteredBalance,
            color = balanceColor(uiState.filteredBalance)
        )
    }
}

@Composable
private fun GroupedTotalsCard(uiState: FinancialUiState) {
    val title = when (uiState.groupBy) {
        FilterGroupBy.CATEGORY -> "Totais por categoria"
        FilterGroupBy.CARD -> "Totais por cartão"
        FilterGroupBy.NONE -> ""
    }
    SectionCard(title = title) {
        uiState.groupedTotals.forEach { group ->
            val label = when (uiState.groupBy) {
                FilterGroupBy.CATEGORY -> group.key?.let { id ->
                    uiState.availableCategories.find { it.id == id }?.title
                } ?: "Sem categoria"

                FilterGroupBy.CARD -> group.key?.let { id ->
                    uiState.availableCards.find { it.id == id }?.bank
                } ?: "Sem cartão"

                FilterGroupBy.NONE -> ""
            }
            AmountLine(label = label, value = group.total, trailing = "${group.count}x")
        }
    }
}

// ---------------------------------------------------------------------------
// Lista de transações (compartilhada pelas duas abas)
// ---------------------------------------------------------------------------

private fun LazyListScope.transactionItems(
    transactions: List<Transaction>,
    uiState: FinancialUiState,
    onEvent: (FinancialEvent) -> Unit,
    emptyMessage: String
) {
    if (!uiState.isLoading && transactions.isEmpty()) {
        item {
            Text(
                text = emptyMessage,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            )
        }
        return
    }
    items(transactions, key = { "${it.id}-${it.occurrenceMonth}" }) { transaction ->
        TransactionListItem(
            transaction = transaction,
            uiState = uiState,
            onClick = { onEvent(FinancialEvent.OnTransactionClicked(transaction)) }
        )
    }
}

@Composable
private fun TransactionListItem(
    transaction: Transaction,
    uiState: FinancialUiState,
    onClick: (() -> Unit)? = null
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val amountColor = if (isIncome) incomeColor else expenseColor
    val signal = if (isIncome) "+" else "-"

    val cardBank = uiState.availableCards.find { it.id == transaction.cardId }?.bank
    val categories = transaction.categoryIds.mapNotNull { id ->
        uiState.availableCategories.find { it.id == id }?.title
    }
    val dateLabel = transaction.date?.toDate()?.let {
        SimpleDateFormat("dd/MM/yyyy", ptBr).format(it)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                val details = buildList {
                    dateLabel?.let { add(it) }
                    cardBank?.let { add(it) }
                    if (categories.isNotEmpty()) add(categories.joinToString(", "))
                }
                if (details.isNotEmpty()) {
                    Text(
                        text = details.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$signal ${formatCurrency(transaction.amount)}",
                    color = amountColor,
                    fontWeight = FontWeight.Bold
                )
                when {
                    transaction.isInvoicePayment -> Text(
                        text = "Pagamento de fatura",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    transaction.isRecurring -> Text(
                        text = if (transaction.hasOverrideForOccurrence) {
                            "Recorrente • ajustado"
                        } else {
                            "Recorrente"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    (transaction.installments ?: 0) > 1 -> Text(
                        text = transaction.installmentNumber?.let {
                            "$it/${transaction.installments}"
                        } ?: "${transaction.installments}x",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FinancialScreenPreview() {
    FinancialScreen(
        uiState = FinancialUiState(),
        onEvent = {}
    )
}
