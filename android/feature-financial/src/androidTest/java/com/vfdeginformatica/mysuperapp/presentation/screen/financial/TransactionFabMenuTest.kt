package com.vfdeginformatica.mysuperapp.presentation.screen.financial

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.isDisplayed
import androidx.test.espresso.Espresso
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.NewTransactionScreen
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.NewTransactionUiState
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TransactionFabMenuTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun opensFloatingChoicesAndClosesAfterSelectionOrToggle() {
        val selected = mutableListOf<TransactionType>()
        compose.setContent {
            MaterialTheme {
                Scaffold(
                    floatingActionButton = {
                        TransactionFabMenu(expanded = false, onNewTransaction = selected::add)
                    },
                ) { padding -> Box(Modifier.padding(padding)) }
            }
        }
        compose.onNodeWithText("Gasto").assertDoesNotExist()
        compose.onNodeWithText("Receita").assertDoesNotExist()
        compose.onNodeWithContentDescription("Criar nova transação").performClick()
        val expense = compose.onNodeWithText("Gasto")
        assertTrue(
            compose.onRoot(useUnmergedTree = true).printToString(),
            expense.isDisplayed(),
        )
        expense.assertHasClickAction().performClick()
        compose.onNodeWithText("Receita").assertDoesNotExist()
        compose.onNodeWithContentDescription("Criar nova transação").performClick()
        compose.onNodeWithText("Receita").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(listOf(TransactionType.EXPENSE, TransactionType.INCOME), selected) }
        compose.onNodeWithContentDescription("Criar nova transação").performClick()
        compose.onNodeWithContentDescription("Fechar opções").performClick()
        compose.onNodeWithText("Gasto").assertDoesNotExist()
        compose.onNodeWithContentDescription("Criar nova transação").performClick()
        Espresso.pressBack()
        compose.onNodeWithText("Gasto").assertDoesNotExist()
        compose.onNodeWithText("Receita").assertDoesNotExist()
    }

    @Test
    fun newTransactionHasNoTypeSwitcherForEitherChoice() {
        val state = mutableStateOf(NewTransactionUiState(transactionType = TransactionType.EXPENSE))
        compose.setContent { MaterialTheme { NewTransactionScreen(state.value, {}) } }
        compose.onNodeWithText("Título").assertIsDisplayed()
        compose.onNodeWithText("Tipo de Transação").assertDoesNotExist()
        compose.onNodeWithText("Receita").assertDoesNotExist()
        compose.onNodeWithText("Despesa").assertDoesNotExist()
        compose.runOnIdle { state.value = NewTransactionUiState(transactionType = TransactionType.INCOME) }
        compose.onNodeWithText("Tipo de Transação").assertDoesNotExist()
        compose.onNodeWithText("Receita").assertDoesNotExist()
        compose.onNodeWithText("Despesa").assertDoesNotExist()
    }
}
