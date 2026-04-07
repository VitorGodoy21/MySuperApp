package com.vfdeginformatica.mysuperapp.data.remote.datasource

import com.vfdeginformatica.mysuperapp.data.remote.dto.MuralCommentDto
import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeDto

interface QrCodeDao {
    suspend fun getQrCodes(): List<QrCodeDto>?

    suspend fun updateQrCode(id: String, qrCodeDto: QrCodeDto): Boolean

    suspend fun getComments(qrCodeId: String): List<MuralCommentDto>?

    suspend fun deleteComment(qrCodeId: String, commentId: String): Boolean
}
