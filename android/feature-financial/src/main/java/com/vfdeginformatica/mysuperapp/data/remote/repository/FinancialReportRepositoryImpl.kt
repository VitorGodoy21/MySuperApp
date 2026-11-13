package com.vfdeginformatica.mysuperapp.data.remote.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.mapper.parseYearMonth
import com.vfdeginformatica.mysuperapp.data.mapper.toModel
import com.vfdeginformatica.mysuperapp.data.remote.datasource.FinancialReportDao
import com.vfdeginformatica.mysuperapp.domain.model.FinancialReport
import com.vfdeginformatica.mysuperapp.domain.repository.FinancialReportRepository
import java.time.YearMonth

class FinancialReportRepositoryImpl(
    private val dao: FinancialReportDao,
) : FinancialReportRepository {

    override suspend fun getReport(month: YearMonth): FinancialReport? =
        dao.getReport(month.toString())?.toModel()

    override suspend fun listReports(): List<FinancialReport> =
        dao.listReports().mapNotNull { it.toModel() }.sortedByDescending { it.month }

    override suspend fun requestReport(month: YearMonth?): Resource<YearMonth> =
        when (val result = dao.requestReport(month?.toString())) {
            is Resource.Success -> parseYearMonth(result.data)
                ?.let { Resource.Success(it) }
                ?: Resource.Error("Resposta inesperada ao gerar o relatório")
            is Resource.Error -> Resource.Error(result.message ?: "Não foi possível gerar o relatório")
            is Resource.Loading -> Resource.Loading()
        }

    override suspend fun requestAiNarrative(month: YearMonth): Resource<Unit> =
        dao.requestAiNarrative(month.toString())
}
