package com.vfdeginformatica.mysuperapp.data.remote.repository

import com.vfdeginformatica.mysuperapp.data.mapper.groupByCityStatistics
import com.vfdeginformatica.mysuperapp.data.mapper.toAccessLog
import com.vfdeginformatica.mysuperapp.data.remote.datasource.QrCodeAccessLogDao
import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeAccessLogDto
import com.vfdeginformatica.mysuperapp.domain.model.AccessLog
import com.vfdeginformatica.mysuperapp.domain.model.CityAccessStatistics
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

    override suspend fun getAccessLogsWithLocations(qrCodeId: String): List<AccessLog>? {
        val dtos = getAccessLogsByQrCode(qrCodeId) ?: return null
        return dtos.map { it.toAccessLog() }
    }

    override suspend fun getAccessLogsByCity(qrCodeId: String, city: String): List<AccessLog>? {
        val logs = getAccessLogsWithLocations(qrCodeId) ?: return null
        return logs.filter { it.city.equals(city, ignoreCase = true) }
    }

    override suspend fun getCityAccessStatistics(qrCodeId: String): List<CityAccessStatistics>? {
        val logs = getAccessLogsWithLocations(qrCodeId) ?: return null
        return logs.groupByCityStatistics()
    }
}
