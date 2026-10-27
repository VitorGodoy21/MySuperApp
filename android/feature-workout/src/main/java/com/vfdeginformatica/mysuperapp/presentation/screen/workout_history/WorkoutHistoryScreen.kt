package com.vfdeginformatica.mysuperapp.presentation.screen.workout_history

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.session.GetRecentWorkoutSessionsUseCase
import com.vfdeginformatica.mysuperapp.presentation.common.SessionStatsRow
import com.vfdeginformatica.mysuperapp.presentation.common.formatDate
import com.vfdeginformatica.mysuperapp.presentation.common.formatWeekDay
import com.vfdeginformatica.mysuperapp.presentation.common.imageVector
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_history.contract.WorkoutHistoryEntry
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_history.contract.WorkoutHistoryEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_history.contract.WorkoutHistoryUiState
import java.time.LocalDate

@Composable
fun WorkoutHistoryScreen(
    uiState: WorkoutHistoryUiState,
    onEvent: (WorkoutHistoryEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    uiState.sessionPendingDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { onEvent(WorkoutHistoryEvent.DeleteSessionDismissed) },
            title = { Text("Excluir registro?") },
            text = { Text("O treino de ${formatDate(session.date)} será removido do histórico.") },
            confirmButton = {
                TextButton(onClick = { onEvent(WorkoutHistoryEvent.DeleteSessionConfirmed) }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(WorkoutHistoryEvent.DeleteSessionDismissed) }) {
                    Text("Cancelar")
                }
            },
        )
    }

    val historyDays = uiState.historyDays
    val isLoading = uiState.isSessionsLoading || uiState.isLoading
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "history_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Histórico (últimos ${GetRecentWorkoutSessionsUseCase.DEFAULT_DAYS} dias)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
        }
        if (historyDays.isEmpty() && !isLoading) {
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
                    onClick = { onEvent(WorkoutHistoryEvent.SessionClicked(entry.session.id)) },
                    onDelete = { onEvent(WorkoutHistoryEvent.DeleteSessionClicked(entry.session.id)) },
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
