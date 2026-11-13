package com.vfdeginformatica.mysuperapp.presentation.screen.financial_report

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.domain.model.FinancialReport
import com.vfdeginformatica.mysuperapp.domain.model.ReportAlertSeverity
import com.vfdeginformatica.mysuperapp.domain.model.ReportComparison
import com.vfdeginformatica.mysuperapp.domain.model.ReportDelta
import com.vfdeginformatica.mysuperapp.presentation.common.balanceColor
import com.vfdeginformatica.mysuperapp.presentation.common.expenseColor
import com.vfdeginformatica.mysuperapp.presentation.common.formatCurrency
import com.vfdeginformatica.mysuperapp.presentation.common.formatPercent
import com.vfdeginformatica.mysuperapp.presentation.common.incomeColor
import com.vfdeginformatica.mysuperapp.presentation.common.markdown.MarkdownText
import com.vfdeginformatica.mysuperapp.presentation.common.monthLabel
import com.vfdeginformatica.mysuperapp.presentation.common.shortMonthLabel
import com.vfdeginformatica.mysuperapp.presentation.screen.financial_report.contract.FinancialReportEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.financial_report.contract.FinancialReportUiState
import com.vfdeginformatica.mysuperapp.presentation.screen.financial_report.contract.NarrativeSource
import com.vfdeginformatica.mysuperapp.presentation.screen.financial_report.contract.ReportTab
import java.time.format.DateTimeFormatter

@Composable
fun FinancialReportScreen(
    uiState: FinancialReportUiState,
    onEvent: (FinancialReportEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if (uiState.availableMonths.isNotEmpty()) {
            MonthChips(uiState, onEvent)
        }
        if (uiState.isGenerating || uiState.isGeneratingAi) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        when {
            uiState.isLoading && uiState.report == null -> Centered { CircularProgressIndicator() }

            uiState.errorMessage != null && uiState.report == null -> Centered {
                Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { onEvent(FinancialReportEvent.OnRefresh) }) { Text("Tentar novamente") }
            }

            uiState.report == null -> EmptyState(uiState, onEvent)

            else -> {
                ReportTabs(uiState.selectedTab, onEvent)
                when (uiState.selectedTab) {
                    ReportTab.SUMMARY -> ReportContent(uiState, uiState.report, onEvent)
                    ReportTab.TEXT -> NarrativeContent(uiState, uiState.report, onEvent)
                }
            }
        }
    }

    if (uiState.showAiConfirmation) {
        AiConfirmationDialog(onEvent)
    }
}

@Composable
private fun ReportTabs(selected: ReportTab, onEvent: (FinancialReportEvent) -> Unit) {
    TabRow(selectedTabIndex = selected.ordinal) {
        Tab(
            selected = selected == ReportTab.SUMMARY,
            onClick = { onEvent(FinancialReportEvent.OnTabSelected(ReportTab.SUMMARY)) },
            text = { Text("Resumo") },
        )
        Tab(
            selected = selected == ReportTab.TEXT,
            onClick = { onEvent(FinancialReportEvent.OnTabSelected(ReportTab.TEXT)) },
            text = { Text("Texto") },
        )
    }
}

@Composable
private fun AiConfirmationDialog(onEvent: (FinancialReportEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onEvent(FinancialReportEvent.OnGenerateAiDismissed) },
        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
        title = { Text("Analisar com IA") },
        text = {
            Text(
                "Os números deste mês (valores, títulos dos lançamentos e categorias — sem seu nome ou " +
                    "e-mail) serão enviados ao Gemini dentro do projeto Google Cloud do app para gerar uma " +
                    "análise em texto. O resultado fica salvo só na sua conta. Pode levar cerca de 30 segundos."
            )
        },
        confirmButton = {
            Button(onClick = { onEvent(FinancialReportEvent.OnGenerateAiConfirmed) }) { Text("Analisar") }
        },
        dismissButton = {
            OutlinedButton(onClick = { onEvent(FinancialReportEvent.OnGenerateAiDismissed) }) { Text("Cancelar") }
        },
    )
}

// ---------------------------------------------------------------------------
// Aba Texto
// ---------------------------------------------------------------------------

@Composable
private fun NarrativeContent(
    uiState: FinancialReportUiState,
    report: FinancialReport,
    onEvent: (FinancialReportEvent) -> Unit,
) {
    val narrative = uiState.activeNarrative
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.selectedNarrative == NarrativeSource.TEMPLATE,
                    onClick = { onEvent(FinancialReportEvent.OnNarrativeSourceSelected(NarrativeSource.TEMPLATE)) },
                    label = { Text("Padrão") },
                )
                if (report.hasAiNarrative) {
                    FilterChip(
                        selected = uiState.selectedNarrative == NarrativeSource.AI,
                        onClick = { onEvent(FinancialReportEvent.OnNarrativeSourceSelected(NarrativeSource.AI)) },
                        label = { Text("IA") },
                        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    )
                }
            }
            if (report.hasNarrative) {
                OutlinedButton(
                    onClick = { onEvent(FinancialReportEvent.OnGenerateAiClicked) },
                    enabled = !uiState.isGeneratingAi && !uiState.isGenerating,
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (report.hasAiNarrative) "Refazer IA" else "Analisar com IA")
                }
            }
        }

        if (uiState.selectedNarrative == NarrativeSource.AI && report.aiGeneratedAt != null) {
            Text(
                "Análise gerada pela IA em ${report.aiGeneratedAt.format(generatedAtFormatter)}. " +
                    "Confira os números no Resumo antes de decidir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        if (narrative == null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Este relatório foi gerado antes da versão com texto.", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Regere o relatório para ver a análise textual.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    GenerateButton(uiState, onEvent)
                }
            }
        } else {
            MarkdownText(markdown = narrative, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}

@Composable
private fun MonthChips(uiState: FinancialReportUiState, onEvent: (FinancialReportEvent) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(uiState.availableMonths) { month ->
            FilterChip(
                selected = month == uiState.selectedMonth,
                onClick = { onEvent(FinancialReportEvent.OnMonthSelected(month)) },
                label = { Text(shortMonthLabel(month)) },
            )
        }
    }
}

@Composable
private fun EmptyState(uiState: FinancialReportUiState, onEvent: (FinancialReportEvent) -> Unit) {
    val selected = uiState.selectedMonth
    Centered {
        Text(
            text = if (selected != null) "Nenhum relatório para ${monthLabel(selected)}"
            else "Nenhum relatório ainda",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "O relatório do mês fechado é gerado automaticamente no dia 1. " +
                "Você também pode gerar agora um resumo parcial.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        GenerateButton(uiState, onEvent)
    }
}

@Composable
private fun GenerateButton(uiState: FinancialReportUiState, onEvent: (FinancialReportEvent) -> Unit) {
    val selected = uiState.selectedMonth
    val label = when {
        selected == null || selected == uiState.currentMonth -> "Gerar agora (mês atual, parcial)"
        uiState.report != null -> "Regerar ${shortMonthLabel(selected)}"
        else -> "Gerar ${shortMonthLabel(selected)}"
    }
    Button(
        onClick = { onEvent(FinancialReportEvent.OnGenerateClicked) },
        enabled = !uiState.isGenerating && (selected == null || uiState.canGenerateSelected),
    ) { Text(label) }
}

// ---------------------------------------------------------------------------
// Conteúdo do relatório
// ---------------------------------------------------------------------------

private val generatedAtFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

@Composable
private fun ReportContent(
    uiState: FinancialReportUiState,
    report: FinancialReport,
    onEvent: (FinancialReportEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item { ReportHeader(report) }
        if (report.alerts.isNotEmpty()) item { AlertsCard(report) }
        item { SummaryCard(report) }
        item { ComparisonCard(report) }
        item { NatureCard(report) }
        if (report.invoices.isNotEmpty()) item { InvoicesCard(report) }
        if (report.categories.isNotEmpty()) item { CategoriesCard(report) }
        if (report.futureInstallments.months.isNotEmpty()) item { FutureInstallmentsCard(report) }
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                GenerateButton(uiState, onEvent)
            }
        }
    }
}

@Composable
private fun ReportHeader(report: FinancialReport) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(monthLabel(report.month), style = MaterialTheme.typography.headlineSmall)
            if (report.partial) {
                AssistChip(onClick = {}, enabled = false, label = { Text("Parcial") })
            }
        }
        report.generatedAt?.let {
            Text(
                "Gerado em ${it.format(generatedAtFormatter)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun AmountRow(label: String, value: Double, color: Color = MaterialTheme.colorScheme.onSurface, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            formatCurrency(value),
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun AlertsCard(report: FinancialReport) {
    SectionCard("Alertas") {
        report.alerts.forEach { alert ->
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val warning = alert.severity == ReportAlertSeverity.WARNING
                Icon(
                    imageVector = if (warning) Icons.Default.Warning else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(alert.message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SummaryCard(report: FinancialReport) {
    val s = report.summary
    SectionCard("Resumo") {
        AmountRow("Receitas", s.income, incomeColor)
        AmountRow("Gastos no débito", s.debitExpense, expenseColor)
        AmountRow("Faturas pagas", s.paidInvoiceTotal, expenseColor)
        HorizontalDivider()
        AmountRow("Saiu da conta", s.cashExpense, expenseColor, bold = true)
        AmountRow("Saldo do mês", s.balance, balanceColor(s.balance), bold = true)
        s.savingsRate?.let {
            Text(
                "Você guardou ${formatPercent(it)} do que recebeu",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        if (s.creditExpense > 0) {
            Text(
                "Compras no crédito do mês: ${formatCurrency(s.creditExpense)} (entram nas próximas faturas)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun ComparisonCard(report: FinancialReport) {
    val previous = report.previousMonth
    val average = report.average3Months
    SectionCard("Comparação") {
        if (previous == null && average == null) {
            Text(
                "Ainda não há meses anteriores para comparar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            return@SectionCard
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("", modifier = Modifier.weight(1.2f))
            Text(
                previous?.month?.let { "vs ${shortMonthLabel(it)}" } ?: "vs mês ant.",
                modifier = Modifier.weight(1f), textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline,
            )
            Text(
                average?.let { "vs média ${it.monthsWithData ?: 3}m" } ?: "vs média",
                modifier = Modifier.weight(1f), textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline,
            )
        }
        listOf(
            "income" to "Receitas",
            "cashExpense" to "Saiu da conta",
            "debitExpense" to "Débito",
            "paidInvoiceTotal" to "Faturas pagas",
            "recurringExpense" to "Fixo",
            "variableExpense" to "Variável",
            "balance" to "Saldo",
        ).forEach { (metric, label) ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(label, modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodyMedium)
                DeltaText(previous, metric, Modifier.weight(1f))
                DeltaText(average, metric, Modifier.weight(1f))
            }
        }
        Text(
            "Para gastos, ▼ é bom; para receitas e saldo, ▲ é bom.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun DeltaText(comparison: ReportComparison?, metric: String, modifier: Modifier) {
    val delta = comparison?.delta(metric) ?: ReportDelta.EMPTY
    Text(
        text = formatDelta(delta),
        modifier = modifier,
        textAlign = TextAlign.End,
        style = MaterialTheme.typography.bodySmall,
        color = deltaColor(delta, positiveIsGood = metric == "income" || metric == "balance"),
    )
}

internal fun formatDelta(delta: ReportDelta): String {
    val amount = delta.amount ?: return "—"
    val arrow = when {
        amount > 0 -> "▲ "
        amount < 0 -> "▼ "
        else -> ""
    }
    val percent = delta.percent?.let { " (${formatPercent(kotlin.math.abs(it))})" } ?: ""
    return arrow + formatCurrency(kotlin.math.abs(amount)) + percent
}

@Composable
private fun deltaColor(delta: ReportDelta, positiveIsGood: Boolean): Color {
    val amount = delta.amount ?: return MaterialTheme.colorScheme.outline
    if (amount == 0.0) return MaterialTheme.colorScheme.onSurface
    val good = (amount > 0) == positiveIsGood
    return if (good) incomeColor else MaterialTheme.colorScheme.error
}

@Composable
private fun NatureCard(report: FinancialReport) {
    val s = report.summary
    SectionCard("Fixo × parcelas × variável") {
        NatureRow("Fixo (recorrentes)", s.recurringExpense, s.recurringPercentOfIncome)
        NatureRow("Parcelas", s.installmentExpense, s.installmentPercentOfIncome)
        NatureRow("Variável", s.variableExpense, s.variablePercentOfIncome)
        Text(
            "Percentuais em relação à receita do mês. Inclui compras no crédito pela data da compra.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun NatureRow(label: String, value: Double, percentOfIncome: Double?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            formatCurrency(value) + (percentOfIncome?.let { "  ·  ${formatPercent(it)}" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun InvoicesCard(report: FinancialReport) {
    SectionCard("Faturas pagas no mês") {
        report.invoices.forEach { inv ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        buildString {
                            append(inv.bank)
                            if (inv.finalCode.isNotBlank()) append(" •${inv.finalCode}")
                            inv.invoiceMonth?.let { append(" · ${shortMonthLabel(it)}") }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(formatCurrency(inv.effectiveTotal), style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    buildString {
                        append(if (inv.isPaid) "Paga" else "Projetada")
                        if (inv.isPaid && inv.paidAmount != null && kotlin.math.abs(inv.paidAmount - inv.projectedTotal) >= 1) {
                            append(" · lançado ${formatCurrency(inv.projectedTotal)}")
                        }
                        append(" · ${inv.itemCount} itens")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    "À vista ${formatCurrency(inv.singleTotal)} · Parcelas ${formatCurrency(inv.installmentTotal)} · Fixo ${formatCurrency(inv.recurringTotal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun CategoriesCard(report: FinancialReport) {
    SectionCard("Categorias") {
        report.categories.forEach { cat ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1.4f)) {
                    Text(cat.name, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${cat.count} itens" + (cat.percent?.let { " · ${formatPercent(it)}" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                Text(
                    formatCurrency(cat.total),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    formatDelta(cat.delta),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodySmall,
                    color = deltaColor(cat.delta, positiveIsGood = false),
                )
            }
        }
    }
}

@Composable
private fun FutureInstallmentsCard(report: FinancialReport) {
    val f = report.futureInstallments
    SectionCard("Parcelas futuras") {
        Text(
            "${formatCurrency(f.totalCommitted)} comprometidos em ${f.groupCount} compra(s)",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        f.months.forEach { m ->
            AmountRow("${shortMonthLabel(m.month)} · ${m.count} parcela(s)", m.total)
        }
        if (f.topGroups.isNotEmpty()) {
            HorizontalDivider()
            Text("Maiores compras em aberto", style = MaterialTheme.typography.labelMedium)
            f.topGroups.forEach { g ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(g.title.ifBlank { g.categoryName }, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            buildString {
                                append("${g.remainingCount}× ${formatCurrency(g.installmentAmount)}")
                                g.lastMonth?.let { append(" · até ${shortMonthLabel(it)}") }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    Text(formatCurrency(g.remainingTotal), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
