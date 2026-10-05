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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.domain.model.RecurrenceEditScope
import com.vfdeginformatica.mysuperapp.domain.model.Transaction
import com.vfdeginformatica.mysuperapp.domain.model.hasOverrideForOccurrence
import com.vfdeginformatica.mysuperapp.presentation.common.CurrencyVisualTransformation
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FilterGroupBy
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialTab
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialUiState
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.OTHERS_KEY
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.PaymentMethodFilter
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.PendingRecurringAction
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.TransactionTypeFilter
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToLong

private val ptBr: Locale = Locale.forLanguageTag("pt-BR")
private val incomeColor = Color(0xFF2E7D32)

private fun formatCurrency(value: Double): String =
    NumberFormat.getCurrencyInstance(ptBr).format(value)

private fun formatPercent(value: Double): String = "%.0f%%".format(ptBr, value)

private fun monthLabel(month: YearMonth): String =
    month.month.getDisplayName(TextStyle.FULL, ptBr)
        .replaceFirstChar { it.uppercase(ptBr) } + " " + month.year

@Composable
fun FinancialScreen(
    uiState: FinancialUiState,
    onEvent: (FinancialEvent) -> Unit,
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
                FinancialTab.MONTH -> MonthContent(uiState, onEvent)
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

    uiState.editingTransaction?.let { transaction ->
        EditTransactionDialog(
            transaction = transaction,
            isSaving = uiState.isSavingEdit,
            onEvent = onEvent
        )
        uiState.pendingRecurringAction?.let { action ->
            RecurrenceScopeDialog(
                transaction = transaction,
                action = action,
                isSaving = uiState.isSavingEdit,
                onEvent = onEvent
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
    onEvent: (FinancialEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item { SummaryCard(uiState) }

        if (uiState.invoiceByCard.isNotEmpty()) {
            item { InvoiceByCardCard(uiState) }
        }

        if (uiState.totalExpense > 0.0) {
            item { PaymentMethodBreakdownCard(uiState) }
            item { ExpenseNatureCard(uiState) }
            if (uiState.expenseByCategory.isNotEmpty()) {
                item { TopCategoriesCard(uiState) }
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
private fun SummaryCard(uiState: FinancialUiState) {
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
                color = if (uiState.balance >= 0) incomeColor else MaterialTheme.colorScheme.error
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
                        text = formatCurrency(uiState.totalExpense),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
private fun InvoiceByCardCard(uiState: FinancialUiState) {
    SectionCard(
        title = "Fatura por cartão",
        subtitle = "Compras e parcelas que vencem neste mês, incluindo recorrentes de crédito"
    ) {
        uiState.invoiceByCard.entries.forEach { entry ->
            val card = uiState.availableCards.find { it.id == entry.key }
            val closing = card?.takeIf { it.closingDay > 0 }?.let { "fecha dia ${it.closingDay}" }
            val due = card?.takeIf { it.invoiceDueDate > 0 }?.let { "vence dia ${it.invoiceDueDate}" }
            Column {
                AmountLine(
                    label = card?.bank ?: "Cartão",
                    value = entry.value,
                    color = MaterialTheme.colorScheme.error
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
        }
        if (uiState.invoiceByCard.size > 1) {
            HorizontalDivider()
            AmountLine(
                label = "Total das faturas",
                value = uiState.projectedInvoiceTotal,
                color = MaterialTheme.colorScheme.error
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
            color = MaterialTheme.colorScheme.error,
            trailing = formatPercent(uiState.creditExpense / total * 100)
        )
        ProportionBar(uiState.creditExpense / total)
        AmountLine(
            label = "Débito",
            value = uiState.debitExpense,
            color = MaterialTheme.colorScheme.error,
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
            color = MaterialTheme.colorScheme.error,
            trailing = formatPercent(uiState.recurringExpense / total * 100)
        )
        AmountLine(
            label = "Parceladas",
            value = uiState.installmentExpense,
            color = MaterialTheme.colorScheme.error,
            trailing = formatPercent(uiState.installmentExpense / total * 100)
        )
        AmountLine(
            label = "Avulsas",
            value = uiState.singleExpense,
            color = MaterialTheme.colorScheme.error,
            trailing = formatPercent(uiState.singleExpense / total * 100)
        )
    }
}

@Composable
private fun TopCategoriesCard(uiState: FinancialUiState) {
    SectionCard(
        title = "Top categorias de despesa",
        subtitle = "Transações com várias categorias contam em cada uma"
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
                color = MaterialTheme.colorScheme.error,
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
                color = MaterialTheme.colorScheme.error
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
                FilterLabel("Categorias")
                if (uiState.categoryFilterIds.isNotEmpty()) {
                    TextButton(onClick = { onEvent(FinancialEvent.OnCategoryFilterCleared) }) {
                        Text("Limpar")
                    }
                }
            }
            if (uiState.categoriesForFilter.isEmpty()) {
                Text(
                    text = "Nenhuma categoria para o tipo selecionado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
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
        AmountLine("Despesas", uiState.filteredExpense, MaterialTheme.colorScheme.error)
        HorizontalDivider()
        AmountLine(
            label = "Saldo",
            value = uiState.filteredBalance,
            color = if (uiState.filteredBalance >= 0) incomeColor else MaterialTheme.colorScheme.error
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
    val amountColor = if (isIncome) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
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

@Composable
private fun EditTransactionDialog(
    transaction: Transaction,
    isSaving: Boolean,
    onEvent: (FinancialEvent) -> Unit
) {
    var title by remember(transaction.id) { mutableStateOf(transaction.title) }
    // Valor em centavos (apenas dígitos), exibido pela máscara de moeda
    var amountCents by remember(transaction.id) {
        mutableStateOf(
            (transaction.amount * 100).roundToLong().toString().trimStart('0')
        )
    }
    val parsedAmount = CurrencyVisualTransformation.centsToDouble(amountCents)

    AlertDialog(
        onDismissRequest = { if (!isSaving) onEvent(FinancialEvent.OnEditDismiss) },
        title = { Text("Editar transação") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountCents,
                    onValueChange = { input ->
                        amountCents = input.filter { it.isDigit() }.trimStart('0').take(12)
                    },
                    label = { Text("Valor") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = remember { CurrencyVisualTransformation() },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (transaction.isRecurring) {
                    Text(
                        text = "Transação recorrente: ao salvar ou excluir, você escolhe se a " +
                                "alteração vale só para este mês ou para este e os próximos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving && title.isNotBlank() && parsedAmount > 0.0,
                onClick = {
                    onEvent(FinancialEvent.OnEditConfirm(title.trim(), parsedAmount))
                }
            ) {
                Text(if (isSaving) "Salvando..." else "Salvar")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    enabled = !isSaving,
                    onClick = { onEvent(FinancialEvent.OnDeleteTransaction) }
                ) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
                TextButton(
                    enabled = !isSaving,
                    onClick = { onEvent(FinancialEvent.OnEditDismiss) }
                ) {
                    Text("Cancelar")
                }
            }
        }
    )
}

@Composable
private fun RecurrenceScopeDialog(
    transaction: Transaction,
    action: PendingRecurringAction,
    isSaving: Boolean,
    onEvent: (FinancialEvent) -> Unit
) {
    val isDelete = action is PendingRecurringAction.Delete
    val monthText = transaction.occurrenceMonth?.let { monthLabel(it) } ?: "este mês"

    AlertDialog(
        onDismissRequest = { if (!isSaving) onEvent(FinancialEvent.OnRecurringScopeDismiss) },
        title = { Text(if (isDelete) "Excluir recorrente" else "Atualizar recorrente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isDelete) {
                        "\"${transaction.title}\" é uma transação recorrente. O que deseja excluir?"
                    } else {
                        "\"${transaction.title}\" é uma transação recorrente. Onde aplicar a alteração?"
                    }
                )
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        onEvent(FinancialEvent.OnRecurringScopeConfirm(RecurrenceEditScope.THIS_MONTH))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Somente $monthText")
                }
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        onEvent(
                            FinancialEvent.OnRecurringScopeConfirm(RecurrenceEditScope.THIS_AND_FUTURE)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Este e os próximos meses",
                        color = if (isDelete) MaterialTheme.colorScheme.error else Color.Unspecified
                    )
                }
                Text(
                    text = "Os meses anteriores são mantidos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                enabled = !isSaving,
                onClick = { onEvent(FinancialEvent.OnRecurringScopeDismiss) }
            ) {
                Text(if (isSaving) "Salvando..." else "Cancelar")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun FinancialScreenPreview() {
    FinancialScreen(
        uiState = FinancialUiState(),
        onEvent = {}
    )
}
