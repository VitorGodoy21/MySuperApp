package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.contract

import android.net.Uri
import com.vfdeginformatica.mysuperapp.domain.model.QrCode

data class QrCodeUiState(
    val isLoading: Boolean = false,
    val qrCode: QrCode? = null,
    val errorMessage: String = "",
    /** New gallery image chosen in the edit dialog, uploaded on save. */
    val pendingImageUri: Uri? = null,
    /** Storage path of the previous image, deleted after a successful save. */
    val pendingDeletePath: String = "",
    /** URL paired with [pendingDeletePath], restored if the user switches back to IMAGE before saving. */
    val pendingDeleteUrlBackup: String = ""
)
