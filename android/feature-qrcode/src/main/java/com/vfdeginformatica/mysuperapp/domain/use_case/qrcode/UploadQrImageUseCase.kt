package com.vfdeginformatica.mysuperapp.domain.use_case.qrcode

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.UploadedImage
import com.vfdeginformatica.mysuperapp.domain.repository.QrImageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UploadQrImageUseCase @Inject constructor(
    private val repository: QrImageRepository
) {
    operator fun invoke(qrId: String, bytes: ByteArray): Flow<Resource<UploadedImage>> = flow {
        emit(Resource.Loading())
        val uploaded = repository.upload(qrId, bytes)
        if (uploaded != null) {
            emit(Resource.Success(uploaded))
        } else {
            emit(Resource.Error("Ocorreu um erro ao enviar a imagem"))
        }
    }
}
