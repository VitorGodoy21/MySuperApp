package com.vfdeginformatica.mysuperapp.domain.use_case.qrcode

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.repository.QrCodeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class DeleteMuralCommentUseCase @Inject constructor(
    private val repository: QrCodeRepository
) {
    operator fun invoke(qrCodeId: String, commentId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        val result = repository.deleteComment(qrCodeId, commentId)
        if (result) {
            emit(Resource.Success(true))
        } else {
            emit(Resource.Error("Ocorreu um erro ao excluir o comentário"))
        }
    }
}

