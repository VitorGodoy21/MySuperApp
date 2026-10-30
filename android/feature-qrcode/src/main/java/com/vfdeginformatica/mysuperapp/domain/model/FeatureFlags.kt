package com.vfdeginformatica.mysuperapp.domain.model

/**
 * Funções do app habilitadas pelo usuário no painel web (`users/{uid}.featureFlags`).
 * Toda flag ausente é tratada como habilitada.
 *
 * Subitens só valem quando o tópico pai está habilitado: use [isQrCodeEnabled],
 * [isNfcEnabled] e [isDietEnabled] em vez dos campos brutos.
 */
data class FeatureFlags(
    val finance: Boolean = true,
    val smartTags: Boolean = true,
    val qrCode: Boolean = true,
    val nfc: Boolean = true,
    val workout: Boolean = true,
    val diet: Boolean = true,
) {
    val isQrCodeEnabled: Boolean get() = smartTags && qrCode
    val isNfcEnabled: Boolean get() = smartTags && nfc
    val isSmartTagsEnabled: Boolean get() = smartTags && (qrCode || nfc)
    val isDietEnabled: Boolean get() = workout && diet
    val hasAnyEnabled: Boolean get() = finance || isSmartTagsEnabled || workout
}
