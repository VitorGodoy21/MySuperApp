package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.components.QrCodeListItem
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.contract.QrCodeListEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.contract.QrCodeListUiState

@Composable
fun QrCodeListScreen(
    uiState: QrCodeListUiState,
    onEvent: (QrCodeListEvent) -> Unit,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues
) {
    Box(
        modifier = Modifier
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
                        onEvent(QrCodeListEvent.OnSelectQrCode(qrCode.id))
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
    }
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
