package com.vfdeginformatica.mysuperapp.domain.use_case.financial.report

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.FinancialReport
import com.vfdeginformatica.mysuperapp.domain.repository.FinancialReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.YearMonth
import javax.inject.Inject

/** Relatório de um mês; `Success(null)` quando ainda não foi gerado. */
class GetFinancialReportUseCase @Inject constructor(
    private val repository: FinancialReportRepository,
) {
    operator fun invoke(month: YearMonth): Flow<Resource<FinancialReport?>> = flow {
        emit(Resource.Loading())
        try {
            emit(Resource.Success(repository.getReport(month)))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Erro ao carregar o relatório"))
        }
    }
}
