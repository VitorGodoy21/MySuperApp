package com.vfdeginformatica.mysuperapp

import android.net.Uri
import com.google.gson.Gson
import com.vfdeginformatica.mysuperapp.domain.model.QrCode

sealed class Screen(val route: String) {
    object SplashScreen : Screen("splash_screen")
    object HomeScreen : Screen("home_screen")
    object LoginScreen : Screen("login_screen")
    object FinancialScreen : Screen("financial_screen")
    object NewTransactionScreen : Screen("new_transaction_screen")
    object QrCodeListScreen : Screen("qr_code_list_screen")
    object QrCodeScreen : Screen("qr_code_screen/{qrCodeData}") {
        const val QR_CODE_DATA = "qrCodeData"
        fun createRoute(qrCode: QrCode): String {
            val json = Gson().toJson(qrCode.copy(qrcodeBitmap = null))
            val encoded = Uri.encode(json)
            return "qr_code_screen/$encoded"
        }
    }

    object AccessLogMapScreen : Screen("access_log_map/{qrCodeId}") {
        const val QR_CODE_ID = "qrCodeId"
        fun createRoute(qrCodeId: String): String = "access_log_map/$qrCodeId"
    }

    object MuralCommentsScreen : Screen("mural_comments/{qrCodeId}/{qrCodeIdentifier}") {
        const val QR_CODE_ID = "qrCodeId"
        const val QR_CODE_IDENTIFIER = "qrCodeIdentifier"
        fun createRoute(qrCodeId: String, identifier: String = ""): String {
            val encodedIdentifier = Uri.encode(identifier.ifEmpty { " " })
            return "mural_comments/$qrCodeId/$encodedIdentifier"
        }
    }
}
