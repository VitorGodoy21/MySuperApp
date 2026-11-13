package com.vfdeginformatica.mysuperapp.domain.use_case.financial.report

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.repository.FinancialReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.YearMonth
import javax.inject.Inject

/**
 * Pede ao backend a análise com IA (Gemini no projeto do app) do relatório de [month]. Só roda
 * sob demanda explícita do usuário; o backend exige que o relatório já exista.
 */
class RequestAiNarrativeUseCase @Inject constructor(
    private val repository: FinancialReportRepository,
) {
    operator fun invoke(month: YearMonth): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        emit(repository.requestAiNarrative(month))
    }
}
