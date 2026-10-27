package com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.domain.model.Workout
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.session.GetRecentWorkoutSessionsUseCase
import com.vfdeginformatica.mysuperapp.presentation.common.imageVector
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub.contract.WorkoutHubEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub.contract.WorkoutHubUiState

@Composable
fun WorkoutHubScreen(
    uiState: WorkoutHubUiState,
    onEvent: (WorkoutHubEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
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
        items(uiState.mainWorkouts, key = { it.id }) { workout ->
            WorkoutCard(
                workout = workout,
                onClick = { onEvent(WorkoutHubEvent.WorkoutClicked(workout.id)) },
            )
        }
        val others = uiState.otherWorkouts
        if (others.isNotEmpty()) {
            item(key = "others_header") {
                OthersHeader(
                    count = others.size,
                    expanded = uiState.isOthersExpanded,
                    onClick = { onEvent(WorkoutHubEvent.OthersToggled) },
                )
            }
            if (uiState.isOthersExpanded) {
                items(others, key = { "other_${it.id}" }) { workout ->
                    WorkoutCard(
                        workout = workout,
                        onClick = { onEvent(WorkoutHubEvent.WorkoutClicked(workout.id)) },
                    )
                }
            }
        }
        item(key = "shortcuts_divider") {
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
        }
        item(key = "diet") {
            HighlightCard(
                icon = Icons.Default.Restaurant,
                title = "Minha dieta",
                subtitle = "Consulte suas refeições do dia",
                onClick = { onEvent(WorkoutHubEvent.DietClicked) },
            )
        }
        item(key = "body_measurements") {
            HighlightCard(
                icon = Icons.Default.MonitorWeight,
                title = "Meus dados físicos",
                subtitle = "Peso, altura, IMC e evolução",
                onClick = { onEvent(WorkoutHubEvent.BodyMeasurementsClicked) },
            )
        }
        item(key = "history") {
            HighlightCard(
                icon = Icons.Default.History,
                title = "Histórico de treinos",
                subtitle = "Treinos registrados nos últimos ${GetRecentWorkoutSessionsUseCase.DEFAULT_DAYS} dias",
                onClick = { onEvent(WorkoutHubEvent.HistoryClicked) },
            )
        }
    }
}

@Composable
private fun OthersHeader(count: Int, expanded: Boolean, onClick: () -> Unit) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "others_chevron")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick, role = Role.Button)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Outros ($count)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Recolher outros treinos" else "Expandir outros treinos",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.rotate(rotation),
        )
    }
}

@Composable
private fun HighlightCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
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
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
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
