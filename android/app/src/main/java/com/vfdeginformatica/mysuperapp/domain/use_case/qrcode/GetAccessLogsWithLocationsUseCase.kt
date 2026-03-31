package com.vfdeginformatica.mysuperapp.domain.use_case.qrcode

import com.vfdeginformatica.mysuperapp.domain.model.AccessLog
import com.vfdeginformatica.mysuperapp.domain.repository.QrCodeAccessLogRepository
import javax.inject.Inject

/**
 * Use case to get all access logs with location information for a QR code
 */
class GetAccessLogsWithLocationsUseCase @Inject constructor(
    private val repository: QrCodeAccessLogRepository
) {
    suspend operator fun invoke(qrCodeId: String): List<AccessLog>? {
        return repository.getAccessLogsWithLocations(qrCodeId)
    }
}

