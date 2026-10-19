package com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract

import com.vfdeginformatica.mysuperapp.data.remote.dto.CardDto
import com.vfdeginformatica.mysuperapp.data.remote.dto.CategoryTransactionDto
import com.vfdeginformatica.mysuperapp.domain.model.Transaction
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType
import java.time.YearMonth

/** Total agrupado na aba Filtros. `key` nulo = "sem categoria"/"sem cartão". */
data class GroupTotal(val key: String?, val total: Double, val count: Int)

/** Total de despesa por categoria no resumo do mês. `categoryId` nulo = sem categoria. */
data class CategoryTotal(val categoryId: String?, val total: Double, val percent: Double)

data class FinancialUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedMonth: YearMonth = YearMonth.now(),
    val selectedTab: FinancialTab = FinancialTab.MONTH,
    val transactions: List<Transaction> = emptyList(),
    val availableCards: List<CardDto> = emptyList(),
    val availableCategories: List<CategoryTransactionDto> = emptyList(),
    // Aba Mês
    val isMonthListExpanded: Boolean = false,
    // Fatura do mês selecionado (query por invoiceMonth + recorrentes materializadas)
    val isLoadingProjection: Boolean = false,
    val invoiceTransactions: List<Transaction> = emptyList(),
    val recurringTransactions: List<Transaction> = emptyList(),
    // Fatura fechada no mês anterior (paga neste mês) — compõe o saldo de caixa
    val isLoadingPaidInvoice: Boolean = false,
    val paidInvoiceTransactions: List<Transaction> = emptyList(),
    val paidInvoiceRecurringTransactions: List<Transaction> = emptyList(),
    // Aba Filtros
    val typeFilter: TransactionTypeFilter = TransactionTypeFilter.ALL,
    val paymentMethodFilter: PaymentMethodFilter = PaymentMethodFilter.ALL,
    val categoryFilterIds: Set<String> = emptySet(),
    val cardFilterId: String? = null,
    val recurringOnly: Boolean = false,
    val installmentsOnly: Boolean = false,
    val groupBy: FilterGroupBy = FilterGroupBy.NONE,
    // Transação em edição (dialog)
    val editingTransaction: Transaction? = null,
    val isSavingEdit: Boolean = false,
    // Ação sobre recorrente aguardando escolha do escopo (abre o dialog de escopo)
    val pendingRecurringAction: PendingRecurringAction? = null
) {
    companion object {
        const val TOP_CATEGORIES = 5
        const val CREDIT = "CREDIT"
    }

    // ---- Resumo do mês (sem filtros) ----

    private val expenses: List<Transaction>
        get() = transactions.filter { it.type == TransactionType.EXPENSE }

    val totalIncome: Double
        get() = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

    val totalExpense: Double
        get() = expenses.sumOf { it.amount }

    /**
     * Saldo de caixa do mês: receitas menos o que efetivamente sai da conta no mês —
     * compras no débito e a fatura fechada no mês anterior. Compras no crédito do mês
     * só debitam no mês seguinte, quando a fatura é paga.
     */
    val balance: Double
        get() = totalIncome - cashExpense

    val creditExpense: Double
        get() = expenses.filter { it.paymentMethod == CREDIT }.sumOf { it.amount }

    val debitExpense: Double
        get() = expenses.filter { it.paymentMethod != CREDIT }.sumOf { it.amount }

    /** Despesas que saem da conta no mês: débito + fatura paga (fechada em M-1). */
    val cashExpense: Double
        get() = debitExpense + paidInvoiceTotal

    val recurringExpense: Double
        get() = expenses.filter { it.isRecurring }.sumOf { it.amount }

    val installmentExpense: Double
        get() = expenses.filter { !it.isRecurring && it.isInstallment }.sumOf { it.amount }

    val singleExpense: Double
        get() = expenses.filter { !it.isRecurring && !it.isInstallment }.sumOf { it.amount }

    /**
     * Despesas por categoria (top N + "Outras"). Transações com várias categorias contam
     * integralmente em cada uma; o percentual usa o total de despesas do mês como base.
     */
    val expenseByCategory: List<CategoryTotal>
        get() {
            val total = totalExpense
            if (total <= 0.0) return emptyList()
            val totals = mutableMapOf<String?, Double>()
            expenses.forEach { t ->
                if (t.categoryIds.isEmpty()) {
                    totals[null] = (totals[null] ?: 0.0) + t.amount
                } else {
                    t.categoryIds.forEach { id -> totals[id] = (totals[id] ?: 0.0) + t.amount }
                }
            }
            val sorted = totals.entries.sortedByDescending { it.value }
            val top = sorted.take(TOP_CATEGORIES)
                .map { CategoryTotal(it.key, it.value, it.value / total * 100) }
            val others = sorted.drop(TOP_CATEGORIES).sumOf { it.value }
            return if (others > 0.0) {
                top + CategoryTotal(OTHERS_KEY, others, others / total * 100)
            } else {
                top
            }
        }

    val expenseByCard: Map<String, Double>
        get() = expenses
            .filter { it.cardId.isNotEmpty() }
            .groupBy { it.cardId }
            .mapValues { (_, items) -> items.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
            .associate { it.key to it.value }

    // ---- Fatura do mês selecionado ----

    val selectedMonthKey: String
        get() = "%04d-%02d".format(selectedMonth.year, selectedMonth.monthValue)

    /**
     * Despesas de crédito recorrentes projetadas na fatura do mês
     * (excluindo as que já entraram pela query de invoiceMonth, para não duplicar).
     */
    val recurringCreditInInvoice: List<Transaction>
        get() = recurringTransactions.filter {
            it.type == TransactionType.EXPENSE &&
                    it.paymentMethod == CREDIT &&
                    it.invoiceMonth != selectedMonthKey
        }

    /** Itens que compõem a fatura do mês (parcelas/compras + recorrentes de crédito). */
    val projectedInvoiceItems: List<Transaction>
        get() = invoiceTransactions + recurringCreditInInvoice

    /** Fatura por cartão (cardId → total). */
    val invoiceByCard: Map<String, Double>
        get() = projectedInvoiceItems
            .groupBy { it.cardId }
            .mapValues { (_, items) -> items.sumOf { it.amount } }

    val projectedInvoiceTotal: Double
        get() = projectedInvoiceItems.sumOf { it.amount }

    // ---- Fatura paga no mês (fechada em M-1) ----

    val paidInvoiceMonth: YearMonth
        get() = selectedMonth.minusMonths(1)

    private val paidInvoiceMonthKey: String
        get() = "%04d-%02d".format(paidInvoiceMonth.year, paidInvoiceMonth.monthValue)

    /** Itens da fatura de M-1 (compras/parcelas + recorrentes de crédito, sem duplicar). */
    val paidInvoiceItems: List<Transaction>
        get() = paidInvoiceTransactions + paidInvoiceRecurringTransactions.filter {
            it.type == TransactionType.EXPENSE &&
                    it.paymentMethod == CREDIT &&
                    it.invoiceMonth != paidInvoiceMonthKey
        }

    val paidInvoiceTotal: Double
        get() = paidInvoiceItems.sumOf { it.amount }

    // ---- Aba Filtros ----

    /** Categorias exibidas no filtro, compatíveis com o filtro de tipo ativo. */
    val categoriesForFilter: List<CategoryTransactionDto>
        get() = when (typeFilter) {
            TransactionTypeFilter.ALL -> availableCategories
            TransactionTypeFilter.INCOME ->
                availableCategories.filter { it.transactionType == TransactionType.INCOME.name }
            TransactionTypeFilter.EXPENSE ->
                availableCategories.filter { it.transactionType == TransactionType.EXPENSE.name }
        }

    val hasActiveFilters: Boolean
        get() = typeFilter != TransactionTypeFilter.ALL ||
                paymentMethodFilter != PaymentMethodFilter.ALL ||
                categoryFilterIds.isNotEmpty() ||
                cardFilterId != null ||
                recurringOnly ||
                installmentsOnly

    /** Transações do mês com todos os filtros da aba Filtros aplicados. */
    val filteredTransactions: List<Transaction>
        get() = transactions.filter { t ->
            val matchesType = when (typeFilter) {
                TransactionTypeFilter.ALL -> true
                TransactionTypeFilter.INCOME -> t.type == TransactionType.INCOME
                TransactionTypeFilter.EXPENSE -> t.type == TransactionType.EXPENSE
            }
            // Receitas não têm forma de pagamento: só casam com "Todas"
            val matchesPayment = when (paymentMethodFilter) {
                PaymentMethodFilter.ALL -> true
                PaymentMethodFilter.CREDIT ->
                    t.type == TransactionType.EXPENSE && t.paymentMethod == CREDIT
                PaymentMethodFilter.DEBIT ->
                    t.type == TransactionType.EXPENSE && t.paymentMethod != CREDIT
            }
            val matchesCategory = categoryFilterIds.isEmpty() ||
                    t.categoryIds.any { it in categoryFilterIds }
            val matchesCard = cardFilterId == null || t.cardId == cardFilterId
            val matchesRecurring = !recurringOnly || t.isRecurring
            val matchesInstallments = !installmentsOnly || t.isInstallment
            matchesType && matchesPayment && matchesCategory &&
                    matchesCard && matchesRecurring && matchesInstallments
        }

    val filteredIncome: Double
        get() = filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

    val filteredExpense: Double
        get() = filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    val filteredBalance: Double
        get() = filteredIncome - filteredExpense

    /**
     * Totais agrupados do resultado filtrado. Por categoria, uma transação com N
     * categorias entra em N grupos; sem categoria/cartão entra na chave nula.
     */
    val groupedTotals: List<GroupTotal>
        get() {
            if (groupBy == FilterGroupBy.NONE) return emptyList()
            val buckets = mutableMapOf<String?, MutableList<Transaction>>()
            filteredTransactions.forEach { t ->
                val keys: List<String?> = when (groupBy) {
                    FilterGroupBy.CATEGORY -> t.categoryIds.ifEmpty { listOf(null) }
                    FilterGroupBy.CARD -> listOf(t.cardId.ifEmpty { null })
                    FilterGroupBy.NONE -> emptyList()
                }
                keys.forEach { key -> buckets.getOrPut(key) { mutableListOf() } += t }
            }
            return buckets.entries
                .map { (key, items) -> GroupTotal(key, items.sumOf { it.amount }, items.size) }
                .sortedByDescending { it.total }
        }
}

/** Chave sintética usada em [FinancialUiState.expenseByCategory] para o agregado "Outras". */
const val OTHERS_KEY = "__others__"

private val Transaction.isInstallment: Boolean
    get() = (installments ?: 0) > 1 || installmentGroupId != null
