package com.vfdeginformatica.mysuperapp.presentation.screen.exercise_list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.domain.model.Exercise
import com.vfdeginformatica.mysuperapp.domain.model.MuscleGroup
import com.vfdeginformatica.mysuperapp.presentation.screen.exercise_list.contract.ExerciseGroup
import com.vfdeginformatica.mysuperapp.presentation.screen.exercise_list.contract.ExerciseListEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.exercise_list.contract.ExerciseListUiState

@Composable
fun ExerciseListScreen(
    uiState: ExerciseListUiState,
    onEvent: (ExerciseListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.query,
            onValueChange = { onEvent(ExerciseListEvent.QueryChanged(it)) },
            label = { Text("Buscar exercício") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (uiState.query.isNotEmpty()) {
                    IconButton(onClick = { onEvent(ExerciseListEvent.QueryChanged("")) }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpar busca")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Box(modifier = Modifier.fillMaxSize()) {
            val groups = uiState.groups
            when {
                uiState.isLoading && uiState.exercises.isEmpty() -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )

                uiState.exercises.isEmpty() -> EmptyMessage(
                    text = "Nenhum exercício cadastrado. Toque em + para criar o primeiro.",
                    modifier = Modifier.align(Alignment.Center)
                )

                groups.isEmpty() -> EmptyMessage(
                    text = "Nenhum exercício encontrado para \"${uiState.query.trim()}\"",
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    groups.forEach { group ->
                        item(key = "header_${group.muscleGroup?.name ?: "none"}") {
                            SectionHeader(group)
                        }
                        items(group.exercises, key = { it.id }) { exercise ->
                            ExerciseListItem(
                                exercise = exercise,
                                onClick = { onEvent(ExerciseListEvent.ExerciseClicked(exercise.id)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMessage(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = modifier.padding(24.dp)
    )
}

@Composable
private fun SectionHeader(group: ExerciseGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = group.label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = group.exercises.size.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExerciseListItem(
    exercise: Exercise,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (exercise.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = exercise.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExerciseListScreenPreview() {
    ExerciseListScreen(
        uiState = ExerciseListUiState(
            exercises = listOf(
                Exercise("1", "Supino reto", MuscleGroup.CHEST, "Barra, 4x10"),
                Exercise("2", "Crucifixo", MuscleGroup.CHEST),
                Exercise("3", "Agachamento", MuscleGroup.LEGS, "Livre"),
                Exercise("4", "Alongamento", null),
            )
        ),
        onEvent = {}
    )
}
