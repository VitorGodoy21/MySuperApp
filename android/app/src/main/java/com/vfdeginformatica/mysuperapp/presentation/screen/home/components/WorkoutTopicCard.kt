package com.vfdeginformatica.mysuperapp.presentation.screen.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.presentation.screen.home.contract.WorkoutSectionState

@Composable
fun WorkoutTopicCard(
    state: WorkoutSectionState,
    showDiet: Boolean = true,
    onOpen: () -> Unit,
    onOpenTodayWorkout: (String) -> Unit,
    onOpenDiet: () -> Unit,
    onRetry: () -> Unit,
) {
    val colors = HomeColors.workout()
    HomeTopicCard(
        title = "Academia",
        subtitle = if (showDiet) "Treinos, dieta e dados físicos" else "Treinos e dados físicos",
        icon = Icons.Default.FitnessCenter,
        colors = colors,
        onHeaderClick = onOpen,
    ) {
        val summary = state.summary
        if (summary != null) {
            HomeMetric(
                label = "Treino de hoje",
                value = summary.todayWorkout?.name ?: "Sem treino agendado hoje",
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HomeMetric("Treinos (7 dias)", "${summary.sessionsLast7Days}", Modifier.weight(1f))
                HomeMetric("Minutos", "${summary.minutesLast7Days}", Modifier.weight(1f))
                HomeMetric("Calorias", "${summary.caloriesLast7Days} kcal", Modifier.weight(1f))
            }
        }
        HomeSectionStatus(isLoading = state.isLoading, error = state.error, onRetry = onRetry)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            summary?.todayWorkout?.let { workout ->
                HomeShortcutChip("Treino de hoje", Icons.AutoMirrored.Filled.DirectionsRun, colors, onClick = {
                    onOpenTodayWorkout(workout.id)
                })
            }
            if (showDiet) HomeShortcutChip("Minha dieta", Icons.Default.Restaurant, colors, onOpenDiet)
        }
    }
}
