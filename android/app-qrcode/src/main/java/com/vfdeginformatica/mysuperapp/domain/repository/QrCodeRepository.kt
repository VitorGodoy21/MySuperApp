package com.vfdeginformatica.mysuperapp.domain.repository

import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeDto
import com.vfdeginformatica.mysuperapp.domain.model.MuralComment
import com.vfdeginformatica.mysuperapp.domain.model.QrCode

interface QrCodeRepository {
    suspend fun getQrCodes(): List<QrCodeDto>?

    suspend fun updateQrCode(id: String, qrCode: QrCode): Boolean

    suspend fun getComments(qrCodeId: String): List<MuralComment>?

    suspend fun deleteComment(qrCodeId: String, commentId: String): Boolean

    suspend fun addComment(qrCodeId: String, comment: MuralComment): Boolean
}
