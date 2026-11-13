package com.vfdeginformatica.mysuperapp.presentation.screen.financial_report.contract

import java.time.YearMonth

sealed interface FinancialReportEvent {
    data object OnRefresh : FinancialReportEvent
    data class OnMonthSelected(val month: YearMonth) : FinancialReportEvent

    /** Gera/regera no backend o mês selecionado (ou o corrente quando não há nenhum). */
    data object OnGenerateClicked : FinancialReportEvent

    data class OnTabSelected(val tab: ReportTab) : FinancialReportEvent
    data class OnNarrativeSourceSelected(val source: NarrativeSource) : FinancialReportEvent

    /** Abre o diálogo de confirmação (aviso de privacidade) da análise com IA. */
    data object OnGenerateAiClicked : FinancialReportEvent
    data object OnGenerateAiConfirmed : FinancialReportEvent
    data object OnGenerateAiDismissed : FinancialReportEvent

    /** Compartilha / copia o Markdown do texto ativo. */
    data object OnShareClicked : FinancialReportEvent
    data object OnCopyClicked : FinancialReportEvent
}
