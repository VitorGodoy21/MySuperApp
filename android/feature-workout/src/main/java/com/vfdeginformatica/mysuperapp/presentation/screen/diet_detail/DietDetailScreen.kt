package com.vfdeginformatica.mysuperapp.presentation.screen.diet_detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.domain.model.DietMeal
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.diet.validationError
import com.vfdeginformatica.mysuperapp.presentation.screen.diet_detail.contract.DietDetailEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.diet_detail.contract.DietDetailUiState

@Composable
fun DietDetailScreen(
    state: DietDetailUiState,
    onEvent: (DietDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { onEvent(DietDetailEvent.DeleteDismissed) },
            title = { Text("Excluir dieta?") },
            text = { Text("Todas as refeições e alimentos da sua dieta serão excluídos.") },
            confirmButton = {
                TextButton(onClick = { onEvent(DietDetailEvent.DeleteConfirmed) }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(DietDetailEvent.DeleteDismissed) }) { Text("Cancelar") }
            },
        )
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            state.isLoading -> item { CircularProgressIndicator() }
            state.error != null -> item {
                Text(state.error, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { onEvent(DietDetailEvent.Retry) }) { Text("Tentar novamente") }
            }
            else -> {
                item {
                    Text("A mesma dieta para todos os dias. As alterações só serão gravadas ao salvar.")
                    DietField(
                        state.diet.notes, "Observações gerais (opcional)", state.canEdit,
                        onChange = { onEvent(DietDetailEvent.NotesChanged(it)) },
                    )
                }
                itemsIndexed(state.diet.meals, key = { _, meal -> meal.id }) { index, meal ->
                    MealEditor(meal, index, state.diet.meals.size, state.canEdit, onEvent)
                }
                item {
                    TextButton(enabled = state.canEdit, onClick = { onEvent(DietDetailEvent.AddMeal) }) {
                        Text("Adicionar refeição")
                    }
                    Button(
                        enabled = state.canEdit && state.diet.meals.all { meal ->
                            meal.validationError() == null && meal.foods.all { it.validationError() == null }
                        },
                        onClick = { onEvent(DietDetailEvent.Save) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Salvar dieta") }
                    if (state.isSaving) CircularProgressIndicator()
                    if (state.exists) {
                        TextButton(enabled = state.canEdit, onClick = { onEvent(DietDetailEvent.DeleteClicked) }) {
                            Text("Excluir dieta", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealEditor(
    meal: DietMeal,
    index: Int,
    count: Int,
    enabled: Boolean,
    onEvent: (DietDetailEvent) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OrderControls(
                "refeição", index, count, enabled,
                onMove = { onEvent(DietDetailEvent.MoveMeal(meal.id, it)) },
                onDelete = { onEvent(DietDetailEvent.RemoveMeal(meal.id)) },
            )
            DietField(
                meal.name, "Nome da refeição", enabled,
                error = if (meal.name.isBlank()) "Informe o nome da refeição" else null,
                onChange = { onEvent(DietDetailEvent.MealChanged(meal.copy(name = it))) },
            )
            DietField(
                meal.time, "Horário (opcional, HH:mm)", enabled,
                error = if (meal.name.isNotBlank()) meal.validationError() else null,
                onChange = { onEvent(DietDetailEvent.MealChanged(meal.copy(time = it))) },
            )
            DietField(
                meal.notes, "Observações da refeição (opcional)", enabled,
                onChange = { onEvent(DietDetailEvent.MealChanged(meal.copy(notes = it))) },
            )
            meal.foods.forEachIndexed { foodIndex, food ->
                key(food.id) {
                    HorizontalDivider()
                    OrderControls(
                        "alimento", foodIndex, meal.foods.size, enabled,
                        onMove = { onEvent(DietDetailEvent.MoveFood(meal.id, food.id, it)) },
                        onDelete = { onEvent(DietDetailEvent.RemoveFood(meal.id, food.id)) },
                    )
                    DietField(
                        food.name, "Alimento", enabled,
                        error = if (food.name.isBlank()) "Informe o nome do alimento" else null,
                        onChange = { onEvent(DietDetailEvent.FoodChanged(meal.id, food.copy(name = it))) },
                    )
                    DietField(
                        food.quantity, "Quantidade (ex.: entre 120 e 160 g)", enabled,
                        error = if (food.quantity.isBlank()) "Informe a quantidade do alimento" else null,
                        onChange = { onEvent(DietDetailEvent.FoodChanged(meal.id, food.copy(quantity = it))) },
                    )
                }
            }
            TextButton(enabled = enabled, onClick = { onEvent(DietDetailEvent.AddFood(meal.id)) }) {
                Text("Adicionar alimento")
            }
        }
    }
}

@Composable
private fun DietField(
    value: String,
    label: String,
    enabled: Boolean,
    error: String? = null,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        enabled = enabled,
        isError = error != null,
        supportingText = if (error != null) { { Text(error) } } else null,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun OrderControls(
    label: String,
    index: Int,
    count: Int,
    enabled: Boolean,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        IconButton(enabled = enabled && index > 0, onClick = { onMove(-1) }) {
            Icon(Icons.Default.ArrowUpward, "Mover $label para cima")
        }
        IconButton(enabled = enabled && index < count - 1, onClick = { onMove(1) }) {
            Icon(Icons.Default.ArrowDownward, "Mover $label para baixo")
        }
        IconButton(enabled = enabled, onClick = onDelete) {
            Icon(Icons.Default.Delete, "Excluir $label", tint = MaterialTheme.colorScheme.error)
        }
    }
}
