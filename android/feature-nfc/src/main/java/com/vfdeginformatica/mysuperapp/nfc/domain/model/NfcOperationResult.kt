package com.vfdeginformatica.mysuperapp.nfc.domain.model

/**
 * Resultado de uma operação de NFC na camada de repositório, antes de ser
 * convertido para [com.vfdeginformatica.mysuperapp.common.Resource] pelas
 * use cases.
 */
sealed class NfcOperationResult<out T> {
    data class Success<T>(val data: T) : NfcOperationResult<T>()
    data class Failure(val error: NfcOperationError) : NfcOperationResult<Nothing>()
}
