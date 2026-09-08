package com.vfdeginformatica.mysuperapp.presentation.screen.card_detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.presentation.common.CurrencyVisualTransformation
import com.vfdeginformatica.mysuperapp.presentation.screen.card_detail.contract.CardDetailEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.card_detail.contract.CardDetailUiState

@Composable
fun CardDetailScreen(
    uiState: CardDetailUiState,
    onEvent: (CardDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currencyMask = remember { CurrencyVisualTransformation() }

    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = uiState.bank,
            onValueChange = { onEvent(CardDetailEvent.BankChanged(it)) },
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
                onClick = { onEvent(CardDetailEvent.MethodToggled("DEBIT")) },
                label = { Text("Débito") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = uiState.paymentMethods.contains("CREDIT"),
                onClick = { onEvent(CardDetailEvent.MethodToggled("CREDIT")) },
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
                onValueChange = { onEvent(CardDetailEvent.FinalCodeChanged(it)) },
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
                    onValueChange = { onEvent(CardDetailEvent.ClosingDayChanged(it)) },
                    label = { Text("Dia de fechamento") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.invoiceDueDate,
                    onValueChange = { onEvent(CardDetailEvent.InvoiceDueDateChanged(it)) },
                    label = { Text("Dia de vencimento") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }

        Button(
            onClick = { onEvent(CardDetailEvent.SaveClicked) },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.isSubmitEnabled && !uiState.isSaving,
        ) {
            Text(if (uiState.isSaving) "Salvando..." else "Salvar alterações")
        }

        HorizontalDivider()

        Text(
            text = "Adicionar saldo",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = uiState.balanceToAdd,
            onValueChange = { onEvent(CardDetailEvent.BalanceToAddChanged(it)) },
            label = { Text("Valor") },
            supportingText = { Text("Vira uma receita editável associada a este cartão") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = currencyMask,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedButton(
            onClick = { onEvent(CardDetailEvent.AddBalanceClicked) },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.isAddBalanceEnabled,
        ) {
            Text(if (uiState.isAddingBalance) "Adicionando..." else "Adicionar saldo")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CardDetailScreenPreview() {
    CardDetailScreen(
        uiState = CardDetailUiState(
            isLoading = false,
            bank = "Nubank",
            paymentMethods = setOf("CREDIT", "DEBIT"),
            finalCode = "1234",
            closingDay = "6",
            invoiceDueDate = "12",
            isSubmitEnabled = true
        ),
        onEvent = {}
    )
}
