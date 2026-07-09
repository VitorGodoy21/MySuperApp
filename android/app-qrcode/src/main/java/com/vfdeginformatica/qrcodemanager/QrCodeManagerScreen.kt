package com.vfdeginformatica.qrcodemanager

import com.vfdeginformatica.mysuperapp.domain.model.QrCode as DomainQrCode
import com.vfdeginformatica.mysuperapp.navigation.SharedQrCodeRoutes

/**
 * Routes for the QR Code Manager standalone app.
 * Mirrors the QR-code-relevant entries of the main app's Screen.kt.
 */
sealed class QrCodeManagerScreen(val route: String) {
    object Splash : QrCodeManagerScreen("splash")
    object Login : QrCodeManagerScreen("login")
    object QrCodeList : QrCodeManagerScreen(SharedQrCodeRoutes.QR_CODE_LIST)
    object Profile : QrCodeManagerScreen("profile")
    object Notifications : QrCodeManagerScreen("notifications")

    /** Detail / edit screen for a single QR code. */
    object QrCodeDetail : QrCodeManagerScreen(SharedQrCodeRoutes.QR_CODE_DETAIL) {
        const val QR_CODE_DATA = SharedQrCodeRoutes.QR_CODE_DATA
        fun createRoute(qrCode: DomainQrCode): String =
            SharedQrCodeRoutes.createQrCodeDetailRoute(qrCode)
    }

    object AccessLogMap : QrCodeManagerScreen(SharedQrCodeRoutes.ACCESS_LOG_MAP) {
        const val QR_CODE_ID = SharedQrCodeRoutes.QR_CODE_ID
        fun createRoute(qrCodeId: String) = SharedQrCodeRoutes.createAccessLogMapRoute(qrCodeId)
    }

    object MuralComments : QrCodeManagerScreen(SharedQrCodeRoutes.MURAL_COMMENTS) {
        const val QR_CODE_ID = SharedQrCodeRoutes.QR_CODE_ID
        const val QR_CODE_IDENTIFIER = SharedQrCodeRoutes.QR_CODE_IDENTIFIER
        fun createRoute(qrCodeId: String, identifier: String = ""): String =
            SharedQrCodeRoutes.createMuralCommentsRoute(qrCodeId, identifier)
    }
}
