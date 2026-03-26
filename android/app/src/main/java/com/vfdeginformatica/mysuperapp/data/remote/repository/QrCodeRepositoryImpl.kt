package com.vfdeginformatica.mysuperapp.data.remote.repository

import com.vfdeginformatica.mysuperapp.data.remote.datasource.QrCodeDao
import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeDto
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.domain.model.toDto
import com.vfdeginformatica.mysuperapp.domain.repository.QrCodeRepository

class QrCodeRepositoryImpl(
    private val qrCodeDao: QrCodeDao
) : QrCodeRepository {
    override suspend fun getQrCodes(): List<QrCodeDto>? {
        return qrCodeDao.getQrCodes()
    }

    override suspend fun updateQrCode(id: String, qrCode: QrCode): Boolean {
        val qrCodeDto = qrCode.toDto()
        return qrCodeDao.updateQrCode(id, qrCodeDto)
    }
}
