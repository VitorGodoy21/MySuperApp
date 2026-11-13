package com.vfdeginformatica.mysuperapp.domain.use_case.financial.report

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.FinancialReport
import com.vfdeginformatica.mysuperapp.domain.repository.FinancialReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/** Todos os relatórios disponíveis, do mais recente para o mais antigo. */
class ListFinancialReportsUseCase @Inject constructor(
    private val repository: FinancialReportRepository,
) {
    operator fun invoke(): Flow<Resource<List<FinancialReport>>> = flow {
        emit(Resource.Loading())
        try {
            emit(Resource.Success(repository.listReports()))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Erro ao carregar os relatórios"))
        }
    }
}
