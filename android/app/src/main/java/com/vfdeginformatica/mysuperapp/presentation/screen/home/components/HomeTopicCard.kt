package com.vfdeginformatica.mysuperapp.presentation.screen.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Full-width Home card: colored header (tap opens the topic) + topic-specific content. */
@Composable
fun HomeTopicCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    colors: HomeTopicColors,
    onHeaderClick: () -> Unit,
    modifier: Modifier = Modifier,
    headerTrailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = colors.container,
            contentColor = colors.onContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onHeaderClick)
                .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(colors.accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = colors.onAccent)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onContainer.copy(alpha = 0.75f),
                )
            }
            headerTrailing?.invoke()
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Abrir $title",
                tint = colors.onContainer.copy(alpha = 0.6f),
            )
        }
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun HomeShortcutChip(
    label: String,
    icon: ImageVector,
    colors: HomeTopicColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        modifier = modifier,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = colors.accent,
            labelColor = colors.onAccent,
            leadingIconContentColor = colors.onAccent,
        ),
        border = null,
    )
}

/** Label above a value inside a Home card. */
@Composable
fun HomeMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color? = null,
) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Loading / error placeholder shared by all Home sections. */
@Composable
fun HomeSectionStatus(isLoading: Boolean, error: String?, onRetry: () -> Unit) {
    when {
        isLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("Carregando...", style = MaterialTheme.typography.bodySmall)
        }

        error != null -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) { Text("Tentar novamente") }
        }
    }
}
