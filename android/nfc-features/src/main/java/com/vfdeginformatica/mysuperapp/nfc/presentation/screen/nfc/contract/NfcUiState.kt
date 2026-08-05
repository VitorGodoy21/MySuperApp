package com.vfdeginformatica.mysuperapp.nfc.presentation.screen.nfc.contract

import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcTagContent

enum class NfcMode {
    NONE,
    READING,
    WRITING
}

data class NfcUiState(
    val isNfcSupported: Boolean = true,
    val isNfcEnabled: Boolean = true,
    val mode: NfcMode = NfcMode.NONE,
    val isWaitingForTag: Boolean = false,
    val pendingLock: Boolean = false,
    val isLoadingQrCodes: Boolean = false,
    val qrCodes: List<QrCode> = emptyList(),
    val selectedQrCode: QrCode? = null,
    val lastReadContent: NfcTagContent? = null,
    val writeSuccessUrl: String? = null,
    val showLockConfirmation: Boolean = false,
    val isProcessing: Boolean = false,
    val errorMessage: String = ""
)
