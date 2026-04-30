package com.vfdeginformatica.mysuperapp.presentation.screen.access_log_map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.vfdeginformatica.mysuperapp.domain.model.AccessLog
import com.vfdeginformatica.mysuperapp.presentation.screen.access_log_map.contract.AccessLogMapEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.access_log_map.contract.AccessLogMapUiState
import com.vfdeginformatica.mysuperapp.presentation.screen.access_log_map.contract.MapViewMode
import java.text.SimpleDateFormat
import java.util.Locale

// ─── Formatters ──────────────────────────────────────────────────────────────

private fun formatLoggedAt(raw: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
        val date = parser.parse(raw)
        if (date != null) formatter.format(date) else raw
    } catch (e: Exception) {
        raw.ifEmpty { "—" }
    }
}

private fun formatTimestamp(date: java.util.Date?): String {
    if (date == null) return "—"
    return SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(date)
}

// ─── Root Screen ─────────────────────────────────────────────────────────────

@Composable
fun AccessLogMapScreen(
    uiState: AccessLogMapUiState,
    onEvent: (AccessLogMapEvent) -> Unit,
    qrCodeId: String,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    LaunchedEffect(qrCodeId) {
        onEvent(AccessLogMapEvent.OnLoadAccessLogs(qrCodeId))
    }

    // Detail bottom sheet
    if (uiState.selectedLog != null) {
        LogDetailBottomSheet(
            log = uiState.selectedLog,
            onDismiss = { onEvent(AccessLogMapEvent.OnClearSelectedLog) }
        )
    }

    when {
        uiState.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.viewMode == MapViewMode.MAP -> {
            MapViewContent(uiState, onEvent, contentPadding, modifier)
        }

        uiState.viewMode == MapViewMode.CITY_LIST -> {
            CityListViewContent(uiState, onEvent, contentPadding, modifier)
        }

        uiState.viewMode == MapViewMode.LOG_LIST -> {
            LogListViewContent(uiState, onEvent, qrCodeId, contentPadding, modifier)
        }

        uiState.errorMessage.isNotEmpty() && uiState.accessLogs.isEmpty() -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "No data",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uiState.errorMessage,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

// ─── Map View ────────────────────────────────────────────────────────────────

@Composable
private fun MapViewContent(
    uiState: AccessLogMapUiState,
    onEvent: (AccessLogMapEvent) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    val filteredLogs = if (uiState.selectedCity != null) {
        uiState.accessLogs.filter { it.city.equals(uiState.selectedCity, ignoreCase = true) }
    } else {
        uiState.accessLogs
    }

    val logsWithLocation = filteredLogs.filter { it.latitude != null && it.longitude != null }

    val cameraPositionState = rememberCameraPositionState {
        position = if (logsWithLocation.isNotEmpty()) {
            val firstLog = logsWithLocation.first()
            CameraPosition.fromLatLngZoom(
                LatLng(firstLog.latitude!!, firstLog.longitude!!),
                11f
            )
        } else {
            CameraPosition.fromLatLngZoom(LatLng(-15.7942, -47.8822), 4f)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            logsWithLocation.forEach { log ->
                Marker(
                    state = MarkerState(position = LatLng(log.latitude!!, log.longitude!!)),
                    title = log.city,
                    snippet = "${log.country} - ${log.timestamp}"
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = contentPadding.calculateTopPadding() + 8.dp,
                    start = 8.dp,
                    end = 8.dp
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onEvent(AccessLogMapEvent.OnToggleViewMode) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "List view",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cidades")
                }

                Button(
                    onClick = { onEvent(AccessLogMapEvent.OnShowLogList) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Logs",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Logs")
                }

                if (uiState.selectedCity != null) {
                    IconButton(
                        onClick = { onEvent(AccessLogMapEvent.OnClearCityFilter) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear filter",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (uiState.selectedCity != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cidade: ${uiState.selectedCity}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            val count = filteredLogs.size
                            Text(
                                text = "$count acesso${if (count != 1) "s" else ""}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        if (logsWithLocation.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.9f)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = contentPadding.calculateBottomPadding() + 16.dp
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Total de Acessos: ${logsWithLocation.size}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Cidades: ${uiState.cityStatistics.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ─── City List View ──────────────────────────────────────────────────────────

@Composable
private fun CityListViewContent(
    uiState: AccessLogMapUiState,
    onEvent: (AccessLogMapEvent) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Acessos por Cidade",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total: ${uiState.cityStatistics.size} cidades",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            IconButton(onClick = { onEvent(AccessLogMapEvent.OnShowLogList) }) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "Logs",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            IconButton(onClick = { onEvent(AccessLogMapEvent.OnToggleViewMode) }) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Map view",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        if (uiState.cityStatistics.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhuma cidade encontrada")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.cityStatistics) { cityStat ->
                    CityStatisticCard(
                        cityStat = cityStat,
                        isSelected = cityStat.city == uiState.selectedCity,
                        onSelect = {
                            onEvent(AccessLogMapEvent.OnSelectCity(cityStat.city))
                            onEvent(AccessLogMapEvent.OnToggleViewMode)
                        }
                    )
                }
            }
        }
    }
}

// ─── Log List View ───────────────────────────────────────────────────────────

@Composable
private fun LogListViewContent(
    uiState: AccessLogMapUiState,
    onEvent: (AccessLogMapEvent) -> Unit,
    qrCodeId: String,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    var logToDelete by remember { mutableStateOf<AccessLog?>(null) }

    // Confirmation dialog
    if (logToDelete != null) {
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title = { Text("Excluir log") },
            text = {
                Text("Deseja excluir o log\n${logToDelete!!.scanId.ifEmpty { logToDelete!!.id }}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEvent(AccessLogMapEvent.OnDeleteLog(qrCodeId, logToDelete!!.id))
                        logToDelete = null
                    }
                ) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { logToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Logs de Acesso",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total: ${uiState.accessLogs.size} registros",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            IconButton(onClick = { onEvent(AccessLogMapEvent.OnToggleViewMode) }) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Voltar ao mapa",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        if (uiState.isDeleting) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        if (uiState.accessLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhum log encontrado")
            }
        } else {
            val sortedLogs = remember(uiState.accessLogs) {
                uiState.accessLogs.sortedByDescending {
                    it.loggedAt.ifEmpty {
                        it.timestamp.toInstant().toString()
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = 8.dp,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(sortedLogs, key = { it.id }) { log ->
                    LogListItem(
                        log = log,
                        onClick = { onEvent(AccessLogMapEvent.OnSelectLog(log)) },
                        onDelete = { logToDelete = log }
                    )
                }
            }
        }
    }
}

@Composable
private fun LogListItem(
    log: AccessLog,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.scanId.ifEmpty { log.id },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatLoggedAt(log.loggedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${log.city.ifEmpty { "—" }}, ${log.country.ifEmpty { "—" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Excluir log",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ─── Log Detail Bottom Sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogDetailBottomSheet(
    log: AccessLog,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Detalhes do Acesso",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            DetailSection(title = "Identificação") {
                DetailRow("Scan ID", log.scanId.ifEmpty { "—" })
                DetailRow("Session ID", log.sessionId.ifEmpty { "—" })
                DetailRow("Doc ID", log.id.ifEmpty { "—" })
                DetailRow("QR Code ID", log.qrCodeId.ifEmpty { "—" })
                DetailRow("Status", log.status.ifEmpty { "—" })
            }

            DetailSection(title = "Data e Hora") {
                DetailRow("loggedAt", formatLoggedAt(log.loggedAt))
                DetailRow("timestamp", formatTimestamp(log.timestamp))
            }

            DetailSection(title = "Localização") {
                DetailRow("Cidade", log.city.ifEmpty { "—" })
                DetailRow("Região", log.region.ifEmpty { "—" })
                DetailRow("País", log.country.ifEmpty { "—" })
                DetailRow("Latitude", log.latitude?.toString() ?: "—")
                DetailRow("Longitude", log.longitude?.toString() ?: "—")
                DetailRow("Método", log.method.ifEmpty { "—" })
            }

            DetailSection(title = "Dispositivo") {
                DetailRow("Plataforma", log.platform.ifEmpty { "—" })
                DetailRow("Idioma", log.language.ifEmpty { "—" })
                DetailRow("Fuso Horário", log.timeZone.ifEmpty { "—" })
                DetailRow(
                    "Resolução",
                    if (log.screenWidth != null && log.screenHeight != null) "${log.screenWidth} × ${log.screenHeight}" else "—"
                )
                DetailRow("User Agent", log.userAgent.ifEmpty { "—" })
            }

            DetailSection(title = "Página") {
                DetailRow("URL", log.pageUrl.ifEmpty { "—" })
                DetailRow("Path", log.pagePath.ifEmpty { "—" })
                DetailRow("Referrer", log.referrer ?: "—")
                DetailRow("UTM Source", log.utmSource ?: "—")
                DetailRow("UTM Medium", log.utmMedium ?: "—")
                DetailRow("UTM Campaign", log.utmCampaign ?: "—")
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
    Divider()
    Spacer(modifier = Modifier.height(4.dp))
    content()
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ─── City Card ───────────────────────────────────────────────────────────────

@Composable
private fun CityStatisticCard(
    cityStat: com.vfdeginformatica.mysuperapp.domain.model.CityAccessStatistics,
    isSelected: Boolean = false,
    onSelect: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "City",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Column {
                        Text(
                            text = cityStat.city,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = cityStat.country,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (cityStat.lastAccessTime != null) {
                    Text(
                        text = "Último acesso: ${cityStat.lastAccessTime}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Card(
                modifier = Modifier.padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "${cityStat.accessCount}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(8.dp),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
