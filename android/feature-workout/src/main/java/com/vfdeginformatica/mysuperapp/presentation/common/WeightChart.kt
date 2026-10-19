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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.time.LocalDate

data class WeightPoint(val date: LocalDate, val weightKg: Double)

/**
 * Minimal line chart drawn with Canvas: points in chronological order, min/max labels on the
 * left and first/last dates underneath. No external chart dependency.
 */
@Composable
internal fun WeightChart(
    points: List<WeightPoint>,
    modifier: Modifier = Modifier,
) {
    if (points.size < 2) {
        Text(
            text = "Registre pelo menos duas pesagens para ver a evolução.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }

    val sorted = points.sortedBy { it.date }
    val minW = sorted.minOf { it.weightKg }
    val maxW = sorted.maxOf { it.weightKg }
    val range = (maxW - minW).takeIf { it > 0 } ?: 1.0
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(end = 8.dp).height(160.dp)) {
                Text(formatKg(maxW), style = MaterialTheme.typography.labelSmall)
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                Text(formatKg(minW), style = MaterialTheme.typography.labelSmall)
            }
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(160.dp)
            ) {
                val w = size.width
                val h = size.height
                val padY = 6.dp.toPx()
                val usableH = h - padY * 2
                val stepX = if (sorted.size > 1) w / (sorted.size - 1) else 0f

                drawLine(gridColor, Offset(0f, padY), Offset(w, padY), strokeWidth = 1f)
                drawLine(gridColor, Offset(0f, h - padY), Offset(w, h - padY), strokeWidth = 1f)

                val offsets = sorted.mapIndexed { index, p ->
                    val x = index * stepX
                    val y = padY + ((maxW - p.weightKg) / range * usableH).toFloat()
                    Offset(x, y)
                }

                val path = Path().apply {
                    offsets.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
                }
                drawPath(path, lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                offsets.forEach { drawCircle(lineColor, radius = 4.dp.toPx(), center = it) }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        ) {
            Text(formatShortDate(sorted.first().date), style = MaterialTheme.typography.labelSmall)
            Text(formatShortDate(sorted.last().date), style = MaterialTheme.typography.labelSmall)
        }
    }
}
