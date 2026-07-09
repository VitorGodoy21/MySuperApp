package com.vfdeginformatica.mysuperapp

import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.navigation.SharedQrCodeRoutes

sealed class Screen(val route: String) {
    object SplashScreen : Screen("splash_screen")
    object HomeScreen : Screen("home_screen")
    object LoginScreen : Screen("login_screen")
    object FinancialScreen : Screen("financial_screen")
    object NewTransactionScreen : Screen("new_transaction_screen")
    object QrCodeListScreen : Screen(SharedQrCodeRoutes.QR_CODE_LIST)
    object QrCodeScreen : Screen(SharedQrCodeRoutes.QR_CODE_DETAIL) {
        const val QR_CODE_DATA = SharedQrCodeRoutes.QR_CODE_DATA
        fun createRoute(qrCode: QrCode): String = SharedQrCodeRoutes.createQrCodeDetailRoute(qrCode)
    }

    object AccessLogMapScreen : Screen(SharedQrCodeRoutes.ACCESS_LOG_MAP) {
        const val QR_CODE_ID = SharedQrCodeRoutes.QR_CODE_ID
        fun createRoute(qrCodeId: String): String = SharedQrCodeRoutes.createAccessLogMapRoute(qrCodeId)
    }

    object MuralCommentsScreen : Screen(SharedQrCodeRoutes.MURAL_COMMENTS) {
        const val QR_CODE_ID = SharedQrCodeRoutes.QR_CODE_ID
        const val QR_CODE_IDENTIFIER = SharedQrCodeRoutes.QR_CODE_IDENTIFIER
        fun createRoute(qrCodeId: String, identifier: String = ""): String =
            SharedQrCodeRoutes.createMuralCommentsRoute(qrCodeId, identifier)
    }
}
