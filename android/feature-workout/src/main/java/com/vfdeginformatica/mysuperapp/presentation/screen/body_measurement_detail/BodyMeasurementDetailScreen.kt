package com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.presentation.common.bmiCategory
import com.vfdeginformatica.mysuperapp.presentation.common.formatBmi
import com.vfdeginformatica.mysuperapp.presentation.common.formatDate
import com.vfdeginformatica.mysuperapp.presentation.common.parseDecimal
import com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_detail.contract.BodyMeasurementDetailEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_detail.contract.BodyMeasurementDetailUiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.pow

@Composable
fun BodyMeasurementDetailScreen(
    uiState: BodyMeasurementDetailUiState,
    onEvent: (BodyMeasurementDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    if (uiState.showDeleteConfirmation) {
        DeleteMeasurementDialog(
            dateLabel = formatDate(uiState.date),
            onConfirm = { onEvent(BodyMeasurementDetailEvent.DeleteConfirmed) },
            onDismiss = { onEvent(BodyMeasurementDetailEvent.DeleteDismissed) },
        )
    }

    if (uiState.showDatePicker) {
        MeasurementDatePickerDialog(
            initialDate = uiState.date,
            onDateSelected = { onEvent(BodyMeasurementDetailEvent.DateChanged(it)) },
            onDismiss = { onEvent(BodyMeasurementDetailEvent.DatePickerToggled(false)) },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.weightText,
                onValueChange = { onEvent(BodyMeasurementDetailEvent.WeightChanged(it)) },
                label = { Text("Peso (kg)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                isError = uiState.weightError != null && uiState.weightText.isNotBlank(),
                supportingText = uiState.weightError
                    ?.takeIf { uiState.weightText.isNotBlank() }
                    ?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            OutlinedTextField(
                value = uiState.heightText,
                onValueChange = { onEvent(BodyMeasurementDetailEvent.HeightChanged(it)) },
                label = { Text("Altura (cm)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                isError = uiState.heightError != null && uiState.heightText.isNotBlank(),
                supportingText = uiState.heightError
                    ?.takeIf { uiState.heightText.isNotBlank() }
                    ?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }

        DateField(
            date = uiState.date,
            error = uiState.dateError,
            enabled = !uiState.isBusy,
            onClick = { onEvent(BodyMeasurementDetailEvent.DatePickerToggled(true)) },
        )

        BmiPreview(weightText = uiState.weightText, heightText = uiState.heightText)

        OutlinedTextField(
            value = uiState.notes,
            onValueChange = { onEvent(BodyMeasurementDetailEvent.NotesChanged(it)) },
            label = { Text("Observações (opcional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            supportingText = {
                Text("${uiState.notes.length}/${BodyMeasurementDetailViewModel.MAX_NOTES_LENGTH}")
            },
        )

        Button(
            onClick = { onEvent(BodyMeasurementDetailEvent.SaveClicked) },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.isSubmitEnabled && !uiState.isBusy,
        ) {
            Text(
                when {
                    uiState.isSaving -> "Salvando..."
                    uiState.isEditing -> "Salvar alterações"
                    else -> "Salvar pesagem"
                }
            )
        }

        if (uiState.isEditing) {
            HorizontalDivider()

            OutlinedButton(
                onClick = { onEvent(BodyMeasurementDetailEvent.DeleteClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isBusy,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(if (uiState.isDeleting) "Excluindo..." else "Excluir registro")
            }
        }
    }
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
            label = { Text("Data da pesagem") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            enabled = false,
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            trailingIcon = {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Escolher data")
            },
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = if (error != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.outline
                },
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledSupportingTextColor = MaterialTheme.colorScheme.error,
            ),
        )
        // Transparent overlay so the whole (disabled) field is tappable
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = enabled) { onClick() }
        )
    }
}

@Composable
private fun BmiPreview(weightText: String, heightText: String) {
    val weight = parseDecimal(weightText) ?: return
    val height = parseDecimal(heightText) ?: return
    if (weight <= 0.0 || height <= 0.0) return

    val bmi = weight / (height / 100.0).pow(2)
    Text(
        text = "IMC: ${formatBmi(bmi)} • ${bmiCategory(bmi)}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeasurementDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let {
                        onDateSelected(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    } ?: onDismiss()
                }
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

@Composable
private fun DeleteMeasurementDialog(
    dateLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluir pesagem?") },
        text = {
            Text("A pesagem de $dateLabel será excluída. Esta ação não pode ser desfeita.")
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
fun BodyMeasurementDetailScreenPreview() {
    BodyMeasurementDetailScreen(
        uiState = BodyMeasurementDetailUiState(
            measurementId = "1",
            weightText = "78,2",
            heightText = "180",
            date = LocalDate.of(2026, 10, 15),
            notes = "Pós-férias",
            isSubmitEnabled = true,
        ),
        onEvent = {}
    )
}
