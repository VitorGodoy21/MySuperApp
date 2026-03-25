package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.contract

import com.vfdeginformatica.mysuperapp.domain.model.QrCode

data class QrCodeUiState(
    val isLoading: Boolean = false,
    val qrCode: QrCode? = null,
    val errorMessage: String = ""
)
