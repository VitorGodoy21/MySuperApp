package com.vfdeginformatica.mysuperapp.nfc.domain.repository

import android.nfc.Tag
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcOperationResult
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcTagContent
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcWriteContent

/**
 * Abstrai a leitura, gravação e bloqueio de tags NDEF, isolando os detalhes
 * de `android.nfc.*` do restante da camada de domínio/apresentação.
 */
interface NfcTagRepository {
    suspend fun read(tag: Tag): NfcOperationResult<NfcTagContent>
    suspend fun write(tag: Tag, content: NfcWriteContent): NfcOperationResult<Unit>
    suspend fun lock(tag: Tag): NfcOperationResult<Unit>
}
