package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.domain.model.QrCodeType
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.contract.QrCodeEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.contract.QrCodeUiState
import java.io.File
import java.io.FileOutputStream

@Suppress("DEPRECATION")
private fun saveQrCodeImage(context: Context, bitmap: Bitmap, name: String): Boolean {
    return try {
        val filename = "${name.replace(" ", "_")}_${System.currentTimeMillis()}.png"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/MySuperApp"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            ) ?: return false
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        } else {
            val picturesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "MySuperApp").also { if (!it.exists()) it.mkdirs() }
            val file = File(appDir, filename)
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
        }
        true
    } catch (e: Exception) {
        false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrCodeScreen(
    uiState: QrCodeUiState,
    onEvent: (QrCodeEvent) -> Unit,
    onViewAccessLogs: (qrCodeId: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val qrCode = uiState.qrCode
    val qrCodeBitmap = qrCode?.qrcodeBitmap
    var showEditDialog by remember { mutableStateOf(false) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }

    // Permission launcher for API < 29
    val writeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val bitmap = qrCode?.qrcodeBitmap
            if (bitmap != null) {
                val name = qrCode.identifier.ifEmpty { qrCode.id }
                val success = saveQrCodeImage(context, bitmap, name)
                Toast.makeText(
                    context,
                    if (success) "QR Code salvo na galeria!" else "Erro ao salvar imagem",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(context, "Permissão negada", Toast.LENGTH_SHORT).show()
        }
    }

    // Download confirmation dialog
    if (showDownloadDialog && qrCodeBitmap != null) {
        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            title = { Text("Salvar imagem") },
            text = { Text("Deseja salvar a imagem do QR Code na galeria do dispositivo?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDownloadDialog = false
                        val name = qrCode?.identifier?.ifEmpty { qrCode.id } ?: "qrcode"
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val success = saveQrCodeImage(context, qrCodeBitmap, name)
                            Toast.makeText(
                                context,
                                if (success) "QR Code salvo na galeria!" else "Erro ao salvar imagem",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            writeLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Baixar",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Baixar Imagem")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showEditDialog && qrCode != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            title = {
                Text(
                    text = "Configurar QR Code",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column {
                    Text(
                        text = "Tipo",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = typeDropdownExpanded,
                        onExpandedChange = { typeDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = when (qrCode.type) {
                                QrCodeType.REDIRECT -> "Redirect"
                                QrCodeType.TEXT -> "Texto"
                                QrCodeType.MURAL -> "Mural"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = typeDropdownExpanded,
                            onDismissRequest = { typeDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Redirect") },
                                onClick = {
                                    onEvent(QrCodeEvent.OnTypeChanged(QrCodeType.REDIRECT))
                                    typeDropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Texto") },
                                onClick = {
                                    onEvent(QrCodeEvent.OnTypeChanged(QrCodeType.TEXT))
                                    typeDropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Mural") },
                                onClick = {
                                    onEvent(QrCodeEvent.OnTypeChanged(QrCodeType.MURAL))
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    when (qrCode.type) {
                        QrCodeType.REDIRECT -> {
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

                        QrCodeType.TEXT -> {
                            OutlinedTextField(
                                value = qrCode.text,
                                onValueChange = { newText ->
                                    onEvent(QrCodeEvent.OnTextChanged(newText))
                                },
                                label = { Text("Texto") },
                                placeholder = { Text("Digite o texto do QR Code") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = false,
                                maxLines = 6,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        QrCodeType.MURAL -> {
                            Text(
                                text = "O QR Code abrirá uma página de mural para comentários. Nenhuma URL ou texto adicional é necessário.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
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
            // QR Code Display — clicável quando o bitmap está disponível
            Box(
                modifier = Modifier
                    .size(256.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .then(
                        if (qrCodeBitmap != null)
                            Modifier.clickable { showDownloadDialog = true }
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (qrCodeBitmap != null) {
                    Image(
                        bitmap = qrCodeBitmap.asImageBitmap(),
                        contentDescription = "QR Code — toque para baixar",
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

            // Download hint
            if (qrCodeBitmap != null) {
                Text(
                    text = "Toque na imagem para baixar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Identifier
            if (qrCode.identifier.isNotEmpty()) {
                Text(
                    text = qrCode.identifier,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

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
