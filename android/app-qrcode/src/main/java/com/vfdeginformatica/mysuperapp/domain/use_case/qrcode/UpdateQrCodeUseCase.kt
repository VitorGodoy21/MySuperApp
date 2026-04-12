package com.vfdeginformatica.mysuperapp.domain.use_case.qrcode

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.domain.repository.QrCodeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UpdateQrCodeUseCase @Inject constructor(
    private val repository: QrCodeRepository
) {
    operator fun invoke(id: String, qrCode: QrCode): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        val result = repository.updateQrCode(id, qrCode)
        if (result) {
            emit(Resource.Success(true))
        } else {
            emit(Resource.Error("Ocorreu um erro ao atualizar o QR Code"))
        }
    }
}

