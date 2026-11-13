package com.vfdeginformatica.mysuperapp.presentation.screen.financial_report.contract

sealed interface FinancialReportEffect {
    data class ShowToast(val message: String) : FinancialReportEffect

    /** Abre o seletor de compartilhamento com o Markdown do relatório. */
    data class ShareText(val subject: String, val text: String) : FinancialReportEffect

    /** Copia o Markdown para a área de transferência. */
    data class CopyText(val text: String) : FinancialReportEffect
}
