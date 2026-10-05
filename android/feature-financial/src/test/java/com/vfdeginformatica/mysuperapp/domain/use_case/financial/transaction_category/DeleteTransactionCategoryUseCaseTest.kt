package com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction_category

import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.CategoryTransactionDto
import com.vfdeginformatica.mysuperapp.domain.repository.CategoryTransactionRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteTransactionCategoryUseCaseTest {

    private class FakeCategoryRepository : CategoryTransactionRepository {
        var deletedIds = mutableListOf<String>()
        var deleteResult: Resource<Unit> = Resource.Success(Unit)

        override suspend fun getCategoriesTransaction(): List<CategoryTransactionDto>? = emptyList()
        override suspend fun addCategory(category: CategoryTransactionDto): Resource<String> =
            Resource.Success("id")

        override suspend fun updateCategory(
            id: String, title: String, description: String, transactionType: String
        ): Resource<Unit> = Resource.Success(Unit)

        override suspend fun deleteCategory(id: String): Resource<Unit> {
            deletedIds += id
            return deleteResult
        }
    }

    private class FakeTransactionRepository :
        com.vfdeginformatica.mysuperapp.domain.repository.FakeTransactionRepository()

    @Test
    fun `removes category from transactions before deleting it`() = runTest {
        val categories = FakeCategoryRepository()
        val transactions = FakeTransactionRepository()
        val useCase = DeleteTransactionCategoryUseCase(categories, transactions)

        val results = useCase("cat-1").toList()

        assertEquals(listOf("cat-1"), transactions.cleanedIds)
        assertEquals(listOf("cat-1"), categories.deletedIds)
        assertTrue(results.last() is Resource.Success)
    }

    @Test
    fun `does not delete category when cleanup fails`() = runTest {
        val categories = FakeCategoryRepository()
        val transactions = FakeTransactionRepository().apply {
            cleanupResult = Resource.Error("permission denied")
        }
        val useCase = DeleteTransactionCategoryUseCase(categories, transactions)

        val results = useCase("cat-1").toList()

        assertEquals(listOf("cat-1"), transactions.cleanedIds)
        assertTrue(categories.deletedIds.isEmpty())
        val last = results.last()
        assertTrue(last is Resource.Error)
        assertEquals("permission denied", last.message)
    }

    @Test
    fun `rejects blank id without touching repositories`() = runTest {
        val categories = FakeCategoryRepository()
        val transactions = FakeTransactionRepository()
        val useCase = DeleteTransactionCategoryUseCase(categories, transactions)

        val results = useCase("  ").toList()

        assertTrue(transactions.cleanedIds.isEmpty())
        assertTrue(categories.deletedIds.isEmpty())
        assertFalse(results.any { it is Resource.Success })
        assertTrue(results.last() is Resource.Error)
    }
}
