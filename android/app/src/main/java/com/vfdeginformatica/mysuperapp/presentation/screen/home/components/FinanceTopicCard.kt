package com.vfdeginformatica.mysuperapp.presentation.screen.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.presentation.common.balanceColor
import com.vfdeginformatica.mysuperapp.presentation.common.formatCurrency
import com.vfdeginformatica.mysuperapp.presentation.screen.home.contract.FinanceSectionState

private const val MASK = "••••"

@Composable
fun FinanceTopicCard(
    state: FinanceSectionState,
    onOpen: () -> Unit,
    onToggleReveal: () -> Unit,
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onRetry: () -> Unit,
) {
    val colors = HomeColors.finance()
    HomeTopicCard(
        title = "Finanças",
        subtitle = "Resumo do mês",
        icon = Icons.Default.AccountBalanceWallet,
        colors = colors,
        onHeaderClick = onOpen,
        headerTrailing = {
            IconButton(onClick = onToggleReveal) {
                Icon(
                    imageVector = if (state.isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (state.isRevealed) "Ocultar valores" else "Mostrar valores",
                    tint = colors.accent,
                )
            }
        },
    ) {
        val summary = state.summary
        if (summary != null) {
            val revealed = state.isRevealed
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HomeMetric(
                    label = "Saldo do mês",
                    value = if (revealed) formatCurrency(summary.balance) else MASK,
                    valueColor = if (revealed) balanceColor(summary.balance) else null,
                    modifier = Modifier.weight(1f),
                )
                HomeMetric(
                    label = "Gastos do mês",
                    value = if (revealed) formatCurrency(summary.cashExpense) else MASK,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        HomeSectionStatus(isLoading = state.isLoading, error = state.error, onRetry = onRetry)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeShortcutChip("Adicionar gasto", Icons.Default.ArrowDownward, colors, onAddExpense)
            HomeShortcutChip("Adicionar receita", Icons.Default.ArrowUpward, colors, onAddIncome)
        }
    }
}
