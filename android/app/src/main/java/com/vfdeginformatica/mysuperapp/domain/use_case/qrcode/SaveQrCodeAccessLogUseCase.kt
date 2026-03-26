package com.vfdeginformatica.mysuperapp.domain.use_case.qrcode

import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeAccessLogDto
import com.vfdeginformatica.mysuperapp.domain.repository.QrCodeAccessLogRepository
import javax.inject.Inject

class SaveQrCodeAccessLogUseCase @Inject constructor(
    private val repository: QrCodeAccessLogRepository
) {
    suspend operator fun invoke(accessLog: QrCodeAccessLogDto): Boolean {
        return repository.saveAccessLog(accessLog)
    }
}

