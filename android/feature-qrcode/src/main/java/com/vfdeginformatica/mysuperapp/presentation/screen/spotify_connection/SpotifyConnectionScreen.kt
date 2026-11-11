package com.vfdeginformatica.mysuperapp.presentation.screen.spotify_connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.presentation.screen.spotify_connection.contract.SpotifyConnectionEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.spotify_connection.contract.SpotifyConnectionUiState

@Composable
fun SpotifyConnectionScreen(
    uiState: SpotifyConnectionUiState,
    onEvent: (SpotifyConnectionEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.showDisconnectConfirm) {
        AlertDialog(
            onDismissRequest = { onEvent(SpotifyConnectionEvent.OnDisconnectDismiss) },
            title = { Text("Desconectar Spotify?") },
            text = {
                Text("As Smart Tags do tipo Spotify deixarão de tocar até você conectar novamente.")
            },
            confirmButton = {
                TextButton(onClick = { onEvent(SpotifyConnectionEvent.OnDisconnectConfirm) }) {
                    Text("Desconectar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(SpotifyConnectionEvent.OnDisconnectDismiss) }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
            return@Box
        }

        val connection = uiState.connection
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val (icon, tint) = when {
                        connection.needsReconnect -> Icons.Default.Warning to MaterialTheme.colorScheme.error
                        connection.isConnected -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
                        else -> Icons.Default.MusicNote to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(48.dp))

                    Text(
                        text = when {
                            connection.needsReconnect -> "Conexão expirada"
                            connection.isConnected -> "Conectado"
                            else -> "Não conectado"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )

                    if (connection.isConnected && connection.displayName.isNotBlank()) {
                        Text(
                            text = connection.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (connection.isConnected && connection.product.isNotBlank() && !connection.isPremium) {
                        Text(
                            text = "Sua conta não é Premium. O Spotify só permite iniciar a reprodução em outros dispositivos com Spotify Premium.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Text(
                text = "Conecte sua conta do Spotify para criar Smart Tags que tocam uma playlist ou música " +
                    "nas suas Alexas (ou em qualquer dispositivo Spotify Connect) ao serem lidas. " +
                    "Os tokens ficam apenas no servidor; o app nunca vê sua senha.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (uiState.errorMessage.isNotBlank()) {
                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = { onEvent(SpotifyConnectionEvent.OnConnectClick) },
                enabled = !uiState.isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isStartingAuth) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        when {
                            connection.needsReconnect -> "Conectar novamente"
                            connection.isConnected -> "Trocar conta"
                            else -> "Conectar ao Spotify"
                        }
                    )
                }
            }

            if (connection.isConnected || connection.needsReconnect) {
                OutlinedButton(
                    onClick = { onEvent(SpotifyConnectionEvent.OnDisconnectClick) },
                    enabled = !uiState.isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Desconectar")
                }
            }
        }
    }
}
