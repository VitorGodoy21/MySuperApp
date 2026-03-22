package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.contract

data class QrCodeListUiState(
    val isLoading: Boolean = false,
    val items: List<String> = emptyList(),
    val errorMessage: String = ""
)
