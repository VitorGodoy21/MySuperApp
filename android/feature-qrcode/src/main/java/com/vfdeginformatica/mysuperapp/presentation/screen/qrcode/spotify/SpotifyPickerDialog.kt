package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.spotify

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyDevice
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyItem
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyItemKind
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyTarget

/**
 * Diálogo para escolher o que tocar (playlist/música) e onde (dispositivo Spotify Connect).
 * Quando a conta não está conectada mostra um CTA que leva à tela de conexão.
 */
@Composable
fun SpotifyPickerDialog(
    initialTarget: SpotifyTarget?,
    onDismiss: () -> Unit,
    onConfirm: (SpotifyTarget) -> Unit,
    onOpenConnection: () -> Unit,
    viewModel: SpotifyPickerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(initialTarget) { viewModel.initFrom(initialTarget) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        title = { Text("Spotify") },
        text = {
            when {
                !state.isConnectionLoaded -> Box(Modifier.fillMaxWidth().height(120.dp), Alignment.Center) {
                    CircularProgressIndicator()
                }

                !state.isConnected -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (state.connection?.needsReconnect == true)
                            "A conexão com o Spotify expirou. Conecte novamente para escolher a playlist."
                        else
                            "Conecte sua conta do Spotify para escolher a playlist ou música e o dispositivo."
                    )
                    Button(onClick = onOpenConnection, modifier = Modifier.fillMaxWidth()) {
                        Text("Conectar ao Spotify")
                    }
                }

                else -> SpotifyPickerContent(state = state, viewModel = viewModel)
            }
        },
        confirmButton = {
            if (state.isConnected) {
                Button(
                    onClick = { state.toTarget()?.let(onConfirm) },
                    enabled = state.canConfirm
                ) { Text("Confirmar") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun SpotifyPickerContent(state: SpotifyPickerUiState, viewModel: SpotifyPickerViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.selectedItem?.let { item ->
            Text("Selecionado", style = MaterialTheme.typography.labelMedium)
            SpotifyItemRow(item = item, selected = true, onClick = {})
        }

        TabRow(selectedTabIndex = state.tab.ordinal) {
            Tab(
                selected = state.tab == SpotifyPickerTab.MY_PLAYLISTS,
                onClick = { viewModel.onTabChanged(SpotifyPickerTab.MY_PLAYLISTS) },
                text = { Text("Minhas playlists") }
            )
            Tab(
                selected = state.tab == SpotifyPickerTab.SEARCH,
                onClick = { viewModel.onTabChanged(SpotifyPickerTab.SEARCH) },
                text = { Text("Buscar") }
            )
        }

        when (state.tab) {
            SpotifyPickerTab.MY_PLAYLISTS -> ItemList(
                items = state.playlists,
                isLoading = state.isLoadingPlaylists,
                error = state.playlistsError,
                emptyMessage = "Nenhuma playlist encontrada",
                selectedUri = state.selectedItem?.uri,
                onRetry = viewModel::retryPlaylists,
                onSelect = viewModel::onItemSelected
            )

            SpotifyPickerTab.SEARCH -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChanged,
                    label = { Text("Buscar no Spotify") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.searchKind == SpotifyItemKind.PLAYLIST,
                        onClick = { viewModel.onSearchKindChanged(SpotifyItemKind.PLAYLIST) },
                        label = { Text("Playlists") }
                    )
                    FilterChip(
                        selected = state.searchKind == SpotifyItemKind.TRACK,
                        onClick = { viewModel.onSearchKindChanged(SpotifyItemKind.TRACK) },
                        label = { Text("Músicas") }
                    )
                }
                ItemList(
                    items = state.searchResults,
                    isLoading = state.isSearching,
                    error = state.searchError,
                    emptyMessage = if (state.query.isBlank()) "Digite para buscar" else "Nada encontrado",
                    selectedUri = state.selectedItem?.uri,
                    onRetry = { viewModel.onQueryChanged(state.query) },
                    onSelect = viewModel::onItemSelected
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tocar em", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = viewModel::refreshDevices, enabled = !state.isLoadingDevices) {
                Icon(Icons.Default.Refresh, contentDescription = "Atualizar dispositivos")
            }
        }
        when {
            state.isLoadingDevices -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            state.devicesError.isNotBlank() -> Text(state.devicesError, color = MaterialTheme.colorScheme.error)
            state.devices.isEmpty() -> Text(
                "Nenhum dispositivo. Diga \"Alexa, abrir Spotify\" ou toque algo uma vez no dispositivo e atualize.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val sorted = state.devices.sortedByDescending { it.isSpeaker }
                sorted.forEach { device ->
                    DeviceRow(
                        device = device,
                        selected = state.selectedDevice?.id == device.id,
                        onClick = { viewModel.onDeviceSelected(device) }
                    )
                }
            }
        }
        state.selectedDevice?.takeIf { sel -> state.devices.none { it.id == sel.id } }?.let { sel ->
            Text(
                "Dispositivo salvo: ${sel.name} (offline agora; será procurado pelo nome ao tocar)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (state.selectedItem?.kind != SpotifyItemKind.TRACK) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Aleatório", modifier = Modifier.weight(1f))
                Switch(checked = state.shuffle, onCheckedChange = viewModel::onShuffleChanged)
            }
        }
    }
}

@Composable
private fun ItemList(
    items: List<SpotifyItem>,
    isLoading: Boolean,
    error: String,
    emptyMessage: String,
    selectedUri: String?,
    onRetry: () -> Unit,
    onSelect: (SpotifyItem) -> Unit
) {
    Box(Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 220.dp)) {
        when {
            isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            error.isNotBlank() -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onRetry) { Text("Tentar novamente") }
            }
            items.isEmpty() -> Text(
                emptyMessage,
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(items, key = { it.uri }) { item ->
                    SpotifyItemRow(item = item, selected = item.uri == selectedUri, onClick = { onSelect(item) })
                }
            }
        }
    }
}

@Composable
internal fun SpotifyItemRow(item: SpotifyItem, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpotifyCover(imageUrl = item.imageUrl, kind = item.kind, size = 44.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (item.subtitle.isNotBlank()) {
                Text(
                    item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
internal fun SpotifyCover(imageUrl: String, kind: SpotifyItemKind, size: androidx.compose.ui.unit.Dp) {
    val shape = RoundedCornerShape(6.dp)
    if (imageUrl.isNotBlank()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(shape)
        )
    } else {
        Box(
            Modifier.size(size).clip(shape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
            Alignment.Center
        ) {
            Icon(
                if (kind == SpotifyItemKind.TRACK) Icons.Default.MusicNote else Icons.Default.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DeviceRow(device: SpotifyDevice, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (device.isSpeaker) Icons.Default.Speaker else Icons.Default.Devices,
            contentDescription = null,
            tint = if (device.isSpeaker) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(device.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                buildString {
                    append(device.type.ifBlank { "Dispositivo" })
                    if (device.isActive) append(" • ativo")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

/** Campo-resumo do alvo Spotify usado nos formulários de criação/edição; toca para abrir o picker. */
@Composable
fun SpotifyTargetField(
    target: SpotifyTarget?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (target == null) {
            Icon(Icons.Default.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(10.dp))
            Text(
                "Escolher playlist ou música e dispositivo",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            SpotifyCover(
                imageUrl = target.imageUrl,
                kind = if (target.isTrack) SpotifyItemKind.TRACK else SpotifyItemKind.PLAYLIST,
                size = 48.dp
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(target.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (target.subtitle.isNotBlank()) {
                    Text(
                        target.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    buildString {
                        append("Tocar em ${target.deviceName}")
                        if (target.shuffle) append(" • aleatório")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
