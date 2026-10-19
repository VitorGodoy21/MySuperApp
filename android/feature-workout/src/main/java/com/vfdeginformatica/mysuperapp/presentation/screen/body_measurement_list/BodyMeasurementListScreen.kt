package com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_list

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.domain.model.BodyMeasurement
import com.vfdeginformatica.mysuperapp.presentation.common.WeightChart
import com.vfdeginformatica.mysuperapp.presentation.common.bmiCategory
import com.vfdeginformatica.mysuperapp.presentation.common.formatBmi
import com.vfdeginformatica.mysuperapp.presentation.common.formatCm
import com.vfdeginformatica.mysuperapp.presentation.common.formatDate
import com.vfdeginformatica.mysuperapp.presentation.common.formatKg
import com.vfdeginformatica.mysuperapp.presentation.common.formatKgDelta
import com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_list.contract.BodyMeasurementListEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_list.contract.BodyMeasurementListUiState
import com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_list.contract.MeasurementRow
import java.time.LocalDate

@Composable
fun BodyMeasurementListScreen(
    uiState: BodyMeasurementListUiState,
    onEvent: (BodyMeasurementListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    uiState.pendingDeleteId?.let { id ->
        val target = uiState.measurements.find { it.id == id }
        DeleteMeasurementDialog(
            dateLabel = target?.let { formatDate(it.measuredAt) },
            onConfirm = { onEvent(BodyMeasurementListEvent.DeleteConfirmed) },
            onDismiss = { onEvent(BodyMeasurementListEvent.DeleteDismissed) },
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.measurements.isEmpty() -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )

            uiState.measurements.isEmpty() -> Text(
                text = "Nenhuma pesagem registrada ainda.",
                style = MaterialTheme.typography.bodyLarge,
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
                uiState.latest?.let { latest ->
                    item(key = "summary") { SummaryCard(latest) }
                }
                item(key = "chart") { ChartCard(uiState) }
                item(key = "header_history") {
                    Text(
                        text = "Histórico",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(uiState.rows, key = { it.measurement.id }) { row ->
                    MeasurementRowItem(
                        row = row,
                        enabled = !uiState.isDeleting,
                        onClick = {
                            onEvent(BodyMeasurementListEvent.MeasurementClicked(row.measurement.id))
                        },
                        onDelete = {
                            onEvent(BodyMeasurementListEvent.DeleteClicked(row.measurement.id))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(latest: BodyMeasurement) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Último peso",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatKg(latest.weightKg),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "em ${formatDate(latest.measuredAt)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Altura",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCm(latest.heightCm),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                latest.bmi?.let { bmi ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "IMC",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${formatBmi(bmi)} • ${bmiCategory(bmi)}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartCard(uiState: BodyMeasurementListUiState) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Evolução do peso",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            WeightChart(points = uiState.chartPoints, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MeasurementRowItem(
    row: MeasurementRow,
    enabled: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val measurement = row.measurement
    val deltaLabel = row.deltaKg?.let { formatKgDelta(it) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = formatDate(measurement.measuredAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatKg(measurement.weightKg),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (deltaLabel != null) {
                        DeltaChip(label = deltaLabel, positive = (row.deltaKg ?: 0.0) > 0)
                    }
                }
                measurement.bmi?.let { bmi ->
                    Text(
                        text = "IMC ${formatBmi(bmi)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (measurement.notes.isNotBlank()) {
                    Text(
                        text = measurement.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete, enabled = enabled) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Excluir pesagem",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun DeltaChip(label: String, positive: Boolean) {
    val color = if (positive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(
            disabledLabelColor = color,
            disabledContainerColor = color.copy(alpha = 0.12f),
        ),
        border = null,
    )
}

@Composable
private fun DeleteMeasurementDialog(
    dateLabel: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluir pesagem?") },
        text = {
            Text(
                if (dateLabel != null) {
                    "A pesagem de $dateLabel será excluída. Esta ação não pode ser desfeita."
                } else {
                    "Este registro será excluído. Esta ação não pode ser desfeita."
                }
            )
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
fun BodyMeasurementListScreenPreview() {
    BodyMeasurementListScreen(
        uiState = BodyMeasurementListUiState(
            measurements = listOf(
                BodyMeasurement("3", 78.2, 180.0, LocalDate.of(2026, 10, 15), "Pós-férias"),
                BodyMeasurement("2", 77.0, 180.0, LocalDate.of(2026, 9, 1)),
                BodyMeasurement("1", 79.5, 180.0, LocalDate.of(2026, 8, 1)),
            )
        ),
        onEvent = {}
    )
}
