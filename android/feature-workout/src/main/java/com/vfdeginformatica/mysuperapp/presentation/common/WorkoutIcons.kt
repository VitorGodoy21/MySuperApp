package com.vfdeginformatica.mysuperapp.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Rowing
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutIcon

/** Compose vector for each predefined [WorkoutIcon]. */
val WorkoutIcon.imageVector: ImageVector
    get() = when (this) {
        WorkoutIcon.DUMBBELL -> Icons.Default.FitnessCenter
        WorkoutIcon.CARDIO -> Icons.Default.MonitorHeart
        WorkoutIcon.RUNNING -> Icons.AutoMirrored.Filled.DirectionsRun
        WorkoutIcon.TREADMILL -> Icons.AutoMirrored.Filled.DirectionsWalk
        WorkoutIcon.BIKE -> Icons.AutoMirrored.Filled.DirectionsBike
        WorkoutIcon.SOCCER -> Icons.Default.SportsSoccer
        WorkoutIcon.FIGHT -> Icons.Default.SportsMma
        WorkoutIcon.MARTIAL_ARTS -> Icons.Default.SportsMartialArts
        WorkoutIcon.SWIMMING -> Icons.Default.Pool
        WorkoutIcon.YOGA -> Icons.Default.SelfImprovement
        WorkoutIcon.FUNCTIONAL -> Icons.Default.SportsGymnastics
        WorkoutIcon.HIIT -> Icons.Default.Timer
        WorkoutIcon.BASKETBALL -> Icons.Default.SportsBasketball
        WorkoutIcon.VOLLEYBALL -> Icons.Default.SportsVolleyball
        WorkoutIcon.TENNIS -> Icons.Default.SportsTennis
        WorkoutIcon.HIKING -> Icons.Default.Hiking
        WorkoutIcon.ROWING -> Icons.Default.Rowing
    }

/** Chip grid to choose the icon of a workout. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutIconPicker(
    selected: WorkoutIcon,
    onSelected: (WorkoutIcon) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "Ícone", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WorkoutIcon.entries.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelected(option) },
                    label = { Text(option.label) },
                    leadingIcon = {
                        Icon(option.imageVector, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    enabled = enabled,
                )
            }
        }
    }
}
