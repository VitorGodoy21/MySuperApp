package com.vfdeginformatica.mysuperapp.domain.model

import java.time.LocalDateTime
import java.time.YearMonth

/**
 * Relatório financeiro mensal gerado no backend (`users/{uid}/financial_reports/{yyyy-MM}`).
 * Espelha o JSON produzido por `web/functions/financialReport.js` (schemaVersion 2).
 */
data class FinancialReport(
    val month: YearMonth,
    /** Mês ainda em andamento (gerado sob demanda antes do fechamento). */
    val partial: Boolean,
    val generatedAt: LocalDateTime?,
    val trigger: ReportTrigger,
    val summary: ReportFigures,
    val previousMonth: ReportComparison?,
    val average3Months: ReportComparison?,
    val invoices: List<ReportInvoice>,
    val categories: List<ReportCategory>,
    val futureInstallments: ReportFutureInstallments,
    val alerts: List<ReportAlert>,
    /** Texto Markdown gerado pelo template determinístico no backend (null em docs antigos). */
    val narrative: String? = null,
    /** Texto Markdown gerado sob demanda pela IA (null até o usuário pedir). */
    val aiNarrative: String? = null,
    val aiGeneratedAt: LocalDateTime? = null,
) {
    val hasNarrative: Boolean get() = !narrative.isNullOrBlank()
    val hasAiNarrative: Boolean get() = !aiNarrative.isNullOrBlank()
}

enum class ReportTrigger { SCHEDULED, MANUAL, UNKNOWN }

/** Números de um mês. Nas médias, apenas as métricas comparadas são preenchidas. */
data class ReportFigures(
    val transactionCount: Int = 0,
    val income: Double = 0.0,
    val incomeCount: Int = 0,
    val debitExpense: Double = 0.0,
    val creditExpense: Double = 0.0,
    val totalExpense: Double = 0.0,
    val paidInvoiceTotal: Double = 0.0,
    val cashExpense: Double = 0.0,
    val balance: Double = 0.0,
    val savingsRate: Double? = null,
    val recurringExpense: Double = 0.0,
    val installmentExpense: Double = 0.0,
    val variableExpense: Double = 0.0,
    val recurringPercentOfIncome: Double? = null,
    val installmentPercentOfIncome: Double? = null,
    val variablePercentOfIncome: Double? = null,
    val uncategorizedCount: Int = 0,
    val uncategorizedTotal: Double = 0.0,
)

/** Variação de uma métrica em relação à referência (mês anterior ou média). */
data class ReportDelta(val amount: Double?, val percent: Double?) {
    companion object {
        val EMPTY = ReportDelta(null, null)
    }
}

data class ReportComparison(
    /** Mês de referência (comparação com o mês anterior) ou null para a média. */
    val month: YearMonth?,
    /** Quantidade de meses com dados que entraram na média. */
    val monthsWithData: Int?,
    val figures: ReportFigures,
    val deltas: Map<String, ReportDelta>,
) {
    fun delta(metric: String): ReportDelta = deltas[metric] ?: ReportDelta.EMPTY
}

data class ReportInvoice(
    val cardId: String,
    val bank: String,
    val finalCode: String,
    val invoiceMonth: YearMonth?,
    val itemCount: Int,
    val projectedTotal: Double,
    val isPaid: Boolean,
    val paidAmount: Double?,
    val effectiveTotal: Double,
    val installmentTotal: Double,
    val recurringTotal: Double,
    val singleTotal: Double,
)

data class ReportCategory(
    val categoryId: String?,
    val name: String,
    val total: Double,
    val count: Int,
    val percent: Double?,
    val previousTotal: Double?,
    val delta: ReportDelta,
)

data class ReportInstallmentMonth(val month: YearMonth, val total: Double, val count: Int)

data class ReportInstallmentGroup(
    val title: String,
    val installments: Int?,
    val installmentAmount: Double,
    val remainingCount: Int,
    val remainingTotal: Double,
    val lastMonth: YearMonth?,
    val categoryName: String,
)

data class ReportFutureInstallments(
    val months: List<ReportInstallmentMonth>,
    val totalCommitted: Double,
    val groupCount: Int,
    val topGroups: List<ReportInstallmentGroup>,
)

enum class ReportAlertSeverity { INFO, WARNING }

data class ReportAlert(val code: String, val severity: ReportAlertSeverity, val message: String)
