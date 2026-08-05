package com.vfdeginformatica.mysuperapp.nfc.domain.model

/**
 * Conteúdo lido de uma tag NFC NDEF (ex.: NTAG213 gravada pelo app).
 */
data class NfcTagContent(
    val url: String,
    val isWritable: Boolean,
    val maxSizeBytes: Int,
    val usedSizeBytes: Int
)
