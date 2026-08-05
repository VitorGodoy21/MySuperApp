package com.vfdeginformatica.mysuperapp.nfc.presentation.screen.nfc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.nfc.presentation.screen.nfc.contract.NfcEvent
import com.vfdeginformatica.mysuperapp.nfc.presentation.screen.nfc.contract.NfcMode
import com.vfdeginformatica.mysuperapp.nfc.presentation.screen.nfc.contract.NfcUiState

@Composable
fun NfcScreen(
    uiState: NfcUiState,
    onEvent: (NfcEvent) -> Unit,
    innerPadding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
    ) {
        NfcHardwareStatusCard(uiState)

        Spacer(Modifier.height(16.dp))

        when (uiState.mode) {
            NfcMode.NONE -> NfcHub(uiState, onEvent)
            NfcMode.READING -> NfcReadingContent(uiState, onEvent)
            NfcMode.WRITING -> NfcWritingContent(uiState, onEvent)
        }

        if (uiState.errorMessage.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (uiState.showLockConfirmation) {
        LockConfirmationDialog(onEvent)
    }
}

@Composable
private fun NfcHardwareStatusCard(uiState: NfcUiState) {
    if (uiState.isNfcSupported && uiState.isNfcEnabled) return

    val message = if (!uiState.isNfcSupported) {
        "Este aparelho não possui suporte a NFC."
    } else {
        "O NFC está desativado. Ative-o nas configurações do aparelho para ler ou gravar tags."
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun NfcHub(uiState: NfcUiState, onEvent: (NfcEvent) -> Unit) {
    val actionsEnabled = uiState.isNfcSupported && uiState.isNfcEnabled

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = { onEvent(NfcEvent.OnStartReading) },
            enabled = actionsEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Nfc, contentDescription = null)
            Text("  Ler tag NFC")
        }

        Button(
            onClick = { onEvent(NfcEvent.OnStartWriting) },
            enabled = actionsEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Nfc, contentDescription = null)
            Text("  Gravar tag NFC")
        }

        Text(
            text = "Grave a URL de um QR Code existente em uma tag NTAG213 e, " +
                "opcionalmente, bloqueie-a contra novas alterações após a gravação.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NfcReadingContent(uiState: NfcUiState, onEvent: (NfcEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (uiState.isWaitingForTag) {
            WaitingForTagCard(text = "Aproxime a tag NFC do celular…")
        }

        uiState.lastReadContent?.let { content ->
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("URL lida", style = MaterialTheme.typography.titleSmall)
                    Text(content.url, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (content.isWritable) "Tag regravável" else "Tag bloqueada (somente leitura)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${content.usedSizeBytes} de ${content.maxSizeBytes} bytes usados",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        OutlinedButton(onClick = { onEvent(NfcEvent.OnCancel) }, modifier = Modifier.fillMaxWidth()) {
            Text("Voltar")
        }
    }
}

@Composable
private fun NfcWritingContent(uiState: NfcUiState, onEvent: (NfcEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            uiState.selectedQrCode == null -> QrCodeSelectionList(uiState, onEvent)

            uiState.writeSuccessUrl != null -> WriteSuccessContent(uiState)

            else -> WaitingForTagCard(
                text = "Aproxime a tag NFC para gravar a URL do QR Code " +
                    "\"${uiState.selectedQrCode.identifier.ifBlank { uiState.selectedQrCode.id }}\"."
            )
        }

        OutlinedButton(onClick = { onEvent(NfcEvent.OnCancel) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (uiState.writeSuccessUrl != null) "Concluir" else "Cancelar")
        }
    }
}

@Composable
private fun QrCodeSelectionList(uiState: NfcUiState, onEvent: (NfcEvent) -> Unit) {
    Text("Selecione o QR Code que será gravado na tag:", style = MaterialTheme.typography.titleSmall)

    if (uiState.isLoadingQrCodes) {
        CircularProgressIndicator(Modifier.padding(16.dp))
        return
    }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(uiState.qrCodes) { qrCode: QrCode ->
            ListItem(
                headlineContent = {
                    Text(qrCode.identifier.ifBlank { qrCode.id })
                },
                supportingContent = { Text(qrCode.staticUrl) },
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = 2.dp),
                leadingContent = { Icon(Icons.Default.Nfc, contentDescription = null) }
            )
            TextButton(
                onClick = { onEvent(NfcEvent.OnSelectQrCode(qrCode)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Gravar este QR Code")
            }
        }
    }
}

@Composable
private fun WriteSuccessContent(uiState: NfcUiState) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("Gravação concluída com sucesso!", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Text(uiState.writeSuccessUrl.orEmpty(), style = MaterialTheme.typography.bodyMedium)

            if (uiState.pendingLock) {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.isWaitingForTag) {
                        CircularProgressIndicator(Modifier.height(16.dp))
                    }
                    Text("Aproxime a mesma tag novamente para bloqueá-la…")
                }
            }
        }
    }
}

@Composable
private fun WaitingForTagCard(text: String) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator()
            Text(text)
        }
    }
}

@Composable
private fun LockConfirmationDialog(onEvent: (NfcEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onEvent(NfcEvent.OnDismissLockConfirmation) },
        icon = { Icon(Icons.Default.Lock, contentDescription = null) },
        title = { Text("Bloquear tag?") },
        text = {
            Text(
                "Bloquear a tag a torna permanentemente somente leitura. " +
                    "Essa ação é IRREVERSÍVEL e não pode ser desfeita. " +
                    "Confirme apenas se já validou o adesivo e a URL gravada."
            )
        },
        confirmButton = {
            TextButton(onClick = { onEvent(NfcEvent.OnConfirmLock) }) {
                Text("Bloquear")
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(NfcEvent.OnDismissLockConfirmation) }) {
                Text("Agora não")
            }
        }
    )
}
