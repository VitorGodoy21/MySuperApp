package com.vfdeginformatica.mysuperapp.domain.use_case.qrcode

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeDto
import com.vfdeginformatica.mysuperapp.domain.repository.QrCodeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetQrCodesUseCase @Inject constructor(
    private val repository: QrCodeRepository
) {
    operator fun invoke(): Flow<Resource<List<QrCodeDto>>> = flow {
        emit(Resource.Loading())
        val qrCodes = repository.getQrCodes()
        if (qrCodes != null) {
            emit(Resource.Success(qrCodes))
        } else {
            emit(Resource.Error("Ocorreu um erro ao carregar os QR Codes"))
        }
    }
}
