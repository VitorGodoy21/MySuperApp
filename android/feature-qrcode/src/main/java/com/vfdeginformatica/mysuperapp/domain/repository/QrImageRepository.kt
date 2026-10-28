package com.vfdeginformatica.mysuperapp.domain.repository

import com.vfdeginformatica.mysuperapp.domain.model.UploadedImage

interface QrImageRepository {
    suspend fun upload(qrId: String, bytes: ByteArray): UploadedImage?
    suspend fun delete(path: String): Boolean
}
