package com.vfdeginformatica.mysuperapp.presentation.screen.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.ui.graphics.vector.ImageVector
import com.vfdeginformatica.mysuperapp.Screen
import com.vfdeginformatica.mysuperapp.nfc.navigation.SharedNfcRoutes

data class SmartTagsOption(
    val title: String,
    val icon: ImageVector,
    val route: String
)

/**
 * Tópico "Smart Tags" da Home: agrupa QR Code e NFC.
 * [PICKER_ROUTE] não é um destino do NavHost; é uma rota sentinela usada apenas
 * para passar pela biometria antes de exibir o seletor de opções.
 */
object SmartTagsMenu {
    const val TITLE = "Smart Tags"
    const val PICKER_ROUTE = "smart_tags_picker"

    val options: List<SmartTagsOption> = listOf(
        SmartTagsOption(
            title = "QR Code",
            icon = Icons.Default.QrCode2,
            route = Screen.QrCodeListScreen.route
        ),
        SmartTagsOption(
            title = "NFC",
            icon = Icons.Default.Nfc,
            route = SharedNfcRoutes.NFC_SCREEN
        )
    )
}
