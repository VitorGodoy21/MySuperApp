package com.vfdeginformatica.mysuperapp.testutil

import android.graphics.Bitmap
import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeDto
import com.vfdeginformatica.mysuperapp.domain.model.MuralComment
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.domain.model.UploadedImage
import com.vfdeginformatica.mysuperapp.domain.model.toDto
import com.vfdeginformatica.mysuperapp.domain.repository.QrCodeRepository
import com.vfdeginformatica.mysuperapp.domain.repository.QrImageRepository
import com.vfdeginformatica.mysuperapp.domain.util.QrCodeGenerator

object NoopQrCodeGenerator : QrCodeGenerator {
    override fun generate(content: String, size: Int): Bitmap? = null
}

/** In-memory [QrCodeRepository] recording create/update calls. */
class FakeQrCodeRepository(
    var createdId: String = "new-id",
    var createSucceeds: Boolean = true,
    var updateSucceeds: Boolean = true
) : QrCodeRepository {
    val updates = mutableListOf<Pair<String, QrCode>>()
    val created = mutableListOf<QrCode>()

    override suspend fun getQrCodes(): List<QrCodeDto>? = emptyList()
    override suspend fun getQrCode(id: String): QrCodeDto? = null

    override suspend fun createQrCode(qrCode: QrCode): QrCodeDto? {
        created += qrCode
        if (!createSucceeds) return null
        return qrCode.toDto().copy(id = createdId, staticUrl = "https://baila.space/qr/?id=$createdId", userId = "u1")
    }

    override suspend fun updateQrCode(id: String, qrCode: QrCode): Boolean {
        updates += id to qrCode
        return updateSucceeds
    }

    override suspend fun getComments(qrCodeId: String): List<MuralComment>? = null
    override suspend fun deleteComment(qrCodeId: String, commentId: String): Boolean = false
    override suspend fun addComment(qrCodeId: String, comment: MuralComment): Boolean = false
}

/** In-memory [QrImageRepository] recording uploads/deletes. */
class FakeQrImageRepository(
    var uploadSucceeds: Boolean = true
) : QrImageRepository {
    val uploads = mutableListOf<Pair<String, ByteArray>>()
    val deleted = mutableListOf<String>()

    override suspend fun upload(qrId: String, bytes: ByteArray): UploadedImage? {
        uploads += qrId to bytes
        if (!uploadSucceeds) return null
        return UploadedImage(url = "https://storage/$qrId/${uploads.size}.jpg", path = "qr_images/u1/$qrId/${uploads.size}.jpg")
    }

    override suspend fun delete(path: String): Boolean {
        deleted += path
        return true
    }
}
