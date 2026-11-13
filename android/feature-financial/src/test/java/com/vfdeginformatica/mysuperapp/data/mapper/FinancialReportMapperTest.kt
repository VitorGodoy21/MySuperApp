package com.vfdeginformatica.mysuperapp.data.mapper

import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.data.remote.dto.FinancialReportDto
import com.vfdeginformatica.mysuperapp.domain.model.ReportAlertSeverity
import com.vfdeginformatica.mysuperapp.domain.model.ReportTrigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset

class FinancialReportMapperTest {

    private fun reportMap(): Map<String, Any?> = mapOf(
        "schemaVersion" to 1L,
        "month" to "2026-11",
        "partial" to false,
        "trigger" to "scheduled",
        "generatedAt" to Timestamp(1_764_000_000L, 0),
        "summary" to mapOf(
            "transactionCount" to 52L,
            "income" to 19873.05,
            "incomeCount" to 2L,
            "debitExpense" to 8638.42,
            "creditExpense" to 3738.11,
            "paidInvoiceTotal" to 8013.46,
            "cashExpense" to 16651.88,
            "balance" to 3221.17,
            "savingsRate" to 16.21,
            "recurringExpense" to 9005.64,
            "uncategorizedCount" to 1L,
            "uncategorizedTotal" to 309L,
        ),
        "comparison" to mapOf(
            "previousMonth" to mapOf(
                "month" to "2026-10",
                "figures" to mapOf("income" to 10000.0, "balance" to -3279.02),
                "delta" to mapOf(
                    "income" to mapOf("amount" to 9873.05, "percent" to 98.73),
                    "balance" to mapOf("amount" to 6500.19, "percent" to null),
                ),
            ),
            "average3Months" to null,
        ),
        "invoices" to listOf(
            mapOf(
                "cardId" to "c1", "bank" to "Itaú", "finalCode" to "1234", "invoiceMonth" to "2026-11",
                "itemCount" to 20L, "projectedTotal" to 8000.0, "isPaid" to true, "paidAmount" to 8013.46,
                "effectiveTotal" to 8013.46, "installmentTotal" to 899.83, "recurringTotal" to 100.0, "singleTotal" to 7000.17,
            )
        ),
        "categories" to listOf(
            mapOf("categoryId" to null, "name" to "Sem categoria", "total" to 309.0, "count" to 1L, "percent" to 2.5,
                "previousTotal" to null, "delta" to mapOf("amount" to 309.0, "percent" to null)),
        ),
        "futureInstallments" to mapOf(
            "months" to listOf(mapOf("month" to "2026-12", "total" to 2100.0, "count" to 4L)),
            "totalCommitted" to 11300.0,
            "groupCount" to 4L,
            "topGroups" to listOf(
                mapOf("title" to "Notebook", "installments" to 10L, "installmentAmount" to 500.0, "remainingCount" to 3L,
                    "remainingTotal" to 1500.0, "lastMonth" to "2027-02", "categoryName" to "Tecnologia")
            ),
        ),
        "alerts" to listOf(
            mapOf("code" to "UNCATEGORIZED", "severity" to "info", "message" to "1 gasto sem categoria"),
            mapOf("code" to "COMMITTED_HIGH", "severity" to "warning", "message" to "Fixo + parcelas comprometem 50% da renda."),
        ),
        "narrative" to "# Relatório de novembro",
        "aiNarrative" to "# Análise IA",
        "aiGeneratedAt" to Timestamp(1_764_100_000L, 0),
    )

    @Test
    fun mapsFullDocumentIntoDomainModel() {
        val report = FinancialReportDto("2026-11", reportMap()).toModel(ZoneOffset.UTC)!!

        assertEquals(YearMonth.of(2026, 11), report.month)
        assertFalse(report.partial)
        assertEquals(ReportTrigger.SCHEDULED, report.trigger)
        assertEquals(LocalDateTime.ofEpochSecond(1_764_000_000L, 0, ZoneOffset.UTC), report.generatedAt)

        assertEquals(19873.05, report.summary.income, 0.001)
        assertEquals(3221.17, report.summary.balance, 0.001)
        assertEquals(1, report.summary.uncategorizedCount)
        assertEquals(309.0, report.summary.uncategorizedTotal, 0.001)
        assertEquals(0.0, report.summary.variableExpense, 0.001)

        val previous = report.previousMonth!!
        assertEquals(YearMonth.of(2026, 10), previous.month)
        assertEquals(-3279.02, previous.figures.balance, 0.001)
        assertEquals(9873.05, previous.delta("income").amount!!, 0.001)
        assertNull(previous.delta("balance").percent)
        assertNull(previous.delta("cashExpense").amount)
        assertNull(report.average3Months)

        val invoice = report.invoices.single()
        assertEquals("Itaú", invoice.bank)
        assertTrue(invoice.isPaid)
        assertEquals(8013.46, invoice.paidAmount!!, 0.001)
        assertEquals(YearMonth.of(2026, 11), invoice.invoiceMonth)

        val category = report.categories.single()
        assertNull(category.categoryId)
        assertNull(category.previousTotal)
        assertEquals(309.0, category.delta.amount!!, 0.001)

        assertEquals(YearMonth.of(2026, 12), report.futureInstallments.months.single().month)
        assertEquals("Notebook", report.futureInstallments.topGroups.single().title)
        assertEquals(YearMonth.of(2027, 2), report.futureInstallments.topGroups.single().lastMonth)

        assertEquals(ReportAlertSeverity.INFO, report.alerts[0].severity)
        assertEquals(ReportAlertSeverity.WARNING, report.alerts[1].severity)

        assertEquals("# Relatório de novembro", report.narrative)
        assertEquals("# Análise IA", report.aiNarrative)
        assertTrue(report.hasNarrative)
        assertTrue(report.hasAiNarrative)
        assertEquals(LocalDateTime.ofEpochSecond(1_764_100_000L, 0, ZoneOffset.UTC), report.aiGeneratedAt)
    }

    @Test
    fun blankNarrativesAreTreatedAsAbsent() {
        val report = FinancialReportDto("2026-10", mapOf("narrative" to "  ", "aiNarrative" to "")).toModel()!!

        assertNull(report.narrative)
        assertNull(report.aiNarrative)
        assertFalse(report.hasNarrative)
        assertFalse(report.hasAiNarrative)
        assertNull(report.aiGeneratedAt)
    }

    @Test
    fun fallsBackToDocumentIdAndDefaultsWhenFieldsAreMissing() {
        val report = FinancialReportDto("2026-10", mapOf("partial" to true, "trigger" to "manual")).toModel()!!

        assertEquals(YearMonth.of(2026, 10), report.month)
        assertTrue(report.partial)
        assertEquals(ReportTrigger.MANUAL, report.trigger)
        assertNull(report.generatedAt)
        assertEquals(0, report.summary.transactionCount)
        assertNull(report.previousMonth)
        assertTrue(report.invoices.isEmpty())
        assertTrue(report.futureInstallments.months.isEmpty())
    }

    @Test
    fun returnsNullForInvalidMonth() {
        assertNull(FinancialReportDto("garbage", mapOf("month" to "not-a-month")).toModel())
    }
}
