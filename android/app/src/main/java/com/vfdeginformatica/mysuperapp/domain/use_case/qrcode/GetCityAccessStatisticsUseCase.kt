package com.vfdeginformatica.mysuperapp.domain.use_case.qrcode

import com.vfdeginformatica.mysuperapp.domain.model.CityAccessStatistics
import com.vfdeginformatica.mysuperapp.domain.repository.QrCodeAccessLogRepository
import javax.inject.Inject

/**
 * Use case to get city-based statistics for QR code access logs
 */
class GetCityAccessStatisticsUseCase @Inject constructor(
    private val repository: QrCodeAccessLogRepository
) {
    suspend operator fun invoke(qrCodeId: String): List<CityAccessStatistics>? {
        return repository.getCityAccessStatistics(qrCodeId)
    }
}

