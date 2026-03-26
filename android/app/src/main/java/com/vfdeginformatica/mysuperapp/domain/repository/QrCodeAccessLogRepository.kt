package com.vfdeginformatica.mysuperapp.domain.repository

import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeAccessLogDto

interface QrCodeAccessLogRepository {
    suspend fun saveAccessLog(accessLog: QrCodeAccessLogDto): Boolean

    suspend fun getAccessLogsByQrCode(qrCodeId: String): List<QrCodeAccessLogDto>?

    suspend fun getAccessLogStatistics(qrCodeId: String): Map<String, Any>?
}

