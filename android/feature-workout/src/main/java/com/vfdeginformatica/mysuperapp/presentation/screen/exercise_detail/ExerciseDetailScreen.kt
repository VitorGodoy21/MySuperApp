package com.vfdeginformatica.mysuperapp.presentation.screen.exercise_detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.domain.model.MuscleGroup
import com.vfdeginformatica.mysuperapp.presentation.screen.exercise_detail.contract.ExerciseDetailEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.exercise_detail.contract.ExerciseDetailUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailScreen(
    uiState: ExerciseDetailUiState,
    onEvent: (ExerciseDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    if (uiState.showDeleteConfirmation) {
        DeleteExerciseDialog(
            name = uiState.name,
            onConfirm = { onEvent(ExerciseDetailEvent.DeleteConfirmed) },
            onDismiss = { onEvent(ExerciseDetailEvent.DeleteDismissed) },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = uiState.name,
            onValueChange = { onEvent(ExerciseDetailEvent.NameChanged(it)) },
            label = { Text("Nome") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = uiState.nameError != null,
            supportingText = {
                when {
                    uiState.nameError != null -> Text(uiState.nameError)
                    uiState.duplicateWarning != null -> Text(
                        text = uiState.duplicateWarning,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        )

        Text(text = "Grupo muscular", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MuscleGroup.entries.forEach { group ->
                val selected = uiState.muscleGroup == group
                FilterChip(
                    selected = selected,
                    onClick = {
                        onEvent(ExerciseDetailEvent.MuscleGroupChanged(if (selected) null else group))
                    },
                    label = { Text(group.label) },
                )
            }
        }

        OutlinedTextField(
            value = uiState.description,
            onValueChange = { onEvent(ExerciseDetailEvent.DescriptionChanged(it)) },
            label = { Text("Descrição (opcional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )

        OutlinedTextField(
            value = uiState.notes,
            onValueChange = { onEvent(ExerciseDetailEvent.NotesChanged(it)) },
            label = { Text("Observações (opcional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )

        Button(
            onClick = { onEvent(ExerciseDetailEvent.SaveClicked) },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.isSubmitEnabled && !uiState.isBusy,
        ) {
            Text(
                when {
                    uiState.isSaving -> "Salvando..."
                    uiState.isEditing -> "Salvar alterações"
                    else -> "Criar exercício"
                }
            )
        }

        if (uiState.isEditing) {
            HorizontalDivider()

            if (uiState.usedInWorkouts.isNotEmpty()) {
                Text(
                    text = "Este exercício está em uso nos treinos: " +
                            "${uiState.usedInWorkouts.joinToString(", ")}. " +
                            "Remova-o dos treinos antes de excluir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = { onEvent(ExerciseDetailEvent.DeleteClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canDelete && !uiState.isBusy,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(if (uiState.isDeleting) "Excluindo..." else "Excluir exercício")
            }
        }
    }
}

@Composable
private fun DeleteExerciseDialog(
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluir exercício?") },
        text = {
            Text("O exercício \"$name\" será excluído. Esta ação não pode ser desfeita.")
        },
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
fun ExerciseDetailScreenPreview() {
    ExerciseDetailScreen(
        uiState = ExerciseDetailUiState(
            exerciseId = "1",
            name = "Supino reto",
            muscleGroup = MuscleGroup.CHEST,
            description = "Barra, pegada média",
            usedInWorkouts = listOf("Treino A"),
            isSubmitEnabled = true
        ),
        onEvent = {}
    )
}
