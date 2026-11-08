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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.session.GetRecentWorkoutSessionsUseCase
import com.vfdeginformatica.mysuperapp.presentation.common.SessionStatsRow
import com.vfdeginformatica.mysuperapp.presentation.common.WeekBarChart
import com.vfdeginformatica.mysuperapp.presentation.common.formatDate
import com.vfdeginformatica.mysuperapp.presentation.common.formatShortDate
import com.vfdeginformatica.mysuperapp.presentation.common.formatWeekDay
import com.vfdeginformatica.mysuperapp.presentation.common.imageVector
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_history.contract.HistoryMode
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_history.contract.WorkoutHistoryEntry
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_history.contract.WorkoutHistoryEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_history.contract.WorkoutHistoryUiState
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
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

    val isLoading = uiState.isSessionsLoading || uiState.isLoading
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "history_mode") {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                HistoryMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = uiState.mode == mode,
                        onClick = { onEvent(WorkoutHistoryEvent.ModeChanged(mode)) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = HistoryMode.entries.size),
                    ) {
                        Text(if (mode == HistoryMode.DAYS) "Dias" else "Semanas")
                    }
                }
            }
        }
        when (uiState.mode) {
            HistoryMode.DAYS -> daysContent(uiState, isLoading, onEvent)
            HistoryMode.WEEKS -> weeksContent(uiState, onEvent)
        }
    }
}

private fun LazyListScope.daysContent(
    uiState: WorkoutHistoryUiState,
    isLoading: Boolean,
    onEvent: (WorkoutHistoryEvent) -> Unit,
) {
    val historyDays = uiState.historyDays
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

private fun LazyListScope.weeksContent(
    uiState: WorkoutHistoryUiState,
    onEvent: (WorkoutHistoryEvent) -> Unit,
) {
    val isLoading = uiState.isWeekSessionsLoading || uiState.isLoading
    val week = uiState.selectedWeek
    val summary = uiState.weekSummary
    val comparison = uiState.weekComparison
    val weekDays = uiState.weekHistoryDays

    item(key = "week_selector") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onEvent(WorkoutHistoryEvent.PreviousWeek) },
                enabled = uiState.canGoPreviousWeek,
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Semana anterior")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (week.isCurrent(uiState.today)) {
                    Text(
                        text = "Esta semana",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = "${formatShortDate(week.start)} – ${formatShortDate(week.end)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            IconButton(
                onClick = { onEvent(WorkoutHistoryEvent.NextWeek) },
                enabled = uiState.canGoNextWeek,
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próxima semana")
            }
        }
    }

    item(key = "week_summary") {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Resumo da semana",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    WeekStat(
                        label = "Treinos",
                        value = summary.sessions.toString(),
                        delta = comparison?.sessionsDelta,
                        modifier = Modifier.weight(1f),
                    )
                    WeekStat(
                        label = "Minutos",
                        value = summary.minutes.toString(),
                        delta = comparison?.minutesDelta,
                        modifier = Modifier.weight(1f),
                    )
                    WeekStat(
                        label = "kcal",
                        value = summary.calories.toString(),
                        delta = comparison?.caloriesDelta,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = if (comparison == null) "Sem semana anterior para comparar"
                    else "Comparado com a semana anterior",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                WeekBarChart(
                    minutesByDay = summary.minutesByDay,
                    sessionsByDay = summary.sessionsByDay,
                    todayIndex = week.indexOf(uiState.today),
                )
            }
        }
    }

    if (weekDays.isEmpty() && !isLoading) {
        item(key = "week_empty") {
            Text(
                text = "Nenhum treino nesta semana.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    weekDays.forEach { day ->
        item(key = "week_day_${day.date}") {
            DayHeader(day.date, modifier = Modifier.padding(top = 4.dp))
        }
        items(day.entries, key = { "week_session_${it.session.id}" }) { entry ->
            HistoryEntryCard(
                entry = entry,
                enabled = !uiState.isBusy,
                onClick = { onEvent(WorkoutHistoryEvent.SessionClicked(entry.session.id)) },
                onDelete = { onEvent(WorkoutHistoryEvent.DeleteSessionClicked(entry.session.id)) },
            )
        }
    }
}

@Composable
private fun WeekStat(label: String, value: String, delta: Int?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val (deltaText, deltaColor) = when {
            delta == null -> "—" to MaterialTheme.colorScheme.onSurfaceVariant
            delta > 0 -> "▲ +$delta" to MaterialTheme.colorScheme.primary
            delta < 0 -> "▼ $delta" to MaterialTheme.colorScheme.error
            else -> "= 0" to MaterialTheme.colorScheme.onSurfaceVariant
        }
        Text(text = deltaText, style = MaterialTheme.typography.bodySmall, color = deltaColor)
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
