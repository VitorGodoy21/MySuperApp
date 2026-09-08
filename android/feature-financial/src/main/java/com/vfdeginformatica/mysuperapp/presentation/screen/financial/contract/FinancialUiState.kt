package com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract

import com.vfdeginformatica.mysuperapp.data.remote.dto.CardDto
import com.vfdeginformatica.mysuperapp.data.remote.dto.CategoryTransactionDto
import com.vfdeginformatica.mysuperapp.domain.model.Transaction
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType
import java.time.YearMonth

data class FinancialUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedMonth: YearMonth = YearMonth.now(),
    val transactions: List<Transaction> = emptyList(),
    val availableCards: List<CardDto> = emptyList(),
    val availableCategories: List<CategoryTransactionDto> = emptyList(),
    val typeFilter: TransactionTypeFilter = TransactionTypeFilter.ALL,
    val categoryFilterId: String? = null,
    val cardFilterId: String? = null,
    // Aba Projeção
    val selectedTab: FinancialTab = FinancialTab.MONTH,
    val projectionMonth: YearMonth = YearMonth.now().plusMonths(1),
    val isLoadingProjection: Boolean = false,
    val invoiceTransactions: List<Transaction> = emptyList(),
    val recurringTransactions: List<Transaction> = emptyList(),
    // Transação em edição (dialog)
    val editingTransaction: Transaction? = null,
    val isSavingEdit: Boolean = false
) {
    /** Transactions of the selected month with all active filters applied. */
    val filteredTransactions: List<Transaction>
        get() = transactions.filter { transaction ->
            val matchesType = when (typeFilter) {
                TransactionTypeFilter.ALL -> true
                TransactionTypeFilter.INCOME -> transaction.type == TransactionType.INCOME
                TransactionTypeFilter.EXPENSE -> transaction.type == TransactionType.EXPENSE
            }
            val matchesCategory =
                categoryFilterId == null || transaction.categoryIds.contains(categoryFilterId)
            val matchesCard = cardFilterId == null || transaction.cardId == cardFilterId
            matchesType && matchesCategory && matchesCard
        }

    val totalIncome: Double
        get() = filteredTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }

    val totalExpense: Double
        get() = filteredTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

    val balance: Double
        get() = totalIncome - totalExpense

    // ---- Projeção do mês alvo ----

    private val projectionMonthKey: String
        get() = "%04d-%02d".format(projectionMonth.year, projectionMonth.monthValue)

    /**
     * Despesas de crédito recorrentes projetadas na fatura do mês alvo
     * (excluindo as que já entraram pela query de invoiceMonth, para não duplicar).
     */
    val recurringCreditInInvoice: List<Transaction>
        get() = recurringTransactions.filter {
            it.type == TransactionType.EXPENSE &&
                    it.paymentMethod == "CREDIT" &&
                    it.invoiceMonth != projectionMonthKey
        }

    /** Itens que compõem a fatura projetada (parcelas/compras + recorrentes de crédito). */
    val projectedInvoiceItems: List<Transaction>
        get() = invoiceTransactions + recurringCreditInInvoice

    /** Fatura projetada por cartão (cardId → total). */
    val invoiceByCard: Map<String, Double>
        get() = projectedInvoiceItems
            .groupBy { it.cardId }
            .mapValues { (_, items) -> items.sumOf { it.amount } }

    val projectedInvoiceTotal: Double
        get() = projectedInvoiceItems.sumOf { it.amount }

    /** Receitas recorrentes previstas (ex.: salário). */
    val projectedIncome: Double
        get() = recurringTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }

    /** Despesas recorrentes fora do crédito (débito direto). */
    val recurringDebitExpense: Double
        get() = recurringTransactions
            .filter { it.type == TransactionType.EXPENSE && it.paymentMethod != "CREDIT" }
            .sumOf { it.amount }

    val projectedBalance: Double
        get() = projectedIncome - projectedInvoiceTotal - recurringDebitExpense

    val recurringIncomeTransactions: List<Transaction>
        get() = recurringTransactions.filter { it.type == TransactionType.INCOME }
}