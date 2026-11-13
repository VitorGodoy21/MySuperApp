package com.vfdeginformatica.mysuperapp.presentation.screen.category_expenses.contract

import com.vfdeginformatica.mysuperapp.domain.model.Transaction
import com.vfdeginformatica.mysuperapp.domain.model.TransactionCategory
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryExpenseReportTest {

    @Test
    fun `splits multi-category expenses and keeps pie percentages at one hundred`() {
        val report = CategoryExpenseReport.from(
            transactions = listOf(
                transaction("shared", 120.0, listOf("home", "food")),
                transaction("uncategorized", 30.0),
                transaction("income", 500.0, type = TransactionType.INCOME),
                transaction("payment", 80.0, paymentMethod = "INVOICE_PAYMENT"),
            ),
            availableCategories = listOf(
                category("home", "Casa"),
                category("food", "Alimentação"),
            )
        )

        assertEquals(150.0, report.total, 0.001)
        assertEquals(60.0, report.categories.single { it.categoryId == "home" }.total, 0.001)
        assertEquals(60.0, report.categories.single { it.categoryId == "food" }.total, 0.001)
        assertEquals(30.0, report.categories.single { it.categoryId == null }.total, 0.001)
        assertEquals(100.0, report.categories.sumOf { it.percent }, 0.001)
    }

    @Test
    fun `chart keeps five largest categories and aggregates the rest`() {
        val report = CategoryExpenseReport.from(
            transactions = (1..7).map { transaction("cat$it", it.toDouble(), listOf("cat$it")) },
            availableCategories = (1..7).map { category("cat$it", "Categoria $it") }
        )

        assertEquals(7, report.categories.size)
        assertEquals(6, report.chartCategories.size)
        assertEquals("Outras", report.chartCategories.last().title)
        assertEquals(3.0, report.chartCategories.last().total, 0.001)
        assertEquals(100.0, report.chartCategories.sumOf { it.percent }, 0.001)
    }

    @Test
    fun `report is empty when there are no expenses`() {
        val report = CategoryExpenseReport.from(
            transactions = listOf(transaction("income", 100.0, type = TransactionType.INCOME)),
            availableCategories = emptyList()
        )

        assertTrue(report.categories.isEmpty())
        assertTrue(report.chartCategories.isEmpty())
        assertEquals(0.0, report.total, 0.001)
    }

    private fun transaction(
        id: String,
        amount: Double,
        categoryIds: List<String> = emptyList(),
        type: TransactionType = TransactionType.EXPENSE,
        paymentMethod: String = "DEBIT",
    ) = Transaction(
        id = id,
        title = id,
        description = "",
        type = type,
        paymentMethod = paymentMethod,
        amount = amount,
        isRecurring = false,
        date = null,
        categoryIds = categoryIds,
        status = "pending",
        cardId = "",
        installments = null,
        installmentNumber = null,
        installmentGroupId = null,
        invoiceMonth = null
    )

    private fun category(id: String, title: String) =
        TransactionCategory(id, title, "", TransactionType.EXPENSE)
}
