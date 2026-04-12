package com.vfdeginformatica.mysuperapp.domain.use_case.qrcode

import com.vfdeginformatica.mysuperapp.domain.model.AccessLog
import com.vfdeginformatica.mysuperapp.domain.repository.QrCodeAccessLogRepository
import javax.inject.Inject

/**
 * Use case to get access logs filtered by city
 */
class GetAccessLogsByCityUseCase @Inject constructor(
    private val repository: QrCodeAccessLogRepository
) {
    suspend operator fun invoke(qrCodeId: String, city: String): List<AccessLog>? {
        return repository.getAccessLogsByCity(qrCodeId, city)
    }
}

