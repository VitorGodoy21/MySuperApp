package com.vfdeginformatica.mysuperapp.presentation.screen.category_detail

import androidx.lifecycle.SavedStateHandle
import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.CategoryTransactionDto
import com.vfdeginformatica.mysuperapp.data.remote.dto.TransactionDto
import com.vfdeginformatica.mysuperapp.domain.repository.CategoryTransactionRepository
import com.vfdeginformatica.mysuperapp.domain.repository.TransactionRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction_category.AddTransactionCategoryUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction_category.DeleteTransactionCategoryUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction_category.GetTransactionsCategoriesUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction_category.UpdateTransactionCategoryUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.category_detail.contract.CategoryDetailEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class FakeCategoryRepository(
        private val existing: List<CategoryTransactionDto> = emptyList()
    ) : CategoryTransactionRepository {
        val added = mutableListOf<CategoryTransactionDto>()
        val updated = mutableListOf<Triple<String, String, String>>()

        override suspend fun getCategoriesTransaction() = existing
        override suspend fun addCategory(category: CategoryTransactionDto): Resource<String> {
            added += category
            return Resource.Success("new-id")
        }

        override suspend fun updateCategory(
            id: String, title: String, description: String, transactionType: String
        ): Resource<Unit> {
            updated += Triple(id, title, transactionType)
            return Resource.Success(Unit)
        }

        override suspend fun deleteCategory(id: String): Resource<Unit> = Resource.Success(Unit)
    }

    private class FakeTransactionRepository : TransactionRepository {
        override suspend fun removeCategoryFromTransactions(categoryId: String) =
            Resource.Success(Unit)

        override suspend fun newTransaction(transaction: TransactionDto) = Resource.Success(Unit)
        override suspend fun newTransactions(transactions: List<TransactionDto>) =
            Resource.Success(Unit)

        override suspend fun updateTransaction(id: String, title: String, amount: Double) =
            Resource.Success(Unit)

        override suspend fun deleteTransaction(id: String) = Resource.Success(Unit)
        override suspend fun getTransactions(start: Timestamp, end: Timestamp) =
            Resource.Success(emptyList<TransactionDto>())

        override suspend fun getTransactionsByInvoiceMonth(invoiceMonth: String) =
            Resource.Success(emptyList<TransactionDto>())

        override suspend fun getRecurringTransactions() =
            Resource.Success(emptyList<TransactionDto>())
    }

    private fun buildViewModel(
        categoryId: String?,
        categoryRepository: FakeCategoryRepository,
    ): CategoryDetailViewModel {
        val handle = SavedStateHandle(
            if (categoryId != null) mapOf(CategoryDetailViewModel.CATEGORY_ID_ARG to categoryId)
            else emptyMap()
        )
        return CategoryDetailViewModel(
            savedStateHandle = handle,
            getTransactionsCategoriesUseCase = GetTransactionsCategoriesUseCase(categoryRepository),
            addTransactionCategoryUseCase = AddTransactionCategoryUseCase(categoryRepository),
            updateTransactionCategoryUseCase = UpdateTransactionCategoryUseCase(categoryRepository),
            deleteTransactionCategoryUseCase = DeleteTransactionCategoryUseCase(
                categoryRepository, FakeTransactionRepository()
            ),
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `without categoryId starts in create mode with EXPENSE and submit disabled`() = runTest {
        val vm = buildViewModel(null, FakeCategoryRepository())
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isEditing)
        assertEquals(TransactionType.EXPENSE, state.transactionType)
        assertFalse(state.isSubmitEnabled)
    }

    @Test
    fun `title is required to enable submit`() = runTest {
        val vm = buildViewModel(null, FakeCategoryRepository())

        vm.onEvent(CategoryDetailEvent.TitleChanged("   "))
        assertFalse(vm.uiState.value.isSubmitEnabled)

        vm.onEvent(CategoryDetailEvent.TitleChanged("Mercado"))
        assertTrue(vm.uiState.value.isSubmitEnabled)
    }

    @Test
    fun `save in create mode adds category with selected type`() = runTest {
        val repo = FakeCategoryRepository()
        val vm = buildViewModel(null, repo)

        vm.onEvent(CategoryDetailEvent.TitleChanged(" Salário "))
        vm.onEvent(CategoryDetailEvent.TypeChanged(TransactionType.INCOME))
        vm.onEvent(CategoryDetailEvent.SaveClicked)
        advanceUntilIdle()

        assertEquals(1, repo.added.size)
        assertEquals("Salário", repo.added.first().title)
        assertEquals("INCOME", repo.added.first().transactionType)
        assertTrue(repo.updated.isEmpty())
    }

    @Test
    fun `with categoryId loads existing category and updates on save`() = runTest {
        val repo = FakeCategoryRepository(
            existing = listOf(
                CategoryTransactionDto(
                    id = "cat-1", title = "Mercado", description = "d", transactionType = "EXPENSE"
                )
            )
        )
        val vm = buildViewModel("cat-1", repo)
        advanceUntilIdle()

        val loaded = vm.uiState.value
        assertTrue(loaded.isEditing)
        assertEquals("Mercado", loaded.title)
        assertTrue(loaded.isSubmitEnabled)

        vm.onEvent(CategoryDetailEvent.TypeChanged(TransactionType.INCOME))
        vm.onEvent(CategoryDetailEvent.SaveClicked)
        advanceUntilIdle()

        assertEquals(listOf(Triple("cat-1", "Mercado", "INCOME")), repo.updated)
        assertTrue(repo.added.isEmpty())
    }

    @Test
    fun `delete requires confirmation dialog`() = runTest {
        val vm = buildViewModel("cat-1", FakeCategoryRepository())
        advanceUntilIdle()

        vm.onEvent(CategoryDetailEvent.DeleteClicked)
        assertTrue(vm.uiState.value.showDeleteConfirmation)

        vm.onEvent(CategoryDetailEvent.DeleteDismissed)
        assertFalse(vm.uiState.value.showDeleteConfirmation)
    }
}
