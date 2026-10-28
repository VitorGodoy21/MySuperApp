package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.vfdeginformatica.mysuperapp.domain.model.QrCodeType
import com.vfdeginformatica.mysuperapp.presentation.components.QrImagePickerField
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.components.QrCodeListItem
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.contract.QrCodeListEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.contract.QrCodeListUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrCodeListScreen(
    uiState: QrCodeListUiState,
    onEvent: (QrCodeListEvent) -> Unit,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues,
    showCreateDialog: Boolean = false,
    onDismissCreateDialog: () -> Unit = {},
    onConfirmCreateQrCode: (QrCodeType, String, String, String, Uri?) -> Unit = { _, _, _, _, _ -> }
) {
    var typeDropdownExpanded by remember(showCreateDialog) { mutableStateOf(false) }
    var selectedType by remember(showCreateDialog) { mutableStateOf(QrCodeType.REDIRECT) }
    var identifier by remember(showCreateDialog) { mutableStateOf("") }
    var redirectUrl by remember(showCreateDialog) { mutableStateOf("") }
    var text by remember(showCreateDialog) { mutableStateOf("") }
    var imageUri by remember(showCreateDialog) { mutableStateOf<Uri?>(null) }
    val trimmedIdentifier = identifier.trim()
    val trimmedRedirectUrl = redirectUrl.trim()
    val trimmedText = text.trim()
    val isTypeValueValid = when (selectedType) {
        QrCodeType.REDIRECT -> trimmedRedirectUrl.isNotEmpty()
        QrCodeType.TEXT -> trimmedText.isNotEmpty()
        QrCodeType.MURAL -> true
        QrCodeType.IMAGE -> imageUri != null
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = onDismissCreateDialog,
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            title = { Text("Novo QR Code") },
            text = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.foundation.layout.Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = identifier,
                            onValueChange = { identifier = it },
                            label = { Text("Identificador") },
                            placeholder = { Text("Ex.: Sticker") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenuBox(
                            expanded = typeDropdownExpanded,
                            onExpandedChange = { typeDropdownExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedType.toLabel(),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Tipo de redirecionamento") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded)
                                },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = typeDropdownExpanded,
                                onDismissRequest = { typeDropdownExpanded = false }
                            ) {
                                QrCodeType.entries.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.toLabel()) },
                                        onClick = {
                                            selectedType = type
                                            typeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        when (selectedType) {
                            QrCodeType.REDIRECT -> OutlinedTextField(
                                value = redirectUrl,
                                onValueChange = { redirectUrl = it },
                                label = { Text("URL de redirecionamento") },
                                placeholder = { Text("https://...") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            QrCodeType.TEXT -> OutlinedTextField(
                                value = text,
                                onValueChange = { text = it },
                                label = { Text("Texto") },
                                placeholder = { Text("Texto exibido ao escanear") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )

                            QrCodeType.MURAL -> Unit

                            QrCodeType.IMAGE -> {
                                QrImagePickerField(
                                    model = imageUri,
                                    onImagePicked = { imageUri = it },
                                    enabled = !uiState.isCreating
                                )
                                OutlinedTextField(
                                    value = text,
                                    onValueChange = { text = it },
                                    label = { Text("Legenda (opcional)") },
                                    placeholder = { Text("Texto exibido abaixo da imagem") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirmCreateQrCode(
                            selectedType,
                            trimmedIdentifier,
                            trimmedRedirectUrl,
                            trimmedText,
                            imageUri
                        )
                    },
                    enabled = !uiState.isCreating && trimmedIdentifier.isNotEmpty() && isTypeValueValid
                ) {
                    Text("Criar")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissCreateDialog) {
                    Text("Cancelar")
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.qrCodes) { qrCode ->
                QrCodeListItem(
                    qrCode = qrCode,
                    onItemClick = {
                        onEvent(QrCodeListEvent.OnSelectQrCode(qrCode))
                    }
                )
            }
        }

        if (uiState.error.isNotBlank()) {
            Text(
                text = uiState.error,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .align(Alignment.Center)
            )
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }

        if (uiState.isCreating) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
}

private fun QrCodeType.toLabel(): String = when (this) {
    QrCodeType.REDIRECT -> "Redirect"
    QrCodeType.TEXT -> "Texto"
    QrCodeType.MURAL -> "Mural"
    QrCodeType.IMAGE -> "Imagem"
}

@Preview(showBackground = true)
@Composable
fun QrCodeListScreenPreview() {
    QrCodeListScreen(
        uiState = QrCodeListUiState(),
        onEvent = {},
        innerPadding = PaddingValues(0.dp)
    )
}
