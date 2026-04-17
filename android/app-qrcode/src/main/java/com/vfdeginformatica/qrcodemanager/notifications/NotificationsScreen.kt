package com.vfdeginformatica.qrcodemanager.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NotificationsScreen(
    uiState: NotificationsUiState,
    onEvent: (NotificationsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator()
        }

        NotificationToggleRow(
            title = "Todas as notificações",
            subtitle = "Habilita ou desabilita todas as notificações",
            checked = uiState.enabledAll,
            enabled = !uiState.isSaving,
            onCheckedChange = { onEvent(NotificationsEvent.OnToggleAll(it)) }
        )

        NotificationToggleRow(
            title = "Notificações de acesso",
            subtitle = "Quando acessarem seus QR Codes",
            checked = uiState.enabledAccess,
            enabled = uiState.enabledAll && !uiState.isSaving,
            onCheckedChange = { onEvent(NotificationsEvent.OnToggleAccess(it)) }
        )

        NotificationToggleRow(
            title = "Notificações de comentário",
            subtitle = "Quando comentarem no mural dos seus QR Codes",
            checked = uiState.enabledMuralComments,
            enabled = uiState.enabledAll && !uiState.isSaving,
            onCheckedChange = { onEvent(NotificationsEvent.OnToggleMuralComments(it)) }
        )

        if (uiState.errorMessage.isNotEmpty()) {
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (uiState.successMessage.isNotEmpty()) {
            Text(
                text = uiState.successMessage,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

