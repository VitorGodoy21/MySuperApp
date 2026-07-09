package com.vfdeginformatica.mysuperapp.navigation

import android.net.Uri
import com.google.gson.Gson
import com.vfdeginformatica.mysuperapp.domain.model.QrCode

object SharedQrCodeRoutes {
    const val QR_CODE_LIST = "qr_code_list_screen"
    const val QR_CODE_DATA = "qrCodeData"
    const val QR_CODE_ID = "qrCodeId"
    const val QR_CODE_IDENTIFIER = "qrCodeIdentifier"
    const val QR_CODE_DETAIL = "qr_code_screen/{$QR_CODE_DATA}"
    const val ACCESS_LOG_MAP = "access_log_map/{$QR_CODE_ID}"
    const val MURAL_COMMENTS = "mural_comments/{$QR_CODE_ID}/{$QR_CODE_IDENTIFIER}"

    fun createQrCodeDetailRoute(qrCode: QrCode): String {
        val json = Gson().toJson(qrCode.copy(qrcodeBitmap = null))
        val encoded = Uri.encode(json)
        return "qr_code_screen/$encoded"
    }

    fun createAccessLogMapRoute(qrCodeId: String): String = "access_log_map/$qrCodeId"

    fun createMuralCommentsRoute(
        qrCodeId: String,
        identifier: String = ""
    ): String {
        val encodedIdentifier = Uri.encode(identifier.ifEmpty { " " })
        return "mural_comments/$qrCodeId/$encodedIdentifier"
    }
}
