package com.vfdeginformatica.mysuperapp.data.remote.repository

import com.vfdeginformatica.mysuperapp.data.remote.datasource.QrCodeAccessLogDao
import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeAccessLogDto
import com.vfdeginformatica.mysuperapp.domain.repository.QrCodeAccessLogRepository

class QrCodeAccessLogRepositoryImpl(
    private val accessLogDao: QrCodeAccessLogDao
) : QrCodeAccessLogRepository {

    override suspend fun saveAccessLog(accessLog: QrCodeAccessLogDto): Boolean {
        return accessLogDao.saveAccessLog(accessLog)
    }

    override suspend fun getAccessLogsByQrCode(qrCodeId: String): List<QrCodeAccessLogDto>? {
        return accessLogDao.getAccessLogsByQrCode(qrCodeId)
    }

    override suspend fun getAccessLogStatistics(qrCodeId: String): Map<String, Any>? {
        return accessLogDao.getAccessLogStatistics(qrCodeId)
    }
}

