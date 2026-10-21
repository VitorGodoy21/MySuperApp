package com.vfdeginformatica.mysuperapp.presentation.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vfdeginformatica.mysuperapp.presentation.screen.settings.contract.SettingsEvent

@Composable
fun SettingsScreen(
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        ListItem(
            headlineContent = { Text("Notificações") },
            supportingContent = { Text("Acessos e comentários nos seus QR Codes") },
            leadingContent = {
                Icon(imageVector = Icons.Default.Notifications, contentDescription = null)
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEvent(SettingsEvent.OnNotificationsClick) }
        )
        HorizontalDivider()
    }
}
