package com.vfdeginformatica.mysuperapp.data.remote.datasource

import com.vfdeginformatica.mysuperapp.domain.model.UploadedImage

interface QrImageStorageDao {
    suspend fun upload(qrId: String, bytes: ByteArray): UploadedImage?
    suspend fun delete(path: String): Boolean
}
