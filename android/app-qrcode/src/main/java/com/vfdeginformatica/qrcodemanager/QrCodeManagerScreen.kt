package com.vfdeginformatica.qrcodemanager

import android.net.Uri
import com.google.gson.Gson
import com.vfdeginformatica.mysuperapp.domain.model.QrCode as DomainQrCode

/**
 * Routes for the QR Code Manager standalone app.
 * Mirrors the QR-code-relevant entries of the main app's Screen.kt.
 */
sealed class QrCodeManagerScreen(val route: String) {
    object Splash : QrCodeManagerScreen("splash")
    object Login : QrCodeManagerScreen("login")
    object QrCodeList : QrCodeManagerScreen("qr_code_list_screen")
    object Profile : QrCodeManagerScreen("profile")
    object Notifications : QrCodeManagerScreen("notifications")

    /** Detail / edit screen for a single QR code. */
    object QrCodeDetail : QrCodeManagerScreen("qr_code_screen/{qrCodeData}") {
        const val QR_CODE_DATA = "qrCodeData"
        fun createRoute(qrCode: DomainQrCode): String {
            val json = Gson().toJson(qrCode.copy(qrcodeBitmap = null))
            val encoded = Uri.encode(json)
            return "qr_code_screen/$encoded"
        }
    }

    object AccessLogMap : QrCodeManagerScreen("access_log_map/{qrCodeId}") {
        const val QR_CODE_ID = "qrCodeId"
        fun createRoute(qrCodeId: String) = "access_log_map/$qrCodeId"
    }

    object MuralComments : QrCodeManagerScreen("mural_comments/{qrCodeId}/{qrCodeIdentifier}") {
        const val QR_CODE_ID = "qrCodeId"
        const val QR_CODE_IDENTIFIER = "qrCodeIdentifier"
        fun createRoute(qrCodeId: String, identifier: String = ""): String {
            val encoded = Uri.encode(identifier.ifEmpty { " " })
            return "mural_comments/$qrCodeId/$encoded"
        }
    }
}
