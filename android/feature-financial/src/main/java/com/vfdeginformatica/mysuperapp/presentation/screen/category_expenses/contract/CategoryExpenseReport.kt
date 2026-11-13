package com.vfdeginformatica.mysuperapp.presentation.screen.category_expenses.contract

import com.vfdeginformatica.mysuperapp.domain.model.FinancialTotals.expenses
import com.vfdeginformatica.mysuperapp.domain.model.Transaction
import com.vfdeginformatica.mysuperapp.domain.model.TransactionCategory
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialUiState
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.OTHERS_KEY
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType

data class CategoryExpenseTotal(
    val categoryId: String?,
    val title: String,
    val total: Double,
    val percent: Double,
)

data class CategoryExpenseReport(
    val total: Double = 0.0,
    val categories: List<CategoryExpenseTotal> = emptyList(),
) {
    val chartCategories: List<CategoryExpenseTotal>
        get() {
            val top = categories.take(FinancialUiState.TOP_CATEGORIES)
            val othersTotal = categories.drop(FinancialUiState.TOP_CATEGORIES).sumOf { it.total }
            return if (othersTotal > 0.0) {
                top + CategoryExpenseTotal(
                    categoryId = OTHERS_KEY,
                    title = "Outras",
                    total = othersTotal,
                    percent = othersTotal / total * 100.0,
                )
            } else {
                top
            }
        }

    companion object {
        fun from(
            transactions: List<Transaction>,
            availableCategories: List<TransactionCategory>,
        ): CategoryExpenseReport {
            val titles = availableCategories
                .filter { it.type == TransactionType.EXPENSE }
                .associate { it.id to it.title }
            val totals = mutableMapOf<String?, Double>()

            transactions.expenses().forEach { transaction ->
                if (transaction.amount <= 0.0) return@forEach
                val categoryIds = transaction.categoryIds.distinct()
                if (categoryIds.isEmpty()) {
                    totals[null] = (totals[null] ?: 0.0) + transaction.amount
                } else {
                    val allocation = transaction.amount / categoryIds.size
                    categoryIds.forEach { id ->
                        totals[id] = (totals[id] ?: 0.0) + allocation
                    }
                }
            }

            val total = totals.values.sum()
            if (total <= 0.0) return CategoryExpenseReport()

            val categories = totals.map { (id, amount) ->
                CategoryExpenseTotal(
                    categoryId = id,
                    title = id?.let { titles[it] } ?: if (id == null) "Sem categoria" else "Categoria",
                    total = amount,
                    percent = amount / total * 100.0,
                )
            }.sortedWith(
                compareByDescending<CategoryExpenseTotal> { it.total }
                    .thenBy { it.title.lowercase() }
            )

            return CategoryExpenseReport(total = total, categories = categories)
        }
    }
}
