package com.vfdeginformatica.mysuperapp.domain.use_case.qrcode

import com.vfdeginformatica.mysuperapp.domain.repository.QrCodeAccessLogRepository
import javax.inject.Inject

class GetQrCodeAccessStatisticsUseCase @Inject constructor(
    private val repository: QrCodeAccessLogRepository
) {
    suspend operator fun invoke(qrCodeId: String): Map<String, Any>? {
        return repository.getAccessLogStatistics(qrCodeId)
    }
}

