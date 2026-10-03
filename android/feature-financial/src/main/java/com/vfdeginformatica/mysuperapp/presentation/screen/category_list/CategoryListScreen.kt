package com.vfdeginformatica.mysuperapp.presentation.screen.category_list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
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
import com.vfdeginformatica.mysuperapp.domain.model.TransactionCategory
import com.vfdeginformatica.mysuperapp.presentation.screen.category_list.contract.CategoryListEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.category_list.contract.CategoryListUiState
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType

@Composable
fun CategoryListScreen(
    uiState: CategoryListUiState,
    onEvent: (CategoryListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )

            uiState.categories.isEmpty() -> Text(
                text = "Nenhuma categoria cadastrada.\nToque em \"Nova categoria\" para começar.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.expenseCategories.isNotEmpty()) {
                    item(key = "header_expense") {
                        SectionHeader("Despesas")
                    }
                    items(uiState.expenseCategories, key = { it.id }) { category ->
                        CategoryListItem(
                            category = category,
                            onClick = { onEvent(CategoryListEvent.CategoryClicked(category.id)) }
                        )
                    }
                }
                if (uiState.incomeCategories.isNotEmpty()) {
                    item(key = "header_income") {
                        SectionHeader("Receitas")
                    }
                    items(uiState.incomeCategories, key = { it.id }) { category ->
                        CategoryListItem(
                            category = category,
                            onClick = { onEvent(CategoryListEvent.CategoryClicked(category.id)) }
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { onEvent(CategoryListEvent.NewCategoryClicked) },
            icon = { Icon(Icons.Default.Add, contentDescription = "Nova categoria") },
            text = { Text("Nova categoria") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun CategoryListItem(
    category: TransactionCategory,
    onClick: () -> Unit,
) {
    val isIncome = category.type == TransactionType.INCOME

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
                imageVector = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = if (isIncome) "Receita" else "Despesa",
                tint = if (isIncome) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (category.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CategoryListScreenPreview() {
    CategoryListScreen(
        uiState = CategoryListUiState(
            categories = listOf(
                TransactionCategory("1", "Mercado", "Compras do mês", TransactionType.EXPENSE),
                TransactionCategory("2", "Transporte", "", TransactionType.EXPENSE),
                TransactionCategory("3", "Salário", "Pagamento mensal", TransactionType.INCOME),
            )
        ),
        onEvent = {}
    )
}
