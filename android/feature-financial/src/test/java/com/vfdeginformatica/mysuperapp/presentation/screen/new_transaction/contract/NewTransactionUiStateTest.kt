package com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract

import com.vfdeginformatica.mysuperapp.data.remote.dto.CategoryTransactionDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewTransactionUiStateTest {

    private val categories = listOf(
        CategoryTransactionDto(id = "e1", title = "Mercado", transactionType = "EXPENSE"),
        CategoryTransactionDto(id = "e2", title = "Transporte", transactionType = "EXPENSE"),
        CategoryTransactionDto(id = "i1", title = "Salário", transactionType = "INCOME"),
    )

    @Test
    fun `categoriesForType returns only expense categories for EXPENSE`() {
        val state = NewTransactionUiState(
            transactionType = TransactionType.EXPENSE,
            availableCategories = categories
        )

        assertEquals(listOf("e1", "e2"), state.categoriesForType.map { it.id })
    }

    @Test
    fun `categoriesForType returns only income categories for INCOME`() {
        val state = NewTransactionUiState(
            transactionType = TransactionType.INCOME,
            availableCategories = categories
        )

        assertEquals(listOf("i1"), state.categoriesForType.map { it.id })
    }

    @Test
    fun `categoriesForType is empty when categories are not loaded`() {
        val state = NewTransactionUiState(availableCategories = null)

        assertTrue(state.categoriesForType.isEmpty())
    }
}
