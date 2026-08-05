package com.vfdeginformatica.mysuperapp.nfc.domain.use_case

import android.nfc.Tag
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcOperationResult
import com.vfdeginformatica.mysuperapp.nfc.domain.repository.NfcTagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Bloqueia (torna somente-leitura) uma tag NDEF já gravada. Ação
 * **irreversível** — a UI deve confirmar explicitamente com o usuário antes
 * de disparar esta use case.
 */
class LockNfcTagUseCase @Inject constructor(
    private val repository: NfcTagRepository
) {
    operator fun invoke(tag: Tag): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        when (val result = repository.lock(tag)) {
            is NfcOperationResult.Success -> emit(Resource.Success(result.data))
            is NfcOperationResult.Failure -> emit(Resource.Error(result.error.message))
        }
    }
}
