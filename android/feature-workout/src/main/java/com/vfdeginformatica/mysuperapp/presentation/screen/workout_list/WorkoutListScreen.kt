package com.vfdeginformatica.mysuperapp.presentation.screen.workout_list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
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
import com.vfdeginformatica.mysuperapp.domain.model.WeekDay
import com.vfdeginformatica.mysuperapp.domain.model.Workout
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutItem
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_list.contract.WorkoutListEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_list.contract.WorkoutListUiState
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_list.contract.WorkoutRow

@Composable
fun WorkoutListScreen(
    uiState: WorkoutListUiState,
    onEvent: (WorkoutListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = uiState.rows

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading && rows.isEmpty() -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )

            rows.isEmpty() -> Text(
                text = "Nenhum treino criado.\nToque em + para montar seu primeiro treino (ex.: Treino A).",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rows, key = { it.workout.id }) { row ->
                    WorkoutListItem(
                        row = row,
                        onClick = { onEvent(WorkoutListEvent.WorkoutClicked(row.workout.id)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutListItem(
    row: WorkoutRow,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = row.workout.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                row.dayLabel?.let { day ->
                    AssistChip(
                        onClick = onClick,
                        label = { Text(day, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            Text(
                text = exerciseCountLabel(row.exerciseCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (row.preview.isNotBlank()) {
                Text(
                    text = row.preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun exerciseCountLabel(count: Int): String =
    if (count == 1) "1 exercício" else "$count exercícios"

@Preview(showBackground = true)
@Composable
fun WorkoutListScreenPreview() {
    WorkoutListScreen(
        uiState = WorkoutListUiState(
            workouts = listOf(
                Workout(
                    id = "w1", name = "Treino A", dayOfWeek = WeekDay.MONDAY,
                    items = listOf(
                        WorkoutItem("e1", 0), WorkoutItem("e2", 1),
                        WorkoutItem("e3", 2), WorkoutItem("e4", 3),
                    )
                ),
                Workout(id = "w2", name = "Treino B", items = listOf(WorkoutItem("e2", 0))),
            ),
            exercises = listOf(
                Exercise("e1", "Supino reto", MuscleGroup.CHEST),
                Exercise("e2", "Agachamento", MuscleGroup.LEGS),
                Exercise("e3", "Remada curvada", MuscleGroup.BACK),
            ),
        ),
        onEvent = {}
    )
}
