package com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.Transaction
import com.vfdeginformatica.mysuperapp.domain.repository.FakeTransactionRepository
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateTransactionUseCaseTest {

    private fun transaction(
        categoryIds: List<String> = listOf("home"),
        installmentGroupId: String? = null
    ) = Transaction(
        id = "t1",
        title = "TV",
        description = "",
        type = TransactionType.EXPENSE,
        paymentMethod = "CREDIT",
        amount = 300.0,
        isRecurring = false,
        date = null,
        categoryIds = categoryIds,
        status = "pending",
        cardId = "c1",
        installments = installmentGroupId?.let { 10 },
        installmentNumber = installmentGroupId?.let { 3 },
        installmentGroupId = installmentGroupId,
        invoiceMonth = "2026-10"
    )

    @Test
    fun `updates title amount and categories of the document`() = runTest {
        val repo = FakeTransactionRepository()
        val results = UpdateTransactionUseCase(repo)(
            transaction(), "TV 4K", 310.0, listOf("home", "tech")
        ).toList()

        assertEquals(
            listOf(FakeTransactionRepository.UpdateCall("t1", "TV 4K", 310.0, listOf("home", "tech"))),
            repo.updates
        )
        assertTrue(repo.groupCategoryUpdates.isEmpty())
        assertTrue(results.last() is Resource.Success)
    }

    @Test
    fun `installment with changed categories propagates to the whole group`() = runTest {
        val repo = FakeTransactionRepository()
        UpdateTransactionUseCase(repo)(
            transaction(installmentGroupId = "g1"), "TV", 300.0, listOf("tech")
        ).toList()

        assertEquals(listOf("g1" to listOf("tech")), repo.groupCategoryUpdates)
    }

    @Test
    fun `installment with same categories does not touch the group`() = runTest {
        val repo = FakeTransactionRepository()
        UpdateTransactionUseCase(repo)(
            transaction(categoryIds = listOf("a", "b"), installmentGroupId = "g1"),
            "TV", 300.0, listOf("b", "a")
        ).toList()

        assertEquals(1, repo.updates.size)
        assertTrue(repo.groupCategoryUpdates.isEmpty())
    }

    @Test
    fun `document update error stops before propagating to the group`() = runTest {
        val repo = FakeTransactionRepository().apply { writeResult = Resource.Error("denied") }
        val results = UpdateTransactionUseCase(repo)(
            transaction(installmentGroupId = "g1"), "TV", 300.0, listOf("tech")
        ).toList()

        val last = results.last()
        assertTrue(last is Resource.Error)
        assertEquals("denied", last.message)
        assertTrue(repo.groupCategoryUpdates.isEmpty())
    }

    @Test
    fun `group propagation error is reported`() = runTest {
        val repo = FakeTransactionRepository().apply {
            groupCategoryResult = Resource.Error("batch failed")
        }
        val results = UpdateTransactionUseCase(repo)(
            transaction(installmentGroupId = "g1"), "TV", 300.0, listOf("tech")
        ).toList()

        val last = results.last()
        assertTrue(last is Resource.Error)
        assertEquals("batch failed", last.message)
    }

    @Test
    fun `rejects invalid amount without writing`() = runTest {
        val repo = FakeTransactionRepository()
        val results = UpdateTransactionUseCase(repo)(transaction(), "TV", 0.0, emptyList()).toList()

        assertTrue(results.last() is Resource.Error)
        assertTrue(repo.updates.isEmpty())
    }
}
