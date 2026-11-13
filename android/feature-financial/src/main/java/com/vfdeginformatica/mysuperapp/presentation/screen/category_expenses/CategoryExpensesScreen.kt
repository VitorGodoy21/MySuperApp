package com.vfdeginformatica.mysuperapp.presentation.screen.category_expenses

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.presentation.common.expenseColor
import com.vfdeginformatica.mysuperapp.presentation.common.formatCurrency
import com.vfdeginformatica.mysuperapp.presentation.common.formatPercent
import com.vfdeginformatica.mysuperapp.presentation.common.monthLabel
import com.vfdeginformatica.mysuperapp.presentation.screen.category_expenses.contract.CategoryExpenseReport
import com.vfdeginformatica.mysuperapp.presentation.screen.category_expenses.contract.CategoryExpenseTotal
import com.vfdeginformatica.mysuperapp.presentation.screen.category_expenses.contract.CategoryExpensesUiState

@Composable
fun CategoryExpensesScreen(
    uiState: CategoryExpensesUiState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            uiState.report.categories.isEmpty() -> Text(
                text = "Nenhuma despesa cadastrada em ${monthLabel(uiState.month)}.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            )
            else -> {
                val colors = categoryColors()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Despesas de ${monthLabel(uiState.month)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    item {
                        ChartCard(
                            report = uiState.report,
                            colors = colors
                        )
                    }
                    item {
                        Text(
                            text = "Todas as categorias",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    itemsIndexed(
                        items = uiState.report.categories,
                        key = { _, item -> item.categoryId?.let { "category:$it" } ?: "uncategorized" }
                    ) { index, item ->
                        CategoryExpenseRow(
                            item = item,
                            rank = index + 1,
                            color = colors[index % colors.size]
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartCard(
    report: CategoryExpenseReport,
    colors: List<Color>,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Distribuição das despesas", style = MaterialTheme.typography.titleSmall)
            Text(
                text = formatCurrency(report.total),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = expenseColor
            )
            PieChart(
                slices = report.chartCategories,
                colors = colors,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
            report.chartCategories.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(colors[index % colors.size], CircleShape)
                    )
                    Text(
                        text = item.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${formatCurrency(item.total)} • ${formatPercent(item.percent)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PieChart(
    slices: List<CategoryExpenseTotal>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawPieSlices(slices, colors)
    }
}

private fun DrawScope.drawPieSlices(
    slices: List<CategoryExpenseTotal>,
    colors: List<Color>,
) {
    if (slices.isEmpty()) return
    val diameter = minOf(size.width, size.height)
    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
    val chartSize = Size(diameter, diameter)
    var startAngle = -90f

    slices.forEachIndexed { index, item ->
        val sweepAngle = (item.total / slices.sumOf { it.total } * 360.0).toFloat()
        drawArc(
            color = colors[index % colors.size],
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = true,
            topLeft = topLeft,
            size = chartSize
        )
        startAngle += sweepAngle
    }
}

@Composable
private fun CategoryExpenseRow(
    item: CategoryExpenseTotal,
    rank: Int,
    color: Color,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "$rank.",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, CircleShape)
            )
            Text(
                text = item.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(item.total),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = expenseColor
                )
                Text(
                    text = formatPercent(item.percent),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun categoryColors(): List<Color> = with(MaterialTheme.colorScheme) {
    listOf(primary, secondary, tertiary, primaryContainer, secondaryContainer, tertiaryContainer)
}

@Preview(showBackground = true)
@Composable
private fun CategoryExpensesScreenPreview() {
    CategoryExpensesScreen(
        uiState = CategoryExpensesUiState(
            isLoadingTransactions = false,
            isLoadingCategories = false,
            report = CategoryExpenseReport(
                total = 400.0,
                categories = listOf(
                    CategoryExpenseTotal("home", "Casa", 250.0, 62.5),
                    CategoryExpenseTotal("food", "Alimentação", 150.0, 37.5),
                )
            )
        )
    )
}
