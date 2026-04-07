package com.vfdeginformatica.mysuperapp.domain.use_case.qrcode

import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.MuralComment
import com.vfdeginformatica.mysuperapp.domain.repository.QrCodeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class AddMuralCommentUseCase @Inject constructor(
    private val repository: QrCodeRepository
) {
    operator fun invoke(
        qrCodeId: String,
        author: String,
        message: String
    ): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        val comment = MuralComment(
            author = author,
            message = message,
            timestamp = Timestamp.now(),
            highlighted = true
        )
        val result = repository.addComment(qrCodeId, comment)
        if (result) {
            emit(Resource.Success(true))
        } else {
            emit(Resource.Error("Ocorreu um erro ao enviar o comentário"))
        }
    }
}

