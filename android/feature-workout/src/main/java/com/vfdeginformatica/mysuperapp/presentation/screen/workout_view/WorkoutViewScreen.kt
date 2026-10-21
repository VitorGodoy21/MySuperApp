package com.vfdeginformatica.mysuperapp.presentation.screen.workout_view

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.domain.model.Workout
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutSession
import com.vfdeginformatica.mysuperapp.presentation.common.ItemDraftDialog
import com.vfdeginformatica.mysuperapp.presentation.common.SessionStatsRow
import com.vfdeginformatica.mysuperapp.presentation.common.YouTubePlayerDialog
import com.vfdeginformatica.mysuperapp.presentation.common.formatCalories
import com.vfdeginformatica.mysuperapp.presentation.common.formatDate
import com.vfdeginformatica.mysuperapp.presentation.common.formatDuration
import com.vfdeginformatica.mysuperapp.presentation.common.formatKg
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_detail.contract.WorkoutItemRow
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_view.contract.SessionDraft
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_view.contract.WorkoutViewEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_view.contract.WorkoutViewUiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun WorkoutViewScreen(
    uiState: WorkoutViewUiState,
    onEvent: (WorkoutViewEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val workout = uiState.workout
    if (uiState.isLoading || workout == null) {
        Box(modifier = modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    uiState.playingVideo?.let { playback ->
        YouTubePlayerDialog(
            title = playback.exerciseName,
            videoId = playback.videoId,
            onDismiss = { onEvent(WorkoutViewEvent.VideoDialogDismissed) },
        )
    }

    uiState.itemDraft?.let { draft ->
        val row = uiState.editingItemIndex?.let { uiState.itemRows.getOrNull(it) }
        ItemDraftDialog(
            exerciseName = row?.exerciseName ?: "Exercício",
            draft = draft,
            isSaving = uiState.isSavingItem,
            onDraftChanged = { onEvent(WorkoutViewEvent.ItemDraftChanged(it)) },
            onConfirm = { onEvent(WorkoutViewEvent.ItemDraftConfirmed) },
            onDismiss = { onEvent(WorkoutViewEvent.ItemDraftDismissed) },
        )
    }

    uiState.sessionDraft?.let { draft ->
        RegisterSessionDialog(
            workoutName = workout.name,
            draft = draft,
            isSaving = uiState.isSavingSession,
            onEvent = onEvent,
        )
    }

    uiState.sessionPendingDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { onEvent(WorkoutViewEvent.DeleteSessionDismissed) },
            title = { Text("Excluir registro?") },
            text = { Text("O treino de ${formatDate(session.date)} será removido do histórico.") },
            confirmButton = {
                TextButton(onClick = { onEvent(WorkoutViewEvent.DeleteSessionConfirmed) }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(WorkoutViewEvent.DeleteSessionDismissed) }) {
                    Text("Cancelar")
                }
            },
        )
    }

    val rows = uiState.itemRows

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") { WorkoutHeader(workout = workout, uiState = uiState) }

        item(key = "exercises_header") {
            Text(
                text = "Exercícios (${rows.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        if (rows.isEmpty()) {
            item(key = "exercises_empty") {
                Text(
                    text = "Este treino ainda não tem exercícios. Edite-o em Menu → Academia → Treinos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            item(key = "exercises_hint") {
                Text(
                    text = "Toque em um exercício para ajustar séries, repetições e carga.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            itemsIndexed(rows, key = { index, row -> "item_${index}_${row.item.exerciseId}" }) { index, row ->
                ReadOnlyItemCard(
                    row = row,
                    enabled = !uiState.isBusy,
                    onClick = { onEvent(WorkoutViewEvent.ItemClicked(index)) },
                    onPlayVideo = { onEvent(WorkoutViewEvent.PlayVideoClicked(index)) },
                )
            }
        }

        item(key = "sessions_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Treinos registrados (${uiState.sessions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (uiState.isSessionsLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
        }

        if (uiState.sessions.isEmpty() && !uiState.isSessionsLoading) {
            item(key = "sessions_empty") {
                Text(
                    text = "Nenhum treino registrado. Toque em \"Registrar treino\" ao terminar na academia.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(uiState.sessions, key = { "session_${it.id}" }) { session ->
                SessionRow(
                    session = session,
                    enabled = !uiState.isBusy,
                    onDelete = { onEvent(WorkoutViewEvent.DeleteSessionClicked(session.id)) },
                )
            }
        }
    }
}

@Composable
private fun WorkoutHeader(workout: Workout, uiState: WorkoutViewUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(workout.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            workout.dayOfWeek?.let {
                Text(
                    it.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (workout.notes.isNotBlank()) {
                Text(
                    workout.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val last = uiState.lastSession
            if (last != null) {
                Spacer(Modifier.size(4.dp))
                Text(
                    text = buildString {
                        append("Último treino: ${formatDate(last.date)}")
                        if (uiState.sessions.size > 1) append(" • ${uiState.sessions.size} vezes")
                        if (uiState.totalMinutes > 0) append(" • ${formatDuration(uiState.totalMinutes)} no total")
                        if (uiState.totalCalories > 0) append(" • ${formatCalories(uiState.totalCalories)}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReadOnlyItemCard(
    row: WorkoutItemRow,
    enabled: Boolean,
    onClick: () -> Unit,
    onPlayVideo: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OrderBadge(number = row.index + 1)
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = row.exerciseName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                row.muscleGroupLabel?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PrescriptionChips(row = row)
                if (row.item.notes.isNotBlank()) {
                    Text(
                        text = row.item.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (row.videoId != null) {
                IconButton(onClick = onPlayVideo) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = "Assistir vídeo do exercício",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** Séries / repetições / carga / descanso as small labelled chips; a hint when nothing is set. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrescriptionChips(row: WorkoutItemRow) {
    val item = row.item
    val chips = buildList {
        item.sets?.let { add("Séries" to it.toString()) }
        item.reps?.takeIf { it.isNotBlank() }?.let { add("Reps" to it) }
        item.loadKg?.let { add("Carga" to formatKg(it)) }
        item.restSeconds?.let { add("Descanso" to "$it s") }
    }
    if (chips.isEmpty()) {
        Text(
            text = "Sem séries/repetições/carga definidas",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        chips.forEach { (label, value) ->
            Row(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.shapes.small,
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$label ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun SessionRow(session: WorkoutSession, enabled: Boolean, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(formatDate(session.date), fontWeight = FontWeight.SemiBold)
                SessionStatsRow(session)
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
private fun OrderBadge(number: Int) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun RegisterSessionDialog(
    workoutName: String,
    draft: SessionDraft,
    isSaving: Boolean,
    onEvent: (WorkoutViewEvent) -> Unit,
) {
    if (draft.showDatePicker) {
        SessionDatePickerDialog(
            initialDate = draft.date,
            onDateSelected = { onEvent(WorkoutViewEvent.SessionDateChanged(it)) },
            onDismiss = { onEvent(WorkoutViewEvent.SessionDatePickerToggled(false)) },
        )
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onEvent(WorkoutViewEvent.SessionDialogDismissed) },
        title = { Text("Registrar treino") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    workoutName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isSaving) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                DateField(
                    date = draft.date,
                    error = draft.dateError,
                    enabled = !isSaving,
                    onClick = { onEvent(WorkoutViewEvent.SessionDatePickerToggled(true)) },
                )
                OutlinedTextField(
                    value = draft.durationText,
                    onValueChange = { onEvent(WorkoutViewEvent.SessionDurationChanged(it)) },
                    label = { Text("Tempo na academia (min)") },
                    placeholder = { Text("Ex.: 60") },
                    singleLine = true,
                    enabled = !isSaving,
                    isError = draft.durationError != null,
                    supportingText = draft.durationError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.caloriesText,
                    onValueChange = { onEvent(WorkoutViewEvent.SessionCaloriesChanged(it)) },
                    label = { Text("Calorias (kcal)") },
                    placeholder = { Text("Ex.: 350") },
                    singleLine = true,
                    enabled = !isSaving,
                    isError = draft.caloriesError != null,
                    supportingText = draft.caloriesError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Default.LocalFireDepartment, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(WorkoutViewEvent.SessionSaveClicked) },
                enabled = draft.isValid && !isSaving,
            ) { Text(if (isSaving) "Salvando..." else "Registrar") }
        },
        dismissButton = {
            TextButton(
                onClick = { onEvent(WorkoutViewEvent.SessionDialogDismissed) },
                enabled = !isSaving,
            ) { Text("Cancelar") }
        },
    )
}

@Composable
private fun DateField(
    date: LocalDate,
    error: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = formatDate(date),
            onValueChange = {},
            label = { Text("Data do treino") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            enabled = false,
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Escolher data") },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = if (error != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledSupportingTextColor = MaterialTheme.colorScheme.error,
            ),
        )
        // Transparent overlay so the whole (disabled) field is tappable
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = enabled) { onClick() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let {
                        onDateSelected(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    } ?: onDismiss()
                },
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    ) {
        DatePicker(state = pickerState)
    }
}
