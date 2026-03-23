package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.contract.QrCodeEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.contract.QrCodeUiState

@Composable
fun QrCodeScreen(
    uiState: QrCodeUiState,
    onEvent: (QrCodeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "QrCode Detail Screen: ${uiState.qrCodeData}")
    }
}

@Preview(showBackground = true)
@Composable
fun QrCodeScreenPreview() {
    QrCodeScreen(
        uiState = QrCodeUiState(qrCodeData = "Sample Data"),
        onEvent = {}
    )
}
