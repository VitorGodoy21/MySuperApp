package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.contract

import com.vfdeginformatica.mysuperapp.domain.model.QrCode

data class QrCodeListUiState(
    val isLoading: Boolean = false,
    val qrCodes: List<QrCode> = emptyList(),
    val error: String = ""
)
