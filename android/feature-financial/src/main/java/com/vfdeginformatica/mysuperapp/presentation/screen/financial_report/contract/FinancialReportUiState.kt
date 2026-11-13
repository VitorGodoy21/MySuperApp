package com.vfdeginformatica.mysuperapp.presentation.screen.financial_report.contract

import com.vfdeginformatica.mysuperapp.domain.model.FinancialReport
import java.time.YearMonth

data class FinancialReportUiState(
    val isLoading: Boolean = true,
    val isGenerating: Boolean = false,
    val isGeneratingAi: Boolean = false,
    val errorMessage: String? = null,
    /** Meses com relatório disponível, do mais recente para o mais antigo. */
    val availableMonths: List<YearMonth> = emptyList(),
    /** Mês pedido (deep link / Dashboard) ou o mais recente disponível. */
    val selectedMonth: YearMonth? = null,
    val report: FinancialReport? = null,
    /** Mês corrente, usado para rotular a geração parcial. */
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedTab: ReportTab = ReportTab.SUMMARY,
    /** Qual texto é mostrado na aba Texto. Volta para TEMPLATE se o mês não tiver análise de IA. */
    val selectedNarrative: NarrativeSource = NarrativeSource.TEMPLATE,
    /** Diálogo de confirmação (aviso de privacidade) antes de chamar a IA. */
    val showAiConfirmation: Boolean = false,
) {
    val isEmpty: Boolean get() = !isLoading && errorMessage == null && report == null
    val canGenerateSelected: Boolean get() = selectedMonth != null && !selectedMonth.isAfter(currentMonth)

    /** Markdown exibido na aba Texto (null quando o doc é antigo e ainda não tem texto). */
    val activeNarrative: String?
        get() = when (selectedNarrative) {
            NarrativeSource.AI -> report?.aiNarrative ?: report?.narrative
            NarrativeSource.TEMPLATE -> report?.narrative
        }
}

enum class ReportTab { SUMMARY, TEXT }

enum class NarrativeSource { TEMPLATE, AI }
