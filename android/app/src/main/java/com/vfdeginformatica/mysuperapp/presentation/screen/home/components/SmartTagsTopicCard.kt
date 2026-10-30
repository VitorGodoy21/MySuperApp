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
    showQrCode: Boolean = true,
    showNfc: Boolean = true,
    onOpenQrCodes: () -> Unit,
    onOpenNfc: () -> Unit,
    onRetry: () -> Unit,
) {
    val colors = HomeColors.smartTags()
    HomeTopicCard(
        title = "Smart Tags",
        subtitle = when {
            showQrCode && showNfc -> "QR Codes e etiquetas NFC"
            showQrCode -> "QR Codes"
            else -> "Etiquetas NFC"
        },
        icon = Icons.Default.Contactless,
        colors = colors,
        onHeaderClick = if (showQrCode) onOpenQrCodes else onOpenNfc,
    ) {
        if (showQrCode) {
            state.qrCount?.let { count ->
                HomeMetric(
                    label = "QR Codes cadastrados",
                    value = if (count == 1) "1 QR Code" else "$count QR Codes",
                )
            }
            HomeSectionStatus(isLoading = state.isLoading, error = state.error, onRetry = onRetry)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showQrCode) HomeShortcutChip("QR Code", Icons.Default.QrCode, colors, onOpenQrCodes)
            if (showNfc) HomeShortcutChip("NFC", Icons.Default.Nfc, colors, onOpenNfc)
        }
    }
}
