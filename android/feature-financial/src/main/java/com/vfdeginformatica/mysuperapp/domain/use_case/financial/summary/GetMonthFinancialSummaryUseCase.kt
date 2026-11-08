package com.vfdeginformatica.mysuperapp.domain.use_case.financial.summary

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.FinancialTotals
import com.vfdeginformatica.mysuperapp.domain.model.FinancialTotals.debitExpenseTotal
import com.vfdeginformatica.mysuperapp.domain.model.FinancialTotals.incomeTotal
import com.vfdeginformatica.mysuperapp.domain.model.MonthFinancialSummary
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.invoice.GetPaidInvoicesUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.GetTransactionsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.time.YearMonth
import javax.inject.Inject

/**
 * Month totals for the Home finance card, using the same cash-balance rule as the dashboard:
 * balance = incomes − (debit purchases + the card invoices paid in the month, per card rule).
 */
class GetMonthFinancialSummaryUseCase @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getPaidInvoicesUseCase: GetPaidInvoicesUseCase,
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

        val paidInvoices = when (val result = getPaidInvoicesUseCase(month).first { it !is Resource.Loading }) {
            is Resource.Success -> result.data.orEmpty()
            else -> {
                emit(Resource.Error(result.message ?: "Erro ao carregar fatura"))
                return@flow
            }
        }

        emit(
            Resource.Success(
                MonthFinancialSummary(
                    month = month,
                    totalIncome = transactions.incomeTotal(),
                    cashExpense = FinancialTotals.cashExpense(
                        transactions.debitExpenseTotal(),
                        FinancialTotals.paidInvoiceTotal(paidInvoices),
                    ),
                )
            )
        )
    }
}
