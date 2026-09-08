package com.vfdeginformatica.mysuperapp.presentation.screen.new_card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.presentation.common.CurrencyVisualTransformation
import com.vfdeginformatica.mysuperapp.presentation.screen.new_card.contract.NewCardEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.new_card.contract.NewCardUiState

@Composable
fun NewCardScreen(
    uiState: NewCardUiState,
    onEvent: (NewCardEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currencyMask = remember { CurrencyVisualTransformation() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = uiState.bank,
            onValueChange = { onEvent(NewCardEvent.BankChanged(it)) },
            label = { Text("Banco") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text(text = "Métodos de pagamento", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.paymentMethods.contains("DEBIT"),
                onClick = { onEvent(NewCardEvent.MethodToggled("DEBIT")) },
                label = { Text("Débito") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = uiState.paymentMethods.contains("CREDIT"),
                onClick = { onEvent(NewCardEvent.MethodToggled("CREDIT")) },
                label = { Text("Crédito") },
                modifier = Modifier.weight(1f)
            )
        }

        if (uiState.hasCredit) {
            HorizontalDivider()
            Text(
                text = "Dados do crédito",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = uiState.finalCode,
                onValueChange = { onEvent(NewCardEvent.FinalCodeChanged(it)) },
                label = { Text("Final do cartão (4 dígitos)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.closingDay,
                    onValueChange = { onEvent(NewCardEvent.ClosingDayChanged(it)) },
                    label = { Text("Dia de fechamento") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.invoiceDueDate,
                    onValueChange = { onEvent(NewCardEvent.InvoiceDueDateChanged(it)) },
                    label = { Text("Dia de vencimento") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = uiState.currentInvoice,
                onValueChange = { onEvent(NewCardEvent.CurrentInvoiceChanged(it)) },
                label = { Text("Fatura atual (opcional)") },
                supportingText = { Text("Fatura em aberto — entra na projeção e pode ser editada depois") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = currencyMask,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        HorizontalDivider()

        OutlinedTextField(
            value = uiState.initialBalance,
            onValueChange = { onEvent(NewCardEvent.InitialBalanceChanged(it)) },
            label = { Text("Saldo inicial (opcional)") },
            supportingText = { Text("Saldo atual do cartão — vira uma receita editável") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = currencyMask,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = { onEvent(NewCardEvent.SaveClicked) },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.isSubmitEnabled && !uiState.isLoading,
        ) {
            Text(if (uiState.isLoading) "Salvando..." else "Salvar Cartão")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NewCardScreenPreview() {
    NewCardScreen(
        uiState = NewCardUiState(paymentMethods = setOf("CREDIT", "DEBIT")),
        onEvent = {}
    )
}
