package com.vfdeginformatica.mysuperapp.presentation.screen.financial

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType

@Composable
fun TransactionFabMenu(
    expanded: Boolean,
    onNewTransaction: (TransactionType) -> Unit,
) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = isOpen) { isOpen = false }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isOpen) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        isOpen = false
                        onNewTransaction(TransactionType.EXPENSE)
                    },
                    icon = { Icon(Icons.Default.TrendingDown, contentDescription = null) },
                    text = { Text("Gasto") },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                ExtendedFloatingActionButton(
                    onClick = {
                        isOpen = false
                        onNewTransaction(TransactionType.INCOME)
                    },
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = null) },
                    text = { Text("Receita") },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
        ExtendedFloatingActionButton(
            expanded = expanded || isOpen,
            onClick = { isOpen = !isOpen },
            icon = {
                Icon(
                    imageVector = if (isOpen) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = if (isOpen) "Fechar opções" else "Criar nova transação",
                )
            },
            text = { Text(if (isOpen) "Fechar" else "Nova transação") },
        )
    }
}
