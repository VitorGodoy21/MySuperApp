package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.contract

data class QrCodeUiState(
    val isLoading: Boolean = false,
    val qrCodeData: String = "",
    val errorMessage: String = ""
)
