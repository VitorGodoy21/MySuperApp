package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.contract

import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeDto

data class QrCodeListUiState(
    val isLoading: Boolean = false,
    val qrCodes: List<QrCodeDto> = emptyList(),
    val error: String = ""
)
