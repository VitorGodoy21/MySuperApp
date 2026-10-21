package com.vfdeginformatica.mysuperapp.domain.use_case.qrcode

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.mapper.toModel
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.domain.repository.QrCodeRepository
import com.vfdeginformatica.mysuperapp.domain.util.QrCodeGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetQrCodeByIdUseCase @Inject constructor(
    private val repository: QrCodeRepository,
    private val qrCodeGenerator: QrCodeGenerator
) {
    operator fun invoke(id: String): Flow<Resource<QrCode>> = flow {
        emit(Resource.Loading())
        val dto = repository.getQrCode(id)
        if (dto != null) {
            emit(Resource.Success(dto.toModel(qrCodeGenerator)))
        } else {
            emit(Resource.Error("QR Code não encontrado"))
        }
    }
}
