package com.vfdeginformatica.mysuperapp.domain.use_case.qrcode

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.repository.QrCodeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/** Number of QR Codes of the current user, without rendering bitmaps (used by the Home card). */
class CountQrCodesUseCase @Inject constructor(
    private val repository: QrCodeRepository
) {
    operator fun invoke(): Flow<Resource<Int>> = flow {
        emit(Resource.Loading())
        val qrCodes = repository.getQrCodes()
        if (qrCodes != null) {
            emit(Resource.Success(qrCodes.size))
        } else {
            emit(Resource.Error("Ocorreu um erro ao contar os QR Codes"))
        }
    }
}
