package com.vfdeginformatica.mysuperapp.data.mapper

import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.data.remote.dto.FinancialReportDto
import com.vfdeginformatica.mysuperapp.domain.model.FinancialReport
import com.vfdeginformatica.mysuperapp.domain.model.ReportAlert
import com.vfdeginformatica.mysuperapp.domain.model.ReportAlertSeverity
import com.vfdeginformatica.mysuperapp.domain.model.ReportCategory
import com.vfdeginformatica.mysuperapp.domain.model.ReportComparison
import com.vfdeginformatica.mysuperapp.domain.model.ReportDelta
import com.vfdeginformatica.mysuperapp.domain.model.ReportFigures
import com.vfdeginformatica.mysuperapp.domain.model.ReportFutureInstallments
import com.vfdeginformatica.mysuperapp.domain.model.ReportInstallmentGroup
import com.vfdeginformatica.mysuperapp.domain.model.ReportInstallmentMonth
import com.vfdeginformatica.mysuperapp.domain.model.ReportInvoice
import com.vfdeginformatica.mysuperapp.domain.model.ReportTrigger
import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeParseException

private fun Map<String, Any?>.map(key: String): Map<String, Any?>? {
    @Suppress("UNCHECKED_CAST")
    return this[key] as? Map<String, Any?>
}

private fun Map<String, Any?>.list(key: String): List<Map<String, Any?>> {
    @Suppress("UNCHECKED_CAST")
    return (this[key] as? List<*>)?.mapNotNull { it as? Map<String, Any?> } ?: emptyList()
}

private fun Map<String, Any?>.double(key: String): Double? = (this[key] as? Number)?.toDouble()
private fun Map<String, Any?>.doubleOrZero(key: String): Double = double(key) ?: 0.0
private fun Map<String, Any?>.int(key: String): Int? = (this[key] as? Number)?.toInt()
private fun Map<String, Any?>.intOrZero(key: String): Int = int(key) ?: 0
private fun Map<String, Any?>.string(key: String): String? = this[key] as? String
private fun Map<String, Any?>.bool(key: String): Boolean = this[key] as? Boolean ?: false

internal fun parseYearMonth(value: String?): YearMonth? = try {
    value?.let { YearMonth.parse(it) }
} catch (e: DateTimeParseException) {
    null
}

private fun Any?.toLocalDateTime(zone: ZoneId): LocalDateTime? = when (this) {
    is Timestamp -> LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds, nanoseconds.toLong()), zone)
    is String -> try {
        LocalDateTime.ofInstant(Instant.parse(this), zone)
    } catch (e: DateTimeParseException) {
        null
    }
    else -> null
}

private fun Map<String, Any?>.toFigures() = ReportFigures(
    transactionCount = intOrZero("transactionCount"),
    income = doubleOrZero("income"),
    incomeCount = intOrZero("incomeCount"),
    debitExpense = doubleOrZero("debitExpense"),
    creditExpense = doubleOrZero("creditExpense"),
    totalExpense = doubleOrZero("totalExpense"),
    paidInvoiceTotal = doubleOrZero("paidInvoiceTotal"),
    cashExpense = doubleOrZero("cashExpense"),
    balance = doubleOrZero("balance"),
    savingsRate = double("savingsRate"),
    recurringExpense = doubleOrZero("recurringExpense"),
    installmentExpense = doubleOrZero("installmentExpense"),
    variableExpense = doubleOrZero("variableExpense"),
    recurringPercentOfIncome = double("recurringPercentOfIncome"),
    installmentPercentOfIncome = double("installmentPercentOfIncome"),
    variablePercentOfIncome = double("variablePercentOfIncome"),
    uncategorizedCount = intOrZero("uncategorizedCount"),
    uncategorizedTotal = doubleOrZero("uncategorizedTotal"),
)

private fun Map<String, Any?>?.toDelta(): ReportDelta =
    if (this == null) ReportDelta.EMPTY else ReportDelta(double("amount"), double("percent"))

private fun Map<String, Any?>?.toComparison(): ReportComparison? {
    if (this == null) return null
    val deltas = map("delta").orEmpty().mapNotNull { (metric, raw) ->
        @Suppress("UNCHECKED_CAST")
        (raw as? Map<String, Any?>)?.let { metric to it.toDelta() }
    }.toMap()
    return ReportComparison(
        month = parseYearMonth(string("month")),
        monthsWithData = int("monthsWithData"),
        figures = map("figures").orEmpty().toFigures(),
        deltas = deltas,
    )
}

private fun Map<String, Any?>.toInvoice() = ReportInvoice(
    cardId = string("cardId") ?: "",
    bank = string("bank") ?: "",
    finalCode = string("finalCode") ?: "",
    invoiceMonth = parseYearMonth(string("invoiceMonth")),
    itemCount = intOrZero("itemCount"),
    projectedTotal = doubleOrZero("projectedTotal"),
    isPaid = bool("isPaid"),
    paidAmount = double("paidAmount"),
    effectiveTotal = doubleOrZero("effectiveTotal"),
    installmentTotal = doubleOrZero("installmentTotal"),
    recurringTotal = doubleOrZero("recurringTotal"),
    singleTotal = doubleOrZero("singleTotal"),
)

private fun Map<String, Any?>.toCategory() = ReportCategory(
    categoryId = string("categoryId"),
    name = string("name") ?: "Sem categoria",
    total = doubleOrZero("total"),
    count = intOrZero("count"),
    percent = double("percent"),
    previousTotal = double("previousTotal"),
    delta = map("delta").toDelta(),
)

private fun Map<String, Any?>?.toFutureInstallments(): ReportFutureInstallments {
    if (this == null) return ReportFutureInstallments(emptyList(), 0.0, 0, emptyList())
    return ReportFutureInstallments(
        months = list("months").mapNotNull { m ->
            parseYearMonth(m.string("month"))?.let {
                ReportInstallmentMonth(it, m.doubleOrZero("total"), m.intOrZero("count"))
            }
        },
        totalCommitted = doubleOrZero("totalCommitted"),
        groupCount = intOrZero("groupCount"),
        topGroups = list("topGroups").map { g ->
            ReportInstallmentGroup(
                title = g.string("title") ?: "",
                installments = g.int("installments"),
                installmentAmount = g.doubleOrZero("installmentAmount"),
                remainingCount = g.intOrZero("remainingCount"),
                remainingTotal = g.doubleOrZero("remainingTotal"),
                lastMonth = parseYearMonth(g.string("lastMonth")),
                categoryName = g.string("categoryName") ?: "Sem categoria",
            )
        },
    )
}

private fun Map<String, Any?>.toAlert() = ReportAlert(
    code = string("code") ?: "",
    severity = if (string("severity") == "warning") ReportAlertSeverity.WARNING else ReportAlertSeverity.INFO,
    message = string("message") ?: "",
)

/** Retorna null quando o documento não tem um mês válido (relatório corrompido/incompatível). */
fun FinancialReportDto.toModel(zone: ZoneId = ZoneId.systemDefault()): FinancialReport? {
    val month = parseYearMonth(data.string("month") ?: id) ?: return null
    val comparison = data.map("comparison")
    return FinancialReport(
        month = month,
        partial = data.bool("partial"),
        generatedAt = data["generatedAt"].toLocalDateTime(zone),
        trigger = when (data.string("trigger")) {
            "scheduled" -> ReportTrigger.SCHEDULED
            "manual" -> ReportTrigger.MANUAL
            else -> ReportTrigger.UNKNOWN
        },
        summary = data.map("summary").orEmpty().toFigures(),
        previousMonth = comparison?.map("previousMonth").toComparison(),
        average3Months = comparison?.map("average3Months").toComparison(),
        invoices = data.list("invoices").map { it.toInvoice() },
        categories = data.list("categories").map { it.toCategory() },
        futureInstallments = data.map("futureInstallments").toFutureInstallments(),
        alerts = data.list("alerts").map { it.toAlert() },
        narrative = data.string("narrative")?.takeIf { it.isNotBlank() },
        aiNarrative = data.string("aiNarrative")?.takeIf { it.isNotBlank() },
        aiGeneratedAt = data["aiGeneratedAt"].toLocalDateTime(zone),
    )
}
