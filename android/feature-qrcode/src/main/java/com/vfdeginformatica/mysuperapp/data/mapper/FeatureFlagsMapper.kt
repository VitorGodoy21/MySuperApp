package com.vfdeginformatica.mysuperapp.data.mapper

import com.vfdeginformatica.mysuperapp.domain.model.FeatureFlags

private fun Map<*, *>?.flag(key: String): Boolean = this?.get(key) as? Boolean ?: true

fun Map<*, *>?.toFeatureFlags(): FeatureFlags = FeatureFlags(
    finance = flag("finance"),
    smartTags = flag("smartTags"),
    qrCode = flag("qrCode"),
    nfc = flag("nfc"),
    workout = flag("workout"),
    diet = flag("diet"),
)

fun FeatureFlags.toFirestoreMap(): Map<String, Boolean> = mapOf(
    "finance" to finance,
    "smartTags" to smartTags,
    "qrCode" to qrCode,
    "nfc" to nfc,
    "workout" to workout,
    "diet" to diet,
)
