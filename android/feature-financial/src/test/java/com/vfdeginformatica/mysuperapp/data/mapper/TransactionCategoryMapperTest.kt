package com.vfdeginformatica.mysuperapp.data.mapper

import com.vfdeginformatica.mysuperapp.data.remote.dto.CategoryTransactionDto
import com.vfdeginformatica.mysuperapp.domain.model.TransactionCategory
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionCategoryMapperTest {

    @Test
    fun `dto without transactionType defaults to EXPENSE`() {
        val dto = CategoryTransactionDto(id = "1", title = "Mercado")

        assertEquals(TransactionType.EXPENSE, dto.toModel().type)
    }

    @Test
    fun `unknown transactionType falls back to EXPENSE`() {
        val dto = CategoryTransactionDto(id = "1", title = "X", transactionType = "whatever")

        assertEquals(TransactionType.EXPENSE, dto.toModel().type)
    }

    @Test
    fun `INCOME transactionType is preserved`() {
        val dto = CategoryTransactionDto(id = "1", title = "Salário", transactionType = "INCOME")

        assertEquals(TransactionType.INCOME, dto.toModel().type)
    }

    @Test
    fun `model to dto writes enum name`() {
        val model = TransactionCategory("1", "Salário", "desc", TransactionType.INCOME)

        val dto = model.toDto()

        assertEquals("INCOME", dto.transactionType)
        assertEquals("Salário", dto.title)
        assertEquals("desc", dto.description)
        assertEquals("1", dto.id)
    }

    @Test
    fun `null string maps to EXPENSE`() {
        assertEquals(TransactionType.EXPENSE, (null as String?).toTransactionType())
    }
}
