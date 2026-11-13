package com.vfdeginformatica.mysuperapp.domain.use_case.financial.report

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.repository.FinancialReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.YearMonth
import javax.inject.Inject

/**
 * Pede ao backend a geração do relatório de [month] (null = mês corrente, parcial) e emite o
 * mês gerado. Meses futuros são recusados localmente.
 */
class RequestFinancialReportUseCase @Inject constructor(
    private val repository: FinancialReportRepository,
) {
    operator fun invoke(month: YearMonth?, today: YearMonth = YearMonth.now()): Flow<Resource<YearMonth>> = flow {
        if (month != null && month.isAfter(today)) {
            emit(Resource.Error("Não é possível gerar relatório de um mês futuro"))
            return@flow
        }
        emit(Resource.Loading())
        emit(repository.requestReport(month))
    }
}
