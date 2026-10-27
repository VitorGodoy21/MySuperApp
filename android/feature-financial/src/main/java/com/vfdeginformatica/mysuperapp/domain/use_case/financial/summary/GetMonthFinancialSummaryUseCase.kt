package com.vfdeginformatica.mysuperapp.domain.use_case.financial.summary

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.FinancialTotals
import com.vfdeginformatica.mysuperapp.domain.model.FinancialTotals.debitExpenseTotal
import com.vfdeginformatica.mysuperapp.domain.model.FinancialTotals.incomeTotal
import com.vfdeginformatica.mysuperapp.domain.model.MonthFinancialSummary
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.GetInvoiceProjectionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.GetTransactionsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.time.YearMonth
import javax.inject.Inject

/**
 * Month totals for the Home finance card, using the same cash-balance rule as the dashboard:
 * balance = incomes − (debit purchases + the invoice closed in the previous month).
 */
class GetMonthFinancialSummaryUseCase @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getInvoiceProjectionUseCase: GetInvoiceProjectionUseCase,
) {
    operator fun invoke(month: YearMonth = YearMonth.now()): Flow<Resource<MonthFinancialSummary>> = flow {
        emit(Resource.Loading())

        val transactions = when (val result = getTransactionsUseCase(month).first { it !is Resource.Loading }) {
            is Resource.Success -> result.data.orEmpty()
            else -> {
                emit(Resource.Error(result.message ?: "Erro ao carregar resumo financeiro"))
                return@flow
            }
        }

        val paidMonth = month.minusMonths(1)
        val projection = when (val result = getInvoiceProjectionUseCase(paidMonth).first { it !is Resource.Loading }) {
            is Resource.Success -> result.data
            else -> {
                emit(Resource.Error(result.message ?: "Erro ao carregar fatura"))
                return@flow
            }
        }

        val paidInvoiceTotal = FinancialTotals.invoiceItems(
            invoiceMonth = paidMonth,
            invoiceTransactions = projection?.invoiceTransactions.orEmpty(),
            recurringTransactions = projection?.recurringTransactions.orEmpty(),
        ).sumOf { it.amount }

        emit(
            Resource.Success(
                MonthFinancialSummary(
                    month = month,
                    totalIncome = transactions.incomeTotal(),
                    cashExpense = FinancialTotals.cashExpense(transactions.debitExpenseTotal(), paidInvoiceTotal),
                )
            )
        )
    }
}
