package com.vfdeginformatica.mysuperapp.domain.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.FinancialReport
import java.time.YearMonth

interface FinancialReportRepository {
    suspend fun getReport(month: YearMonth): FinancialReport?
    suspend fun listReports(): List<FinancialReport>

    /** Gera/regera no backend e retorna o mês gerado (null = mês corrente, parcial). */
    suspend fun requestReport(month: YearMonth?): Resource<YearMonth>

    /** Gera no backend a análise com IA do relatório já existente de [month]. */
    suspend fun requestAiNarrative(month: YearMonth): Resource<Unit>
}
