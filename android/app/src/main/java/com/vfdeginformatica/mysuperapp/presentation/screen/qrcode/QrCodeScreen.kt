package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.contract.QrCodeEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.contract.QrCodeUiState

@Composable
fun QrCodeScreen(
    uiState: QrCodeUiState,
    onEvent: (QrCodeEvent) -> Unit,
    onViewAccessLogs: (qrCodeId: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val qrCode = uiState.qrCode
    val qrCodeBitmap = qrCode?.qrcodeBitmap
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog && qrCode != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            title = {
                Text(
                    text = "Editar URL de Redirecionamento",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = qrCode.redirectUrl,
                        onValueChange = { newUrl ->
                            onEvent(QrCodeEvent.OnRedirectUrlChanged(newUrl))
                        },
                        label = { Text("URL de Redirecionamento") },
                        placeholder = { Text("https://example.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 4,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEvent(QrCodeEvent.OnSaveQrCode)
                        showEditDialog = false
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Salvar",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Salvar Alterações")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (uiState.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (qrCode != null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // QR Code Display
            Box(
                modifier = Modifier
                    .size(256.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (qrCodeBitmap != null) {
                    Image(
                        bitmap = qrCodeBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = "QR Code Placeholder",
                        modifier = Modifier.size(128.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // QR Code ID
            Text(
                text = "ID: ${qrCode.id}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Edit URL Button
            Button(
                onClick = { showEditDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar URL",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Editar URL de Redirecionamento")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // View Access Logs Map Button
            Button(
                onClick = { onViewAccessLogs(qrCode.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "View map",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Ver Mapa de Acessos")
            }

            if (uiState.errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    } else {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "QR Code não encontrado")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun QrCodeScreenPreview() {
    MaterialTheme {
        QrCodeScreen(
            uiState = QrCodeUiState(
                qrCode = QrCode(
                    id = "qr_001",
                    redirectUrl = "https://www.example.com/qrcode/details?id=12345"
                )
            ),
            onEvent = {}
        )
    }
}
