package com.vfdeginformatica.mysuperapp.presentation.screen.financial

import com.google.firebase.Timestamp
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
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.GetTransactionsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.UpdateRecurringOccurrenceUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.UpdateTransactionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction_category.GetTransactionsCategoriesUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FilterGroupBy
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialTab
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.PaymentMethodFilter
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.TransactionTypeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FinancialViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class FakeCardRepository : CardRepository {
        override suspend fun getCards(): List<CardDto>? = emptyList()
        override suspend fun addCard(card: CardDto): Resource<String> = Resource.Success("id")
        override suspend fun updateCard(
            cardId: String, bank: String, paymentMethods: List<String>,
            finalCode: String, closingDay: Int, invoiceDueDate: Int
        ): Resource<Unit> = Resource.Success(Unit)
    }

    private class FakeCategoryRepository : CategoryTransactionRepository {
        override suspend fun getCategoriesTransaction() = listOf(
            CategoryTransactionDto(id = "food", title = "Mercado", transactionType = "EXPENSE"),
            CategoryTransactionDto(id = "sal", title = "Salário", transactionType = "INCOME")
        )

        override suspend fun addCategory(category: CategoryTransactionDto): Resource<String> =
            Resource.Success("id")

        override suspend fun updateCategory(
            id: String, title: String, description: String, transactionType: String
        ): Resource<Unit> = Resource.Success(Unit)

        override suspend fun deleteCategory(id: String): Resource<Unit> = Resource.Success(Unit)
    }

    /** Conta as consultas de mês e de fatura para verificar o recarregamento. */
    private class CountingTransactionRepository : FakeTransactionRepository() {
        var monthQueries = 0
        var invoiceQueries = 0
        val requestedInvoiceMonths = mutableListOf<String>()
        var invoicesByMonth: Map<String, List<TransactionDto>> = emptyMap()

        override suspend fun getTransactions(start: Timestamp, end: Timestamp): Resource<List<TransactionDto>> {
            monthQueries++
            return super.getTransactions(start, end)
        }

        override suspend fun getTransactionsByInvoiceMonth(invoiceMonth: String): Resource<List<TransactionDto>> {
            invoiceQueries++
            requestedInvoiceMonths += invoiceMonth
            return Resource.Success(invoicesByMonth[invoiceMonth].orEmpty())
        }
    }

    private fun buildViewModel(repo: CountingTransactionRepository) = FinancialViewModel(
        getTransactionsUseCase = GetTransactionsUseCase(repo),
        getCardsUseCase = GetCardsUseCase(FakeCardRepository()),
        getTransactionsCategoriesUseCase = GetTransactionsCategoriesUseCase(FakeCategoryRepository()),
        getInvoiceProjectionUseCase = GetInvoiceProjectionUseCase(repo),
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
    fun `init loads month transactions and invoice for selected month`() = runTest {
        val repo = CountingTransactionRepository()
        buildViewModel(repo)
        advanceUntilIdle()

        assertEquals(1, repo.monthQueries)
        // fatura do mês selecionado (projeção) + fatura de M-1 (paga no mês) + parcelas do mês
        assertEquals(3, repo.invoiceQueries)
    }

    @Test
    fun `paid invoice loads documents whose invoiceMonth is the previous month`() = runTest {
        val repo = CountingTransactionRepository()
        val now = java.time.YearMonth.now()
        val prev = now.minusMonths(1)
        val key = { m: java.time.YearMonth -> "%04d-%02d".format(m.year, m.monthValue) }
        repo.invoicesByMonth = mapOf(
            key(prev) to listOf(
                TransactionDto(
                    id = "prev-credit", title = "TV", transactionType = "EXPENSE",
                    paymentMethod = "CREDIT", amount = 300.0, cardId = "c1", invoiceMonth = key(prev)
                )
            ),
            key(now) to listOf(
                TransactionDto(
                    id = "cur-credit", title = "Shoes", transactionType = "EXPENSE",
                    paymentMethod = "CREDIT", amount = 200.0, cardId = "c1", invoiceMonth = key(now)
                )
            )
        )

        val vm = buildViewModel(repo)
        advanceUntilIdle()

        assertTrue(repo.requestedInvoiceMonths.contains(key(prev)))
        assertEquals(listOf("prev-credit"), vm.uiState.value.paidInvoiceTransactions.map { it.id })
        assertEquals(300.0, vm.uiState.value.paidInvoiceTotal, 0.001)
        assertEquals(-300.0, vm.uiState.value.balance, 0.001)
        assertFalse(vm.uiState.value.isLoadingPaidInvoice)
    }

    @Test
    fun `changing month reloads both transactions and invoice`() = runTest {
        val repo = CountingTransactionRepository()
        val vm = buildViewModel(repo)
        advanceUntilIdle()
        val initial = vm.uiState.value.selectedMonth

        vm.onEvent(FinancialEvent.OnNextMonth)
        advanceUntilIdle()

        assertEquals(initial.plusMonths(1), vm.uiState.value.selectedMonth)
        assertEquals(2, repo.monthQueries)
        assertEquals(6, repo.invoiceQueries)
    }

    @Test
    fun `changing tab does not trigger reload`() = runTest {
        val repo = CountingTransactionRepository()
        val vm = buildViewModel(repo)
        advanceUntilIdle()

        vm.onEvent(FinancialEvent.OnTabChanged(FinancialTab.FILTERS))
        advanceUntilIdle()

        assertEquals(FinancialTab.FILTERS, vm.uiState.value.selectedTab)
        assertEquals(1, repo.monthQueries)
        assertEquals(3, repo.invoiceQueries)
    }

    @Test
    fun `clicking a card invoice emits navigation with the displayed invoice month`() = runTest(dispatcher) {
        val vm = buildViewModel(CountingTransactionRepository())
        val effects = mutableListOf<FinancialEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.effect.toList(effects)
        }
        advanceUntilIdle()
        val month = vm.uiState.value.selectedMonth.plusMonths(1)

        vm.onEvent(FinancialEvent.OnCardInvoiceClicked("c1"))
        advanceUntilIdle()

        val expectedMonth = "%04d-%02d".format(month.year, month.monthValue)
        assertEquals(
            listOf(FinancialEffect.NavigateToCardInvoice("c1", expectedMonth)),
            effects
        )
    }

    @Test
    fun `invoice card loads the next month invoice`() = runTest {
        val repo = CountingTransactionRepository()
        val next = java.time.YearMonth.now().plusMonths(1)
        val key = "%04d-%02d".format(next.year, next.monthValue)
        repo.invoicesByMonth = mapOf(
            key to listOf(
                TransactionDto(
                    id = "next-credit", title = "Phone", transactionType = "EXPENSE",
                    paymentMethod = "CREDIT", amount = 150.0, cardId = "c1", invoiceMonth = key
                )
            )
        )

        val vm = buildViewModel(repo)
        advanceUntilIdle()

        assertEquals(key, vm.uiState.value.invoiceMonthKey)
        assertEquals(listOf("next-credit"), vm.uiState.value.invoiceTransactions.map { it.id })
        assertEquals(150.0, vm.uiState.value.projectedInvoiceTotal, 0.001)
    }

    @Test
    fun `type filter change drops incompatible categories`() = runTest {
        val vm = buildViewModel(CountingTransactionRepository())
        advanceUntilIdle()

        vm.onEvent(FinancialEvent.OnCategoryFilterToggled("food"))
        vm.onEvent(FinancialEvent.OnCategoryFilterToggled("sal"))
        assertEquals(setOf("food", "sal"), vm.uiState.value.categoryFilterIds)

        vm.onEvent(FinancialEvent.OnTypeFilterChanged(TransactionTypeFilter.INCOME))
        assertEquals(setOf("sal"), vm.uiState.value.categoryFilterIds)
    }

    @Test
    fun `category toggle removes already selected id`() = runTest {
        val vm = buildViewModel(CountingTransactionRepository())
        advanceUntilIdle()

        vm.onEvent(FinancialEvent.OnCategoryFilterToggled("food"))
        vm.onEvent(FinancialEvent.OnCategoryFilterToggled("food"))
        assertTrue(vm.uiState.value.categoryFilterIds.isEmpty())
    }

    @Test
    fun `clear filters resets everything`() = runTest {
        val vm = buildViewModel(CountingTransactionRepository())
        advanceUntilIdle()

        vm.onEvent(FinancialEvent.OnTypeFilterChanged(TransactionTypeFilter.EXPENSE))
        vm.onEvent(FinancialEvent.OnPaymentMethodFilterChanged(PaymentMethodFilter.CREDIT))
        vm.onEvent(FinancialEvent.OnCategoryFilterToggled("food"))
        vm.onEvent(FinancialEvent.OnCardFilterChanged("c1"))
        vm.onEvent(FinancialEvent.OnRecurringOnlyToggled)
        vm.onEvent(FinancialEvent.OnInstallmentsOnlyToggled)
        vm.onEvent(FinancialEvent.OnGroupByChanged(FilterGroupBy.CARD))
        assertTrue(vm.uiState.value.hasActiveFilters)

        vm.onEvent(FinancialEvent.OnClearFilters)
        val state = vm.uiState.value
        assertFalse(state.hasActiveFilters)
        assertEquals(TransactionTypeFilter.ALL, state.typeFilter)
        assertEquals(PaymentMethodFilter.ALL, state.paymentMethodFilter)
        assertTrue(state.categoryFilterIds.isEmpty())
        assertNull(state.cardFilterId)
        assertFalse(state.recurringOnly)
        assertFalse(state.installmentsOnly)
        assertEquals(FilterGroupBy.NONE, state.groupBy)
    }

    @Test
    fun `month list toggles`() = runTest {
        val vm = buildViewModel(CountingTransactionRepository())
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isMonthListExpanded)
        vm.onEvent(FinancialEvent.OnToggleMonthList)
        assertTrue(vm.uiState.value.isMonthListExpanded)
    }
}
