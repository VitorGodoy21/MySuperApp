package com.vfdeginformatica.mysuperapp.nfc.domain.model

import android.nfc.NdefMessage
import android.nfc.NdefRecord

/**
 * Idioma usado ao codificar um NDEF Text Record. Mantido fixo para que a
 * estimativa de tamanho e a gravação usem sempre o mesmo cabeçalho.
 */
private const val NFC_TEXT_LANGUAGE_CODE = "pt"

/**
 * Conteúdo a ser gravado em uma tag NFC. Uma tag pode receber a URL de um
 * QR Code já existente (mantendo o mesmo fluxo de resolução via Firestore) ou
 * um valor de texto totalmente personalizado, escolhido livremente pelo
 * usuário e gravado como um NDEF Text Record.
 */
sealed class NfcWriteContent {
    abstract val value: String

    data class Url(override val value: String) : NfcWriteContent()
    data class CustomText(override val value: String) : NfcWriteContent()
}

/**
 * Converte o conteúdo para o [NdefRecord] equivalente: um URI Record para
 * [NfcWriteContent.Url] ou um Text Record para [NfcWriteContent.CustomText].
 */
fun NfcWriteContent.toNdefRecord(): NdefRecord = when (this) {
    is NfcWriteContent.Url -> NdefRecord.createUri(value)
    is NfcWriteContent.CustomText -> NdefRecord.createTextRecord(NFC_TEXT_LANGUAGE_CODE, value)
}

/** Mensagem NDEF final, sempre com um único registro. */
fun NfcWriteContent.toNdefMessage(): NdefMessage = NdefMessage(arrayOf(toNdefRecord()))

/**
 * Tamanho em bytes que a mensagem NDEF ocupará. Usado tanto pela gravação
 * real (comparado contra `Ndef.maxSize`) quanto pela UI, para avisar o
 * usuário antes mesmo de aproximar a tag caso o conteúdo digitado seja maior
 * do que a capacidade das tags mais comuns.
 */
val NfcWriteContent.payloadSizeBytes: Int
    get() = toNdefMessage().toByteArray().size
