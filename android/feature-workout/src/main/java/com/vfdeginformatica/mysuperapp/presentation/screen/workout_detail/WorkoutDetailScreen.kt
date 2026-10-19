package com.vfdeginformatica.mysuperapp.presentation.screen.workout_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.domain.model.Exercise
import com.vfdeginformatica.mysuperapp.domain.model.MuscleGroup
import com.vfdeginformatica.mysuperapp.domain.model.WeekDay
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutItem
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_detail.contract.ItemDraft
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_detail.contract.WorkoutDetailEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_detail.contract.WorkoutDetailUiState
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_detail.contract.WorkoutItemRow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutDetailScreen(
    uiState: WorkoutDetailUiState,
    onEvent: (WorkoutDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    if (uiState.showDeleteConfirmation) {
        DeleteWorkoutDialog(
            name = uiState.name,
            onConfirm = { onEvent(WorkoutDetailEvent.DeleteConfirmed) },
            onDismiss = { onEvent(WorkoutDetailEvent.DeleteDismissed) },
        )
    }

    if (uiState.showExercisePicker) {
        ExercisePickerSheet(
            exercises = uiState.pickerExercises,
            hasAnyExercise = uiState.availableExercises.isNotEmpty(),
            query = uiState.pickerQuery,
            onQueryChanged = { onEvent(WorkoutDetailEvent.PickerQueryChanged(it)) },
            onPick = { onEvent(WorkoutDetailEvent.ExercisePicked(it)) },
            onDismiss = { onEvent(WorkoutDetailEvent.PickerDismissed) },
        )
    }

    val editingIndex = uiState.editingItemIndex
    val draft = uiState.itemDraft
    if (editingIndex != null && draft != null) {
        val row = uiState.itemRows.getOrNull(editingIndex)
        ItemDraftDialog(
            exerciseName = row?.exerciseName ?: WorkoutDetailUiState.REMOVED_EXERCISE_LABEL,
            draft = draft,
            onDraftChanged = { onEvent(WorkoutDetailEvent.ItemDraftChanged(it)) },
            onConfirm = { onEvent(WorkoutDetailEvent.ItemDraftConfirmed) },
            onDismiss = { onEvent(WorkoutDetailEvent.ItemDraftDismissed) },
        )
    }

    val rows = uiState.itemRows

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "name") {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { onEvent(WorkoutDetailEvent.NameChanged(it)) },
                label = { Text("Nome do treino") },
                placeholder = { Text("Ex.: Treino A") },
                isError = uiState.nameError != null,
                supportingText = uiState.nameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !uiState.isBusy,
            )
        }

        item(key = "day") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Dia da semana (opcional)", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    WeekDay.entries.forEach { day ->
                        val selected = uiState.dayOfWeek == day
                        FilterChip(
                            selected = selected,
                            onClick = {
                                onEvent(WorkoutDetailEvent.DayChanged(if (selected) null else day))
                            },
                            label = { Text(day.shortLabel) },
                            enabled = !uiState.isBusy,
                        )
                    }
                }
            }
        }

        item(key = "notes") {
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { onEvent(WorkoutDetailEvent.NotesChanged(it)) },
                label = { Text("Observações (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                enabled = !uiState.isBusy,
            )
        }

        item(key = "exercises_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Exercícios (${rows.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { onEvent(WorkoutDetailEvent.AddExerciseClicked) },
                    enabled = !uiState.isBusy,
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Adicionar")
                }
            }
        }

        if (rows.isEmpty()) {
            item(key = "exercises_empty") {
                Text(
                    text = "Nenhum exercício adicionado. Toque em \"Adicionar\" para montar o treino.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            itemsIndexed(rows, key = { index, row -> "item_${index}_${row.item.exerciseId}" }) { index, row ->
                WorkoutItemCard(
                    row = row,
                    canMoveUp = uiState.canMoveUp(index),
                    canMoveDown = uiState.canMoveDown(index),
                    enabled = !uiState.isBusy,
                    onClick = { onEvent(WorkoutDetailEvent.EditItemClicked(index)) },
                    onMoveUp = { onEvent(WorkoutDetailEvent.MoveItemUp(index)) },
                    onMoveDown = { onEvent(WorkoutDetailEvent.MoveItemDown(index)) },
                    onRemove = { onEvent(WorkoutDetailEvent.RemoveItem(index)) },
                )
            }
        }

        item(key = "save") {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { onEvent(WorkoutDetailEvent.SaveClicked) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.isSubmitEnabled && !uiState.isBusy,
                ) {
                    Text(
                        when {
                            uiState.isSaving -> "Salvando..."
                            uiState.isEditing -> "Salvar alterações"
                            else -> "Criar treino"
                        }
                    )
                }

                if (uiState.isEditing) {
                    HorizontalDivider()
                    OutlinedButton(
                        onClick = { onEvent(WorkoutDetailEvent.DeleteClicked) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isBusy,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(if (uiState.isDeleting) "Excluindo..." else "Excluir treino")
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutItemCard(
    row: WorkoutItemRow,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OrderBadge(number = row.index + 1)
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = enabled, onClick = onClick)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = row.exerciseName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                row.muscleGroupLabel?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = row.prescription.ifBlank { "Toque para definir séries/carga" },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (row.prescription.isBlank()) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column {
                IconButton(onClick = onMoveUp, enabled = enabled && canMoveUp, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Mover para cima", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onMoveDown, enabled = enabled && canMoveDown, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Mover para baixo", modifier = Modifier.size(18.dp))
                }
            }
            IconButton(onClick = onRemove, enabled = enabled) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remover exercício",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun OrderBadge(number: Int) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerSheet(
    exercises: List<Exercise>,
    hasAnyExercise: Boolean,
    query: String,
    onQueryChanged: (String) -> Unit,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Adicionar exercício",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (!hasAnyExercise) {
                Text(
                    text = "Cadastre exercícios primeiro na seção Exercícios.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                return@Column
            }

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChanged,
                label = { Text("Buscar exercício") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (exercises.isEmpty()) {
                Text(
                    text = "Nenhum exercício encontrado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(exercises, key = { it.id }) { exercise ->
                        ListItem(
                            headlineContent = { Text(exercise.name) },
                            supportingContent = exercise.muscleGroup?.let { group -> { Text(group.label) } },
                            modifier = Modifier.clickable { onPick(exercise.id) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemDraftDialog(
    exerciseName: String,
    draft: ItemDraft,
    onDraftChanged: (ItemDraft) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exerciseName, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft.sets,
                        onValueChange = { onDraftChanged(draft.copy(sets = it)) },
                        label = { Text("Séries") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = draft.reps,
                        onValueChange = { onDraftChanged(draft.copy(reps = it)) },
                        label = { Text("Repetições") },
                        placeholder = { Text("10-12") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft.loadKg,
                        onValueChange = { onDraftChanged(draft.copy(loadKg = it)) },
                        label = { Text("Carga (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = draft.restSeconds,
                        onValueChange = { onDraftChanged(draft.copy(restSeconds = it)) },
                        label = { Text("Descanso (s)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = draft.notes,
                    onValueChange = { onDraftChanged(draft.copy(notes = it)) },
                    label = { Text("Observações") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Confirmar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun DeleteWorkoutDialog(
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluir treino?") },
        text = { Text("O treino \"$name\" será excluído. Esta ação não pode ser desfeita.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Excluir", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun WorkoutDetailScreenPreview() {
    WorkoutDetailScreen(
        uiState = WorkoutDetailUiState(
            workoutId = "w1",
            name = "Treino A",
            dayOfWeek = WeekDay.MONDAY,
            items = listOf(
                WorkoutItem("e1", 0, sets = 3, reps = "10-12", loadKg = 20.0, restSeconds = 60),
                WorkoutItem("e2", 1),
            ),
            availableExercises = listOf(
                Exercise("e1", "Supino reto", MuscleGroup.CHEST),
                Exercise("e2", "Agachamento", MuscleGroup.LEGS),
            ),
            isSubmitEnabled = true,
        ),
        onEvent = {}
    )
}
