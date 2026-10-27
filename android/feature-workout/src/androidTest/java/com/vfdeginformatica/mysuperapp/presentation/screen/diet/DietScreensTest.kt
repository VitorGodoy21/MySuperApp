package com.vfdeginformatica.mysuperapp.presentation.screen.diet

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.vfdeginformatica.mysuperapp.domain.model.Diet
import com.vfdeginformatica.mysuperapp.domain.model.DietFood
import com.vfdeginformatica.mysuperapp.domain.model.DietMeal
import com.vfdeginformatica.mysuperapp.presentation.screen.diet_detail.DietDetailScreen
import com.vfdeginformatica.mysuperapp.presentation.screen.diet_detail.contract.DietDetailEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.diet_detail.contract.DietDetailUiState
import com.vfdeginformatica.mysuperapp.presentation.screen.diet_view.DietViewScreen
import com.vfdeginformatica.mysuperapp.presentation.screen.diet_view.contract.DietViewUiState
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub.WorkoutHubScreen
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub.contract.WorkoutHubEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub.contract.WorkoutHubUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DietScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun dietCardRemainsBetweenWorkoutsAndHistoryWithoutDataAndWhileLoading() {
        val state = mutableStateOf(WorkoutHubUiState())
        val events = mutableListOf<WorkoutHubEvent>()
        compose.setContent { MaterialTheme { WorkoutHubScreen(state.value, events::add) } }
        val diet = compose.onNodeWithText("Minha dieta").assertIsDisplayed()
        compose.onNodeWithText("Consulte suas refeições do dia").assertIsDisplayed()
        val workouts = compose.onNodeWithText("Meus treinos")
        val measurements = compose.onNodeWithText("Meus dados físicos")
        val history = compose.onNodeWithText("Histórico de treinos")
        assertTrue(workouts.fetchSemanticsNode().boundsInRoot.top < diet.fetchSemanticsNode().boundsInRoot.top)
        assertTrue(diet.fetchSemanticsNode().boundsInRoot.top < measurements.fetchSemanticsNode().boundsInRoot.top)
        assertTrue(measurements.fetchSemanticsNode().boundsInRoot.top < history.fetchSemanticsNode().boundsInRoot.top)
        diet.performClick()
        measurements.performClick()
        history.performClick()
        compose.runOnIdle {
            assertEquals(
                listOf(
                    WorkoutHubEvent.DietClicked,
                    WorkoutHubEvent.BodyMeasurementsClicked,
                    WorkoutHubEvent.HistoryClicked,
                ),
                events,
            )
            state.value = WorkoutHubUiState(isLoading = true)
        }
        compose.onNodeWithText("Minha dieta").assertIsDisplayed().performClick()
    }

    @Test
    fun consultationShowsFreeQuantitiesAndHasNoEditableFields() {
        val state = DietViewUiState(
            isLoading = false,
            diet = Diet(listOf(DietMeal(
                "meal", "Refeição 1", "07:30", "Todos os dias",
                listOf(DietFood("food", "Banana", "entre 120 e 160 g")),
            ))),
        )
        compose.setContent { MaterialTheme { DietViewScreen(state, {}) } }
        compose.onNodeWithText("Refeição 1").assertIsDisplayed()
        compose.onNodeWithText("07:30").assertIsDisplayed()
        compose.onNodeWithText("Banana — entre 120 e 160 g").assertIsDisplayed()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.onNodeWithText("Salvar dieta").assertDoesNotExist()
        compose.onNodeWithText("Adicionar refeição").assertDoesNotExist()
        compose.onNodeWithText("Excluir dieta").assertDoesNotExist()
    }

    @Test
    fun editorChangesDraftAndConfirmsWholeDietDeletion() {
        val state = mutableStateOf(DietDetailUiState(isLoading = false, ownerId = "owner", exists = true))
        val events = mutableListOf<DietDetailEvent>()
        compose.setContent {
            MaterialTheme {
                DietDetailScreen(state.value, { event ->
                    events += event
                    when (event) {
                        is DietDetailEvent.NotesChanged -> state.value = state.value.copy(
                            diet = state.value.diet.copy(notes = event.notes),
                        )
                        DietDetailEvent.DeleteClicked -> state.value = state.value.copy(showDeleteConfirmation = true)
                        DietDetailEvent.DeleteDismissed -> state.value = state.value.copy(showDeleteConfirmation = false)
                        else -> Unit
                    }
                })
            }
        }
        compose.onNodeWithText("Observações gerais (opcional)").performTextReplacement("Rascunho")
        compose.onNodeWithText("Salvar dieta").assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals("Rascunho", state.value.diet.notes)
            assertTrue(DietDetailEvent.Save in events)
        }
        compose.onNodeWithText("Excluir dieta").performScrollTo().performClick()
        compose.onNodeWithText("Excluir dieta?").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Excluir dieta?").assertDoesNotExist()
        compose.onNodeWithText("Excluir dieta").performClick()
        compose.onNodeWithText("Excluir").performClick()
        compose.runOnIdle { assertTrue(DietDetailEvent.DeleteConfirmed in events) }
    }
}
