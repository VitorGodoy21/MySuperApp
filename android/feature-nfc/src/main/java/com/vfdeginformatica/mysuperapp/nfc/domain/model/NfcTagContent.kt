package com.vfdeginformatica.mysuperapp.nfc.domain.model

/**
 * Tipo de registro NDEF lido de uma tag: uma URL (ex.: link de um QR Code) ou
 * um valor de texto personalizado gravado pelo usuário.
 */
enum class NfcContentType {
    URL,
    TEXT
}

/**
 * Conteúdo lido de uma tag NFC NDEF (ex.: NTAG213 gravada pelo app).
 */
data class NfcTagContent(
    val value: String,
    val contentType: NfcContentType,
    val isWritable: Boolean,
    val maxSizeBytes: Int,
    val usedSizeBytes: Int
)
