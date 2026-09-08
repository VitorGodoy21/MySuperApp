package com.vfdeginformatica.mysuperapp.presentation.screen.financial

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.vfdeginformatica.mysuperapp.domain.model.Transaction
import com.vfdeginformatica.mysuperapp.presentation.common.CurrencyVisualTransformation
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialTab
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialUiState
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.TransactionTypeFilter
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToLong

private val ptBr: Locale = Locale.forLanguageTag("pt-BR")

private fun formatCurrency(value: Double): String =
    NumberFormat.getCurrencyInstance(ptBr).format(value)

private fun monthLabel(month: YearMonth): String =
    month.month.getDisplayName(TextStyle.FULL, ptBr)
        .replaceFirstChar { it.uppercase(ptBr) } + " " + month.year

@OptIn(ExperimentalMaterial3Api::class)
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
            when (uiState.selectedTab) {
                FinancialTab.MONTH -> {
                    MonthSelector(uiState, onEvent)
                    SummaryCard(uiState)
                    TypeFilterChips(uiState, onEvent)
                    FilterDropdowns(uiState, onEvent)
                    HorizontalDivider()
                    TransactionList(uiState, onEvent)
                }

                FinancialTab.PROJECTION -> {
                    ProjectionContent(uiState, onEvent)
                }
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
            selected = uiState.selectedTab == FinancialTab.PROJECTION,
            onClick = { onEvent(FinancialEvent.OnTabChanged(FinancialTab.PROJECTION)) },
            label = {
                Text(
                    "Fatura / Projeção",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
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
    val monthLabel = monthLabel(uiState.selectedMonth)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = { onEvent(FinancialEvent.OnPreviousMonth) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mês anterior")
        }
        Text(text = monthLabel, style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { onEvent(FinancialEvent.OnNextMonth) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próximo mês")
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
                color = if (uiState.balance >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Receitas", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = formatCurrency(uiState.totalIncome),
                        color = Color(0xFF2E7D32),
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
private fun TypeFilterChips(
    uiState: FinancialUiState,
    onEvent: (FinancialEvent) -> Unit
) {
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

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = uiState.recurringExpensesOnly,
            onClick = { onEvent(FinancialEvent.OnRecurringExpensesFilterToggled) },
            label = { Text("Só recorrentes", style = MaterialTheme.typography.labelMedium) }
        )
        if (uiState.recurringExpensesOnly) {
            Text(
                text = "Total recorrente: ${formatCurrency(uiState.monthRecurringExpenseTotal)}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdowns(
    uiState: FinancialUiState,
    onEvent: (FinancialEvent) -> Unit
) {
    var categoryExpanded by remember { mutableStateOf(false) }
    var cardExpanded by remember { mutableStateOf(false) }

    val categoryLabel = uiState.availableCategories
        .find { it.id == uiState.categoryFilterId }?.title ?: "Todas"
    val cardLabel = uiState.availableCards
        .find { it.id == uiState.cardFilterId }?.bank ?: "Todos"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = categoryLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Categoria") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Todas") },
                    onClick = {
                        onEvent(FinancialEvent.OnCategoryFilterChanged(null))
                        categoryExpanded = false
                    }
                )
                uiState.availableCategories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.title) },
                        onClick = {
                            onEvent(FinancialEvent.OnCategoryFilterChanged(category.id))
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = cardExpanded,
            onExpandedChange = { cardExpanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = cardLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Cartão") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = cardExpanded)
                },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = cardExpanded,
                onDismissRequest = { cardExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Todos") },
                    onClick = {
                        onEvent(FinancialEvent.OnCardFilterChanged(null))
                        cardExpanded = false
                    }
                )
                uiState.availableCards.forEach { card ->
                    DropdownMenuItem(
                        text = { Text(card.bank) },
                        onClick = {
                            onEvent(FinancialEvent.OnCardFilterChanged(card.id))
                            cardExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionList(
    uiState: FinancialUiState,
    onEvent: (FinancialEvent) -> Unit
) {
    if (!uiState.isLoading && uiState.filteredTransactions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Nenhuma transação no período",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        items(uiState.filteredTransactions, key = { it.id }) { transaction ->
            TransactionListItem(
                transaction = transaction,
                uiState = uiState,
                onClick = { onEvent(FinancialEvent.OnTransactionClicked(transaction)) }
            )
        }
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
                        text = "Recorrente",
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
private fun ProjectionContent(
    uiState: FinancialUiState,
    onEvent: (FinancialEvent) -> Unit
) {
    // Seletor do mês alvo da projeção
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = { onEvent(FinancialEvent.OnProjectionPreviousMonth) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mês anterior")
        }
        Text(
            text = monthLabel(uiState.projectionMonth),
            style = MaterialTheme.typography.titleMedium
        )
        IconButton(onClick = { onEvent(FinancialEvent.OnProjectionNextMonth) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próximo mês")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Saldo projetado
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "Saldo projetado", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = formatCurrency(uiState.projectedBalance),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.projectedBalance >= 0) Color(0xFF2E7D32)
                        else MaterialTheme.colorScheme.error
                    )
                    ProjectionLine("Receitas previstas", uiState.projectedIncome, Color(0xFF2E7D32))
                    ProjectionLine(
                        "Fatura de crédito",
                        -uiState.projectedInvoiceTotal,
                        MaterialTheme.colorScheme.error
                    )
                    if (uiState.recurringDebitExpense > 0) {
                        ProjectionLine(
                            "Despesas recorrentes (débito)",
                            -uiState.recurringDebitExpense,
                            MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Fatura por cartão de crédito
        items(uiState.invoiceByCard.entries.toList(), key = { "invoice-${it.key}" }) { entry ->
            val card = uiState.availableCards.find { it.id == entry.key }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fatura ${card?.bank ?: "Cartão"}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = formatCurrency(entry.value),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    card?.let {
                        val closing = if (it.closingDay > 0) "fecha dia ${it.closingDay}" else null
                        val due = if (it.invoiceDueDate > 0) "vence dia ${it.invoiceDueDate}" else null
                        val label = listOfNotNull(closing, due).joinToString(" • ")
                        if (label.isNotEmpty()) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Receitas recorrentes previstas (ex.: salário) — clicáveis para ajuste
        if (uiState.recurringIncomeTransactions.isNotEmpty()) {
            item {
                Text(
                    text = "Receitas previstas (toque para ajustar)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(
                uiState.recurringIncomeTransactions,
                key = { "recurring-${it.id}" }
            ) { transaction ->
                TransactionListItem(
                    transaction = transaction,
                    uiState = uiState,
                    onClick = { onEvent(FinancialEvent.OnTransactionClicked(transaction)) }
                )
            }
        }

        // Itens da fatura projetada
        if (uiState.projectedInvoiceItems.isNotEmpty()) {
            item {
                Text(
                    text = "Compras na fatura",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(uiState.projectedInvoiceItems, key = { "item-${it.id}" }) { transaction ->
                TransactionListItem(
                    transaction = transaction,
                    uiState = uiState,
                    onClick = { onEvent(FinancialEvent.OnTransactionClicked(transaction)) }
                )
            }
        }

        if (!uiState.isLoadingProjection &&
            uiState.projectedInvoiceItems.isEmpty() &&
            uiState.recurringTransactions.isEmpty()
        ) {
            item {
                Text(
                    text = "Nenhuma fatura ou recorrência para o período",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                )
            }
        }
    }
}

@Composable
private fun ProjectionLine(label: String, value: Double, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = formatCurrency(value),
            color = color,
            fontWeight = FontWeight.SemiBold
        )
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
                        text = "Transação recorrente: o novo valor ajusta a projeção dos próximos meses.",
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

@Preview(showBackground = true)
@Composable
fun FinancialScreenPreview() {
    FinancialScreen(
        uiState = FinancialUiState(),
        onEvent = {}
    )
}