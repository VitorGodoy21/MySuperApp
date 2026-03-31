package com.vfdeginformatica.mysuperapp.presentation.screen.access_log_map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.vfdeginformatica.mysuperapp.presentation.screen.access_log_map.contract.AccessLogMapEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.access_log_map.contract.AccessLogMapUiState
import com.vfdeginformatica.mysuperapp.presentation.screen.access_log_map.contract.MapViewMode

@Composable
fun AccessLogMapScreen(
    uiState: AccessLogMapUiState,
    onEvent: (AccessLogMapEvent) -> Unit,
    qrCodeId: String,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(qrCodeId) {
        onEvent(AccessLogMapEvent.OnLoadAccessLogs(qrCodeId))
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
            MapViewContent(uiState, onEvent, modifier)
        }

        uiState.viewMode == MapViewMode.CITY_LIST -> {
            CityListViewContent(uiState, onEvent, modifier)
        }

        uiState.errorMessage.isNotEmpty() && uiState.accessLogs.isEmpty() -> {
            Box(
                modifier = modifier.fillMaxSize(),
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

@Composable
private fun MapViewContent(
    uiState: AccessLogMapUiState,
    onEvent: (AccessLogMapEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredLogs = if (uiState.selectedCity != null) {
        uiState.accessLogs.filter { it.city.equals(uiState.selectedCity, ignoreCase = true) }
    } else {
        uiState.accessLogs
    }

    // Calculate bounds for the map
    val logsWithLocation = filteredLogs.filter { it.latitude != null && it.longitude != null }

    val cameraPositionState = rememberCameraPositionState {
        position = if (logsWithLocation.isNotEmpty()) {
            val firstLog = logsWithLocation.first()
            CameraPosition.fromLatLngZoom(
                LatLng(firstLog.latitude!!, firstLog.longitude!!),
                11f
            )
        } else {
            // Default center at Brazil
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

        // Top Bar with controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // View Mode Toggle and City Filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
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
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Cidades")
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

            // Selected City Display
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

        // Access count info at bottom
        if (logsWithLocation.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.9f)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Total de Acessos: ${logsWithLocation.size}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Cidades: ${uiState.cityStatistics.size}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CityListViewContent(
    uiState: AccessLogMapUiState,
    onEvent: (AccessLogMapEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
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
                    text = "Acessos por Cidade",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total: ${uiState.cityStatistics.size} cidades",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }

            IconButton(
                onClick = { onEvent(AccessLogMapEvent.OnToggleViewMode) }
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Map view",
                    tint = Color.White
                )
            }
        }

        // City List
        if (uiState.cityStatistics.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhuma cidade encontrada")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.cityStatistics) { cityStat ->
                    CityStatisticCard(
                        cityStat = cityStat,
                        isSelected = cityStat.city == uiState.selectedCity,
                        onSelect = { onEvent(AccessLogMapEvent.OnSelectCity(cityStat.city)) }
                    )
                }
            }
        }
    }
}

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
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
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
                        tint = MaterialTheme.colorScheme.primary
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

            // Access Count Badge
            Card(
                modifier = Modifier.padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "${cityStat.accessCount}",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    modifier = Modifier.padding(8.dp),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

