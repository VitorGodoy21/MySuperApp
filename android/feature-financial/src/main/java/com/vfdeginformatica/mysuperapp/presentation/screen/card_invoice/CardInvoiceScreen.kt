package com.vfdeginformatica.mysuperapp.presentation.screen.card_invoice

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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.data.mapper.toModel
import com.vfdeginformatica.mysuperapp.domain.model.Transaction
import com.vfdeginformatica.mysuperapp.domain.model.hasOverrideForOccurrence
import com.vfdeginformatica.mysuperapp.presentation.common.EditTransactionDialog
import com.vfdeginformatica.mysuperapp.presentation.common.RecurrenceScopeDialog
import com.vfdeginformatica.mysuperapp.presentation.common.expenseColor
import com.vfdeginformatica.mysuperapp.presentation.common.formatCurrency
import com.vfdeginformatica.mysuperapp.presentation.common.monthLabel
import com.vfdeginformatica.mysuperapp.presentation.common.ptBr
import com.vfdeginformatica.mysuperapp.presentation.screen.card_invoice.contract.CardInvoiceEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.card_invoice.contract.CardInvoiceUiState
import com.vfdeginformatica.mysuperapp.presentation.screen.card_invoice.contract.installmentPurchaseTotal
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.PendingRecurringAction
import java.text.SimpleDateFormat

@Composable
fun CardInvoiceScreen(
    uiState: CardInvoiceUiState,
    onEvent: (CardInvoiceEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
        ) {
            item { InvoiceHeaderCard(uiState) }

            if (!uiState.isLoading && uiState.errorMessage == null && uiState.items.isEmpty()) {
                item {
                    Text(
                        text = "Nenhum lançamento nesta fatura",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }
            }

            items(uiState.items, key = { "${it.id}-${it.occurrenceMonth}" }) { transaction ->
                InvoiceItem(
                    transaction = transaction,
                    uiState = uiState,
                    onClick = { onEvent(CardInvoiceEvent.OnTransactionClicked(transaction)) }
                )
            }
        }

        if (uiState.isLoading) {
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
            availableCategories = uiState.availableCategories.map { it.toModel() },
            isSaving = uiState.isSavingEdit,
            onDismiss = { onEvent(CardInvoiceEvent.OnEditDismiss) },
            onConfirm = { title, amount, categoryIds ->
                onEvent(CardInvoiceEvent.OnEditConfirm(title, amount, categoryIds))
            },
            onDelete = { onEvent(CardInvoiceEvent.OnDeleteTransaction) }
        )
        uiState.pendingRecurringAction?.let { action ->
            RecurrenceScopeDialog(
                transaction = transaction,
                isDelete = action is PendingRecurringAction.Delete,
                isSaving = uiState.isSavingEdit,
                onScope = { onEvent(CardInvoiceEvent.OnRecurringScopeConfirm(it)) },
                onDismiss = { onEvent(CardInvoiceEvent.OnRecurringScopeDismiss) }
            )
        }
    }
}

@Composable
private fun InvoiceHeaderCard(uiState: CardInvoiceUiState) {
    val card = uiState.card
    val cardTitle = buildString {
        append(card?.bank ?: "Cartão")
        card?.finalCode?.takeIf { it.isNotBlank() }?.let { append(" • final $it") }
    }
    val closing = card?.takeIf { it.closingDay > 0 }?.let { "fecha dia ${it.closingDay}" }
    val due = card?.takeIf { it.invoiceDueDate > 0 }?.let { "vence dia ${it.invoiceDueDate}" }
    val info = listOfNotNull(closing, due).joinToString(" • ")

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = cardTitle, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Fatura de ${monthLabel(uiState.invoiceMonth)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatCurrency(uiState.total),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = expenseColor
            )
            val meta = listOfNotNull(
                info.takeIf { it.isNotEmpty() },
                "${uiState.items.size} lançamento${if (uiState.items.size == 1) "" else "s"}"
            ).joinToString(" • ")
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (uiState.items.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                SubtotalLine("Parceladas", uiState.installmentsTotal)
                SubtotalLine("À vista", uiState.singleTotal)
                SubtotalLine("Recorrentes", uiState.recurringTotal)
            }
        }
    }
}

@Composable
private fun SubtotalLine(label: String, value: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = formatCurrency(value),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun InvoiceItem(
    transaction: Transaction,
    uiState: CardInvoiceUiState,
    onClick: () -> Unit
) {
    val categories = transaction.categoryIds.mapNotNull { id ->
        uiState.availableCategories.find { it.id == id }?.title
    }
    val dateLabel = transaction.date?.toDate()?.let {
        SimpleDateFormat("dd/MM/yyyy", ptBr).format(it)
    }
    val details = buildList {
        dateLabel?.let { add(it) }
        if (categories.isNotEmpty()) add(categories.joinToString(", "))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                    text = formatCurrency(transaction.amount),
                    color = expenseColor,
                    fontWeight = FontWeight.Bold
                )
                val purchaseTotal = transaction.installmentPurchaseTotal
                val tag = when {
                    transaction.isRecurring ->
                        if (transaction.hasOverrideForOccurrence) "Recorrente • ajustado" else "Recorrente"

                    purchaseTotal != null -> {
                        val number = transaction.installmentNumber
                        val parcel = if (number != null) {
                            "Parcela $number/${transaction.installments}"
                        } else {
                            "${transaction.installments}x"
                        }
                        "$parcel • total ${formatCurrency(purchaseTotal)}"
                    }

                    else -> null
                }
                tag?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
