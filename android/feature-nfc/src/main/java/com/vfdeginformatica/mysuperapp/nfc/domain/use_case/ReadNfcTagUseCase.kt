package com.vfdeginformatica.mysuperapp.nfc.domain.use_case

import android.nfc.Tag
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcOperationResult
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcTagContent
import com.vfdeginformatica.mysuperapp.nfc.domain.repository.NfcTagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ReadNfcTagUseCase @Inject constructor(
    private val repository: NfcTagRepository
) {
    operator fun invoke(tag: Tag): Flow<Resource<NfcTagContent>> = flow {
        emit(Resource.Loading())
        when (val result = repository.read(tag)) {
            is NfcOperationResult.Success -> emit(Resource.Success(result.data))
            is NfcOperationResult.Failure -> emit(Resource.Error(result.error.message))
        }
    }
}
