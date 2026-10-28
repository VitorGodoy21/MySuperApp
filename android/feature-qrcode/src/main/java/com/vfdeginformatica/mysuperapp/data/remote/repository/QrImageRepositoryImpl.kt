package com.vfdeginformatica.mysuperapp.data.remote.repository

import com.vfdeginformatica.mysuperapp.data.remote.datasource.QrImageStorageDao
import com.vfdeginformatica.mysuperapp.domain.model.UploadedImage
import com.vfdeginformatica.mysuperapp.domain.repository.QrImageRepository

class QrImageRepositoryImpl(
    private val dao: QrImageStorageDao
) : QrImageRepository {
    override suspend fun upload(qrId: String, bytes: ByteArray): UploadedImage? = dao.upload(qrId, bytes)
    override suspend fun delete(path: String): Boolean = dao.delete(path)
}
