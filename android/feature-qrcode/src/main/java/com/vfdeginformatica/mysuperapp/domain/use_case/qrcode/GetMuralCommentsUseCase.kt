package com.vfdeginformatica.mysuperapp.domain.use_case.qrcode

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.MuralComment
import com.vfdeginformatica.mysuperapp.domain.repository.QrCodeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetMuralCommentsUseCase @Inject constructor(
    private val repository: QrCodeRepository
) {
    operator fun invoke(qrCodeId: String): Flow<Resource<List<MuralComment>>> = flow {
        emit(Resource.Loading())
        val comments = repository.getComments(qrCodeId)
        if (comments != null) {
            emit(Resource.Success(comments))
        } else {
            emit(Resource.Error("Ocorreu um erro ao carregar os comentários"))
        }
    }
}

