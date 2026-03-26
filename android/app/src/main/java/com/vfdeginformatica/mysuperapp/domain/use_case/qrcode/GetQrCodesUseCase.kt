package com.vfdeginformatica.mysuperapp.domain.use_case.qrcode

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.mapper.toModel
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.domain.repository.QrCodeRepository
import com.vfdeginformatica.mysuperapp.domain.util.QrCodeGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetQrCodesUseCase @Inject constructor(
    private val repository: QrCodeRepository,
    private val qrCodeGenerator: QrCodeGenerator
) {
    operator fun invoke(): Flow<Resource<List<QrCode>>> = flow {
        emit(Resource.Loading())
        val qrCodesDto = repository.getQrCodes()
        if (qrCodesDto != null) {
            val qrCodes = qrCodesDto.map { it.toModel(qrCodeGenerator) }
            emit(Resource.Success(qrCodes))
        } else {
            emit(Resource.Error("Ocorreu um erro ao carregar os QR Codes"))
        }
    }
}
