package com.vfdeginformatica.mysuperapp.presentation.screen.card_list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.data.remote.dto.CardDto
import com.vfdeginformatica.mysuperapp.presentation.screen.card_list.contract.CardListEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.card_list.contract.CardListUiState

@Composable
fun CardListScreen(
    uiState: CardListUiState,
    onEvent: (CardListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )

            uiState.cards.isEmpty() -> Text(
                text = "Nenhum cartão cadastrado.\nToque em \"Novo cartão\" para começar.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.cards, key = { it.id }) { card ->
                    CardListItem(
                        card = card,
                        onClick = { onEvent(CardListEvent.CardClicked(card.id)) }
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { onEvent(CardListEvent.NewCardClicked) },
            icon = { Icon(Icons.Default.Add, contentDescription = "Novo cartão") },
            text = { Text("Novo cartão") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

@Composable
private fun CardListItem(
    card: CardDto,
    onClick: () -> Unit,
) {
    val hasCredit = card.paymentMethods.contains("CREDIT")
    val methodsLabel = card.paymentMethods.joinToString(" • ") { method ->
        when (method) {
            "CREDIT" -> "Crédito"
            "DEBIT" -> "Débito"
            else -> method
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CreditCard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = card.bank,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (card.finalCode.isNotBlank()) {
                        Text(
                            text = "•••• ${card.finalCode}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = methodsLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (hasCredit && card.closingDay > 0) {
                    Text(
                        text = "Fecha dia ${card.closingDay} • vence dia ${card.invoiceDueDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CardListScreenPreview() {
    CardListScreen(
        uiState = CardListUiState(
            cards = listOf(
                CardDto(
                    id = "1",
                    bank = "Nubank",
                    finalCode = "1234",
                    paymentMethods = listOf("CREDIT", "DEBIT"),
                    closingDay = 6,
                    invoiceDueDate = 12
                ),
                CardDto(id = "2", bank = "Caixa", paymentMethods = listOf("DEBIT"))
            )
        ),
        onEvent = {}
    )
}
