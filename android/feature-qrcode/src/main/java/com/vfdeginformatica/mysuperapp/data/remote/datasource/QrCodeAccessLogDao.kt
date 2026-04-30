package com.vfdeginformatica.mysuperapp.data.remote.datasource

import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeAccessLogDto

interface QrCodeAccessLogDao {
    suspend fun saveAccessLog(accessLog: QrCodeAccessLogDto): Boolean

    suspend fun getAccessLogsByQrCode(qrCodeId: String): List<QrCodeAccessLogDto>?

    suspend fun getAccessLogStatistics(qrCodeId: String): Map<String, Any>?

    suspend fun deleteAccessLog(qrCodeId: String, logId: String): Boolean
}

