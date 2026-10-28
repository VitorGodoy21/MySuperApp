package com.vfdeginformatica.mysuperapp.domain.use_case.qrcode

import com.vfdeginformatica.mysuperapp.domain.repository.QrImageRepository
import javax.inject.Inject

/** Best-effort removal of a Storage image; failures are swallowed (already logged by the DAO). */
class DeleteQrImageUseCase @Inject constructor(
    private val repository: QrImageRepository
) {
    suspend operator fun invoke(path: String): Boolean {
        if (path.isBlank()) return false
        return repository.delete(path)
    }
}
