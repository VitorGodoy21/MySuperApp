package com.vfdeginformatica.mysuperapp.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import kotlin.math.roundToLong

/** Dialog de edição de título/valor (com exclusão), compartilhado pelas telas financeiras. */
@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double) -> Unit,
    onDelete: () -> Unit
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
        onDismissRequest = { if (!isSaving) onDismiss() },
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
                onClick = { onConfirm(title.trim(), parsedAmount) }
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
