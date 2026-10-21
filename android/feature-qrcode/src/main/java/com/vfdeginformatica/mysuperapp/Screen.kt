package com.vfdeginformatica.mysuperapp

import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.navigation.SharedQrCodeRoutes

sealed class Screen(val route: String) {
    object SplashScreen : Screen("splash_screen")
    object HomeScreen : Screen("home_screen")
    object LoginScreen : Screen("login_screen")
    object FinancialScreen : Screen("financial_screen")
    object NewTransactionScreen : Screen("new_transaction_screen")
    object NewCardScreen : Screen("new_card_screen")
    object CardListScreen : Screen("card_list_screen")
    object CardDetailScreen : Screen("card_detail_screen/{cardId}") {
        const val CARD_ID = "cardId"
        fun createRoute(cardId: String): String = "card_detail_screen/$cardId"
    }
    object CardInvoiceScreen : Screen("card_invoice_screen/{cardId}/{invoiceMonth}") {
        const val CARD_ID = "cardId"
        const val INVOICE_MONTH = "invoiceMonth"
        /** [invoiceMonth] no formato yyyy-MM. */
        fun createRoute(cardId: String, invoiceMonth: String): String =
            "card_invoice_screen/$cardId/$invoiceMonth"
    }
    object CategoryListScreen : Screen("category_list_screen")
    object CategoryDetailScreen : Screen("category_detail_screen?categoryId={categoryId}") {
        const val CATEGORY_ID = "categoryId"
        // Sem categoryId a tela abre em modo "nova categoria"
        fun createRoute(categoryId: String? = null): String =
            if (categoryId.isNullOrBlank()) "category_detail_screen"
            else "category_detail_screen?categoryId=$categoryId"
    }

    // Academia (feature-workout)
    object WorkoutHubScreen : Screen("workout_hub_screen")
    object BodyMeasurementListScreen : Screen("body_measurement_list_screen")
    object BodyMeasurementDetailScreen :
        Screen("body_measurement_detail_screen?measurementId={measurementId}") {
        const val MEASUREMENT_ID = "measurementId"
        fun createRoute(measurementId: String? = null): String =
            if (measurementId.isNullOrBlank()) "body_measurement_detail_screen"
            else "body_measurement_detail_screen?measurementId=$measurementId"
    }
    object ExerciseListScreen : Screen("exercise_list_screen")
    object ExerciseDetailScreen : Screen("exercise_detail_screen?exerciseId={exerciseId}") {
        const val EXERCISE_ID = "exerciseId"
        fun createRoute(exerciseId: String? = null): String =
            if (exerciseId.isNullOrBlank()) "exercise_detail_screen"
            else "exercise_detail_screen?exerciseId=$exerciseId"
    }
    object WorkoutListScreen : Screen("workout_list_screen")
object WorkoutViewScreen : Screen("workout_view_screen/{workoutId}") {
    const val WORKOUT_ID = "workoutId"
    fun createRoute(workoutId: String): String = "workout_view_screen/$workoutId"
}
    object WorkoutDetailScreen : Screen("workout_detail_screen?workoutId={workoutId}") {
        const val WORKOUT_ID = "workoutId"
        fun createRoute(workoutId: String? = null): String =
            if (workoutId.isNullOrBlank()) "workout_detail_screen"
            else "workout_detail_screen?workoutId=$workoutId"
    }
    object QrCodeListScreen : Screen(SharedQrCodeRoutes.QR_CODE_LIST)
    object SettingsScreen : Screen("settings_screen")
    object NotificationsScreen : Screen("notifications_screen")
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
