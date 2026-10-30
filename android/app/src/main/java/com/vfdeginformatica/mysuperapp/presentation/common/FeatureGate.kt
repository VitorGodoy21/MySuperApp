package com.vfdeginformatica.mysuperapp.presentation.common

import com.vfdeginformatica.mysuperapp.Screen
import com.vfdeginformatica.mysuperapp.domain.model.FeatureFlags
import com.vfdeginformatica.mysuperapp.nfc.navigation.SharedNfcRoutes

/** Tópico do painel web ao qual uma rota de navegação pertence. */
enum class GatedFeature(val label: String) {
    FINANCE("Finanças"),
    QR_CODE("QR Code"),
    NFC("NFC"),
    WORKOUT("Academia"),
    DIET("Dieta"),
}

object FeatureGate {
    private val financeRoutes = listOf(
        Screen.FinancialScreen, Screen.NewTransactionScreen, Screen.NewCardScreen,
        Screen.CardListScreen, Screen.CardDetailScreen, Screen.CardInvoiceScreen,
        Screen.CategoryListScreen, Screen.CategoryDetailScreen,
    ).map { it.route }

    private val qrCodeRoutes = listOf(
        Screen.QrCodeListScreen, Screen.QrCodeScreen, Screen.AccessLogMapScreen, Screen.MuralCommentsScreen,
    ).map { it.route }

    private val dietRoutes = listOf(Screen.DietViewScreen, Screen.DietDetailScreen).map { it.route }

    private val workoutRoutes = listOf(
        Screen.WorkoutHubScreen, Screen.WorkoutHistoryScreen, Screen.WorkoutViewScreen,
        Screen.BodyMeasurementListScreen, Screen.BodyMeasurementDetailScreen,
        Screen.ExerciseListScreen, Screen.ExerciseDetailScreen,
        Screen.WorkoutListScreen, Screen.WorkoutDetailScreen,
    ).map { it.route }

    private fun baseOf(route: String) = route.substringBefore('?').substringBefore('/')

    /**
     * Tópico da [route] (padrão do destino, ex. `card_detail_screen/{cardId}`, ou rota
     * concreta) ou `null` para rotas sempre disponíveis (Home, Login, Configurações…).
     */
    fun featureOf(route: String?): GatedFeature? {
        if (route == null) return null
        val base = baseOf(route)
        fun List<String>.has() = any { baseOf(it) == base }
        return when {
            financeRoutes.has() -> GatedFeature.FINANCE
            qrCodeRoutes.has() -> GatedFeature.QR_CODE
            base == SharedNfcRoutes.NFC_SCREEN -> GatedFeature.NFC
            dietRoutes.has() -> GatedFeature.DIET
            workoutRoutes.has() -> GatedFeature.WORKOUT
            else -> null
        }
    }

    fun isEnabled(flags: FeatureFlags, feature: GatedFeature): Boolean = when (feature) {
        GatedFeature.FINANCE -> flags.finance
        GatedFeature.QR_CODE -> flags.isQrCodeEnabled
        GatedFeature.NFC -> flags.isNfcEnabled
        GatedFeature.WORKOUT -> flags.workout
        GatedFeature.DIET -> flags.isDietEnabled
    }

    /** Tópico desabilitado que bloqueia a [route], ou `null` quando a navegação é permitida. */
    fun blockedBy(flags: FeatureFlags, route: String?): GatedFeature? =
        featureOf(route)?.takeUnless { isEnabled(flags, it) }
}

fun FeatureFlags.allows(route: String?): Boolean = FeatureGate.blockedBy(this, route) == null
