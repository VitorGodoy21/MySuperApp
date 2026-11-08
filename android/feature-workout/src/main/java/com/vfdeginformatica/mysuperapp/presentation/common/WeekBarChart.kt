package com.vfdeginformatica.mysuperapp.presentation.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val weekDayInitials = listOf("S", "T", "Q", "Q", "S", "S", "D")

/**
 * Seven bars (Monday→Sunday) sized by [minutesByDay]; a day with sessions but no minutes gets a
 * minimal bar so it is still visible. [todayIndex] (0 = Monday) is drawn in the tertiary color.
 */
@Composable
internal fun WeekBarChart(
    minutesByDay: List<Int>,
    sessionsByDay: List<Int>,
    todayIndex: Int?,
    modifier: Modifier = Modifier,
) {
    val maxMinutes = minutesByDay.maxOrNull()?.takeIf { it > 0 } ?: 1
    val todayColor = MaterialTheme.colorScheme.tertiary
    val barColor = MaterialTheme.colorScheme.primary
    val baseline = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
        ) {
            val slot = size.width / 7f
            val barWidth = slot * 0.55f
            val minBar = 4.dp.toPx()
            val usableH = size.height - minBar
            drawLine(baseline, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1f)
            repeat(7) { index ->
                val minutes = minutesByDay.getOrElse(index) { 0 }
                val hasSession = sessionsByDay.getOrElse(index) { 0 } > 0
                val barH = when {
                    minutes > 0 -> minBar + usableH * (minutes.toFloat() / maxMinutes)
                    hasSession -> minBar
                    else -> 0f
                }
                if (barH <= 0f) return@repeat
                val color = if (index == todayIndex) todayColor else barColor
                val left = slot * index + (slot - barWidth) / 2f
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, size.height - barH),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            weekDayInitials.forEachIndexed { index, initial ->
                Text(
                    text = initial,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (index == todayIndex) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
