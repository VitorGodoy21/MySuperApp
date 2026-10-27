package com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction

import androidx.lifecycle.SavedStateHandle
import com.vfdeginformatica.mysuperapp.Screen
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.CardDto
import com.vfdeginformatica.mysuperapp.data.remote.dto.CategoryTransactionDto
import com.vfdeginformatica.mysuperapp.domain.repository.CardRepository
import com.vfdeginformatica.mysuperapp.domain.repository.CategoryTransactionRepository
import com.vfdeginformatica.mysuperapp.domain.repository.FakeTransactionRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.card.GetCardsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.NewTransactionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction_category.GetTransactionsCategoriesUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.NewTransactionEvent
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NewTransactionViewModelTest {
    private class Cards : CardRepository {
        override suspend fun getCards() = listOf(
            CardDto(id = "card", bank = "Banco", paymentMethods = listOf("DEBIT", "CREDIT")),
        )
        override suspend fun addCard(card: CardDto): Resource<String> = Resource.Success("card")
        override suspend fun updateCard(
            cardId: String, bank: String, paymentMethods: List<String>,
            finalCode: String, closingDay: Int, invoiceDueDate: Int,
        ): Resource<Unit> = Resource.Success(Unit)
    }

    private class Categories : CategoryTransactionRepository {
        override suspend fun getCategoriesTransaction() = listOf(
            CategoryTransactionDto(id = "expense", title = "Mercado", transactionType = "EXPENSE"),
            CategoryTransactionDto(id = "income", title = "Salário", transactionType = "INCOME"),
        )
        override suspend fun addCategory(category: CategoryTransactionDto): Resource<String> = Resource.Success("id")
        override suspend fun updateCategory(
            id: String, title: String, description: String, transactionType: String,
        ): Resource<Unit> = Resource.Success(Unit)
        override suspend fun deleteCategory(id: String): Resource<Unit> = Resource.Success(Unit)
    }

    private fun build(type: TransactionType, repository: FakeTransactionRepository) =
        NewTransactionViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(Screen.NewTransactionScreen.TRANSACTION_TYPE to type.name),
            ),
            getCardsUseCase = GetCardsUseCase(Cards()),
            getTransactionsCategoriesUseCase = GetTransactionsCategoriesUseCase(Categories()),
            newTransactionUseCase = NewTransactionUseCase(repository),
        )

    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `navigation choice fixes type and matching categories through reload and save`() = runTest {
        TransactionType.entries.forEach { type ->
            val repository = FakeTransactionRepository()
            val vm = build(type, repository)
            assertEquals(type, vm.uiState.value.transactionType)
            advanceUntilIdle()
            assertEquals(listOf(type.name), vm.uiState.value.categoriesForType.map { it.transactionType })
            vm.onEvent(NewTransactionEvent.TitleChanged("Registro"))
            vm.onEvent(NewTransactionEvent.AmountChanged("1000"))
            vm.onEvent(NewTransactionEvent.CardIdChanged("card"))
            vm.onEvent(NewTransactionEvent.CategoryToggled(if (type == TransactionType.INCOME) "income" else "expense"))
            vm.onEvent(NewTransactionEvent.ReloadCards)
            advanceUntilIdle()
            assertEquals(type, vm.uiState.value.transactionType)
            assertTrue(vm.uiState.value.isSubmitEnabled)
            vm.onEvent(NewTransactionEvent.SaveClicked)
            advanceUntilIdle()
            val saved = repository.added.single()
            assertEquals(type.name, saved.transactionType)
            assertEquals(if (type == TransactionType.INCOME) "" else "DEBIT", saved.paymentMethod)
            assertEquals(listOf(if (type == TransactionType.INCOME) "income" else "expense"), saved.category)
        }
    }
}
