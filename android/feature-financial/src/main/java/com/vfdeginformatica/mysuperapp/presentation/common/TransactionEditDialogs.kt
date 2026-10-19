package com.vfdeginformatica.mysuperapp.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.domain.model.RecurrenceEditScope
import com.vfdeginformatica.mysuperapp.domain.model.Transaction
import com.vfdeginformatica.mysuperapp.domain.model.TransactionCategory
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType
import kotlin.math.roundToLong

/**
 * Dialog de edição de título/valor/categorias (com exclusão), compartilhado pelas telas
 * financeiras. [availableCategories] é filtrada pelo tipo da transação; ids selecionados que
 * não existem mais na lista são descartados ao confirmar.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    availableCategories: List<TransactionCategory>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, categoryIds: List<String>) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember(transaction.id) { mutableStateOf(transaction.title) }
    // Valor em centavos (apenas dígitos), exibido pela máscara de moeda
    var amountCents by remember(transaction.id) {
        mutableStateOf(
            (transaction.amount * 100).roundToLong().toString().trimStart('0')
        )
    }
    var selectedCategoryIds by remember(transaction.id) {
        mutableStateOf(transaction.categoryIds.toSet())
    }
    val parsedAmount = CurrencyVisualTransformation.centsToDouble(amountCents)
    val categoriesForType = remember(availableCategories, transaction.type) {
        availableCategories.filter { it.type == transaction.type }
    }
    val isInstallment = transaction.installmentGroupId != null

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Editar transação") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Categorias",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (categoriesForType.isEmpty()) {
                        val typeLabel = when (transaction.type) {
                            TransactionType.INCOME -> "receita"
                            TransactionType.EXPENSE -> "despesa"
                        }
                        Text(
                            text = "Nenhuma categoria de $typeLabel cadastrada",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            categoriesForType.forEach { category ->
                                val isSelected = category.id in selectedCategoryIds
                                FilterChip(
                                    selected = isSelected,
                                    enabled = !isSaving,
                                    onClick = {
                                        selectedCategoryIds =
                                            if (isSelected) selectedCategoryIds - category.id
                                            else selectedCategoryIds + category.id
                                    },
                                    label = { Text(category.title) }
                                )
                            }
                        }
                    }
                    if (transaction.isRecurring) {
                        Text(
                            text = "As categorias valem para toda a recorrência.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (isInstallment) {
                        Text(
                            text = "As categorias serão aplicadas a todas as parcelas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
                    // Mantém a ordem da lista e descarta ids de categorias inexistentes/outro tipo
                    val categoryIds = categoriesForType
                        .map { it.id }
                        .filter { it in selectedCategoryIds }
                    onConfirm(title.trim(), parsedAmount, categoryIds)
                }
            ) {
                Text(if (isSaving) "Salvando..." else "Salvar")
            }
        },
        dismissButton = {
            Row {
                TextButton(enabled = !isSaving, onClick = onDelete) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
                TextButton(enabled = !isSaving, onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}

/** Escolha do escopo (só este mês / este e os próximos) ao editar ou excluir uma recorrente. */
@Composable
fun RecurrenceScopeDialog(
    transaction: Transaction,
    isDelete: Boolean,
    isSaving: Boolean,
    onScope: (RecurrenceEditScope) -> Unit,
    onDismiss: () -> Unit
) {
    val monthText = transaction.occurrenceMonth?.let { monthLabel(it) } ?: "este mês"

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
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
                    onClick = { onScope(RecurrenceEditScope.THIS_MONTH) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Somente $monthText")
                }
                TextButton(
                    enabled = !isSaving,
                    onClick = { onScope(RecurrenceEditScope.THIS_AND_FUTURE) },
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
            TextButton(enabled = !isSaving, onClick = onDismiss) {
                Text(if (isSaving) "Salvando..." else "Cancelar")
            }
        }
    )
}
