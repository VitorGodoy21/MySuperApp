package com.vfdeginformatica.mysuperapp.domain.repository

import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeAccessLogDto
import com.vfdeginformatica.mysuperapp.domain.model.AccessLog
import com.vfdeginformatica.mysuperapp.domain.model.CityAccessStatistics

interface QrCodeAccessLogRepository {
    suspend fun saveAccessLog(accessLog: QrCodeAccessLogDto): Boolean

    suspend fun getAccessLogsByQrCode(qrCodeId: String): List<QrCodeAccessLogDto>?

    suspend fun getAccessLogStatistics(qrCodeId: String): Map<String, Any>?

    /**
     * Get access logs with location data as domain models
     */
    suspend fun getAccessLogsWithLocations(qrCodeId: String): List<AccessLog>?

    /**
     * Get access logs filtered by city
     */
    suspend fun getAccessLogsByCity(qrCodeId: String, city: String): List<AccessLog>?

    /**
     * Get city-based statistics for access logs
     */
    suspend fun getCityAccessStatistics(qrCodeId: String): List<CityAccessStatistics>?
}
