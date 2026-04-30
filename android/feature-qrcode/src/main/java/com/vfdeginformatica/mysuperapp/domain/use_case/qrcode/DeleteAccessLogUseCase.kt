package com.vfdeginformatica.mysuperapp.domain.use_case.qrcode

import com.vfdeginformatica.mysuperapp.domain.repository.QrCodeAccessLogRepository
import javax.inject.Inject

class DeleteAccessLogUseCase @Inject constructor(
    private val repository: QrCodeAccessLogRepository
) {
    suspend operator fun invoke(qrCodeId: String, logId: String): Boolean {
        return repository.deleteAccessLog(qrCodeId, logId)
    }
}

