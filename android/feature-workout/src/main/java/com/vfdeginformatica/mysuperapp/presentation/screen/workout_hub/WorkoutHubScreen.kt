package com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.presentation.common.formatBmi
import com.vfdeginformatica.mysuperapp.presentation.common.formatDate
import com.vfdeginformatica.mysuperapp.presentation.common.formatKg
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub.contract.WorkoutHubEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub.contract.WorkoutHubUiState

@Composable
fun WorkoutHubScreen(
    uiState: WorkoutHubUiState,
    onEvent: (WorkoutHubEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (uiState.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        val latest = uiState.latestMeasurement
        HubCard(
            icon = Icons.Default.MonitorWeight,
            title = "Dados físicos",
            subtitle = if (latest == null) {
                "Registre seu peso e altura para acompanhar a evolução"
            } else {
                buildString {
                    append("Último peso: ${formatKg(latest.weightKg)} em ${formatDate(latest.measuredAt)}")
                    latest.bmi?.let { append(" • IMC ${formatBmi(it)}") }
                }
            },
            onClick = { onEvent(WorkoutHubEvent.BodyMeasurementsClicked) },
        )

        HubCard(
            icon = Icons.AutoMirrored.Filled.List,
            title = "Exercícios",
            subtitle = when (uiState.exerciseCount) {
                0 -> "Cadastre os exercícios que usará nos treinos"
                1 -> "1 exercício cadastrado"
                else -> "${uiState.exerciseCount} exercícios cadastrados"
            },
            onClick = { onEvent(WorkoutHubEvent.ExercisesClicked) },
        )

        HubCard(
            icon = Icons.AutoMirrored.Filled.EventNote,
            title = "Treinos",
            subtitle = when (uiState.workoutCount) {
                0 -> "Monte seus treinos (ex.: Treino A, B, C)"
                1 -> "1 treino montado"
                else -> "${uiState.workoutCount} treinos montados"
            },
            onClick = { onEvent(WorkoutHubEvent.WorkoutsClicked) },
        )

        if (uiState.workouts.isNotEmpty()) {
            Text(
                text = "Meus treinos",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            uiState.workouts.forEach { workout ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEvent(WorkoutHubEvent.WorkoutClicked(workout.id)) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(workout.name, fontWeight = FontWeight.SemiBold)
                            val detail = listOfNotNull(
                                workout.dayOfWeek?.label,
                                "${workout.items.size} exercício" + if (workout.items.size == 1) "" else "s",
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
        }
    }
}

@Composable
private fun HubCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
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
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
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
