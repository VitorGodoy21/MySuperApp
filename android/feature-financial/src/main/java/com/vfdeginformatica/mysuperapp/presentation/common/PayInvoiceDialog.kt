package com.vfdeginformatica.mysuperapp.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.domain.model.PaidInvoice
import java.time.LocalDate
import kotlin.math.roundToLong

/**
 * Dialog de pagamento de fatura: valor pré-preenchido com o total projetado (editável, em
 * centavos pela máscara de moeda) e data (padrão hoje). Confirma com `(valor, data)`.
 */
@Composable
fun PayInvoiceDialog(
    invoice: PaidInvoice,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, date: LocalDate) -> Unit,
) {
    val key = invoice.cardId + invoice.invoiceMonthKey
    var amountCents by remember(key) {
        mutableStateOf((invoice.projectedTotal * 100).roundToLong().toString().trimStart('0'))
    }
    var date by remember(key) { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val parsedAmount = CurrencyVisualTransformation.centsToDouble(amountCents)

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Pagar fatura") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${invoice.bank} • Fatura de ${monthLabel(invoice.invoiceMonth)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Valor projetado: ${formatCurrency(invoice.projectedTotal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amountCents,
                    onValueChange = { input ->
                        amountCents = input.filter { it.isDigit() }.trimStart('0').take(12)
                    },
                    label = { Text("Valor pago") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = remember { CurrencyVisualTransformation() },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                DateField(
                    date = date,
                    label = "Data do pagamento",
                    onClick = { showDatePicker = true },
                    enabled = !isSaving,
                )
                Text(
                    text = "O valor pago substitui o total da fatura no saldo do mês.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving && parsedAmount > 0.0,
                onClick = { onConfirm(parsedAmount, date) }
            ) { Text("Pagar") }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismiss) { Text("Cancelar") }
        }
    )

    if (showDatePicker) {
        FinancialDatePickerDialog(
            initialDate = date,
            onDateSelected = {
                date = it
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}
