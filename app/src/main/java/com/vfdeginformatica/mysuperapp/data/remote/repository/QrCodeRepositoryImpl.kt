package com.vfdeginformatica.mysuperapp.data.remote.repository

import com.vfdeginformatica.mysuperapp.data.remote.datasource.QrCodeDao
import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeDto
import com.vfdeginformatica.mysuperapp.domain.repository.QrCodeRepository

class QrCodeRepositoryImpl(
    private val qrCodeDao: QrCodeDao
) : QrCodeRepository {
    override suspend fun getQrCodes(): List<QrCodeDto>? {
        return qrCodeDao.getQrCodes()
    }
}
