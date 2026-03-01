package com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewTransactionScreen(
    uiState: NewTransactionUiState,
    onEvent: (NewTransactionEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var cardMenuExpanded by remember { mutableStateOf(false) }
    var paymentMethodMenuExpanded by remember { mutableStateOf(false) }

    val selectedCard = remember(uiState.cardId, uiState.availableCards) {
        uiState.availableCards?.find { it.id == uiState.cardId }
    }
    val selectedCardLabel = selectedCard?.bank ?: ""
    val availablePaymentMethods = selectedCard?.paymentMethods ?: emptyList()

    if (uiState.isLoadingCards || uiState.isLoadingCategories) {
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
            // Seletor de Tipo (Receita / Despesa)
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

            // 1. Campos Iniciais
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

            // 2. Seção de Cartão (Sempre visível para Despesa, ou dependendo da lógica de negócio)
            Text(
                text = "Cartão e Pagamento",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            ExposedDropdownMenuBox(
                expanded = cardMenuExpanded,
                onExpandedChange = { cardMenuExpanded = it },
            ) {
                OutlinedTextField(
                    value = selectedCardLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Escolha o Cartão") },
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
                                onEvent(NewTransactionEvent.CardIdChanged(card.id))
                                cardMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // 3. Método de Pagamento (Condicional se o cartão for selecionado)
            if (uiState.cardId.isNotBlank()) {
                if (availablePaymentMethods.size > 1) {
                    ExposedDropdownMenuBox(
                        expanded = paymentMethodMenuExpanded,
                        onExpandedChange = { paymentMethodMenuExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = uiState.paymentMethod,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Método de Pagamento") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentMethodMenuExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = paymentMethodMenuExpanded,
                            onDismissRequest = { paymentMethodMenuExpanded = false },
                        ) {
                            availablePaymentMethods.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method) },
                                    onClick = {
                                        onEvent(NewTransactionEvent.PaymentMethodChanged(method))
                                        paymentMethodMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else if (availablePaymentMethods.size == 1) {
                    // Se só tem um, exibe como campo informativo (já selecionado na ViewModel)
                    OutlinedTextField(
                        value = uiState.paymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Método de Pagamento") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false
                    )
                }

                // 4. Campo de Parcelas (Se o método for Crédito)
                if (uiState.paymentMethod == "CREDIT") {
                    OutlinedTextField(
                        value = uiState.installments,
                        onValueChange = { onEvent(NewTransactionEvent.InstallmentsChanged(it)) },
                        label = { Text("Número de Parcelas") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }

            // Campos Adicionais
            if (uiState.transactionType == TransactionType.EXPENSE) {
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

                // Categorias
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
                        uiState.availableCategories?.forEach { category ->
                            val isSelected = uiState.selectedCategories.any { it.id == category.id }
                            FilterChip(
                                selected = isSelected,
                                onClick = { onEvent(NewTransactionEvent.CategoryToggled(category.id)) },
                                label = { Text(category.title) }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { onEvent(NewTransactionEvent.SaveClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.isSubmitEnabled && !uiState.isLoading,
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
            selectedCategories = listOf()
        ),
        onEvent = {}
    )
}
