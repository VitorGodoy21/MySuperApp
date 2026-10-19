package com.vfdeginformatica.mysuperapp.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutItem
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_detail.contract.ItemDraft
import java.util.Locale

/**
 * Shared prescription (séries/repetições/carga/descanso/observação) editing logic used by the
 * workout edit screen and the read-only workout view.
 */
const val MAX_REPS_LENGTH = 10
const val MAX_ITEM_NOTES_LENGTH = 100

fun WorkoutItem.toItemDraft(): ItemDraft = ItemDraft(
    sets = sets?.toString().orEmpty(),
    reps = reps.orEmpty(),
    loadKg = loadKg?.let { formatDraftKg(it) }.orEmpty(),
    restSeconds = restSeconds?.toString().orEmpty(),
    notes = notes,
)

fun sanitizeItemDraft(draft: ItemDraft): ItemDraft = ItemDraft(
    sets = sanitizeIntInput(draft.sets, maxDigits = 3),
    reps = draft.reps.take(MAX_REPS_LENGTH),
    loadKg = sanitizeDecimalInput(draft.loadKg, maxDecimals = 2, maxIntDigits = 3),
    restSeconds = sanitizeIntInput(draft.restSeconds, maxDigits = 3),
    notes = draft.notes.take(MAX_ITEM_NOTES_LENGTH),
)

fun WorkoutItem.applyDraft(draft: ItemDraft): WorkoutItem = copy(
    sets = draft.sets.trim().toIntOrNull(),
    reps = draft.reps.trim().takeIf { it.isNotEmpty() },
    loadKg = parseDecimal(draft.loadKg),
    restSeconds = draft.restSeconds.trim().toIntOrNull(),
    notes = draft.notes.trim(),
)

private fun formatDraftKg(value: Double): String {
    val text = if (value % 1.0 == 0.0) String.format(Locale.ROOT, "%.0f", value)
    else String.format(Locale.ROOT, "%.2f", value).trimEnd('0').trimEnd('.')
    return text.replace('.', ',')
}

@Composable
fun ItemDraftDialog(
    exerciseName: String,
    draft: ItemDraft,
    onDraftChanged: (ItemDraft) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isSaving: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(exerciseName, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft.sets,
                        onValueChange = { onDraftChanged(draft.copy(sets = it)) },
                        label = { Text("Séries") },
                        singleLine = true,
                        enabled = !isSaving,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = draft.reps,
                        onValueChange = { onDraftChanged(draft.copy(reps = it)) },
                        label = { Text("Repetições") },
                        placeholder = { Text("10-12") },
                        singleLine = true,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft.loadKg,
                        onValueChange = { onDraftChanged(draft.copy(loadKg = it)) },
                        label = { Text("Carga (kg)") },
                        singleLine = true,
                        enabled = !isSaving,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = draft.restSeconds,
                        onValueChange = { onDraftChanged(draft.copy(restSeconds = it)) },
                        label = { Text("Descanso (s)") },
                        singleLine = true,
                        enabled = !isSaving,
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
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isSaving) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Confirmar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancelar") }
        }
    )
}
