package com.vfdeginformatica.mysuperapp.presentation.screen.card_invoice

import androidx.lifecycle.SavedStateHandle
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.CardDto
import com.vfdeginformatica.mysuperapp.data.remote.dto.CategoryTransactionDto
import com.vfdeginformatica.mysuperapp.data.remote.dto.TransactionDto
import com.vfdeginformatica.mysuperapp.domain.repository.CardRepository
import com.vfdeginformatica.mysuperapp.domain.repository.CategoryTransactionRepository
import com.vfdeginformatica.mysuperapp.domain.repository.FakeTransactionRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.card.GetCardsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.DeleteRecurringOccurrenceUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.DeleteTransactionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.GetInvoiceProjectionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.UpdateRecurringOccurrenceUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.UpdateTransactionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction_category.GetTransactionsCategoriesUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.card_invoice.contract.CardInvoiceEvent
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class CardInvoiceViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class FakeCardRepository : CardRepository {
        override suspend fun getCards(): List<CardDto>? = listOf(
            CardDto(id = "c1", bank = "Nubank", finalCode = "1234", closingDay = 5, invoiceDueDate = 12),
            CardDto(id = "c2", bank = "Inter")
        )

        override suspend fun addCard(card: CardDto): Resource<String> = Resource.Success("id")
        override suspend fun updateCard(
            cardId: String, bank: String, paymentMethods: List<String>,
            finalCode: String, closingDay: Int, invoiceDueDate: Int
        ): Resource<Unit> = Resource.Success(Unit)
    }

    private class FakeCategoryRepository : CategoryTransactionRepository {
        override suspend fun getCategoriesTransaction() = emptyList<CategoryTransactionDto>()
        override suspend fun addCategory(category: CategoryTransactionDto): Resource<String> =
            Resource.Success("id")

        override suspend fun updateCategory(
            id: String, title: String, description: String, transactionType: String
        ): Resource<Unit> = Resource.Success(Unit)

        override suspend fun deleteCategory(id: String): Resource<Unit> = Resource.Success(Unit)
    }

    private class InvoiceRepository : FakeTransactionRepository() {
        val requestedMonths = mutableListOf<String>()
        var invoicesByMonth: Map<String, List<TransactionDto>> = emptyMap()

        override suspend fun getTransactionsByInvoiceMonth(invoiceMonth: String): Resource<List<TransactionDto>> {
            requestedMonths += invoiceMonth
            return Resource.Success(invoicesByMonth[invoiceMonth].orEmpty())
        }
    }

    private fun buildViewModel(repo: InvoiceRepository, cardId: String, month: String) =
        CardInvoiceViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    CardInvoiceViewModel.ARG_CARD_ID to cardId,
                    CardInvoiceViewModel.ARG_INVOICE_MONTH to month
                )
            ),
            getInvoiceProjectionUseCase = GetInvoiceProjectionUseCase(repo),
            getCardsUseCase = GetCardsUseCase(FakeCardRepository()),
            getTransactionsCategoriesUseCase = GetTransactionsCategoriesUseCase(FakeCategoryRepository()),
            updateTransactionUseCase = UpdateTransactionUseCase(repo),
            deleteTransactionUseCase = DeleteTransactionUseCase(repo),
            updateRecurringOccurrenceUseCase = UpdateRecurringOccurrenceUseCase(repo),
            deleteRecurringOccurrenceUseCase = DeleteRecurringOccurrenceUseCase(repo)
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads the invoice month from arguments and exposes only the card items`() = runTest {
        val repo = InvoiceRepository()
        repo.invoicesByMonth = mapOf(
            "2026-10" to listOf(
                TransactionDto(
                    id = "tv", title = "TV", transactionType = "EXPENSE", paymentMethod = "CREDIT",
                    amount = 300.0, cardId = "c1", invoiceMonth = "2026-10",
                    installments = 10, installmentNumber = 3, installmentGroupId = "g1"
                ),
                TransactionDto(
                    id = "other", title = "Other", transactionType = "EXPENSE", paymentMethod = "CREDIT",
                    amount = 50.0, cardId = "c2", invoiceMonth = "2026-10"
                )
            )
        )

        val vm = buildViewModel(repo, cardId = "c1", month = "2026-10")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(listOf("2026-10"), repo.requestedMonths)
        assertEquals(YearMonth.of(2026, 10), state.invoiceMonth)
        assertEquals("Nubank", state.card?.bank)
        assertEquals(listOf("tv"), state.items.map { it.id })
        assertEquals(300.0, state.total, 0.001)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `deleting a non recurring item reloads the invoice`() = runTest {
        val repo = InvoiceRepository()
        repo.invoicesByMonth = mapOf(
            "2026-10" to listOf(
                TransactionDto(
                    id = "shoes", title = "Shoes", transactionType = "EXPENSE", paymentMethod = "CREDIT",
                    amount = 200.0, cardId = "c1", invoiceMonth = "2026-10"
                )
            )
        )
        val vm = buildViewModel(repo, cardId = "c1", month = "2026-10")
        advanceUntilIdle()

        vm.onEvent(CardInvoiceEvent.OnTransactionClicked(vm.uiState.value.items.first()))
        vm.onEvent(CardInvoiceEvent.OnDeleteTransaction)
        advanceUntilIdle()

        assertEquals(listOf("shoes"), repo.deletedIds)
        assertNull(vm.uiState.value.editingTransaction)
        assertEquals(2, repo.requestedMonths.size)
    }
}
