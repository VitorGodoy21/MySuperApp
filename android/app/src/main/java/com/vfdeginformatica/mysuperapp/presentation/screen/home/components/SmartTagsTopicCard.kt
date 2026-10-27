package com.vfdeginformatica.mysuperapp.presentation.screen.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.presentation.screen.home.contract.SmartTagsSectionState

@Composable
fun SmartTagsTopicCard(
    state: SmartTagsSectionState,
    onOpenQrCodes: () -> Unit,
    onOpenNfc: () -> Unit,
    onRetry: () -> Unit,
) {
    val colors = HomeColors.smartTags()
    HomeTopicCard(
        title = "Smart Tags",
        subtitle = "QR Codes e etiquetas NFC",
        icon = Icons.Default.Contactless,
        colors = colors,
        onHeaderClick = onOpenQrCodes,
    ) {
        state.qrCount?.let { count ->
            HomeMetric(
                label = "QR Codes cadastrados",
                value = if (count == 1) "1 QR Code" else "$count QR Codes",
            )
        }
        HomeSectionStatus(isLoading = state.isLoading, error = state.error, onRetry = onRetry)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeShortcutChip("QR Code", Icons.Default.QrCode, colors, onOpenQrCodes)
            HomeShortcutChip("NFC", Icons.Default.Nfc, colors, onOpenNfc)
        }
    }
}
