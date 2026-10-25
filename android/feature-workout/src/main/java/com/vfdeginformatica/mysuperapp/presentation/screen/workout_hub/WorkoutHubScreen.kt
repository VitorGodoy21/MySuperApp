package com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.domain.model.Workout
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.session.GetRecentWorkoutSessionsUseCase
import com.vfdeginformatica.mysuperapp.presentation.common.SessionStatsRow
import com.vfdeginformatica.mysuperapp.presentation.common.formatDate
import com.vfdeginformatica.mysuperapp.presentation.common.formatWeekDay
import com.vfdeginformatica.mysuperapp.presentation.common.imageVector
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub.contract.WorkoutHistoryEntry
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub.contract.WorkoutHubEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub.contract.WorkoutHubUiState
import java.time.LocalDate

@Composable
fun WorkoutHubScreen(
    uiState: WorkoutHubUiState,
    onEvent: (WorkoutHubEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    uiState.sessionPendingDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { onEvent(WorkoutHubEvent.DeleteSessionDismissed) },
            title = { Text("Excluir registro?") },
            text = { Text("O treino de ${formatDate(session.date)} será removido do histórico.") },
            confirmButton = {
                TextButton(onClick = { onEvent(WorkoutHubEvent.DeleteSessionConfirmed) }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(WorkoutHubEvent.DeleteSessionDismissed) }) {
                    Text("Cancelar")
                }
            },
        )
    }

    val historyDays = uiState.historyDays
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "title") {
            Text(
                text = "Meus treinos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        if (uiState.isLoading) {
            item(key = "workouts_loading") { CircularProgressIndicator() }
        }
        if (uiState.workouts.isEmpty() && !uiState.isLoading) {
            item(key = "workouts_empty") {
                Text(
                    text = "Nenhum treino montado. Monte seus treinos em Menu → Academia → Treinos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(uiState.workouts, key = { it.id }) { workout ->
            WorkoutCard(
                workout = workout,
                onClick = { onEvent(WorkoutHubEvent.WorkoutClicked(workout.id)) },
            )
        }
        item(key = "diet") {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HorizontalDivider()
                Text(
                    text = "Alimentação",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Card(
                    onClick = { onEvent(WorkoutHubEvent.DietClicked) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Minha dieta",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Consulte suas refeições do dia",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }
                HorizontalDivider()
            }
        }
        item(key = "history_header") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Histórico (últimos ${GetRecentWorkoutSessionsUseCase.DEFAULT_DAYS} dias)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (uiState.isSessionsLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
        }
        if (historyDays.isEmpty() && !uiState.isSessionsLoading) {
            item(key = "history_empty") {
                Text(
                    text = "Nenhum treino registrado nos últimos ${GetRecentWorkoutSessionsUseCase.DEFAULT_DAYS} dias.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        historyDays.forEach { day ->
            item(key = "day_${day.date}") {
                DayHeader(day.date, modifier = Modifier.padding(top = 4.dp))
            }
            items(day.entries, key = { "session_${it.session.id}" }) { entry ->
                HistoryEntryCard(
                    entry = entry,
                    enabled = !uiState.isBusy,
                    onClick = { onEvent(WorkoutHubEvent.SessionClicked(entry.session.id)) },
                    onDelete = { onEvent(WorkoutHubEvent.DeleteSessionClicked(entry.session.id)) },
                )
            }
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatDate(date),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatWeekDay(date),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HistoryEntryCard(
    entry: WorkoutHistoryEntry,
    enabled: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = entry.workoutIcon.imageVector,
                contentDescription = null,
                tint = if (entry.workoutExists) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = entry.workoutName ?: "Treino removido",
                    fontWeight = FontWeight.SemiBold,
                    color = if (entry.workoutExists) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SessionStatsRow(entry.session)
            }
            IconButton(onClick = onDelete, enabled = enabled) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Excluir registro",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun WorkoutCard(workout: Workout, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = workout.icon.imageVector,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(workout.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val count = workout.items.size
                val detail = listOfNotNull(
                    workout.dayOfWeek?.label,
                    "$count exercício" + if (count == 1) "" else "s",
                ).joinToString(" • ")
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
