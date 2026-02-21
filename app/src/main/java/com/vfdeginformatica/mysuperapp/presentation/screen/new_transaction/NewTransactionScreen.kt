package com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.NewTransactionEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.NewTransactionUiState
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTransactionScreen(
    uiState: NewTransactionUiState,
    onEvent: (NewTransactionEvent) -> Unit,
    modifier: Modifier = Modifier,
) {

    var cardMenuExpanded by remember { mutableStateOf(false) }

    val selectedCardLabel = remember(uiState.cardId, uiState.availableCards) {
        uiState.availableCards?.firstOrNull() { it.id == uiState.cardId }?.bank ?: ""
    }

    if (uiState.isLoadingCards) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Selector de Tipo
            Text(text = "Tipo de Transação", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.transactionType == TransactionType.INCOME,
                    onClick = { onEvent(NewTransactionEvent.TypeChanged(TransactionType.INCOME)) },
                    label = { Text("Receita") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = uiState.transactionType == TransactionType.EXPENSE,
                    onClick = { onEvent(NewTransactionEvent.TypeChanged(TransactionType.EXPENSE)) },
                    label = { Text("Despesa") },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            // Campos Comuns
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { onEvent(NewTransactionEvent.TitleChanged(it)) },
                label = { Text("Título") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.amount,
                onValueChange = { onEvent(NewTransactionEvent.AmountChanged(it)) },
                label = { Text("Valor") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("R$ ") },
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { onEvent(NewTransactionEvent.DescriptionChanged(it)) },
                label = { Text("Descrição (Opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            ExposedDropdownMenuBox(
                expanded = cardMenuExpanded,
                onExpandedChange = { cardMenuExpanded = it },
            ) {
                OutlinedTextField(
                    value = selectedCardLabel,
                    onValueChange = { /* leitura apenas; seleção pelo menu */ },
                    readOnly = true,
                    label = { Text("Cartão") },
                    isError = uiState.errorMessage != null,
                    supportingText = {
                        uiState.errorMessage?.let { Text(it) }
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cardMenuExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = cardMenuExpanded,
                    onDismissRequest = { cardMenuExpanded = false },
                ) {
                    uiState.availableCards?.forEach { card ->
                        DropdownMenuItem(
                            text = { Text(card.bank) },
                            onClick = {
                                onEvent(NewTransactionEvent.CardIdChanged(card.id ?: ""))
                                cardMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Categorias (Visual de Tags/Chips)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Categorias",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    uiState.availableCategories.forEach { category ->
                        val isSelected = uiState.selectedCategories.contains(category)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onEvent(NewTransactionEvent.CategoryToggled(category)) },
                            label = { Text(category) }
                        )
                    }
                }
            }

            // Campos Específicos para Despesa (Expense)
            if (uiState.transactionType == TransactionType.EXPENSE) {
                Text(
                    text = "Detalhes da Despesa",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = uiState.paymentMethod,
                    onValueChange = { onEvent(NewTransactionEvent.PaymentMethodChanged(it)) },
                    label = { Text("Método de Pagamento") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.invoiceMonth,
                    onValueChange = { onEvent(NewTransactionEvent.InvoiceMonthChanged(it)) },
                    label = { Text("Mês da Fatura") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Transação Recorrente?")
                    Switch(
                        checked = uiState.isRecurring,
                        onCheckedChange = { onEvent(NewTransactionEvent.RecurringChanged(it)) }
                    )
                }
            }

            Button(
                onClick = { onEvent(NewTransactionEvent.SaveClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && uiState.title.isNotBlank() && uiState.amount.isNotBlank()
            ) {
                Text(if (uiState.isLoading) "Salvando..." else "Salvar Transação")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NewTransactionScreenPreview() {
    NewTransactionScreen(
        uiState = NewTransactionUiState(
            transactionType = TransactionType.EXPENSE,
            isLoadingCards = false,
            selectedCategories = listOf(
                "Lazer",
                "Alimentação"
            ) // Exemplo de seleção múltipla no preview
        ),
        onEvent = {}
    )
}