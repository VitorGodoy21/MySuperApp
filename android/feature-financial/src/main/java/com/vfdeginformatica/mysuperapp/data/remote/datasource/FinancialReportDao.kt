package com.vfdeginformatica.mysuperapp.data.remote.datasource

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.FinancialReportDto

interface FinancialReportDao {
    /** Relatório de [month] (`yyyy-MM`) do usuário logado, ou null se ainda não existir. */
    suspend fun getReport(month: String): FinancialReportDto?

    /** Todos os relatórios do usuário logado, do mais recente para o mais antigo. */
    suspend fun listReports(): List<FinancialReportDto>

    /**
     * Pede ao backend para gerar/regerar o relatório de [month] (null = mês corrente, parcial).
     * Retorna o mês efetivamente gerado.
     */
    suspend fun requestReport(month: String?): Resource<String>

    /** Pede ao backend a análise com IA do relatório já existente de [month] (`yyyy-MM`). */
    suspend fun requestAiNarrative(month: String): Resource<Unit>
}
