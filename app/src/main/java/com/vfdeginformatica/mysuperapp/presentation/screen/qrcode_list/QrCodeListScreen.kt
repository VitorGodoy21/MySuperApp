package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.contract.QrCodeListEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.contract.QrCodeListUiState

@Composable
fun QrCodeListScreen(
    uiState: QrCodeListUiState,
    onEvent: (QrCodeListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "QrCode List Screen")
    }
}

@Preview(showBackground = true)
@Composable
fun QrCodeListScreenPreview() {
    QrCodeListScreen(
        uiState = QrCodeListUiState(),
        onEvent = {}
    )
}
